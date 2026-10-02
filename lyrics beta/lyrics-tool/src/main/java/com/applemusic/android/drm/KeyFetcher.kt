package com.applemusic.android.drm

import com.applemusic.android.util.ByteOps

/**
 * 内容密钥获取的统一接口。
 *
 * Android 上有两条路，业务代码（AppleDownloader）不关心用哪条：
 *  - MediaDrmKeyFetcher：系统 Widevine（首选，简单可靠）
 *  - WvdKeyFetcher    ：内置 WVD 凭证 + 纯 Kotlin CDM（备用，完全自包含）
 */
interface KeyFetcher {
    /** 用哪条路的描述，用于日志/界面显示。 */
    val name: String

    /**
     * 换回内容密钥。
     *
     * @param keyUri  子清单 #EXT-X-KEY 的 URI（形如 `data:;base64,<16 字节 KID>`，
     *                也可能是完整 pssh box 的 base64）
     * @param adamId  目录歌曲 id（**必须是 catalog id**，用资料库 id 会得到 status=-1003）
     * @param licenseServer license 交换地址（来自 /v1/play/assets 的 keyServerUrl，
     *                取不到时退回 Apple 的固定端点）
     */
    fun fetchKey(keyUri: String, adamId: String, licenseServer: String): ByteArray
}

/** license 交换的固定端点（/v1/play/assets 没给 keyServerUrl 时的退路）。 */
const val DEFAULT_LICENSE_SERVER =
    "https://play.itunes.apple.com/WebObjects/MZPlay.woa/wa/acquireWebPlaybackLicense"

/**
 * 由 `#EXT-X-KEY` 的 URI 构造 Widevine PSSH **payload**。
 *
 * 规则同 gamdl：解码后 > 30 字节视为完整 PSSH box，否则视为 16 字节 KID，
 * 包进 WidevinePsshData { algorithm = 1 (AES-CTR); repeated bytes key_ids = 2; }。
 */
object Pssh {
    fun payloadFromKeyUri(keyUri: String): ByteArray {
        val parts = keyUri.split(",")
        val b64 = parts[parts.size - 1]
        val decoded = try {
            android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
        } catch (e: Throwable) {
            throw IllegalArgumentException("密钥 URI 不是可解析的 base64: ${keyUri.take(64)}")
        }
        if (decoded.size > 30) return decoded
        return com.applemusic.android.util.PbWriter()
            .uint(1, 1)
            .bytes(2, decoded)
            .toByteArray()
    }

    /** KID 的十六进制形式，便于日志对照。 */
    fun kidHex(keyUri: String): String {
        return try {
            val parts = keyUri.split(",")
            val decoded = android.util.Base64.decode(parts[parts.size - 1], android.util.Base64.DEFAULT)
            if (decoded.size <= 30) ByteOps.toHex(decoded) else "(pssh ${decoded.size}B)"
        } catch (e: Throwable) {
            "(unparsable)"
        }
    }
}
