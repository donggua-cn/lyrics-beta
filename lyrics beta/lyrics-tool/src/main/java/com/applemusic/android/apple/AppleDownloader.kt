package com.applemusic.android.apple

import com.applemusic.android.cenc.CencDecryptor
import com.applemusic.android.drm.KeyFetcher
import com.applemusic.android.drm.MediaDrmKeyFetcher
import com.applemusic.android.drm.WvdKeyFetcher
import com.applemusic.android.hls.HlsByteRange
import com.applemusic.android.hls.HlsMediaInfo
import com.applemusic.android.hls.HlsParser
import com.applemusic.android.mp4.Mp4Cleaner
import com.applemusic.android.mp4.Mp4Remuxer
import com.applemusic.android.util.ByteOps
import com.applemusic.android.util.Logx
import java.io.File
import java.io.RandomAccessFile

/**
 * Apple Music 下载器（单曲六步流水线）—— 对应鸿蒙版 `AppleDownloader.ets`。
 *
 *   ① getWebPlayback  取资产清单（flavor 挑一条）
 *   ② 下子清单（m3u8）→ HlsParser 解析：keyUri / init 段 / 分片列表
 *   ③ fetchKey        Widevine license 交换（**必须用 catalog id**）
 *   ④ downloadMedia   按 Range 拉整段音频字节（并发分片）
 *   ⑤ CencDecryptor   逐 sample AES-CTR 解密
 *   ⑥ Mp4Cleaner      ftyp brand 归一 + 删 moov 里的 pssh → 原子写文件
 *
 * 【坑】资料库 id（i.xxx）在 webPlayback 返回里会被换成 catalog id（数字），
 *      license 必须用后者，否则返回 `{"status":-1003}`。
 */
object AppleDownloader {

    private const val TAG = "AppleDownloader"

    enum class Stage { RESOLVING, LICENSING, DOWNLOADING, DECRYPTING, WRITING, DONE, FAILED }

    class Result {
        var ok = false
        var filePath = ""
        var message = ""
        var bytes = 0
        var keyHex = ""
    }

    /** 并发分片下载的并发度（1 = 整段单请求）。 */
    @Volatile
    var parallelChunks = 4

    /** license 交换用哪条路：先 MediaDrm，失败自动回落内置 WVD 凭证。 */
    @Volatile
    var useBuiltinCdmFirst = false

    /** 最近一次实际使用的密钥获取方式（界面显示用）。 */
    @Volatile
    var lastKeySource = ""

