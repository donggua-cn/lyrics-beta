package com.applemusic.android.mp4

import com.applemusic.android.util.Logx

/**
 * 把**分片 MP4（fMP4）重封装成普通 MP4**。
 *
 * ## 为什么必须做这一步
 *
 * Apple 的 HLS 音频是分片的，原始文件里：
 *  - `mvhd.duration = 0`（时长要客户端自己从分片累加）
 *  - `mvex` 里只有 `trex`，**没有 `mehd`**
 *  - **没有 `sidx`**（分片索引）
 *
 * 于是 ExoPlayer 得到的是：
 * ```
 * duration=C.TIME_UNSET   seekable=false
 * ```
 * 真机表现就是「进度条拖不动、一拖就跳回开头」，而且时长只能靠歌曲元数据兜底。
 * 系统 MediaPlayer 同样拿不到时长（`duration=-1`）。
 *
 * 转成普通 MP4 之后：
 *  - 时长写进 `mvhd`/`tkhd`/`mdhd`
 *  - 定位信息由 `stbl`（`stts`/`stsc`/`stsz`/`stco`）完整描述
 *  - 去掉 `mvex`，文件不再是分片的
 * 任何播放器都能正常显示时长并精确拖动。
 *
 * ## 做法
 *
 * 1. 从 `moov` 里取出每轨的 `tkhd`/`mdhd`/`hdlr`/`smhd`/`dinf`/`stsd`（原样保留）；
 * 2. 遍历所有 `moof`，按 `tfhd`/`trun` 把每个样本的
 *    「长度 / 时长 / 数据偏移」收集起来（`trex` 提供缺省值）；
 * 3. 重新生成 `stts`/`stsc`/`stsz`/`stco`，**一个 `moof` 分片作为一个 chunk**
 *    （分片内样本本来就是连续的，所以天然满足 chunk 的定义）；
 * 4. 把所有样本按轨道顺序拼成**一个 `mdat`**，据此算出每个 chunk 的偏移；
 * 5. 修好三处 duration，丢掉 `mvex`，输出 `ftyp + moov' + mdat'`。
 *
 * 任何一步对不上（结构异常）就返回 null，调用方保留原文件，不会把文件弄坏。
 */
object Mp4Remuxer {

    private const val TAG = "Mp4Remuxer"

    /** 一个样本：长度、时长、在原文件里的绝对偏移。 */
    private class Sample(val size: Int, val duration: Int, val offset: Int)

    /** 一条轨道。 */
    private class Track(val id: Int) {
        var timescale = 0
        var tkhd: ByteArray? = null
        var mdhd: ByteArray? = null
        var hdlr: ByteArray? = null
        var mediaHeader: ByteArray? = null
        var dinf: ByteArray? = null
        var stsd: ByteArray? = null

        /** stbl 里除 stts/stsc/stsz/stco/stss 之外要保留的子 box。 */
        val keepStbl = ArrayList<ByteArray>()

        val samples = ArrayList<Sample>(8192)

        /** 每个 chunk 的样本数（一个 moof 分片 = 一个 chunk）。 */
        val chunkCounts = ArrayList<Int>(64)

        var defaultDuration = 0
        var defaultSize = 0
    }

    // ─────────────────────── 对外入口 ───────────────────────

    /**
     * @return 普通 MP4 字节；结构不符合预期时返回 null（调用方应保留原文件）
     */
    fun remux(data: ByteArray): ByteArray? {
        return try {
            doRemux(data)
        } catch (t: Throwable) {
            Logx.w(TAG, "重封装失败（保留原文件）：${t.message}")
            null
        }
    }

