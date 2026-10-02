package com.applemusic.android.util

import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-128 的纯 Kotlin 实现（CTR 模式）。
 *
 * 为什么需要它：CENC 是 **per-sample** 加密的，一首 4 分钟的 AAC 有 5000~14000 个
 * sample。用 `Cipher.getInstance("AES/CTR/NoPadding")` 每个 sample init 一次
 * 虽然可行，但固定开销不小；更重要的是它是**可测的参考实现** ——
 * 工程里的单元测试会拿它与 JCE 的结果逐字节比对（见 `AesCtrTest`）。
 *
 * 与鸿蒙版 `widevine/Crypto.ets` 里 `aesCtrDecryptBatch` 的定位一致：
 * 复用一把展开好的密钥，按 IV 增量处理多段独立密文。
 */
object AesCtr {

    /** 首选走 JCE（硬件/汇编优化），失败或不可用时用下面的纯 Kotlin 实现。 */
    private val jceAvailable: Boolean by lazy {
        try {
            Cipher.getInstance("AES/CTR/NoPadding")
            true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * AES-CTR 解密一段数据（CTR 是对称流模式，解密 = 加密）。
     *
     * @param key 16 字节密钥
     * @param iv 16 字节初始计数器（CENC 里是 8 字节 IV + 8 个 0）
     */
    fun decrypt(key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray {
        require(key.size == 16) { "只支持 AES-128，收到 ${key.size} 字节密钥" }
        require(iv.size == 16) { "CTR 计数器必须是 16 字节，收到 ${iv.size}" }
        if (jceAvailable) {
            return try {
                val cipher = Cipher.getInstance("AES/CTR/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES") as Key, IvParameterSpec(iv))
                cipher.doFinal(data)
            } catch (_: Throwable) {
                decryptPure(key, iv, data)
            }
        }
        return decryptPure(key, iv, data)
    }

    /** 纯 Kotlin 的 AES-128-CTR（不依赖任何 provider）。 */
    fun decryptPure(key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray {
        val expanded = KeyExpansion(key).expanded
        val out = ByteArray(data.size)
        val counter = iv.copyOf()
        val keystream = ByteArray(16)
        var offset = 0
        while (offset < data.size) {
            encryptBlock(expanded, counter, keystream)
            val n = minOf(16, data.size - offset)
            for (i in 0 until n) out[offset + i] = (data[offset + i].toInt() xor keystream[i].toInt()).toByte()
            offset += n
            // 计数器按大端 +1（CTR 规范）
            var i = 15
            while (i >= 0) {
                counter[i] = (counter[i] + 1).toByte()
                if (counter[i].toInt() != 0) break
                i--
            }
        }
        return out
    }

    // ─────────────────────── AES 核心 ───────────────────────

    private val SBOX = IntArray(256)
    private val INV_SBOX = IntArray(256)

    init {
        // 【坑】不能用「反复乘 3」建 log/antilog 表来求逆元：0x03 在 GF(2^8) 里
        // 不是本原元（它的阶是 51），生成的表会在 51 项后循环，一半以上表项为 0。
        // 本项目第一版就是这么写的，结果 S 盒错了一大半，CTR 解出来的音频全是噪声。
        // 直接 256×256 暴力求逆：只在类初始化时跑一次，开销可以忽略。
        for (i in 0 until 256) {
            val inv = if (i == 0) 0 else gfInverse(i)
            var s = inv
            var acc = inv
            // 仿射变换：s = inv ⊕ rotl(inv,1) ⊕ rotl(inv,2) ⊕ rotl(inv,3) ⊕ rotl(inv,4) ⊕ 0x63
            for (k in 0 until 4) {
                acc = ((acc shl 1) or (acc ushr 7)) and 0xFF
                s = s xor acc
            }
            s = s xor 0x63
            SBOX[i] = s and 0xFF
            INV_SBOX[s and 0xFF] = i
        }
    }

    /** GF(2^8) 乘法（模 x^8 + x^4 + x^3 + x + 1，即 0x11B）。 */
    private fun gfMul(a0: Int, b0: Int): Int {
        var a = a0
        var b = b0
        var p = 0
        for (i in 0 until 8) {
            if (b and 1 != 0) p = p xor a
            val hi = a and 0x80
            a = (a shl 1) and 0xFF
            if (hi != 0) a = a xor 0x1B
            b = b ushr 1
        }
        return p
    }

    /** 暴力求乘法逆元（0 没有逆元，返回 0）。 */
    private fun gfInverse(v: Int): Int {
        if (v == 0) return 0
        for (x in 1 until 256) {
            if (gfMul(v, x) == 1) return x
        }
        return 0
    }

    private val RCON = intArrayOf(0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1B, 0x36)

    private class KeyExpansion(key: ByteArray) {
        /** 11 个轮密钥 × 16 字节（AES-128）。 */
        val expanded = ByteArray(176)

        init {
            System.arraycopy(key, 0, expanded, 0, 16)
            var bytesGenerated = 16
            var rconIndex = 0
            val temp = ByteArray(4)
            while (bytesGenerated < 176) {
                System.arraycopy(expanded, bytesGenerated - 4, temp, 0, 4)
                if (bytesGenerated % 16 == 0) {
                    // RotWord + SubWord + Rcon
                    val t0 = temp[0]
                    temp[0] = (SBOX[temp[1].toInt() and 0xFF] xor RCON[rconIndex]).toByte()
                    temp[1] = SBOX[temp[2].toInt() and 0xFF].toByte()
                    temp[2] = SBOX[temp[3].toInt() and 0xFF].toByte()
                    temp[3] = SBOX[t0.toInt() and 0xFF].toByte()
                    rconIndex++
                }
                for (i in 0 until 4) {
                    expanded[bytesGenerated] =
                        (expanded[bytesGenerated - 16].toInt() xor temp[i].toInt()).toByte()
                    bytesGenerated++
                }
            }
        }
    }

    private fun encryptBlock(expanded: ByteArray, input: ByteArray, output: ByteArray) {
        val state = ByteArray(16)
        System.arraycopy(input, 0, state, 0, 16)
        addRoundKey(state, expanded, 0)
        for (round in 1..9) {
            subBytes(state)
            shiftRows(state)
            mixColumns(state)
            addRoundKey(state, expanded, round * 16)
        }
        subBytes(state)
        shiftRows(state)
        addRoundKey(state, expanded, 160)
        System.arraycopy(state, 0, output, 0, 16)
    }

    private fun addRoundKey(state: ByteArray, expanded: ByteArray, offset: Int) {
        for (i in 0 until 16) state[i] = (state[i].toInt() xor expanded[offset + i].toInt()).toByte()
    }

    private fun subBytes(state: ByteArray) {
        for (i in 0 until 16) state[i] = SBOX[state[i].toInt() and 0xFF].toByte()
    }

    /** AES 状态按列主序存放：state[c*4 + r]。 */
    private fun shiftRows(state: ByteArray) {
        val tmp = ByteArray(16)
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                tmp[c * 4 + r] = state[((c + r) % 4) * 4 + r]
            }
        }
        System.arraycopy(tmp, 0, state, 0, 16)
    }

    private fun mixColumns(state: ByteArray) {
        for (c in 0 until 4) {
            val a0 = state[c * 4].toInt() and 0xFF
            val a1 = state[c * 4 + 1].toInt() and 0xFF
            val a2 = state[c * 4 + 2].toInt() and 0xFF
            val a3 = state[c * 4 + 3].toInt() and 0xFF
            state[c * 4] = (xtime(a0) xor (xtime(a1) xor a1) xor a2 xor a3).toByte()
            state[c * 4 + 1] = (a0 xor xtime(a1) xor (xtime(a2) xor a2) xor a3).toByte()
            state[c * 4 + 2] = (a0 xor a1 xor xtime(a2) xor (xtime(a3) xor a3)).toByte()
            state[c * 4 + 3] = ((xtime(a0) xor a0) xor a1 xor a2 xor xtime(a3)).toByte()
        }
    }

    private fun xtime(v: Int): Int {
        val r = (v shl 1) and 0xFF
        return if (v and 0x80 != 0) r xor 0x1B else r
    }
}
