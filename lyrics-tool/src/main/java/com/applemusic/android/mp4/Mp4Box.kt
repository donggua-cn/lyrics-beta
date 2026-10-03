package com.applemusic.android.mp4

import com.applemusic.android.util.ByteOps

/**
 * MP4（ISO BMFF）box 级工具。
 *
 * 直接对应鸿蒙版 `Mp4Box.ets`：只实现解密链路需要的部分 —— box 遍历、
 * 大端整数读取、按类型/路径查找。所有偏移都是「整段文件」内的绝对偏移。
 */
class Box {
    var type: String = ""
    var start = 0
    var size = 0
    var bodyStart = 0
    var bodyEnd = 0
}

object Mp4 {

    fun u32(d: ByteArray, off: Int): Long = ByteOps.u32(d, off)
    fun i32(d: ByteArray, off: Int): Int = ByteOps.i32(d, off)
    fun u16(d: ByteArray, off: Int): Int = ByteOps.u16(d, off)
    fun u64(d: ByteArray, off: Int): Long = ByteOps.u64(d, off)
    fun fourcc(d: ByteArray, off: Int): String = ByteOps.fourcc(d, off)

    /**
     * 遍历 [start, end) 范围内的同级 box。
     *
     * 【实现注意】循环前进量必须是「整个 box 的长度 size」（含 8/16 字节头），
     * 不是 header。写成 `off += header` 会导致每轮只跳过 8 字节，把 box 内容
     * 当成新的 box 头解析 —— 现象是「一个 box 只解析出它自己、子 box 全找不到」，
     * CENC 解密链路会因此完全失效（moof/traf/trun/senc 全部定位不到）。
     * 这个 bug 在本项目里真实出现过，靠单元测试才发现。
     */
    fun boxes(d: ByteArray, start: Int, end: Int): List<Box> {
        val out = ArrayList<Box>(8)
        var off = start
        while (true) {
            if (off + 8 > end) break
            var boxSize = u32(d, off)
            val type = fourcc(d, off + 4)
            var headerLen = 8
            if (boxSize == 1L) {
                // 64 位长度：size 字段本身变成 16 字节
                boxSize = u64(d, off + 8)
                headerLen = 16
            } else if (boxSize == 0L) {
                // size == 0 表示「一直到末尾」
                boxSize = (end - off).toLong()
            }
            if (boxSize < headerLen) break
            if (off + boxSize > end) break

            val b = Box()
            b.type = type
            b.start = off
            b.size = boxSize.toInt()
            b.bodyStart = off + headerLen
            b.bodyEnd = off + boxSize.toInt()
            out.add(b)

            off = off + boxSize.toInt()
        }
        return out
    }

    /** 在 [start, end) 找第一个指定类型的 box。 */
    fun find(d: ByteArray, type: String, start: Int, end: Int): Box? {
        for (b in boxes(d, start, end)) {
            if (b.type == type) return b
        }
        return null
    }

    fun findTop(d: ByteArray, type: String): Box? = find(d, type, 0, d.size)

    /** 按路径逐层查找，如 ["moov","trak","mdia","minf","stbl","stsd"]。 */
    fun findPath(d: ByteArray, path: List<String>, start: Int, end: Int): Box? {
        var s = start
        var e = end
        var found: Box? = null
        for (t in path) {
            found = find(d, t, s, e) ?: return null
            s = found.bodyStart
            e = found.bodyEnd
        }
        return found
    }

    /** 在字节流里搜 4 字符标记（用于定位不在 box 树里的 senc）。 */
    fun indexOf(d: ByteArray, tag: String, from: Int): Int {
        val a = tag[0].code.toByte()
        val b = tag[1].code.toByte()
        val c = tag[2].code.toByte()
        val e = tag[3].code.toByte()
        var i = from
        while (i + 4 <= d.size) {
            if (d[i] == a && d[i + 1] == b && d[i + 2] == c && d[i + 3] == e) return i
            i++
        }
        return -1
    }
}
