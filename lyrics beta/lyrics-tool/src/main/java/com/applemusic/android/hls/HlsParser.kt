package com.applemusic.android.hls

/**
 * HLS（m3u8）解析 —— Apple Music 音频清单。
 *
 * 对应鸿蒙版 `HlsParser.ets`。Apple 的形态（实测）：
 *
 *   主清单：变体列表 + `#EXT-X-SESSION-KEY`（Widevine PSSH）
 *   子清单：`#EXT-X-KEY:METHOD=ISO-23001-7,URI="data:;base64,..."`
 *           `#EXT-X-MAP:URI="xxx.mp4",BYTERANGE="1006@0"`
 *           `#EXT-X-BYTERANGE:324055@1006`   ← 分片都在同一个 mp4 文件里
 */
class HlsByteRange {
    var length = 0
    var offset = 0
}

class HlsMediaInfo {
    /** 密钥 URI（Apple 给的是 data:;base64,<16 字节 KID>） */
    var keyUri = ""

    /** 密钥方法（ISO-23001-7 = CENC） */
    var keyMethod = ""

    /** init segment 的文件名 */
    var initUri = ""

    /** init segment 的字节范围 */
    var initRange = HlsByteRange()

    /** 分片所属媒体文件名（通常和 init 同文件） */
    var mediaUri = ""

    /** 各分片的字节范围 */
    val segments = ArrayList<HlsByteRange>()

    /** 清单所在目录（用于拼绝对地址） */
    var baseUrl = ""
}

class HlsMasterInfo {
    val variants = ArrayList<String>()
    var widevinePssh = ""
    val skdUris = ArrayList<String>()
    var baseUrl = ""
}

object HlsParser {

    private fun dirOf(url: String): String {
        val i = url.lastIndexOf('/')
        return if (i >= 0) url.substring(0, i + 1) else url
    }

    private fun abs(base: String, uri: String): String {
        if (uri.startsWith("http://") || uri.startsWith("https://") || uri.startsWith("data:")) return uri
        return base + uri
    }

    /**
     * 解析 `#EXT-X-BYTERANGE:<长度>[@偏移]`。
     *
     * HLS 规范：**`@偏移` 可以省略**，省略时表示「紧接上一个 sub-range 之后」，
     * 即 offset = 上一段的 offset + length。原项目在这里踩过一个大坑：省略时
     * 默认成 0，导致同一清单里所有分片偏移都是 0 —— 整段拼装时后写的分片全部
     * 覆盖在文件开头，下出来的文件完全是坏的（表现为「点了歌一直不播」）。
     */
    private fun parseRange(s: String, prevEnd: Int): HlsByteRange {
        val r = HlsByteRange()
        val parts = s.split("@")
        r.length = parts[0].trim().toIntOrNull() ?: 0
        r.offset = if (parts.size > 1) (parts[1].trim().toIntOrNull() ?: prevEnd) else prevEnd
        return r
    }

    private val uriRe = Regex("URI=\"([^\"]+)\"")
    private val keyFormatRe = Regex("KEYFORMAT=\"([^\"]+)\"")
    private val byterangeRe = Regex("BYTERANGE=\"([^\"]+)\"")

    /** 解析主清单。 */
    fun parseMaster(text: String, url: String): HlsMasterInfo {
        val info = HlsMasterInfo()
        info.baseUrl = dirOf(url)
        for (raw in text.split("\n")) {
            val l = raw.trim()
            if (l.startsWith("#EXT-X-SESSION-KEY")) {
                val m = uriRe.find(l) ?: continue
                val uri = m.groupValues[1]
                val fmt = keyFormatRe.find(l)
                if (fmt != null && fmt.groupValues[1].contains("edef8ba9")) {
                    info.widevinePssh = uri
                } else if (uri.startsWith("skd://")) {
                    info.skdUris.add(uri)
                }
            } else if (l.isNotEmpty() && !l.startsWith("#")) {
                info.variants.add(abs(info.baseUrl, l))
            }
        }
        return info
    }

    /** 解析子清单（音频清单）。 */
    fun parseMedia(text: String, url: String): HlsMediaInfo {
        val info = HlsMediaInfo()
        info.baseUrl = dirOf(url)
        var pendingRange: HlsByteRange? = null
        /** 上一段 byte-range 的结束位置（省略 @offset 时用它累加）。 */
        var prevEnd = 0

        for (raw in text.split("\n")) {
            val l = raw.trim()
            when {
                l.startsWith("#EXT-X-KEY") -> {
                    Regex("METHOD=([^,]+)").find(l)?.let { info.keyMethod = it.groupValues[1].trim() }
                    uriRe.find(l)?.let { info.keyUri = it.groupValues[1] }
                }

                l.startsWith("#EXT-X-MAP") -> {
                    uriRe.find(l)?.let {
                        info.initUri = it.groupValues[1]
                        info.mediaUri = it.groupValues[1]
                    }
                    byterangeRe.find(l)?.let {
                        info.initRange = parseRange(it.groupValues[1], 0)
                        prevEnd = info.initRange.offset + info.initRange.length
                    }
                }

                l.startsWith("#EXT-X-BYTERANGE") -> {
                    val v = l.substring("#EXT-X-BYTERANGE:".length)
                    pendingRange = parseRange(v, prevEnd)
                }

                l.isNotEmpty() && !l.startsWith("#") -> {
                    // 媒体分片行
                    if (info.mediaUri.isEmpty()) info.mediaUri = l
                    val pr = pendingRange
                    if (pr != null) {
                        info.segments.add(pr)
                        prevEnd = pr.offset + pr.length
                        pendingRange = null
                    } else {
                        // 没有 byterange：整文件作为一个分片
                        info.segments.add(HlsByteRange())
                    }
                }
            }
        }
        return info
    }

    /** 把相对 URI 拼成绝对地址。 */
    fun absolute(baseUrl: String, uri: String): String = abs(baseUrl, uri)
}
