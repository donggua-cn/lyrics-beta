package com.applemusic.android.mp4

import com.applemusic.android.util.Logx

/**
 * 写文件前的 MP4 归一化（对应鸿蒙版 `Mp4Cleaner.ets`）。
 *
 * ⚠️ 背景（原项目真机实测 + 本项目移植实测）：
 *
 * 1) `pssh`：Apple 下发的部分曲目在 `moov` 里带一个 pssh（Widevine 的 DRM 系统头）。
 *    音频解密成明文后如果留着它，部分播放器会去等一个永远不会到来的授权，
 *    表现为 prepare 永不完成、点了歌一直没反应。删除它只改变 moov 的长度。
 *
 * 2) `sinf` / `enca`（**移植到安卓后必须补的一步**）：
 *    原项目注释说「不要顺手剥离 senc/saiz/sinf，否则 native 播放器会崩」——
 *    那对 HarmonyOS 的 AVPlayer 成立。但安卓的 MediaCodec 不能容忍 stsd 采样条目里
 *    残留的 CENC 加密标识：只要还有 `enca` + `sinf`，它就会要求用**安全缓冲区**
 *    送数据：
 *
 *        java.lang.IllegalArgumentException:
 *            queuing secure buffer without mCrypto or mDescrambler!
 *            at android.media.MediaCodec.queueSecureInputBuffer
 *
 *    结果是「解密明明成功、点播放瞬间失败」。所以这里必须删掉采样条目里的 `sinf`、
 *    把 `enca` 改回 `mp4a`。
 *
 * 3) `moof`/`traf` 里的 `senc`/`saiz`（per-sample 加密描述）**保持不动** ——
 *    删它们会破坏 moof 内部偏移。
 *
 * 所有修改都是**等长或缩短**，并同步更新父 box 的长度字段，不影响 `moof` 的
 * `trun.data_offset`（它是相对 moof 起点算的）。
 */
object Mp4Cleaner {

    private const val TAG = "Mp4Cleaner"

    /** 可以安全递归进去重建的容器 box 类型。 */
    private val CONTAINERS = setOf(
        "moov", "trak", "mdia", "minf", "stbl", "dinf", "udta", "mvex", "edts",
        "sinf", "schi", "meta", "ilst"
    )

    /** 需要从 moov 里剔除的 DRM 相关 box。 */
    private val DROP = setOf("pssh")

    /**
     * 去掉解密后残留的 DRM 标记（**关键步骤，缺了安卓上就没法播**）。
     *
     *   1. 删掉 `moov` 子树里的 `pssh`；
     *   2. 删掉 `stsd` 采样条目里的 `sinf`，并把条目类型 `enca`/`enct` 改回原始格式；
     *   3. 删掉 `moof`/`traf` 里的 `senc`/`saiz`/`saio`/`sdtp`/`sgpd`/`sbgp`
     *      —— 音频已经是明文，再留着这些 CENC 描述会让解复用器按「加密样本」算位置，
     *      而 `stsd` 又声明是明文 `mp4a`，两边矛盾时 media3 会算出越界位置
     *      （真机表现为 `FileDataSourceException: EOF /
     *      ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE`）。
     */
    fun stripDrmHeaders(data: ByteArray): ByteArray {
        val noPssh = stripPssh(data)
        val noSinf = stripSampleEntryEncryption(noPssh)
        val fixedTrex = normalizeTrex(noSinf)
        val noCenc = stripFragmentEncryption(fixedTrex)
        return noCenc
    }

