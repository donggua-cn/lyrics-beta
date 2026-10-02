package com.applemusic.lyricstool.data

import android.content.Context
import android.content.SharedPreferences
import com.applemusic.android.lyrics.Lyrics
import com.applemusic.android.lyrics.LyricsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class SongHit(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val durationMs: Long,
    val providerSongId: String? = null,
    val isrc: String? = null
)

data class FetchedLyrics(
    val lyrics: Lyrics,
    val ttml: String?,
    val translationWarning: String? = null,
    val qqQrcDebugText: String? = null
)

class AppleLyricsClient(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences("apple_music_auth", Context.MODE_PRIVATE).also { shared ->
            val previous = context.getSharedPreferences("lyrics_tool_auth", Context.MODE_PRIVATE)
            if (shared.getString("media_user_token", null) == null) {
                val oldToken = previous.getString("media_user_token", null)
                if (!oldToken.isNullOrBlank()) {
                    shared.edit()
                        .putString("media_user_token", oldToken)
                        .putString("developer_token", previous.getString("developer_token", ""))
                        .putString("storefront", previous.getString("storefront", "cn"))
                        .apply()
                }
            }
        }

    @Volatile
    var developerToken: String = preferences.getString("developer_token", "").orEmpty()
        private set

    @Volatile
    var userToken: String = preferences.getString("media_user_token", "").orEmpty()
        private set

    @Volatile
    var storefront: String = preferences.getString("storefront", "cn") ?: "cn"
        private set

    @Volatile
    var lastSearchLocalizationWarning: String? = null
        private set

    val isSignedIn: Boolean
        get() = userToken.length > 50

    fun reloadSavedSession() {
        developerToken = preferences.getString("developer_token", "").orEmpty()
        userToken = preferences.getString("media_user_token", "").orEmpty()
        storefront = preferences.getString("storefront", "cn") ?: "cn"
    }

    fun saveUserToken(token: String) {
        userToken = token
        preferences.edit().putString("media_user_token", token).apply()
    }

    fun extractUserToken(cookie: String): String =
        Regex("media-user-token=([^;]+)").find(cookie)?.groupValues?.get(1)
            ?.takeIf { it.length > 50 }
            .orEmpty()

    suspend fun prepare() = withContext(Dispatchers.IO) {
        if (developerToken.length <= 50) refreshDeveloperToken()
    }

    suspend fun refreshStorefront() = withContext(Dispatchers.IO) {
        val response = JSONObject(request("/v1/me/account?meta=subscription"))
        val region = response.optJSONObject("meta")
            ?.optJSONObject("subscription")
            ?.optString("storefront")
            ?.lowercase(Locale.US)
            ?.takeIf { it.matches(Regex("[a-z]{2}")) }
            ?: return@withContext
        storefront = region
        preferences.edit().putString("storefront", region).apply()
    }

    suspend fun searchSongs(term: String): List<SongHit> = withContext(Dispatchers.IO) {
        ensureDeveloperToken()
        lastSearchLocalizationWarning = null
        val storefrontLanguages = when (storefront) {
            "cn", "zh" -> listOf("zh-Hans-CN", "en-US")
            "jp" -> listOf("ja-JP", "en-US")
            "kr" -> listOf("ko-KR", "en-US")
            "tw", "hk" -> listOf("zh-Hant-TW", "en-US")
            else -> listOf("en-US")
        }
        val languages = (listOf("zh-Hans-CN") + storefrontLanguages).distinct()
        val results = LinkedHashMap<String, SongHit>()
        var successfulRequests = 0
        var firstError: Throwable? = null

        for (language in languages) {
            try {
                val encodedTerm = URLEncoder.encode(term, Charsets.UTF_8.name())
                val response = request(
                    "/v1/catalog/$storefront/search?term=$encodedTerm&types=songs&limit=30&l=$language"
                )
                val root = JSONObject(response)
                successfulRequests++
                val songs = root.optJSONObject("results")
                    ?.optJSONObject("songs")?.optJSONArray("data") ?: continue
                for (i in 0 until songs.length()) {
                    val song = songs.optJSONObject(i) ?: continue
                    val attributes = song.optJSONObject("attributes") ?: continue
                    val id = song.optString("id")
                    if (id.isBlank()) continue
                    val artwork = attributes.optJSONObject("artwork")?.optString("url").orEmpty()
                        .replace("{w}", "160")
                        .replace("{h}", "160")
                        .replace("{f}", "jpg")
                    results.putIfAbsent(
                        id,
                        SongHit(
                            id = id,
                            title = attributes.optString("name", "未知歌曲"),
                            artist = attributes.optString("artistName", "未知艺人"),
                            album = attributes.optString("albumName"),
                            artworkUrl = artwork,
                            durationMs = attributes.optLong("durationInMillis"),
                            isrc = attributes.optString("isrc").takeIf { it.isNotBlank() }
                        )
                    )
                }
            } catch (error: Exception) {
                if (firstError == null) firstError = error
            }
        }

        if (successfulRequests == 0) {
            throw firstError ?: IllegalStateException("Apple Music 搜索暂时不可用")
        }
        localizeSearchResults(term, results.values.toList())
    }

    private suspend fun localizeSearchResults(
        term: String,
        songs: List<SongHit>
    ): List<SongHit> {
        if (songs.isEmpty() || songs.all { it.hasChineseMetadata() }) return songs

        return try {
            val encodedTerm = URLEncoder.encode(term, Charsets.UTF_8.name())
            val response = request(
                "/v1/catalog/cn/search?term=$encodedTerm&types=songs&limit=50&l=zh-Hans-CN"
            )
            val data = JSONObject(response).optJSONObject("results")
                ?.optJSONObject("songs")?.optJSONArray("data")
                ?: run {
                    lastSearchLocalizationWarning =
                        "国区搜索没有返回本地化曲目信息，当前显示原地区信息"
                    return songs
                }
            val localizedByIsrc = buildMap {
                for (index in 0 until data.length()) {
                    val item = data.optJSONObject(index) ?: continue
                    val attributes = item.optJSONObject("attributes") ?: continue
                    val isrc = attributes.optString("isrc")
                        .takeIf { it.isNotBlank() }?.uppercase(Locale.US) ?: continue
                    val title = attributes.optString("name").takeIf { it.isNotBlank() } ?: continue
                    val artist = attributes.optString("artistName")
                        .takeIf { it.isNotBlank() } ?: continue
                    if (title.none { it.isCjkIdeograph() } &&
                        artist.none { it.isCjkIdeograph() }
                    ) continue
                    put(
                        isrc,
                        LocalizedSongMetadata(
                            title = title,
                            artist = artist,
                            album = attributes.optString("albumName")
                        )
                    )
                }
            }
            var localizedCount = 0
            songs.map { song ->
                val isrc = song.isrc?.uppercase(Locale.US)
                val localized = isrc?.let(localizedByIsrc::get)
                    ?: return@map song
                localizedCount++
                song.copy(
                    title = localized.title,
                    artist = localized.artist,
                    album = localized.album.ifBlank { song.album }
                )
            }.also {
                val unresolvedCount = songs.count { song ->
                    !song.hasChineseMetadata() &&
                        (song.isrc?.uppercase(Locale.US)?.let { it !in localizedByIsrc } ?: true)
                }
                if (unresolvedCount > 0) {
                    lastSearchLocalizationWarning =
                        if (localizedCount == 0) {
                            "没有找到对应的国区中文曲目信息，当前显示原地区信息"
                        } else {
                            "部分歌曲没有匹配到国区中文信息，仍显示原地区名称"
                        }
                }
            }
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            lastSearchLocalizationWarning =
                "国区中文曲目信息暂时不可用，当前显示原地区信息（${error.message ?: "请求失败"}）"
            songs
        }
    }

    private data class LocalizedSongMetadata(
        val title: String,
        val artist: String,
        val album: String
    )

    private fun SongHit.hasChineseMetadata(): Boolean =
        title.any { it.isCjkIdeograph() } && artist.any { it.isCjkIdeograph() }

    suspend fun fetchChineseSongMetadata(song: SongHit): SongHit? = withContext(Dispatchers.IO) {
        val directLookup = song.id.takeIf { it.isNotBlank() && it.all(Char::isDigit) }?.let { id ->
            requestOrNull("/v1/catalog/cn/songs/$id?l=zh-Hans-CN")
                ?.let { localizedSongFromResponse(it, song) }
        }
        if (directLookup != null) return@withContext directLookup

        val isrc = song.isrc?.takeIf { it.isNotBlank() } ?: return@withContext null
        val encodedIsrc = URLEncoder.encode(isrc, Charsets.UTF_8.name())
        val response = requestOrNull(
            "/v1/catalog/cn/songs?filter%5Bisrc%5D=$encodedIsrc&l=zh-Hans-CN&limit=5"
        ) ?: return@withContext null
        val data = JSONObject(response).optJSONArray("data") ?: return@withContext null
        val exactTrack = (0 until data.length()).asSequence()
            .mapNotNull(data::optJSONObject)
            .firstOrNull { it.optJSONObject("attributes")?.optString("isrc")
                ?.equals(isrc, ignoreCase = true) == true
            } ?: return@withContext null
        localizedSongFromItem(exactTrack, song)
    }

    private fun localizedSongFromResponse(response: String, original: SongHit): SongHit? {
        val data = JSONObject(response).optJSONArray("data") ?: return null
        val matchingId = (0 until data.length())
            .asSequence()
            .mapNotNull(data::optJSONObject)
            .firstOrNull { it.optString("id") == original.id }
            ?: return null
        return localizedSongFromItem(matchingId, original)
    }

    private fun localizedSongFromItem(item: JSONObject, original: SongHit): SongHit? {
        val attributes = item.optJSONObject("attributes")
            ?: return null
        val localizedTitle = attributes.optString("name").takeIf { it.isNotBlank() }
            ?: return null
        val localizedArtist = attributes.optString("artistName").takeIf { it.isNotBlank() }
            ?: return null
        if (localizedTitle.none { it.isCjkIdeograph() } &&
            localizedArtist.none { it.isCjkIdeograph() }
        ) return null
        return SongHit(
            id = item.optString("id", original.id),
            title = localizedTitle,
            artist = localizedArtist,
            album = attributes.optString("albumName", original.album),
            artworkUrl = original.artworkUrl,
            durationMs = attributes.optLong("durationInMillis", original.durationMs),
            providerSongId = original.providerSongId,
            isrc = original.isrc
        )
    }

    private fun Char.isCjkIdeograph(): Boolean =
        code in 0x3400..0x4DBF || code in 0x4E00..0x9FFF || code in 0xF900..0xFAFF

    suspend fun fetchLyrics(songId: String): FetchedLyrics? = withContext(Dispatchers.IO) {
        ensureDeveloperToken()
        val base = "/v1/catalog/$storefront/songs/$songId"
        val localizedQuery = lyricsLocalizationQuery()
        val syllableJson = requestOrNull("$base/syllable-lyrics?$localizedQuery")
            ?: requestOrNull("$base/syllable-lyrics")
        var translationWarning: String? = null
        val standardLyricsJsons = LinkedHashSet<String>()
        var standardLyricsError: Exception? = null
        for (
            path in listOf(
                "$base/lyrics?$localizedQuery",
                "$base/lyrics?l%5Blyrics%5D=ttml"
            )
        ) {
            try {
                val response = requestOrNull(path) ?: continue
                standardLyricsJsons.add(response)
                val current = LyricsParser.parse(response)
                if (current != null && current.lines.any { !it.translation.isNullOrBlank() }) break
            } catch (error: Exception) {
                standardLyricsError = error
            }
        }
        if (standardLyricsJsons.isEmpty() && standardLyricsError != null) {
            if (syllableJson == null) throw standardLyricsError
            translationWarning = standardLyricsError.message ?: "暂时无法获取翻译歌词"
        }
        val primaryJson = syllableJson ?: standardLyricsJsons.firstOrNull() ?: return@withContext null
        val lyrics = LyricsParser.parse(primaryJson) ?: return@withContext null
        val mergedLyrics = if (syllableJson == null) {
            lyrics
        } else {
            standardLyricsJsons.fold(lyrics) { current, json ->
                LyricsParser.mergeTranslations(current, json)
            }
        }
        val ttml = runCatching {
            val attributes = JSONObject(primaryJson).optJSONArray("data")
                ?.optJSONObject(0)?.optJSONObject("attributes")
            attributes?.optString("ttmlLocalizations")?.takeIf { it.isNotBlank() }
                ?: attributes?.optString("ttml")?.takeIf { it.isNotBlank() }
        }.getOrNull()
        FetchedLyrics(mergedLyrics, ttml, translationWarning)
    }

    private fun lyricsLocalizationQuery(): String {
        val locale = lyricsLocale()
        val script = when {
            locale.startsWith("zh-Hant") -> "zh-Hant,zh-Latn"
            locale.startsWith("zh") -> "zh-Hans,zh-Latn"
            locale.startsWith("ja") -> "ja-Jpan,ja-Latn"
            locale.startsWith("ko") -> "ko-Kore,ko-Latn"
            locale.startsWith("en") -> "en-Latn"
            else -> "$locale-Latn"
        }
        return listOf(
            "l%5Blyrics%5D=${URLEncoder.encode(locale, Charsets.UTF_8.name())}",
            "l%5Bscript%5D=${URLEncoder.encode(script, Charsets.UTF_8.name())}",
            "extend=ttmlLocalizations"
        ).joinToString("&")
    }

    private fun lyricsLocale(): String {
        val deviceLocale = Locale.getDefault()
        return when {
            deviceLocale.language == "zh" && deviceLocale.country in setOf("TW", "HK", "MO") ->
                "zh-Hant-TW"
            deviceLocale.language == "zh" -> "zh-Hans-CN"
            deviceLocale.language == "ja" -> "ja-JP"
            deviceLocale.language == "ko" -> "ko-KR"
            else -> deviceLocale.toLanguageTag().takeIf { it.isNotBlank() } ?: "en-US"
        }
    }

    private fun ensureDeveloperToken() {
        if (developerToken.length <= 50) refreshDeveloperToken()
    }

    private fun refreshDeveloperToken() {
        val html = getText("https://music.apple.com/us/browse")
        val assetPath = Regex("/assets/index[~-][^/\"]+\\.js")
            .find(html)?.value
            ?: throw IllegalStateException("Apple Music 页面未找到认证脚本，请稍后重试")
        val script = getText("https://music.apple.com$assetPath")
        developerToken = Regex("\"(eyJ[A-Za-z0-9_\\-]+\\.eyJ[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+)\"")
            .find(script)?.groupValues?.get(1)
            ?: throw IllegalStateException("未能获取 Apple Music 访问令牌")
        preferences.edit().putString("developer_token", developerToken).apply()
    }

    private fun requestOrNull(path: String): String? {
        ensureDeveloperToken()
        val response = doRequest("https://amp-api.music.apple.com$path")
        return when (response.code) {
            200 -> response.body
            404 -> null
            401, 403 -> throw IllegalStateException("Apple Music 拒绝了请求，请先登录 Apple Music 后重试")
            else -> throw IllegalStateException("获取歌词失败（HTTP ${response.code}）")
        }
    }

    private fun request(path: String): String {
        val response = doRequest("https://amp-api.music.apple.com$path")
        if (response.code !in 200..299) {
            val message = if (response.code == 401 || response.code == 403) {
                "Apple Music 拒绝了搜索请求，请稍后重试"
            } else {
                "Apple Music 搜索失败（HTTP ${response.code}）"
            }
            throw IllegalStateException(message)
        }
        return response.body
    }

    private fun doRequest(url: String): Response {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Origin", "https://music.apple.com")
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
            )
            connection.setRequestProperty("Authorization", "Bearer $developerToken")
            if (userToken.isNotEmpty()) {
                connection.setRequestProperty("Cookie", "media-user-token=$userToken")
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            return Response(code, body)
        } finally {
            connection.disconnect()
        }
    }

    private fun getText(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 25_000
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
            )
            if (connection.responseCode != 200) {
                throw IllegalStateException("连接 Apple Music 失败（HTTP ${connection.responseCode}）")
            }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private data class Response(val code: Int, val body: String)
}
