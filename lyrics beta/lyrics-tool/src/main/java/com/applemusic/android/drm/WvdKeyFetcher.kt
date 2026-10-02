package com.applemusic.android.drm

import android.util.Base64
import com.applemusic.android.util.Logx
import com.applemusic.android.widevine.WidevineCdm
import com.applemusic.android.widevine.WvdDevice

/**
 * 内置 WVD 凭证路：备用方案（鸿蒙版就是这么干的）。
 *
 * 和 `MediaDrmKeyFetcher` 的差别只有「challenge 谁签、密钥谁保管」：
 * 这里用工程内置的 L3 设备凭证在本机构造 SignedMessage，
 * 再用它解 license 响应里的 session_key，全程不依赖系统 CDM。
 *
 * 什么时候会走到这条路：
 *  - 设备没有 Widevine（部分国产 ROM / 模拟器）
 *  - MediaDrm 的 challenge 被 Apple 的 key server 拒绝
 */
class WvdKeyFetcher(
    private val postLicense: (body: String) -> String
) : KeyFetcher {

    override val name: String = "内置 WVD 凭证"

    private val cdm: WidevineCdm by lazy {
        val dev: WvdDevice = WvdDevice.parseBuiltin()
        Logx.i(TAG, "内置 CDM 就绪: type=${dev.type} level=${dev.securityLevel} rsa=${dev.rsaBits} " +
                "clientId=${dev.clientId.size}B")
        WidevineCdm(dev)
    }

    override fun fetchKey(keyUri: String, adamId: String, licenseServer: String): ByteArray {
        val pssh = Pssh.payloadFromKeyUri(keyUri)
        val challenge = cdm.buildChallenge(pssh, 1)
        val challengeB64 = Base64.encodeToString(challenge, Base64.NO_WRAP)

        val o = org.json.JSONObject()
        o.put("challenge", challengeB64)
        o.put("key-system", "com.widevine.alpha")
        o.put("uri", keyUri)
        o.put("adamId", adamId)
        o.put("isLibrary", false)
        o.put("user-initiated", true)
        val respText = postLicense(o.toString())

        val licB64 = try {
            org.json.JSONObject(respText).optString("license", "")
        } catch (t: Throwable) {
            ""
        }
        if (licB64.isEmpty()) {
            throw IllegalStateException("license 响应异常: ${respText.take(200)}")
        }
        val licBytes = Base64.decode(licB64, Base64.DEFAULT)
        val keys = cdm.parseLicense(licBytes)
        val content = cdm.contentKeys(keys)
        if (content.isEmpty()) throw IllegalStateException("license 里没有 CONTENT 类型的密钥")
        Logx.i(TAG, "内容密钥 (${content[0].key.size}B) 获取成功，debug=${cdm.lastDebug}")
        return content[0].key
    }

    companion object {
        private const val TAG = "WvdKey"
    }
}