    /**
     * 修正 `mvex`/`trex` 里不合规范的默认值。
     *
     * 真机拉下来的文件里 `trex` 是：
     *   ```
     *   @894 size=32 'trex'
     *   @902 version/flags = 0
     *   @906 track_ID = 1
     *   @910 default_sample_description_index = 0   ← 规范要求 >= 1
     *   @914 default_sample_duration = 0xFFFFB000   ← 负数
     *   ```
     * 规范（ISO/IEC 14496-12 `TrackExtendsBox`）明确要求
     * `default_sample_description_index` 必须 >= 1，它指向 `stsd` 里的第 N 个采样条目。
     * 给 0 时解复用器会去取「第 0 个」采样描述 —— 不存在，
     * 于是算不出样本格式/位置，真机表现为
     * `FileDataSourceException: EOF` / `ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE`。
     *
     * 这里把它规整成 1（`stsd` 里只有一个采样条目）；duration 若为非法值也一并给个合理默认。
     */
    fun normalizeTrex(data: ByteArray): ByteArray {
        val out = data.copyOf()
        var fixed = 0
        var searchFrom = 0
        val ascii = String(out, Charsets.ISO_8859_1)
        while (true) {
            val typePos = ascii.indexOf("trex", searchFrom)
            if (typePos < 4) break
            searchFrom = typePos + 1
            val boxStart = typePos - 4
            val size = be32(out, boxStart)
            if (size < 32 || boxStart + size > out.size) continue
            // trex: boxStart+0 size, +4 type, +8 ver/flags, +12 track_ID,
            //       +16 desc_idx, +20 duration, +24 size, +28 flags
            val descIdx = be32(out, boxStart + 16)
            if (descIdx < 1) {
                setBoxSizeAt(out, boxStart + 16, 1)
                fixed++
                Logx.i(TAG, "trex@$boxStart 的 default_sample_description_index $descIdx -> 1")
            }
            val duration = be32(out, boxStart + 20)
            if (duration <= 0) {
                // 给一个常见 AAC 帧时长（1024 采样 @48kHz ≈ 21333us 的近似值），
                // 真实时长以 trun/tfhd 为准，这里只是避免非法值。
                setBoxSizeAt(out, boxStart + 20, 1024)
                fixed++
                Logx.i(TAG, "trex@$boxStart 的 default_sample_duration $duration -> 1024")
            }
        }
        if (fixed == 0) return data
        Logx.i(TAG, "normalizeTrex: 修了 $fixed 个字段")
        return out
    }

    private fun setBoxSizeAt(d: ByteArray, off: Int, v: Int) {
        d[off] = ((v ushr 24) and 0xFF).toByte()
        d[off + 1] = ((v ushr 16) and 0xFF).toByte()
        d[off + 2] = ((v ushr 8) and 0xFF).toByte()
        d[off + 3] = (v and 0xFF).toByte()
    }

    /** 删掉 `moov` 子树里的 `pssh`。没有 pssh 时原样返回入参。 */
    fun stripPssh(data: ByteArray): ByteArray {
        if (data.size < 16) return data
        val top = Mp4.boxes(data, 0, data.size)
        var hasPssh = false
        val parts = ArrayList<ByteArray>(top.size)
        for (b in top) {
            if (b.type != "moov") {
                parts.add(data.copyOfRange(b.start, b.bodyEnd))
                continue
            }
            if (countPssh(data, b.bodyStart, b.bodyEnd) == 0) {
                parts.add(data.copyOfRange(b.start, b.bodyEnd))
                continue
            }
            hasPssh = true
            val body = rebuild(data, b.bodyStart, b.bodyEnd)
            parts.add(makeBox("moov", body))
        }
        if (!hasPssh) return data
        val out = concat(*parts.toTypedArray())
        Logx.i(TAG, "stripPssh: ${data.size} -> ${out.size}")
        return out
    }