    private fun doRemux(data: ByteArray): ByteArray? {
        if (data.size < 64) return null
        val top = Mp4.boxes(data, 0, data.size)
        val ftyp = top.firstOrNull { it.type == "ftyp" } ?: return null
        val moov = top.firstOrNull { it.type == "moov" } ?: return null
        val hasMoof = top.any { it.type == "moof" }
        if (!hasMoof) {
            // 已经是普通 MP4，不需要重封装
            Logx.i(TAG, "没有 moof，文件已经是普通 MP4，跳过重封装")
            return null
        }

        // ── ① 读出轨道骨架 ──
        val mvhd = Mp4.find(data, "mvhd", moov.bodyStart, moov.bodyEnd) ?: return null
        val movieTimescale = readTimescale(data, mvhd)
        if (movieTimescale <= 0) return null

        val tracks = ArrayList<Track>(2)
        val trexDefaults = HashMap<Int, IntArray>()   // trackId -> [duration, size]
        val trakBoxes = Mp4.boxes(data, moov.bodyStart, moov.bodyEnd).filter { it.type == "trak" }
        if (trakBoxes.isEmpty()) return null

        for (trak in trakBoxes) {
            val tkhd = Mp4.find(data, "tkhd", trak.bodyStart, trak.bodyEnd) ?: return null
            val trackId = readTrackId(data, tkhd)
            if (trackId <= 0) return null
            val mdia = Mp4.find(data, "mdia", trak.bodyStart, trak.bodyEnd) ?: return null
            val mdhd = Mp4.find(data, "mdhd", mdia.bodyStart, mdia.bodyEnd) ?: return null
            val minf = Mp4.find(data, "minf", mdia.bodyStart, mdia.bodyEnd) ?: return null
            val stbl = Mp4.find(data, "stbl", minf.bodyStart, minf.bodyEnd) ?: return null

            val t = Track(trackId)
            t.timescale = readTimescale(data, mdhd)
            if (t.timescale <= 0) return null
            t.tkhd = data.copyOfRange(tkhd.start, tkhd.bodyEnd)
            t.mdhd = data.copyOfRange(mdhd.start, mdhd.bodyEnd)
            Mp4.find(data, "hdlr", mdia.bodyStart, mdia.bodyEnd)
                ?.let { t.hdlr = data.copyOfRange(it.start, it.bodyEnd) }
            for (mh in listOf("smhd", "vmhd", "nmhd", "gmhd")) {
                Mp4.find(data, mh, minf.bodyStart, minf.bodyEnd)?.let {
                    t.mediaHeader = data.copyOfRange(it.start, it.bodyEnd)
                }
            }
            Mp4.find(data, "dinf", minf.bodyStart, minf.bodyEnd)
                ?.let { t.dinf = data.copyOfRange(it.start, it.bodyEnd) }
            Mp4.find(data, "stsd", stbl.bodyStart, stbl.bodyEnd)
                ?.let { t.stsd = data.copyOfRange(it.start, it.bodyEnd) }
            // 只保留「我们不重建」的子 box。
            // 【易错】必须把 `stsd` 也排除掉 —— 它已经单独存进 `t.stsd`，
            // 若再进 keepStbl 就会在 stbl 里出现**两个 stsd**，
            // ffprobe 直接报 `Duplicate stsd found in this track`、整个文件读不了。
            for (b in Mp4.boxes(data, stbl.bodyStart, stbl.bodyEnd)) {
                if (b.type == "stsd" || b.type in REBUILT_STBL) continue
                t.keepStbl.add(data.copyOfRange(b.start, b.bodyEnd))
            }
            tracks.add(t)
        }

        // mvex/trex：缺省样本参数
        Mp4.find(data, "mvex", moov.bodyStart, moov.bodyEnd)?.let { mvex ->
            for (trex in Mp4.boxes(data, mvex.bodyStart, mvex.bodyEnd)) {
                if (trex.type != "trex") continue
                val tid = Mp4.u32(data, trex.bodyStart + 4).toInt()
                val dur = Mp4.u32(data, trex.bodyStart + 12).toInt()
                val size = Mp4.u32(data, trex.bodyStart + 16).toInt()
                trexDefaults[tid] = intArrayOf(dur, size)
            }
        }

        // ── ② 遍历分片，收集样本 ──
        var moofCount = 0
        for ((i, b) in top.withIndex()) {
            if (b.type != "moof") continue
            val mdat = top.getOrNull(i + 1)?.takeIf { it.type == "mdat" }
            if (!collectFragment(data, b, mdat, tracks, trexDefaults)) return null
            moofCount++
        }
        if (moofCount == 0) return null
        for (t in tracks) if (t.samples.isEmpty()) return null

        // ── ③ 布局新 mdat，并算出每个 chunk 的偏移 ──
        var cursor = 0
        val chunkOffsets = HashMap<Int, IntArray>()
        for (t in tracks) {
            val offs = IntArray(t.chunkCounts.size)
            var idx = 0
            for (c in t.chunkCounts.indices) {
                offs[c] = cursor
                for (k in 0 until t.chunkCounts[c]) {
                    cursor += t.samples[idx].size
                    idx++
                }
            }
            chunkOffsets[t.id] = offs
        }
        val mdatPayload = cursor
        val ftypOut = data.copyOfRange(ftyp.start, ftyp.bodyEnd)

        // ── ④ 重建 moov ──
        //
        // 【必须两遍】`stco` 里存的是**相对整个文件的绝对偏移**，而它又取决于
        // `moov` 自身的长度（`moov` 一变长，`mdat` 的位置就跟着变）。
        // 但 `stco` 的长度只由 chunk 个数决定、与偏移值无关，所以：
        //   第一遍用基准 0 求出 `moov` 长度 → 定出 `mdat` 的绝对起点；
        //   第二遍带入绝对起点，长度不变。
        // 先把每轨的总时长算定（放在 buildMoov 外面，避免闭包里出现失败分支）
        val mediaDurations = HashMap<Int, Long>()
        val movieDurations = HashMap<Int, Long>()
        var maxMovieDuration = 0L
        for (t in tracks) {
            val md = t.samples.sumOf { it.duration.toLong() }
            if (md <= 0) return null
            val mv = md * movieTimescale / t.timescale
            mediaDurations[t.id] = md
            movieDurations[t.id] = mv
            if (mv > maxMovieDuration) maxMovieDuration = mv
        }

        fun buildMoov(mdatAbsolute: Int): ByteArray {
            val trakOut = ArrayList<ByteArray>(tracks.size)
            for (t in tracks) {
                val mediaDuration = mediaDurations[t.id]!!
                val movieDuration = movieDurations[t.id]!!
                val rel = chunkOffsets[t.id]!!
                val abs = IntArray(rel.size) { rel[it] + mdatAbsolute }

                val stbl = makeBox(
                    "stbl",
                    concat(
                        *(t.stsd?.let { arrayOf(it) } ?: emptyArray()),
                        *t.keepStbl.toTypedArray(),
                        makeStts(t.samples),
                        makeStsc(t.chunkCounts),
                        makeStsz(t.samples),
                        makeStco(abs)
                    )
                )
                val minf = makeBox(
                    "minf",
                    concat(
                        *(t.mediaHeader?.let { arrayOf(it) } ?: emptyArray()),
                        *(t.dinf?.let { arrayOf(it) } ?: emptyArray()),
                        stbl
                    )
                )
                val mdia = makeBox(
                    "mdia",
                    concat(
                        patchDuration(t.mdhd!!, mediaDuration),
                        *(t.hdlr?.let { arrayOf(it) } ?: emptyArray()),
                        minf
                    )
                )
                trakOut.add(makeBox("trak", concat(patchDuration(t.tkhd!!, movieDuration), mdia)))
            }
            val newMvhd = patchMvhdDuration(data, mvhd, maxMovieDuration.toInt())
            return makeBox(
                "moov",
                concat(newMvhd, *trakOut.toTypedArray())
                // 注意：**不放 mvex** —— 转成普通 MP4 之后就不再是分片文件了
            )
        }

        val probeMoov = buildMoov(0)
        // mdat 净荷起点 = ftyp + moov + mdat 的 8 字节头
        val mdatAbsolute = ftypOut.size + probeMoov.size + 8
        val moovOut = buildMoov(mdatAbsolute)
        check(moovOut.size == probeMoov.size) {
            "moov 长度在两遍之间发生变化：${probeMoov.size} -> ${moovOut.size}"
        }

        // ── ⑤ 输出 ftyp + moov + mdat ──
        val mdatOut = ByteArray(8 + mdatPayload)
        putU32(mdatOut, 0, mdatOut.size)
        System.arraycopy("mdat".toByteArray(Charsets.US_ASCII), 0, mdatOut, 4, 4)
        var w = 8
        for (t in tracks) {
            for (s in t.samples) {
                System.arraycopy(data, s.offset, mdatOut, w, s.size)
                w += s.size
            }
        }
        check(w == mdatOut.size) { "mdat 写入长度不符：$w != ${mdatOut.size}" }

        val out = concat(ftypOut, moovOut, mdatOut)
        Logx.i(
            TAG,
            "重封装：${data.size} -> ${out.size} 字节，" +
                    "${tracks.size} 轨、${moofCount} 分片、" +
                    "${tracks.sumOf { it.samples.size }} 样本、" +
                    "mdat 起点 $mdatAbsolute、" +
                    "时长 ${"%.2f".format(maxMovieDuration / (movieTimescale / 1000.0) / 1000.0)}s"
        )
        return out
    }

