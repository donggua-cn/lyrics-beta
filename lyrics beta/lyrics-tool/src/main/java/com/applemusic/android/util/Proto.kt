package com.applemusic.android.util

import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * 手写 protobuf wire format 编解码。
 *
 * 对应鸿蒙版的 `widevine/Pb.ets`，只实现 Widevine 用得到的三件套：
 * varint（wire type 0）、length-delimited（wire type 2），以及跳过其他类型。
 * 之所以不引 protobuf-javalite：整个工程零 protobuf 依赖，代码量也只多 100 行。
 */
class PbWriter {
    private val buf = ByteArrayOutputStream(256)

    private fun putVarint(value: Long) {
        var v = value
        while (v > 0x7FL || v < 0) {
            buf.write(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
        buf.write((v and 0x7F).toInt())
    }

    private fun putTag(field: Int, wireType: Int) = putVarint((field * 8 + wireType).toLong())

    fun uint(field: Int, value: Int): PbWriter {
        putTag(field, 0)
        putVarint(value.toLong() and 0xFFFFFFFFL)
        return this
    }

    fun int64(field: Int, value: Long): PbWriter {
        putTag(field, 0)
        putVarint(value)
        return this
    }

    fun bytes(field: Int, value: ByteArray): PbWriter {
        putTag(field, 2)
        putVarint(value.size.toLong())
        buf.write(value, 0, value.size)
        return this
    }

    fun message(field: Int, value: ByteArray): PbWriter = bytes(field, value)

    fun str(field: Int, value: String): PbWriter = bytes(field, value.toByteArray(Charsets.UTF_8))

    fun toByteArray(): ByteArray = buf.toByteArray()
}

class PbField {
    var field = 0
    var wireType = 0
    var varint: Long = 0
    var bytes: ByteArray = ByteArray(0)
}

class PbReader(private val data: ByteArray) {
    private var pos = 0

    fun eof(): Boolean = pos >= data.size

    private fun readVarint(): Long {
        var result = 0L
        var shift = 0
        while (pos < data.size) {
            val b = data[pos++].toInt() and 0xFF
            result = result or ((b and 0x7F).toLong() shl shift)
            if (b and 0x80 == 0) break
            shift += 7
            if (shift > 63) break
        }
        return result
    }

    /** 读下一个字段；到末尾返回 null。 */
    fun next(): PbField? {
        if (eof()) return null
        val f = PbField()
        val tag = readVarint()
        f.field = (tag ushr 3).toInt()
        f.wireType = (tag and 7L).toInt()
        when (f.wireType) {
            0 -> f.varint = readVarint()
            2 -> {
                val len = readVarint().toInt()
                val end = minOf(pos + len, data.size)
                f.bytes = data.copyOfRange(pos, end)
                pos = end
            }
            5 -> pos += 4
            1 -> pos += 8
            else -> pos = data.size
        }
        return f
    }
}

/** 读一段流里的全部字节。 */
fun InputStream.readAll(): ByteArray {
    val out = ByteArrayOutputStream(64 * 1024)
    val tmp = ByteArray(16 * 1024)
    while (true) {
        val n = read(tmp)
        if (n <= 0) break
        out.write(tmp, 0, n)
    }
    return out.toByteArray()
}
