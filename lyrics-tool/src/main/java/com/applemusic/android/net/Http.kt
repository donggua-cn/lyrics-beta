package com.applemusic.android.net

import com.applemusic.android.util.Logx
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 极简 HTTP 客户端（java.net.HttpURLConnection，不引第三方库）。
 *
 * 对应鸿蒙版 `AppleApi` 里的 `rawGet/requestJson/downloadRange` 三件套。
 * 注意两处与原实现一致的关键点：
 *  - 带 Range 时服务端返回 **206 Partial Content**；少数场景回 200（整段），两种都接受；
 *  - 连接/读取超时分开设，HLS 分片用的是长 readTimeout。
 */
object Http {

    private const val TAG = "Http"

    class Response(
        val code: Int,
        val body: ByteArray,
        val headers: Map<String, List<String>>
    ) {
        val text: String get() = String(body, Charsets.UTF_8)
    }

    /**
     * 发起一次请求。
     *
     * @param rangeStart >=0 时带 Range 头；rangeEnd >=0 时是 `bytes=start-end`，否则 `bytes=start-`
     */
    fun request(
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        body: ByteArray? = null,
        connectTimeoutMs: Int = 15000,
        readTimeoutMs: Int = 30000,
        rangeStart: Int = -1,
        rangeEnd: Int = -1
    ): Response {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection)
            conn.requestMethod = method
            conn.connectTimeout = connectTimeoutMs
            conn.readTimeout = readTimeoutMs
            conn.instanceFollowRedirects = true
            conn.useCaches = false
            for ((k, v) in headers) {
                conn.setRequestProperty(k, v)
            }
            if (rangeStart >= 0) {
                conn.setRequestProperty(
                    "Range",
                    if (rangeEnd >= 0) "bytes=$rangeStart-$rangeEnd" else "bytes=$rangeStart-"
                )
            }
            if (body != null) {
                conn.doOutput = true
                conn.setFixedLengthStreamingMode(body.size)
                conn.outputStream.use { it.write(body) }
            }
            val code = conn.responseCode
            val stream: InputStream? = try {
                conn.inputStream
            } catch (e: Exception) {
                conn.errorStream
            }
            val bytes = stream?.readBytesSafe() ?: ByteArray(0)
            val hs = HashMap<String, List<String>>()
            for ((k, v) in conn.headerFields) {
                if (k != null) hs[k.lowercase()] = v ?: emptyList()
            }
            if (code >= 400) {
                Logx.w(TAG, "$method $url -> HTTP $code body=${String(bytes, Charsets.UTF_8).take(160)}")
            }
            return Response(code, bytes, hs)
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Throwable) {
            }
        }
    }

    /** GET 文本，非 2xx 抛异常。 */
    fun getText(url: String, headers: Map<String, String> = emptyMap(), readTimeoutMs: Int = 25000): String {
        val r = request(url, "GET", headers, null, 15000, readTimeoutMs)
        if (r.code != 200) throw RuntimeException("HTTP ${r.code} @ $url")
        return r.text
    }

    /** GET 字节，接受 200/206。 */
    fun getBytes(
        url: String,
        headers: Map<String, String> = emptyMap(),
        rangeStart: Int = -1,
        rangeEnd: Int = -1,
        readTimeoutMs: Int = 60000
    ): ByteArray {
        val r = request(url, "GET", headers, null, 15000, readTimeoutMs, rangeStart, rangeEnd)
        if (r.code != 200 && r.code != 206) {
            throw RuntimeException("HTTP ${r.code} @ $url")
        }
        return r.body
    }

    /** POST JSON，期望 2xx。 */
    fun postJson(
        url: String,
        jsonBody: String,
        headers: Map<String, String>,
        readTimeoutMs: Int = 30000
    ): String {
        val r = request(
            url, "POST", headers, jsonBody.toByteArray(Charsets.UTF_8), 15000, readTimeoutMs
        )
        if (r.code !in 200..299) {
            throw RuntimeException("HTTP ${r.code} @ $url body=${r.text.take(200)}")
        }
        return r.text
    }

    /** POST，返回原始 code（资料库写入要区分 200/202/204）。 */
    fun postRaw(
        url: String,
        jsonBody: String,
        headers: Map<String, String>,
        readTimeoutMs: Int = 20000
    ): Response = request(url, "POST", headers, jsonBody.toByteArray(Charsets.UTF_8), 15000, readTimeoutMs)

    private fun InputStream.readBytesSafe(): ByteArray {
        val out = ByteArrayOutputStream(64 * 1024)
        val buf = ByteArray(32 * 1024)
        while (true) {
            val n = read(buf)
            if (n <= 0) break
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }
}