    /**
     * 这些 `stbl` 子 box 会被重新生成，所以不要原样保留。
     *
     * 【注意】**不含 `stss`** —— 同步样本表我们不重建，视频轨道靠它做关键帧定位，
     * 丢掉会让拖动定位到错误的位置。原样保留最安全。
     */
    private val REBUILT_STBL = setOf("stts", "stsc", "stsz", "stco", "co64")

    // ─────────────────────── 分片解析 ───────────────────────

    /** 解析一个 `moof`，把样本追加到对应轨道。结构异常返回 false。 */
    private fun collectFragment(
        data: ByteArray,
        moof: Box,
        mdat: Box?,
        tracks: List<Track>,
        trexDefaults: Map<Int, IntArray>
    ): Boolean {
        for (traf in Mp4.boxes(data, moof.bodyStart, moof.bodyEnd)) {
            if (traf.type != "traf") continue
            val tfhd = Mp4.find(data, "tfhd", traf.bodyStart, traf.bodyEnd) ?: return false
            val tFlags = (Mp4.u32(data, tfhd.bodyStart) and 0xFFFFFF).toInt()
            val trackId = Mp4.u32(data, tfhd.bodyStart + 4).toInt()
            val track = tracks.firstOrNull { it.id == trackId } ?: return false

            var p = tfhd.bodyStart + 8
            var baseDataOffset = -1L
            if (tFlags and 0x000001 != 0) {
                baseDataOffset = Mp4.u64(data, p)
                p += 8
            }
            if (tFlags and 0x000002 != 0) p += 4          // sample_description_index
            var defDuration = trexDefaults[trackId]?.get(0) ?: 0
            var defSize = trexDefaults[trackId]?.get(1) ?: 0
            if (tFlags and 0x000008 != 0) {
                defDuration = Mp4.u32(data, p).toInt()
                p += 4
            }
            if (tFlags and 0x000010 != 0) {
                defSize = Mp4.u32(data, p).toInt()
                p += 4
            }
            if (tFlags and 0x000020 != 0) p += 4          // default_sample_flags

            // 数据基准：`base_data_offset` 存在就用它，否则按规范相对 `moof` 起点
            // （Apple 会置 default-base-is-moof，两者等价）
            val base = if (baseDataOffset >= 0) baseDataOffset else moof.start.toLong()

            for (trun in Mp4.boxes(data, traf.bodyStart, traf.bodyEnd)) {
                if (trun.type != "trun") continue
                val rFlags = (Mp4.u32(data, trun.bodyStart) and 0xFFFFFF).toInt()
                val count = Mp4.u32(data, trun.bodyStart + 4).toInt()
                if (count <= 0) continue
                var q = trun.bodyStart + 8
                var dataOffset = Int.MIN_VALUE
                if (rFlags and 0x000001 != 0) {
                    dataOffset = Mp4.i32(data, q)
                    q += 4
                }
                if (rFlags and 0x000004 != 0) q += 4      // first_sample_flags

                val at0 = if (dataOffset != Int.MIN_VALUE) {
                    (base + dataOffset).toInt()
                } else {
                    mdat?.bodyStart ?: return false
                }
                if (at0 < 0 || at0 > data.size) return false
                var at = at0

                val startCount = track.samples.size
                for (k in 0 until count) {
                    var dur = defDuration
                    var size = defSize
                    if (rFlags and 0x000100 != 0) {
                        dur = Mp4.u32(data, q).toInt()
                        q += 4
                    }
                    if (rFlags and 0x000200 != 0) {
                        size = Mp4.u32(data, q).toInt()
                        q += 4
                    }
                    if (rFlags and 0x000400 != 0) q += 4  // sample_flags
                    if (rFlags and 0x000800 != 0) q += 4  // composition time offset
                    if (size <= 0 || at + size > data.size) return false
                    track.samples.add(Sample(size, if (dur > 0) dur else 1024, at))
                    at += size
                }
                if (track.samples.size == startCount) return false
                track.chunkCounts.add(track.samples.size - startCount)
            }
        }
        return true
    }