    /**
     * 清掉 `stsd` 采样条目里的加密标识（`sinf` + `enca`/`enct` 类型）。
     *
     * 【为什么用字节直扫而不是 box 树遍历】实测在真机产出的文件上，
     * `stsd` / 采样条目里混着 `esds`（其内容本身又是嵌套结构），box 树遍历
     * 在这条路径上会静默返回空列表 —— 清理就悄悄失效了，现象是「明明解密成功
     * 但一播就报 queuing secure buffer without mCrypto」。改成直接按
     * **box 长度字段自洽性**扫描字节流，逻辑独立、可在真实文件上复现验证。
     */
    /**
     * 清掉 `stsd` 采样条目里的加密标识（`sinf` + `enca`/`enct` 类型）。
     *
     * 【为什么用字节直扫而不是 box 树遍历】实测在真机产出的文件上，
     * `stsd` / 采样条目里混着 `esds`（其内容本身又是嵌套结构），box 树遍历
     * 在这条路径上会静默返回空列表 —— 清理就悄悄失效了，现象是「明明解密成功
     * 但一播就报 queuing secure buffer without mCrypto」。所以这里直接扫描字节流，
     * 只依赖「box 长度字段自洽」这一个不变量。
     *
     * 【偏移约定】`sinf` 类型字段出现在 pos 时：长度字段在 pos-4，第一个子 box
     * （frma/schm）的**类型**在 pos+8。真机实例：670=长度0x50，674="sinf"，682="frma"。
     */
    fun stripSampleEntryEncryption(data: ByteArray): ByteArray {
        if (data.size < 16) return data
        val ascii = String(data, Charsets.ISO_8859_1)

        // 找出所有 sinf 的「类型字段」位置（长度字段在它前面 4 字节）
        val sinfPositions = ArrayList<Int>(4)
        var searchFrom = 0
        while (true) {
            val p = ascii.indexOf("sinf", searchFrom)
            if (p < 0) break
            sinfPositions.add(p)
            searchFrom = p + 1
        }
        if (sinfPositions.isEmpty()) {
            Logx.w(TAG, "stripSampleEntryEncryption: 没有 sinf（文件本来就干净）")
            return data
        }

        // 记录所有祖先 box 的**长度字段偏移**，稍后统一减去删掉的字节数。
        // 【关键】只改采样条目自己的长度是不够的 —— `stsd`/`stbl`/`minf`/`mdia`/`trak`/`moov`
        // 每一层都写着长度，少改了就会让 moov 多吃掉后面 moof 的头部，
        // ExoPlayer 会直接报 `ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE`。
        val ancestors = collectSizeFieldOffsets(data)

        val chunks = ArrayList<ByteArray>(8)
        var copiedUpTo = 0
        var handled = 0
        var removedTotal = 0

        for (pos in sinfPositions) {
            if (pos < 4) continue
            val sinfSize = be32(data, pos - 4)
            if (sinfSize < 16) continue
            val sinfStart = pos - 4
            val sinfEnd = sinfStart + sinfSize
            if (sinfEnd > data.size) continue
            // 第一个子 box 必须是 frma / schm，确认这是真的 DRM 标识
            if (pos + 12 > data.size) continue
            val firstChild = ascii.substring(pos + 8, pos + 12)
            if (firstChild != "frma" && firstChild != "schm") continue

            val h = findSampleEntryStart(ascii, sinfStart)
            if (h < 0) {
                Logx.w(TAG, "sinf@$pos 找不到对应的 enca 条目头，跳过")
                continue
            }
            val eSize = be32(data, h)
            val eEnd = h + eSize
            if (eEnd > data.size) continue

            // 采样条目里 frma 的内容 4 字节 = 原始格式（通常是 mp4a）
            val frmaPos = ascii.indexOf("frma", pos)
            if (frmaPos < 0 || frmaPos + 8 > data.size) continue
            val orig = ascii.substring(frmaPos + 4, frmaPos + 8)
            if (orig.length != 4) continue

            chunks.add(data.copyOfRange(copiedUpTo, h))
            val entry = data.copyOfRange(h, eEnd)
            val rel = sinfStart - h
            val sinfEndInEntry = rel + sinfSize
            val tailLen = maxOf(0, entry.size - sinfEndInEntry)
            val out = ByteArray(entry.size - sinfSize)
            if (tailLen > 0) {
                System.arraycopy(entry, entry.size - tailLen, out, out.size - tailLen, tailLen)
            }
            if (rel > 0) {
                System.arraycopy(entry, 0, out, 0, minOf(rel, out.size))
            }
            for (k in 0 until 4) out[4 + k] = orig[k].code.toByte()
            setBoxSize(out, out.size)
            chunks.add(out)
            copiedUpTo = eEnd
            handled++
            removedTotal += sinfSize
            Logx.i(TAG, "采样条目 enca@$h -> $orig，删 sinf(${sinfSize}B)：$eSize -> ${out.size}")
        }

        if (handled == 0) {
            Logx.w(TAG, "stripSampleEntryEncryption: 认到了 sinf 但没能清理，文件原样返回")
            return data
        }
        chunks.add(data.copyOfRange(copiedUpTo, data.size))
        val out = concat(*chunks.toTypedArray())

        // 沿**祖先链**把每一层的长度都减掉。
        // 【易错点，真机踩过】必须只减 stsd 及其父容器（moov→trak→mdia→minf→stbl→stsd）
        // 的长度。早期版本收集了 moov 里**所有** box 的偏移并统统减 80，
        // 结果把 mvhd / tkhd / mdat 这些无关 box 的长度也改小了 80 ——
        // 文件顶层看着连续（总长对），但 ffprobe 报
        // `overread end of atom 'mvhd' by 80 bytes`，所有播放器都打不开。
        var patched = 0
        Logx.i(TAG, "祖先链偏移 = $ancestors（removedTotal=$removedTotal handled=$handled）")
        for (off in ancestors) {
            if (off < 0 || off + 4 > out.size) {
                Logx.w(TAG, "  祖先 @$off 越界（out=${out.size}），跳过")
                continue
            }
            val old = be32(out, off)
            if (old < 8) {
                Logx.w(TAG, "  祖先 @$off 的长度字段非法（$old），跳过")
                continue
            }
            setBoxSize(out, off, old - removedTotal)
            patched++
        }
        Logx.i(
            TAG,
            "stripSampleEntryEncryption: ${data.size} -> ${out.size}（处理 $handled 处，删 $removedTotal B，" +
                    "修补 $patched 层祖先长度）"
        )
        return out
    }

