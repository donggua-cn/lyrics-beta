package com.applemusic.android.widevine

import java.security.PrivateKey
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Widevine 用到的密码学操作封装（JCA 版）。
 *
 * 对应鸿蒙版 `widevine/Crypto.ets`，同样的五种操作：
 *   RSA-PSS(SHA1, salt=20) 签名 / RSA-OAEP(SHA1) 解密 / AES-CBC / AES-CMAC / HMAC-SHA256
 *
 * 【易错点（原项目踩过）】签名必须是 **PSS 且 salt 长度 = 20**（SHA1 摘要长度）。
 * 默认的「最大 salt」会被 Apple 的 license 服务端拒绝（表现为 status = -1021）。
 */
object Crypto {

    /**
     * RSA-PSS(SHA1) 签名 —— Widevine LicenseRequest 要求 PSS。
     *
     * 【易错点】算法名在不同 provider 上不一样：
     *  - Android/Conscrypt 认 `SHA1withRSA/PSS`，**不接受** setParameter
     *  - 桌面 JDK（SunRsaSign）认 `RSASSA-PSS` + setParameter，
     *    不认 `SHA1withRSA/PSS`
     * 所以这里两个名字都试；关键是 salt 长度必须 = 20（SHA1 摘要长度），
     * 用默认的「最大 salt」Apple 的 key server 会拒绝（status = -1021）。
     */
    fun rsaPssSha1Sign(key: PrivateKey, data: ByteArray): ByteArray {
        val attempts = listOf("SHA1withRSA/PSS", "RSASSA-PSS", "SHA1withRSAandMGF1")
        var lastError: Throwable? = null
        for (alg in attempts) {
            try {
                val sig = Signature.getInstance(alg)
                try {
                    sig.setParameter(
                        java.security.spec.PSSParameterSpec(
                            "SHA-1", "MGF1", java.security.spec.MGF1ParameterSpec.SHA1, 20, 1
                        )
                    )
                } catch (t: Throwable) {
                    // Conscrypt 的 "SHA1withRSA/PSS" 已隐含 salt=20 与 SHA-1，不接受参数设置
                    if (alg == "SHA1withRSA/PSS") {
                        // 忽略：这正是我们想要的参数组合
                    } else {
                        throw t
                    }
                }
                sig.initSign(key)
                sig.update(data)
                return sig.sign()
            } catch (t: Throwable) {
                lastError = t
            }
        }
        throw IllegalStateException("找不到可用的 RSA-PSS(SHA1) 签名算法：${lastError?.message}", lastError)
    }

    /** RSA-OAEP(SHA1) 解密 —— 解 license 响应里的 session_key。 */
    fun rsaOaepSha1Decrypt(key: PrivateKey, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding")
        cipher.init(Cipher.DECRYPT_MODE, key)
        return cipher.doFinal(data)
    }

    /** AES-CBC 解密（NoPadding，PKCS#7 由调用方剥离）。 */
    fun aesCbcDecrypt(key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(data)
    }

    /**
     * AES-CMAC（RFC 4493 手写实现）。
     *
     * JCA 没有可移植的 AES-CMAC（Android 的 Conscrypt 在 API 28+ 才有
     * "AESCMAC"，为了兼容 24 就自己实现，算法本身只有 60 行）。
     * Widevine 的密钥派生完全依赖它，必须精确。
     */
    fun aesCmac(key: ByteArray, data: ByteArray): ByteArray {
        val blockSize = 16
        val zero = ByteArray(blockSize)
        val l = aesEcbEncrypt(key, zero)

        // 子密钥 K1 = L<<1 (mod 多项式 x^128 + x^7 + x^2 + x + 1)
        val k1 = shiftLeftOne(l)
        // 子密钥 K2 = K1<<1
        val k2 = shiftLeftOne(k1)

        val n = (data.size + blockSize - 1) / blockSize
        val lastComplete = data.isNotEmpty() && data.size % blockSize == 0
        val blocks = if (n == 0) 1 else n

        var lastBlock = ByteArray(blockSize)
        if (lastComplete) {
            System.arraycopy(data, (blocks - 1) * blockSize, lastBlock, 0, blockSize)
            for (i in 0 until blockSize) lastBlock[i] = (lastBlock[i].toInt() xor k1[i].toInt()).toByte()
        } else {
            val rem = if (data.isEmpty()) 0 else data.size % blockSize
            val start = (blocks - 1) * blockSize
            if (rem > 0) System.arraycopy(data, start, lastBlock, 0, rem)
            lastBlock[rem] = 0x80.toByte()
            for (i in 0 until blockSize) lastBlock[i] = (lastBlock[i].toInt() xor k2[i].toInt()).toByte()
        }

        var x = ByteArray(blockSize)
        for (i in 0 until blocks - 1) {
            val y = ByteArray(blockSize)
            for (k in 0 until blockSize) y[k] = (x[k].toInt() xor data[i * blockSize + k].toInt()).toByte()
            x = aesEcbEncrypt(key, y)
        }
        val y = ByteArray(blockSize)
        for (k in 0 until blockSize) y[k] = (x[k].toInt() xor lastBlock[k].toInt()).toByte()
        return aesEcbEncrypt(key, y)
    }

    private fun shiftLeftOne(input: ByteArray): ByteArray {
        val out = ByteArray(input.size)
        var carry = 0
        for (i in input.indices.reversed()) {
            val v = input[i].toInt() and 0xFF
            out[i] = ((v shl 1) or carry).toByte()
            carry = (v ushr 7) and 1
        }
        if (carry == 1) {
            // Rb = 0x00...0087
            out[out.size - 1] = (out[out.size - 1].toInt() xor 0x87).toByte()
        }
        return out
    }

    private fun aesEcbEncrypt(key: ByteArray, block: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(block)
    }

    /** HMAC-SHA256 —— 校验 license 响应签名。 */
    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    /** 去掉 PKCS#7 填充。 */
    fun stripPkcs7(data: ByteArray): ByteArray {
        if (data.isEmpty()) return data
        val pad = data[data.size - 1].toInt() and 0xFF
        if (pad <= 0 || pad > 16 || pad > data.size) return data
        return data.copyOfRange(0, data.size - pad)
    }
}
