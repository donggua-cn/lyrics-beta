package com.applemusic.android.widevine

import com.applemusic.android.util.ByteOps
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec

/**
 * WVD（Widevine Device）文件解析 + PKCS#1 → PKCS#8 私钥转换。
 *
 * 二进制布局（v2），与鸿蒙版 `WvdParser.ets` 一致：
 *   "WVD"(3) | version(1)=2 | type(1) | security_level(1) | flags(1) |
 *   private_key_len(2, BE) | private_key(DER, PKCS#1) |
 *   client_id_len(2, BE) | client_id(protobuf ClientIdentification)
 */
class WvdDevice {
    var type = 2            // 1 = CHROME, 2 = ANDROID
    var securityLevel = 3   // 1/2/3
    var privateKeyDer: ByteArray = ByteArray(0)
    var clientId: ByteArray = ByteArray(0)
    var rsaBits = 2048

    /** 转成 JCA 可用的私钥对象（内部做 PKCS#1 → PKCS#8 包装）。 */
    fun privateKey(): java.security.PrivateKey {
        val pkcs8 = pkcs1ToPkcs8(privateKeyDer, rsaBits)
        return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(pkcs8))
    }

    companion object {
        fun parse(data: ByteArray): WvdDevice {
            val dev = WvdDevice()
            if (data.size < 12) throw IllegalArgumentException("WVD 数据太短")
            val magic = String(charArrayOf(data[0].toInt().toChar(), data[1].toInt().toChar(), data[2].toInt().toChar()))
            if (magic != "WVD") throw IllegalArgumentException("WVD 魔数不正确: $magic")
            dev.type = data[4].toInt() and 0xFF
            dev.securityLevel = data[5].toInt() and 0xFF

            var off = 7 // 跳过 magic(3) + version(1) + type(1) + security(1) + flags(1)
            val pkLen = ((data[off].toInt() and 0xFF) shl 8) or (data[off + 1].toInt() and 0xFF)
            off += 2
            dev.privateKeyDer = data.copyOfRange(off, off + pkLen)
            off += pkLen
            val cidLen = ((data[off].toInt() and 0xFF) shl 8) or (data[off + 1].toInt() and 0xFF)
            off += 2
            dev.clientId = data.copyOfRange(off, minOf(off + cidLen, data.size))
            dev.rsaBits = detectRsaBits(dev.privateKeyDer)
            if (dev.privateKeyDer.isEmpty() || dev.clientId.isEmpty()) {
                throw IllegalArgumentException("WVD 缺少私钥或 client_id")
            }
            return dev
        }

        fun parseBuiltin(): WvdDevice = parse(BuiltinWvd.BYTES)

        /** DER(PKCS#1 RSAPrivateKey) 里解析模数位数：1024 / 2048 / 4096。 */
        fun detectRsaBits(der: ByteArray): Int {
            try {
                var p = 0
                if ((der[p].toInt() and 0xFF) != 0x30) return 2048
                p++
                if ((der[p].toInt() and 0x80) != 0) {
                    p += 1 + (der[p].toInt() and 0x7F)
                } else {
                    p++
                }
                // 第一个 INTEGER：version
                if ((der[p].toInt() and 0xFF) != 0x02) return 2048
                p++
                val vlen = der[p].toInt() and 0xFF
                p += 1 + vlen
                // 第二个 INTEGER：modulus
                if ((der[p].toInt() and 0xFF) != 0x02) return 2048
                p++
                var mlen = der[p].toInt() and 0xFF
                if (mlen and 0x80 != 0) {
                    val n = mlen and 0x7F
                    mlen = 0
                    for (i in 0 until n) mlen = mlen * 256 + (der[p + 1 + i].toInt() and 0xFF)
                    p += 1 + n
                } else {
                    p++
                }
                var bytes = mlen
                if (der[p].toInt() == 0) bytes--
                val bits = bytes * 8
                return if (bits == 1024 || bits == 2048 || bits == 4096) bits else 2048
            } catch (_: Throwable) {
                return 2048
            }
        }

        /**
         * PKCS#1 `RSAPrivateKey` → PKCS#8 `PrivateKeyInfo`。
         *
         * Android 的 KeyFactory 不接受 PKCS#1，所以手工套一层：
         *   PrivateKeyInfo ::= SEQUENCE { version INTEGER(0),
         *                                 algorithm AlgorithmIdentifier(rsaEncryption, NULL),
         *                                 privateKey OCTET STRING (PKCS#1) }
         * DER 长度用最简形式编码（不追求最短，够用即可）。
         */
        fun pkcs1ToPkcs8(pkcs1: ByteArray, rsaBits: Int): ByteArray {
            val algId = byteArrayOf(
                0x30, 0x0D,
                0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x01,
                0x05, 0x00
            )
            val version = byteArrayOf(0x02, 0x01, 0x00)
            val octet = derTag(0x04, pkcs1)
            val body = ByteOps.concat(version, algId, octet)
            return derTag(0x30, body)
        }

        /** 用 DER 长度规则包一个 TLV（长形式按需 1~3 字节）。 */
        private fun derTag(tag: Int, body: ByteArray): ByteArray {
            val len = body.size
            val lenBytes: ByteArray = when {
                len < 0x80 -> byteArrayOf(len.toByte())
                len < 0x100 -> byteArrayOf(0x81.toByte(), len.toByte())
                len < 0x10000 -> byteArrayOf(0x82.toByte(), ((len shr 8) and 0xFF).toByte(), (len and 0xFF).toByte())
                else -> byteArrayOf(
                    0x83.toByte(),
                    ((len shr 16) and 0xFF).toByte(),
                    ((len shr 8) and 0xFF).toByte(),
                    (len and 0xFF).toByte()
                )
            }
            return ByteOps.concat(byteArrayOf(tag.toByte()), lenBytes, body)
        }
    }
}