    /** traf 里属于 CENC 加密描述的 box（音频解密成明文后必须删掉）。 */
    private val FRAGMENT_DRM_BOXES = setOf("senc", "saiz", "saio", "sdtp", "sgpd", "sbgp")

    /**
     * 删掉 `moof`/`traf` 里的 CENC 加密描述 box（`senc`/`saiz`/`saio`/`sdtp`/`sgpd`/`sbgp`）。
     *
     * 【为什么必须删】音频已经解成明文、`stsd` 也声明成 `mp4a` 了，但 `traf` 里若还留着
     * `senc`（per-sample 的 IV 表）/`saiz`（辅助信息尺寸），解复用器会按「这些样本是加密的」
     * 去算数据位置，与明文声明矛盾 → 算出越界位置 → 真机报
     * `FileDataSourceException: EOF` / `ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE`。
     *
     * 只删 `traf` 的子 box，**不动 `mdat` 里的样本字节**；`trun` 保留（样本尺寸/时长还要用）。
     *
     * 【必须重算 `trun.data_offset`】`data_offset` 是**相对 `moof` 起点**的绝对量：
     *   moofStart + data_offset = 样本数据位置
     * 删掉 `senc`（Apple 的文件里每分片约 6896 字节）会让 `moof` 变短，
     * 旧实现直接原样保留 `data_offset`，它就指到了 `mdat` 之后 →
     * ExoPlayer 抛
     * ```
     * ParserException: Offset to end of mdat was negative.
     *   at FragmentedMp4Extractor.readSample(FragmentedMp4Extractor.java:1481)
     * ```
     * 真机表现是「能播几秒然后突然崩」，极难定位。
     *
     * 正确做法：`moof` 之后紧跟的就是 `mdat`，所以
     *   新 data_offset = 新 moof 长度 + mdat 头长度（8，或 16 用于 largesize）
     */
    fun stripFragmentEncryption(data: ByteArray): ByteArray {
        if (data.size < 16) return data
        val top = Mp4.boxes(data, 0, data.size)
        val parts = ArrayList<ByteArray>(top.size)
        var changed = false
        var removedCount = 0
        var fixedOffsets = 0

        // 预计算每个 moof 后面那个 mdat 的头长度
        val mdatHeader = IntArray(top.size) { 8 }
        for (i in top.indices) {
            if (top[i].type != "moof") continue
            val next = top.getOrNull(i + 1)
            if (next != null && next.type == "mdat" && next.size == 1) mdatHeader[i] = 16
        }

        for ((bi, b) in top.withIndex()) {
            if (b.type != "moof") {
                parts.add(data.copyOfRange(b.start, b.bodyEnd))
                continue
            }
            val moofKids = Mp4.boxes(data, b.bodyStart, b.bodyEnd)
            if (moofKids.isEmpty()) {
                parts.add(data.copyOfRange(b.start, b.bodyEnd))
                continue
            }
            var moofChanged = false
            val moofParts = ArrayList<ByteArray>(moofKids.size)
            for (k in moofKids) {
                if (k.type != "traf") {
                    moofParts.add(data.copyOfRange(k.start, k.bodyEnd))
                    continue
                }
                val trafKids = Mp4.boxes(data, k.bodyStart, k.bodyEnd)
                var trafChanged = false
                val trafParts = ArrayList<ByteArray>(trafKids.size)
                for (c in trafKids) {
                    if (FRAGMENT_DRM_BOXES.contains(c.type)) {
                        trafChanged = true
                        removedCount++
                        continue
                    }
                    trafParts.add(data.copyOfRange(c.start, c.bodyEnd))
                }
                if (trafChanged) {
                    moofChanged = true
                    moofParts.add(makeBox("traf", concatList(trafParts)))
                } else {
                    moofParts.add(data.copyOfRange(k.start, k.bodyEnd))
                }
            }
            if (moofChanged) {
                changed = true
                val newMoof = makeBox("moof", concatList(moofParts))
                // moof 变短了，data_offset 必须按新布局重算
                val want = newMoof.size + mdatHeader[bi]
                if (retargetTrunDataOffset(newMoof, want)) fixedOffsets++
                parts.add(newMoof)
            } else {
                parts.add(data.copyOfRange(b.start, b.bodyEnd))
            }
        }

        if (!changed) return data
        val out = concat(*parts.toTypedArray())
        Logx.i(
            TAG,
            "stripFragmentEncryption: ${data.size} -> ${out.size}" +
                    "（删了 $removedCount 个加密描述 box，修正 $fixedOffsets 处 trun.data_offset）"
        )
        return out
    }