    // ─────────────────────── 重建 stbl 子 box ───────────────────────

    /** `stts`：时长做行程编码（相邻相同的时长合并成一条）。 */
    private fun makeStts(samples: List<Sample>): ByteArray {
        val entries = ArrayList<IntArray>(64)
        var cur = samples[0].duration
        var n = 0
        for (s in samples) {
            if (s.duration == cur) {
                n++
            } else {
                entries.add(intArrayOf(n, cur))
                cur = s.duration
                n = 1
            }
        }
        entries.add(intArrayOf(n, cur))

        val body = ByteArray(8 + entries.size * 8)
        putU32(body, 0, entries.size)
        var p = 4
        for (e in entries) {
            putU32(body, p, e[0])
            putU32(body, p + 4, e[1])
            p += 8
        }
        return makeFullBox("stts", 0, 0, body)
    }

    /** `stsc`：chunk → 样本数（相邻相同的一起编码）。 */
    private fun makeStsc(chunkCounts: List<Int>): ByteArray {
        val entries = ArrayList<IntArray>(64)
        var i = 0
        while (i < chunkCounts.size) {
            val c = chunkCounts[i]
            var j = i
            while (j < chunkCounts.size && chunkCounts[j] == c) j++
            entries.add(intArrayOf(i + 1, c, 1))   // first_chunk, samples_per_chunk, desc_index
            i = j
        }
        if (entries.isEmpty()) entries.add(intArrayOf(1, 1, 1))

        val body = ByteArray(8 + entries.size * 12)
        putU32(body, 0, entries.size)
        var p = 4
        for (e in entries) {
            putU32(body, p, e[0])
            putU32(body, p + 4, e[1])
            putU32(body, p + 8, e[2])
            p += 12
        }
        return makeFullBox("stsc", 0, 0, body)
    }