    /**
     * 下载并解密一首歌。
     *
     * @param songId Apple Music 歌曲 ID
     * @param isLibrary 是否资料库歌曲
     * @param outPath 目标文件绝对路径
     */
    fun downloadSong(
        api: AppleApi,
        songId: String,
        isLibrary: Boolean,
        outPath: String,
        onProgress: ((Stage, Int, String) -> Unit)? = null
    ): Result {
        val res = Result()
        val report: (Stage, Int, String) -> Unit = { s, p, n -> onProgress?.invoke(s, p, n) }
        try {
            // ① 拿播放资产
            report(Stage.RESOLVING, 2, "解析播放地址")
            val wb = api.getWebPlayback(songId, isLibrary)
            val target = pickAsset(wb.assets)
            if (target == null || target.url.isEmpty()) {
                throw RuntimeException("该歌曲没有可用的音频资源（可能需要订阅）")
            }
            // webPlayback 返回的 songId 是 **catalog id**；license 交换必须用它。
            val catalogId = if (wb.songId.isNotEmpty()) wb.songId else songId
            Logx.i(TAG, "asset flavor=${target.flavor} libraryId=$songId catalogId=$catalogId")

            // ② 子清单
            report(Stage.RESOLVING, 6, "解析音频清单")
            val subText = api.downloadText(target.url)
            val media = HlsParser.parseMedia(subText, target.url)
            if (media.keyUri.isEmpty()) throw RuntimeException("清单里没有密钥信息")
            Logx.i(
                TAG, "子清单: method=${media.keyMethod} init=${media.initUri} segs=${media.segments.size} " +
                        "kid=${com.applemusic.android.drm.Pssh.kidHex(media.keyUri)}"
            )

            // ③ 取密钥
            report(Stage.LICENSING, 12, "获取解密密钥")
            // 注意：/v1/play/assets 只认 **catalog id**，传资料库 id（i.xxx）会 400。
            // 这里用 webPlayback 换回来的 catalogId，拿不到就退回官方固定端点。
            val licenseServer = runCatching { api.getPlayAssets(catalogId).keyServerUrl }.getOrDefault("")
            val key = fetchKey(api, catalogId, media.keyUri, licenseServer)
            res.keyHex = ByteOps.toHex(key)
            Logx.i(TAG, "content key = ${res.keyHex}")

            // ④ 下载整段音频
            report(Stage.DOWNLOADING, 20, "下载音频数据")
            val mediaUrl = HlsParser.absolute(media.baseUrl, media.mediaUri)
            val totalLen = totalLength(media)
            val data = downloadMedia(api, mediaUrl, media, totalLen) { p ->
                report(Stage.DOWNLOADING, 20 + (p * 0.45f).toInt(), "下载音频数据")
            }

            // ⑤ 解密
            report(Stage.DECRYPTING, 68, "解密中")
            // 采样数据在 mdat 里、不在文件头 —— 所以按 moof 找到样本再抽样检查
            val probeBefore = probeFirstSample(data)
            val stats = CencDecryptor.decryptWholeFile(key, data)
            val probeAfter = probeFirstSample(data)
            Logx.i(TAG, "解密完成: fragments=${stats.fragments} samples=${stats.samples} bytes=${stats.bytes}")
            Logx.i(TAG, "解密前首个样本: $probeBefore")
            Logx.i(TAG, "解密后首个样本: $probeAfter")
            if (stats.samples == 0) {
                throw RuntimeException("没有解出任何音频数据（格式可能变了）")
            }

            // ⑥ 写文件：清 DRM 残留 → 重封装成普通 MP4 → ftyp 归一化。
            //
            //    三步都不能省：
            //    a) 删 moov 里的 pssh —— 留着部分播放器会去等永远不会到来的授权；
            //    b) 删 stsd 采样条目里的 sinf 并把 enca 改回 mp4a —— 音频已经解成明文，
            //       但容器里只要还留着 CENC 加密标识，**安卓的 MediaCodec 就会要求
            //       用安全缓冲区送数据**（queuing secure buffer without mCrypto or
            //       mDescrambler!），播放瞬间失败。鸿蒙的 AVPlayer 对残留 sinf 不敏感，
            //       所以原项目只做了 (a)；这一条是移植到安卓后必须补上的。
            //    c) 重封装：Apple 给的是分片 MP4，且 mvhd.duration=0、没有 mehd/sidx，
            //       导致 ExoPlayer 报 duration=TIME_UNSET、seekable=false ——
            //       现象就是「时长显示不出来、进度条一拖就跳回开头」。
            //       转成普通 MP4 后时长写进 mvhd、定位靠 stbl，两个问题一起解决。
            report(Stage.WRITING, 92, "写入文件")
            val cleaned = Mp4Cleaner.stripDrmHeaders(data)
            val remuxed = Mp4Remuxer.remux(cleaned)
            val final = remuxed ?: cleaned
            if (remuxed == null) {
                Logx.w(TAG, "重封装未生效，仍写分片 MP4（时长/拖动可能异常）")
            }
            Mp4Cleaner.normalizeFtyp(final)
            writeFileAtomic(outPath, final)

            res.ok = true
            res.filePath = outPath
            res.bytes = final.size
            res.message = "frags=${stats.fragments} samples=${stats.samples} decBytes=${stats.bytes} " +
                    "key-source=$lastKeySource remux=${remuxed != null}"
            report(Stage.DONE, 100, "完成")
            Logx.i(TAG, "已写出 $outPath (${final.size} bytes, remux=${remuxed != null})")
        } catch (e: Throwable) {
            val msg = e.message ?: e.toString()
            res.ok = false
            res.message = msg
            report(Stage.FAILED, 0, msg)
            Logx.e(TAG, "downloadSong failed: $msg", e)
            try {
                File(outPath + ".part").delete()
            } catch (_: Throwable) {
            }
        }
        return res
    }

