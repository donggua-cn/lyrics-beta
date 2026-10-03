package com.applemusic.android.drm

import android.media.MediaDrm
import android.os.Build
import android.util.Base64
import com.applemusic.android.util.ByteOps
import com.applemusic.android.util.Logx
import com.applemusic.android.util.PbReader
import java.util.UUID

/**
 * 系统 Widevine（MediaDrm）路：首选方案。
 *
 * 为什么优先用它：Android 自带 Widevine CDM，challenge 由设备签发、密钥由
 * MediaDrm 内部保管，我们完全不碰私钥 —— 这是 Android 上消费 Widevine 的标准姿势。
 * 鸿蒙版之所以自己实现 CDM，是因为 HarmonyOS 没有可用的 Widevine 实现。
 *
 * 流程（与 gamdl 的 sequence 一致）：
 *   1. MediaDrm(WIDEVINE_UUID).getKeyRequest(null, psshPayload, "video/mp4", STREAMING, null)
 *   2. challenge（= SignedMessage）base64 → POST 到 Apple 的 key server
 *   3. response.license base64 → provideKeyResponse → 拿到 16 字节内容密钥
 *
 * 注意：Apple 的 license 端点的 body 里必须带 `adamId`（**catalog id**）、
 * `uri`（清单里的 key URI）和 `key-system: com.widevine.alpha`，
 * 它靠这些字段（而不只是 PSSH）来定位曲目。
 */
