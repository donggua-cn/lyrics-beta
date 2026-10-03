package com.applemusic.android.util

/**
 * 字节级工具。
 *
 * 对应鸿蒙版的 `widevine/Pb.ets` 里的 `concatBytes/bytesEqual/toHex/fromHex`，
 * 另外补上大端整数读写 —— 原来的 ArkTS 是直接按下标算的，Kotlin 里用这些函数更好读。
 */
object ByteOps {

    fun concat(vararg parts: ByteArray): ByteArray {
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

    fun equal(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    fun toHex(d: ByteArray, from: Int = 0, len: Int = d.size - from): String {
        val sb = StringBuilder(len * 2)
        for (i in from until from + len) {
            val v = d[i].toInt() and 0xFF
            if (v < 0x10) sb.append('0')
            sb.append(v.toString(16))
        }
        return sb.toString()
    }

    fun fromHex(s: String): ByteArray {
        val clean = s.filter { it.isDigit() || (it in 'a'..'f') || (it in 'A'..'F') }
        val out = ByteArray(clean.length / 2)
        for (i in out.indices) {
            out[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return out
    }

    /** 大端无符号 32 位。 */
    fun u32(d: ByteArray, off: Int): Long {
        return ((d[off].toLong() and 0xFF) shl 24) or
                ((d[off + 1].toLong() and 0xFF) shl 16) or
                ((d[off + 2].toLong() and 0xFF) shl 8) or
                (d[off + 3].toLong() and 0xFF)
    }

    /** 大端有符号 32 位（trun 的 data_offset 用得到）。 */
    fun i32(d: ByteArray, off: Int): Int {
        return ((d[off].toInt() and 0xFF) shl 24) or
                ((d[off + 1].toInt() and 0xFF) shl 16) or
                ((d[off + 2].toInt() and 0xFF) shl 8) or
                (d[off + 3].toInt() and 0xFF)
    }

    fun u16(d: ByteArray, off: Int): Int {
        return ((d[off].toInt() and 0xFF) shl 8) or (d[off + 1].toInt() and 0xFF)
    }

    /** 大端无符号 64 位（拆成 Long，本用途精度足够）。 */
    fun u64(d: ByteArray, off: Int): Long {
        val hi = u32(d, off)
        val lo = u32(d, off + 4)
        return (hi shl 32) or lo
    }

    fun fourcc(d: ByteArray, off: Int): String {
        return String(
            charArrayOf(
                d[off].toInt().toChar(),
                d[off + 1].toInt().toChar(),
                d[off + 2].toInt().toChar(),
                d[off + 3].toInt().toChar()
            )
        )
    }

    /** ASCII 字符串 → 字节。 */
    fun ascii(s: String): ByteArray {
        val out = ByteArray(s.length)
        for (i in s.indices) out[i] = (s[i].code and 0xFF).toByte()
        return out
    }
}