    /**
     * 把 `moof` 里所有 `trun` 的 `data_offset` 改成 [want]（相对 moof 起点）。
     * 只处理带 `data_offset`（flags & 0x1）的 trun；其余保持原样。
     */
    private fun retargetTrunDataOffset(moof: ByteArray, want: Int): Boolean {
        var fixed = false
        var off = 8
        while (off + 8 <= moof.size) {
            val size = be32(moof, off)
            if (size < 8 || off + size > moof.size) break
            if (String(moof, off + 4, 4, Charsets.ISO_8859_1) == "traf") {
                var c = off + 8
                while (c + 8 <= off + size) {
                    val cs = be32(moof, c)
                    if (cs < 8 || c + cs > off + size) break
                    if (String(moof, c + 4, 4, Charsets.ISO_8859_1) == "trun") {
                        val flags = (be32(moof, c + 8) and 0xFFFFFF)
                        if (flags and 0x000001 != 0 && c + 16 <= moof.size) {
                            setBoxSize(moof, c + 16, want)
                            fixed = true
                        }
                    }
                    c += cs
                }
            }
            off += size
        }
        return fixed
    }

    /**
     * 收集「包含待清理 stsd 的祖先链」的长度字段偏移。
     *
     * 【为什么不是收集全部】删掉采样条目里的 sinf 之后，需要跟着缩短长度的**只有**
     * 指向该 stsd 的那条链：`moov → trak → mdia → minf → stbl → stsd`。
     * 早期版本把 moov 子树里所有 box 的偏移都收集起来、统统减 80，
     * 结果 `mvhd` / `tkhd` / `mdat` 这些无关 box 的长度也被改小 80 ——
     * 文件顶层看着连续（总长对得上），但 ffprobe 报
     * `overread end of atom 'mvhd' by 80 bytes`，所有播放器都打不开。
     * 这是本项目最隐蔽的一个 bug，必须有单元测试盯住。
     */
    private fun collectSizeFieldOffsets(data: ByteArray): List<Int> {
        val out = ArrayList<Int>(12)
        val moov = findTopLevel(data, "moov") ?: return out
        collectStsdPath(data, moov, out, 0)
        return out
    }