    /** `stsz`：每个样本的字节数。 */
    private fun makeStsz(samples: List<Sample>): ByteArray {
        val body = ByteArray(12 + samples.size * 4)
        putU32(body, 0, 0)                    // sample_size = 0 → 逐样本给
        putU32(body, 4, samples.size)
        var p = 8
        for (s in samples) {
            putU32(body, p, s.size)
            p += 4
        }
        return makeFullBox("stsz", 0, 0, body)
    }

    /** `stco`：每个 chunk 在新 `mdat` 里的偏移（文件 ≤ 2GB，够用）。 */
    private fun makeStco(offsets: IntArray): ByteArray {
        val body = ByteArray(8 + offsets.size * 4)
        putU32(body, 0, offsets.size)
        var p = 4
        for (o in offsets) {
            putU32(body, p, o)
            p += 4
        }
        return makeFullBox("stco", 0, 0, body)
    }

    // ─────────────────────── 时长与字段读写 ───────────────────────

    private fun readTimescale(data: ByteArray, box: Box): Int {
        val v = data[box.bodyStart].toInt() and 0xFF
        val off = if (v == 1) box.bodyStart + 20 else box.bodyStart + 12
        if (off + 4 > data.size) return -1
        return Mp4.u32(data, off).toInt()
    }