    /**
     * 抽样检查第一个 moof 的第一个样本，用来判断「解密到底对不对」。
     *
     * AAC 的 raw_data_block 头 3 bit 是元素类型（0=单声道 SCE、1=双声道 CPE），
     * 正常音频几乎总是 0 或 1。如果解密后这里明显不是 0/1，说明密钥/IV/偏移有错。
     */
    private fun probeFirstSample(data: ByteArray): String {
        return try {
            var off = 0
            var moofStart = -1
            while (off + 8 <= data.size) {
                val size = com.applemusic.android.mp4.Mp4.u32(data, off).toInt()
                if (size < 8) break
                if (com.applemusic.android.mp4.Mp4.fourcc(data, off + 4) == "moof") {
                    moofStart = off
                    break
                }
                off += size
            }
            if (moofStart < 0) return "找不到 moof"
            val info = CencDecryptor.parseFragment(data, moofStart) ?: return "parseFragment 失败"
            val n = minOf(info.sizes.size, info.ivs.size)
            if (n == 0) return "没有样本"
            val sb = StringBuilder()
            sb.append("sizes[0..2]=")
            for (i in 0 until minOf(3, n)) sb.append(info.sizes[i]).append(",")
            sb.append(" iv0=").append(ByteOps.toHex(info.ivs[0], 0, minOf(16, info.ivs[0].size)))
            sb.append(" subs0=")
            val subs = info.subsamples.getOrNull(0)
            if (subs == null || subs.isEmpty()) {
                sb.append("无")
            } else {
                for (s in subs) sb.append("(clear=${s.clear},enc=${s.encrypted})")
            }
            sb.append(" dataOffset=").append(info.dataOffset)
            // 头 8 字节
            if (info.dataOffset + 8 <= data.size) {
                sb.append(" head=").append(ByteOps.toHex(data, info.dataOffset, 8))
                val b0 = data[info.dataOffset].toInt() and 0xFF
                sb.append(" 首元素id=").append((b0 shr 5) and 7)
            }
            sb.toString()
        } catch (t: Throwable) {
            "probe 异常: ${t.message}"
        }
    }

    /** 从 assets 里挑 256kbps 的 aac-web（28:ctrp256），退而求其次选任意 256 / 第一条。 */
    fun pickAsset(assets: List<AppleAsset>): AppleAsset? {        if (assets.isEmpty()) return null
        assets.firstOrNull { it.flavor.contains("ctrp256") }?.let { return it }
        assets.firstOrNull { it.flavor.contains("256") }?.let { return it }
        return assets[0]
    }

    /**
     * 走 Widevine 取 content key：先试系统 MediaDrm，失败再回落内置 WVD 凭证。
     *
     * 两条路的协议完全一样（challenge → POST → license → 内容密钥），
     * 差别只在「challenge 谁签、密钥谁保管」。
     */
    private fun fetchKey(api: AppleApi, catalogId: String, keyUri: String, licenseServer: String): ByteArray {
        val post: (String) -> String = { body ->
            val r = com.applemusic.android.net.Http.request(
                licenseServer.ifEmpty { com.applemusic.android.drm.DEFAULT_LICENSE_SERVER },
                "POST", api.licenseHeaders(), body.toByteArray(Charsets.UTF_8), 15000, 30000
            )
            Logx.i(TAG, "license http=${r.code} len=${r.body.size}")
            r.text
        }
        val mediaDrm: KeyFetcher = MediaDrmKeyFetcher(post)
        val wvd: KeyFetcher = WvdKeyFetcher(post)
        val order: List<KeyFetcher> =
            if (useBuiltinCdmFirst) listOf(wvd, mediaDrm) else listOf(mediaDrm, wvd)

        var lastError: Throwable? = null
        for (f in order) {
            try {
                val key = f.fetchKey(keyUri, catalogId, licenseServer)
                lastKeySource = f.name
                return key
            } catch (t: Throwable) {
                lastError = t
                Logx.w(TAG, "密钥获取失败（${f.name}）：${t.message}")
            }
        }
        throw RuntimeException("密钥获取失败：${lastError?.message ?: "未知原因"}", lastError)
    }