    /**
     * 从 [boxStart] 往下找「子树里含有 stsd 且该 stsd 的采样条目带 sinf」的路径。
     * 命中的每一层都把**长度字段偏移**加进 [out]。
     */
    private fun collectStsdPath(data: ByteArray, boxStart: Int, out: MutableList<Int>, depth: Int): Boolean {
        if (depth > 8) return false
        val size = be32(data, boxStart)
        if (size < 8 || boxStart + size > data.size) return false
        val type = String(data, boxStart + 4, 4, Charsets.ISO_8859_1)

        // 叶子：stsd —— 看它的采样条目里有没有 sinf
        if (type == "stsd") {
            if (!stsdHasSinf(data, boxStart, size)) return false
            out.add(boxStart)
            return true
        }
        // 只沿可能通向 stsd 的容器往下走
        if (type != "moov" && type != "trak" && type != "mdia" && type != "minf" && type != "stbl") {
            return false
        }
        var child = boxStart + 8
        val end = boxStart + size
        while (child + 8 <= end) {
            val cs = be32(data, child)
            if (cs < 8 || child + cs > end) break
            if (collectStsdPath(data, child, out, depth + 1)) {
                out.add(boxStart)
                return true
            }
            child += cs
        }
        return false
    }

    /** 某个 stsd 的采样条目里是否带 sinf。 */
    private fun stsdHasSinf(data: ByteArray, stsdStart: Int, stsdSize: Int): Boolean {
        val end = stsdStart + stsdSize
        // 跳过 version/flags(4) + entry_count(4)
        var p = stsdStart + 16
        var guard = 0
        while (p + 8 <= end && guard < 16) {
            val es = be32(data, p)
            if (es < 8 || p + es > end) break
            val et = String(data, p + 4, 4, Charsets.ISO_8859_1)
            if (et == "enca" || et == "enct") {
                // 【不要在条目内推算子 box 的固定头长度】AudioSampleEntry 的固定头
                // 各家实现写法不一（真机上也出现过不带标准 20 字节的情况），算错就会
                // 漏掉 sinf。这里直接在**本条目范围内**扫 4 字符标记 ——
                // 条目长度是自洽的，所以这个判据既简单又不会误判。
                val entryEnd = minOf(p + es, data.size)
                var i = p + 8
                while (i + 4 <= entryEnd) {
                    if (data[i] == 's'.code.toByte() && data[i + 1] == 'i'.code.toByte() &&
                        data[i + 2] == 'n'.code.toByte() && data[i + 3] == 'f'.code.toByte()
                    ) {
                        return true
                    }
                    i++
                }
            }
            p += es
            guard++
        }
        return false
    }