    /** `tkhd` 里的 track_ID。 */
    private fun readTrackId(data: ByteArray, tkhd: Box): Int {
        val v = data[tkhd.bodyStart].toInt() and 0xFF
        val off = if (v == 1) tkhd.bodyStart + 20 else tkhd.bodyStart + 12
        if (off + 4 > data.size) return -1
        return Mp4.u32(data, off).toInt()
    }

    /** 改 `mdhd`/`tkhd` 的 duration（版本 0 = 32 位，版本 1 = 64 位）。 */
    private fun patchDuration(box: ByteArray, duration: Long): ByteArray {
        val out = box.copyOf()
        val v = out[8].toInt() and 0xFF          // box 头 8 字节后是 version
        val body = 8
        // mdhd：duration 在 +16(v0)/+24(v1)；tkhd：+20(v0)/+28(v1)
        // 用 box 类型区分
        val type = String(out, 4, 4, Charsets.ISO_8859_1)
        val off = when (type) {
            "mdhd" -> if (v == 1) body + 24 else body + 16
            "tkhd" -> if (v == 1) body + 28 else body + 20
            else -> return out
        }
        if (off + 4 > out.size) return out
        if (v == 1) {
            putU32(out, off, (duration ushr 32).toInt())
            putU32(out, off + 4, duration.toInt())
        } else {
            putU32(out, off, duration.toInt())
        }
        return out
    }

    /** 改 `mvhd` 的 duration。 */
    private fun patchMvhdDuration(data: ByteArray, mvhd: Box, duration: Int): ByteArray {
        val out = data.copyOfRange(mvhd.start, mvhd.bodyEnd)
        val v = out[8].toInt() and 0xFF
        val body = 8
        val off = if (v == 1) body + 24 else body + 16
        if (off + 4 > out.size) return out
        if (v == 1) {
            putU32(out, off, 0)
            putU32(out, off + 4, duration)
        } else {
            putU32(out, off, duration)
        }
        return out
    }

    // ─────────────────────── box 拼装工具 ───────────────────────

    private fun makeBox(type: String, payload: ByteArray): ByteArray {
        val out = ByteArray(8 + payload.size)
        putU32(out, 0, out.size)
        System.arraycopy(type.toByteArray(Charsets.US_ASCII), 0, out, 4, 4)
        System.arraycopy(payload, 0, out, 8, payload.size)
        return out
    }

    private fun makeFullBox(type: String, version: Int, flags: Int, payload: ByteArray): ByteArray {
        val body = ByteArray(4 + payload.size)
        body[0] = version.toByte()
        body[1] = ((flags ushr 16) and 0xFF).toByte()
        body[2] = ((flags ushr 8) and 0xFF).toByte()
        body[3] = (flags and 0xFF).toByte()
        System.arraycopy(payload, 0, body, 4, payload.size)
        return makeBox(type, body)
    }

    private fun putU32(d: ByteArray, off: Int, v: Int) {
        d[off] = ((v ushr 24) and 0xFF).toByte()
        d[off + 1] = ((v ushr 16) and 0xFF).toByte()
        d[off + 2] = ((v ushr 8) and 0xFF).toByte()
        d[off + 3] = (v and 0xFF).toByte()
    }

    private fun concat(vararg parts: ByteArray): ByteArray {
        var n = 0
        for (p in parts) n += p.size
        val out = ByteArray(n)
        var off = 0
        for (p in parts) {
            System.arraycopy(p, 0, out, off, p.size)
            off += p.size
        }
        return out
    }
}