class MediaDrmKeyFetcher(
    private val postLicense: (body: String) -> String
) : KeyFetcher {

    override val name: String = "MediaDrm(系统 Widevine)"

    override fun fetchKey(keyUri: String, adamId: String, licenseServer: String): ByteArray {
        val pssh = Pssh.payloadFromKeyUri(keyUri)
        Logx.i(TAG, "PSSH payload=${pssh.size}B kid=${Pssh.kidHex(keyUri)} adamId=$adamId")

        var drm: MediaDrm? = null
        var sessionId: ByteArray? = null
        try {
            drm = MediaDrm(WIDEVINE_UUID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // 只要 L3（软件）即可满足音频；提高要求反而可能在没有安全级别的机型上失败。
                // 注意：属性开关在 framework 里叫 setPropertyString（API 28+），
                // 早期版本的 MediaDrm 只有 setPropertyByteArray。
                try {
                    drm.setPropertyString("securityLevel", "L3")
                } catch (t: Throwable) {
                    Logx.w(TAG, "setPropertyString(securityLevel) 不支持：${t.message}")
                }
            }

            // 显式要一个 session：老式 getKeyRequest 与 provideKeyResponse 都按 session 走。
            val sid = drm.openSession()
            sessionId = sid

            // 顺便要求带上 privacy 证书（拿不到就算了，Apple 的 key server 对此不敏感）。
            val optionalParams = HashMap<String, String>()
            try {
                if (drm.getPropertyString("privacyMode") == "enable") {
                    optionalParams["PRIVACY_MODE"] = "enable"
                }
            } catch (_: Throwable) {
            }

            @Suppress("DEPRECATION")
            var request: MediaDrm.KeyRequest
            try {
                request = drm.getKeyRequest(sid, pssh, "video/mp4", MediaDrm.KEY_TYPE_STREAMING, optionalParams)
            } catch (t: Throwable) {
                Logx.w(TAG, "getKeyRequest(video/mp4) 失败，改用 audio/mp4：${t.message}")
                @Suppress("DEPRECATION")
                request = drm.getKeyRequest(sid, pssh, "audio/mp4", MediaDrm.KEY_TYPE_STREAMING, optionalParams)
            }
            val challenge = request.data
                ?: throw IllegalStateException("MediaDrm 没有返回 challenge")

            val challengeB64 = Base64.encodeToString(challenge, Base64.NO_WRAP)
            Logx.i(TAG, "challenge=${challenge.size}B b64Len=${challengeB64.length}")

            val body = buildLicenseBody(challengeB64, keyUri, adamId)
            val respText = postLicense(body)

            val licenseB64 = extractLicense(respText)
                ?: throw IllegalStateException("license 响应里没有 license 字段: ${respText.take(200)}")
            val licenseBytes = Base64.decode(licenseB64, Base64.DEFAULT)
            Logx.i(TAG, "license=${licenseBytes.size}B")

            @Suppress("DEPRECATION")
            val keyResponse = drm.provideKeyResponse(sid, licenseBytes)
                ?: throw IllegalStateException("provideKeyResponse 返回空")
            val key = extractContentKey(keyResponse)
                ?: throw IllegalStateException("provideKeyResponse 里没有 CONTENT 密钥（${keyResponse.size}B）")
            Logx.i(TAG, "内容密钥 (${key.size}B) 获取成功")
            return key
        } finally {
            val d = drm
            val sid = sessionId
            if (d != null && sid != null) {
                try {
                    d.closeSession(sid)
                } catch (_: Throwable) {
                }
            }
            try {
                d?.close()
            } catch (_: Throwable) {
            }
            try {
                d?.release()
            } catch (_: Throwable) {
            }
        }
    }

    /** 组装 Apple license 端点的请求体。 */
    private fun buildLicenseBody(challengeB64: String, keyUri: String, adamId: String): String {
        // 用 JSONObject 而不是手拼：keyUri 里可能有 base64 的 `+` `/` `=`。
        val o = org.json.JSONObject()
        o.put("challenge", challengeB64)
        o.put("key-system", "com.widevine.alpha")
        o.put("uri", keyUri)
        o.put("adamId", adamId)
        o.put("isLibrary", false)
        o.put("user-initiated", true)
        return o.toString()
    }

    /** 从 license 响应 JSON 里取 license 字段。 */
    private fun extractLicense(text: String): String? {
        return try {
            val o = org.json.JSONObject(text)
            val lic = o.optString("license", "")
            if (lic.isNotEmpty()) lic else null
        } catch (t: Throwable) {
            Logx.w(TAG, "license 响应不是 JSON：${text.take(120)}")
            null
        }
    }

    companion object {
        private const val TAG = "MediaDrmKey"

        /** Widevine system id（对应 AppleConst.WIDEVINE_UUID）。 */
        val WIDEVINE_UUID: UUID = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed")

        /**
         * 从 provideKeyResponse 的返回值里找 CONTENT 类型的密钥。
         *
         * 返回的是 KeyResponse protobuf：
         *   KeyResponse { License? license = 2; repeated KeyContainer keys = 3; }
         *   KeyContainer { bytes key_id = 1; bytes key = 3; KeyType type = 4; }
         * KeyType: 1=SIGNING 2=CONTENT 3=KEY_CONTROL 4=OPERATOR_SESSION ...
         *
         * 取不到 CONTENT 时退回「第一个非空 key」（实测多数机型直接给内容密钥）。
         */
        fun extractContentKey(keyResponse: ByteArray): ByteArray? {
            val candidates = ArrayList<Pair<Int, ByteArray>>()
            try {
                val r = PbReader(keyResponse)
                while (!r.eof()) {
                    val f = r.next() ?: break
                    if (f.field != 3) continue
                    var key = ByteArray(0)
                    var type = 0
                    val kr = PbReader(f.bytes)
                    while (!kr.eof()) {
                        val g = kr.next() ?: break
                        when (g.field) {
                            3 -> key = g.bytes
                            4 -> type = g.varint.toInt()
                        }
                    }
                    if (key.isNotEmpty()) candidates.add(type to key)
                }
            } catch (t: Throwable) {
                Logx.w(TAG, "解析 KeyResponse 失败：${t.message}")
            }
            for ((type, key) in candidates) {
                if (type == 2) return key
            }
            candidates.firstOrNull()?.let { return it.second }
            return null
        }
    }
}