    /** 找顶层第一个指定类型的 box，返回其长度字段的偏移。 */
    private fun findTopLevel(data: ByteArray, type: String): Int? {
        var off = 0
        while (off + 8 <= data.size) {
            val size = be32(data, off)
            if (size < 8 || off + size > data.size) return null
            val t = String(data, off + 4, 4, Charsets.ISO_8859_1)
            if (t == type) return off
            off += size
        }
        return null
    }

    private fun setBoxSize(box: ByteArray, offset: Int, size: Int) {
        box[offset] = ((size ushr 24) and 0xFF).toByte()
        box[offset + 1] = ((size ushr 16) and 0xFF).toByte()
        box[offset + 2] = ((size ushr 8) and 0xFF).toByte()
        box[offset + 3] = (size and 0xFF).toByte()
    }

    /**
     * 找承载这个 `sinf` 的采样条目头。
     *
     * 【判据】直接从字节流里定位 `enca` / `enct` 类型字段（长度字段就在它前面 4 字节），
     * 要求：
     *   1. 条目起始不晚于 sinf 的长度字段；
     *   2. 条目声明的范围能**覆盖住 sinf 的长度字段**（真机实测条目末尾可能比
     *      sinf 末尾差几个字节，所以不能用「完全包含 sinf」这种严格判据）；
     *   3. 类型字段落在条目范围内。
     * 多个候选时取范围最小的那个（最贴近的父条目）。
     *
     * 【为什么不用「长度字段紧邻 sinf」】真机实测：条目是 [417,749)，
     * sinf 是 [670,750)，两者既不紧邻、也不完全包含 ——
     * 按紧邻筛会漏掉真条目，反而挑中恰好等于 sinfStart 的假候选。
     *
     * @return 条目起始偏移，找不到返回 -1
     */
    private fun findSampleEntryStart(ascii: String, sinfStart: Int): Int {
        var best = -1
        var bestSize = Int.MAX_VALUE
        for (typeTag in listOf("enca", "enct")) {
            var from = 0
            while (true) {
                val typePos = ascii.indexOf(typeTag, from)
                if (typePos < 0) break
                from = typePos + 1
                if (typePos < 4) continue
                val h = typePos - 4
                val size = be32b(ascii, h)
                if (size < 16) continue
                val end = h + size
                // 要能覆盖住 sinf 的长度字段
                if (end < sinfStart + 4) continue
                // 条目不能从 sinf 之后开始
                if (h > sinfStart) continue
                // 类型字段必须在条目内
                if (typePos + 4 > end) continue
                if (size < bestSize) {
                    best = h
                    bestSize = size
                }
            }
        }
        return best
    }

    /** 从 ISO-8859-1 视图里读大端 32 位（每个 char 就是一个字节）。 */
    private fun be32b(s: String, off: Int): Int {
        if (off < 0 || off + 4 > s.length) return -1
        return ((s[off].code and 0xFF) shl 24) or
                ((s[off + 1].code and 0xFF) shl 16) or
                ((s[off + 2].code and 0xFF) shl 8) or
                (s[off + 3].code and 0xFF)
    }

    /** 大端 32 位（越界读返回 -1）。 */
    private fun be32(d: ByteArray, off: Int): Int {
        if (off < 0 || off + 4 > d.size) return -1
        return ((d[off].toInt() and 0xFF) shl 24) or
                ((d[off + 1].toInt() and 0xFF) shl 16) or
                ((d[off + 2].toInt() and 0xFF) shl 8) or
                (d[off + 3].toInt() and 0xFF)
    }

    // ─────────────────────── 内部实现 ───────────────────────

    /** 把「整个 box」的长度写进它自己的头 4 字节。 */
    private fun setBoxSize(box: ByteArray, size: Int) = setBoxSize(box, 0, size)