    /** 计算整段媒体的总长度（init + 全部分片）。 */
    fun totalLength(media: HlsMediaInfo): Int {
        var max = media.initRange.offset + media.initRange.length
        for (s in media.segments) {
            val end = s.offset + s.length
            if (end > max) max = end
        }
        return max
    }

    /**
     * 校验 HLS 分片能否无缝铺满 [initEnd, totalLen)。
     *
     * 拼装式下载只有在分片**不重叠、不空洞、且用完整个文件**时才安全；
     * 一旦清单格式有变，宁可回退整段单请求，也不能悄悄拼出一个打不开的文件。
     */
    private fun rangesCover(init: HlsByteRange, segs: List<HlsByteRange>, totalLen: Int): Boolean {
        var expect = init.offset + init.length
        for (seg in segs) {
            if (seg.length <= 0) return false
            if (seg.offset != expect) return false
            expect = seg.offset + seg.length
        }
        return expect == totalLen
    }

    /**
     * 下载媒体：默认按 HLS 分片并发 Range 拉取并拼成整段；
     * 分片信息不可用时回退成一次整段 GET。
     */
    private fun downloadMedia(
        api: AppleApi,
        url: String,
        media: HlsMediaInfo,
        totalLen: Int,
        onProgress: (Float) -> Unit
    ): ByteArray {
        val segs = media.segments
        if (parallelChunks <= 1 || segs.size <= 1 || !rangesCover(media.initRange, segs, totalLen)) {
            Logx.i(TAG, "downloadMedia: 分片不连续，回退整段单请求")
            onProgress(0.05f)
            val whole = api.downloadRange(url, -1, -1)
            onProgress(1f)
            return whole
        }
        val buf = ByteArray(totalLen)
        val counter = java.util.concurrent.atomic.AtomicInteger(0)
        val done = java.util.concurrent.atomic.AtomicInteger(0)

        // init segment（ftyp+moov）单独取一次，它不在分片列表里。
        val initLen = media.initRange.length
        if (initLen > 0) {
            val head = api.downloadRange(url, media.initRange.offset, media.initRange.offset + initLen - 1)
            val hn = minOf(head.size, initLen)
            System.arraycopy(head, 0, buf, media.initRange.offset, hn)
            done.addAndGet(hn)
        }

        val workers = minOf(parallelChunks, segs.size)
        val threads = ArrayList<Thread>(workers)
        val errors = java.util.Collections.synchronizedList(ArrayList<Throwable>())
        for (w in 0 until workers) {
            val t = Thread {
                while (true) {
                    val i = counter.getAndIncrement()
                    if (i >= segs.size) return@Thread
                    val seg = segs[i]
                    try {
                        val part = api.downloadRange(url, seg.offset, seg.offset + seg.length - 1)
                        val n = minOf(part.size, seg.length)
                        synchronized(buf) {
                            System.arraycopy(part, 0, buf, seg.offset, n)
                        }
                        val d = done.addAndGet(n)
                        onProgress(minOf(1f, d.toFloat() / totalLen))
                    } catch (t2: Throwable) {
                        errors.add(t2)
                        return@Thread
                    }
                }
            }
            t.isDaemon = true
            threads.add(t)
            t.start()
        }
        for (t in threads) t.join(180_000)
        if (errors.isNotEmpty()) {
            throw RuntimeException("分片下载失败：${errors[0].message}", errors[0])
        }
        return buf
    }

    /**
     * 原子写入：先写 `<path>.part`，落盘后再改名到目标路径。
     *
     * 直接 TRUNC 写目标路径的话，一旦进程被杀/并发写，会在缓存路径上留下一个
     * 半截的 m4a，而它会被当成有效缓存长期命中 —— 对应歌曲就再也播不出来。
     */
    private fun writeFileAtomic(path: String, data: ByteArray) {
        val tmp = File(path + ".part")
        tmp.parentFile?.mkdirs()
        RandomAccessFile(tmp, "rw").use { raf ->
            raf.setLength(0)
            raf.write(data)
            try {
                raf.fd.sync()
            } catch (_: Throwable) {
                // 部分设备不支持 fsync，忽略
            }
        }
        val target = File(path)
        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) {
            // 少数情况下 rename 失败（跨卷/占用），退回复制
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
    }
}