    /** 统计范围内（递归）的 pssh 数量。 */
    private fun countPssh(data: ByteArray, start: Int, end: Int): Int {
        var n = 0
        for (b in Mp4.boxes(data, start, end)) {
            if (b.type == "pssh") {
                n++
            } else if (CONTAINERS.contains(b.type)) {
                n += countPssh(data, b.bodyStart, b.bodyEnd)
            }
        }
        return n
    }

    /** 递归重建范围内的 box 序列，剔除 DROP 列表里的 box。 */
    private fun rebuild(data: ByteArray, start: Int, end: Int): ByteArray {
        val parts = ArrayList<ByteArray>(8)
        for (b in Mp4.boxes(data, start, end)) {
            if (DROP.contains(b.type)) continue
            if (CONTAINERS.contains(b.type)) {
                parts.add(makeBox(b.type, rebuild(data, b.bodyStart, b.bodyEnd)))
            } else {
                // 叶子 box（含 stsd 采样条目这类带固定头的）一律原样拷贝
                parts.add(data.copyOfRange(b.start, b.bodyEnd))
            }
        }
        return concatList(parts)
    }

    /** 组装一个 box：4 字节 size + 4 字节 type + 内容。 */
    private fun makeBox(type: String, payload: ByteArray): ByteArray {
        val out = ByteArray(payload.size + 8)
        setBoxSize(out, out.size)
        for (i in 0 until 4) out[4 + i] = (type[i].code and 0xFF).toByte()
        System.arraycopy(payload, 0, out, 8, payload.size)
        return out
    }

    private fun concat(vararg parts: ByteArray): ByteArray = concatList(parts.toList())

    private fun concatList(parts: List<ByteArray>): ByteArray {
        var total = 0
        for (p in parts) total += p.size
        val out = ByteArray(total)
        var off = 0
        for (p in parts) {
            System.arraycopy(p, 0, out, off, p.size)
            off += p.size
        }
        return out
    }

    /**
     * 把 fMP4 的 `ftyp` major_brand 归一化成 `iso5`（等长字节替换，不移动任何偏移）。
     *
     * 实测 Apple 给不同曲目下发的 fMP4 容器 brand 不统一（部分是 mp42），
     * 播放器对 brand 挑剔，所以统一改写。只改 brand 这 4/8 个字节，绝不碰其他 box。
     */
    fun normalizeFtyp(data: ByteArray) {
        if (data.size < 16) return
        if (!(data[4] == 'f'.code.toByte() && data[5] == 't'.code.toByte() &&
                    data[6] == 'y'.code.toByte() && data[7] == 'p'.code.toByte())
        ) {
            return
        }
        val size = Mp4.u32(data, 0)
        if (size < 16 || size > data.size) return
        if (data[8] == 'm'.code.toByte() && data[9] == 'p'.code.toByte() &&
            data[10] == '4'.code.toByte()
        ) {
            data[8] = 'i'.code.toByte()
            data[9] = 's'.code.toByte()
            data[10] = 'o'.code.toByte()
            data[11] = '5'.code.toByte()
        }
        var off = 16
        while (off + 4 <= size) {
            if (data[off] == 'm'.code.toByte() && data[off + 1] == 'p'.code.toByte() &&
                data[off + 2] == '4'.code.toByte()
            ) {
                when (data[off + 3]) {
                    '1'.code.toByte() -> {
                        data[off] = 'i'.code.toByte()
                        data[off + 1] = 's'.code.toByte()
                        data[off + 2] = 'o'.code.toByte()
                        data[off + 3] = '5'.code.toByte()
                    }

                    '2'.code.toByte() -> {
                        data[off] = 'i'.code.toByte()
                        data[off + 1] = 's'.code.toByte()
                        data[off + 2] = 'o'.code.toByte()
                        data[off + 3] = 'm'.code.toByte()
                    }
                }
            }
            off += 4
        }
        Logx.i(TAG, "ftyp normalized to iso5")
    }
}
