package com.applemusic.lyricstool.data

import android.util.Base64
import com.applemusic.android.lyrics.LyricLine
import com.applemusic.android.lyrics.LyricWord
import com.applemusic.android.lyrics.Lyrics
import com.applemusic.android.lyrics.LyricsParser
import com.applemusic.lyricstool.data.qq.DESHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.util.zip.InflaterInputStream

class QQMusicLyricsClient {
    private data class SearchMetadata(
        val title: String,
        val artist: String,
        val album: String
    )

    suspend fun searchSongs(term: String): List<SongHit> = withContext(Dispatchers.IO) {
        parseSongSearch(requestSearch(term, searchType = 0))
    }

    suspend fun searchSongsByLyrics(term: String): List<SongHit> = withContext(Dispatchers.IO) {
        val root = requestSearch(term, searchType = 7)
        if (root.optInt("code", 0) != 0) {
            throw IllegalStateException("QQ 音乐歌词搜索失败（错误码 ${root.optInt("code")}）")
        }
        val songs = root.optJSONObject("data")
            ?.optJSONObject("lyric")
            ?.optJSONArray("list")
            ?: return@withContext emptyList()

        buildList {
            for (index in 0 until songs.length()) {
                val item = songs.optJSONObject(index) ?: continue
                val mid = item.optString("songmid").takeIf { it.isNotBlank() } ?: continue
                val title = item.optString("songname").trim()
                if (title.isBlank()) continue
                val singer = item.optString("singer")
                    .takeIf { it.isNotBlank() }
                    ?: item.optString("content")
                        .lineSequence()
                        .firstOrNull()
                        ?.let { line ->
                            Regex("""^\s*.+?\s+-\s+(.+?)\s*(?:\([^)]*\))?\s*$""")
                                .find(line)?.groupValues?.get(1)
                                ?.replace(Regex("""\s*\([^)]*\)\s*$"""), "")
                        }
                        .orEmpty()
                val albumMid = item.optString("albummid")
                add(
                    SongHit(
                        id = mid,
                        title = title,
                        artist = singer.ifBlank { "未知艺人" },
                        album = item.optString("albumname"),
                        artworkUrl = albumMid.takeIf { it.isNotBlank() }?.let {
                            "https://y.gtimg.cn/music/photo_new/T002R300x300M000$it.jpg"
                        }.orEmpty(),
                        durationMs = item.optLong("interval") * 1000L,
                        providerSongId = item.optString("songid").takeIf { it.isNotBlank() }
                    )
                )
            }
        }.distinctBy { it.id }
    }

    private fun requestSearch(term: String, searchType: Int): JSONObject {
        val encodedTerm = URLEncoder.encode(term, Charsets.UTF_8.name())
        val pageSize = if (searchType == 7) 10 else 30
        val root = JSONObject(
            request("$SEARCH_URL?format=json&p=1&n=$pageSize&w=$encodedTerm&t=$searchType")
        )
        if (root.optInt("code", 0) != 0) {
            throw IllegalStateException("QQ 音乐搜索失败（错误码 ${root.optInt("code")}）")
        }
        return root
    }

    private fun parseSongSearch(root: JSONObject): List<SongHit> {
        val songs = root.optJSONObject("data")
            ?.optJSONObject("song")
            ?.optJSONArray("list")
            ?: return emptyList()
        return buildList {
            for (index in 0 until songs.length()) {
                val song = songs.optJSONObject(index) ?: continue
                val mid = song.optString("songmid").takeIf { it.isNotBlank() } ?: continue
                val singers = song.optJSONArray("singer")
                val artist = (0 until (singers?.length() ?: 0))
                    .mapNotNull { singers?.optJSONObject(it)?.optString("name") }
                    .filter { it.isNotBlank() }
                    .joinToString(" / ")
                    .ifBlank { "未知艺人" }
                val albumMid = song.optString("albummid")
                add(
                    SongHit(
                        id = mid,
                        title = song.optString("songname", "未知歌曲"),
                        artist = artist,
                        album = song.optString("albumname"),
                        artworkUrl = albumMid.takeIf { it.isNotBlank() }?.let {
                            "https://y.gtimg.cn/music/photo_new/T002R300x300M000$it.jpg"
                        }.orEmpty(),
                        durationMs = song.optLong("interval") * 1000L,
                        providerSongId = song.optString("songid").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }

    private suspend fun fetchLyrics(song: SongHit): FetchedLyrics? = withContext(Dispatchers.IO) {
        fetchModernLyrics(song)
    }

    suspend fun fetchMatchingLyrics(
        title: String,
        artist: String,
        durationMs: Long,
        album: String = "",
        localizedAliases: List<SongHit> = emptyList()
    ): FetchedLyrics? = withContext(Dispatchers.IO) {
        val normalizedTitle = normalizeMatchText(title)
        if (normalizedTitle.isBlank()) return@withContext null
        val metadata = (listOf(SearchMetadata(title, artist, album)) +
            localizedAliases.map { SearchMetadata(it.title, it.artist, it.album) }).distinctBy {
            listOf(it.title, it.artist, it.album).joinToString("|") { value ->
                normalizeMatchText(value)
            }
        }
        val titles = metadata.map { normalizeMatchText(it.title) }.filter { it.isNotBlank() }
        val artistGroups = metadata.map { splitArtists(it.artist) }.filter { it.isNotEmpty() }
        val albums = metadata.map { normalizeMatchText(it.album) }.filter { it.isNotBlank() }
        var firstError: Exception? = null
        val triedIds = HashSet<String>()
        var lineTimedFallback: FetchedLyrics? = null
        val queries = metadata.flatMap { listOf(it.title, "${it.title} ${it.artist}") }
            .distinctBy(::normalizeMatchText)

        for ((queryIndex, query) in queries.withIndex()) {
            val matches = try {
                searchSongs(query)
                    .mapIndexedNotNull { rank, candidate ->
                        rankCandidate(
                            candidate = candidate,
                            rank = rank,
                            queryIndex = queryIndex,
                            titles = titles,
                            artistGroups = artistGroups,
                            albums = albums,
                            durationMs = durationMs
                        )
                    }
                    .sortedWith(
                        compareByDescending<RankedCandidate> { it.score }
                            .thenBy { it.durationDifferenceMs }
                            .thenBy { it.rank }
                    )
            } catch (error: Exception) {
                if (firstError == null) firstError = error
                emptyList()
            }

            for (match in matches.take(3)) {
                if (!triedIds.add(match.song.id)) continue
                try {
                    fetchLyrics(match.song)?.let { lyrics ->
                        if (lyrics.lyrics.wordTimedLines() > 0) return@withContext lyrics
                        if (lineTimedFallback == null) lineTimedFallback = lyrics
                    }
                } catch (error: Exception) {
                    if (firstError == null) firstError = error
                }
            }
        }
        lineTimedFallback?.let { return@withContext it }
        firstError?.let { throw it }
        null
    }

    private fun rankCandidate(
        candidate: SongHit,
        rank: Int,
        queryIndex: Int,
        titles: List<String>,
        artistGroups: List<List<String>>,
        albums: List<String>,
        durationMs: Long
    ): RankedCandidate? {
        val candidateTitle = normalizeMatchText(candidate.title)
        val titleMatches = candidateTitle.isNotBlank() && titles.any { title ->
            candidateTitle == title ||
                candidateTitle.contains(title) || title.contains(candidateTitle)
        }
        val candidateArtists = splitArtists(candidate.artist)
        val artistMatches = artistGroups.any { artists ->
            candidateArtists.any { candidateArtist ->
                artists.any {
                    it == candidateArtist ||
                        it.contains(candidateArtist) ||
                        candidateArtist.contains(it)
                }
            }
        }
        val candidateAlbum = normalizeMatchText(candidate.album)
        val albumMatches = candidateAlbum.isNotBlank() && albums.any { album ->
            album == candidateAlbum ||
                album.contains(candidateAlbum) ||
                candidateAlbum.contains(album)
        }
        val durationDifference = if (durationMs > 0L && candidate.durationMs > 0L) {
            kotlin.math.abs(candidate.durationMs - durationMs)
        } else {
            Long.MAX_VALUE
        }
        if (durationDifference != Long.MAX_VALUE && durationDifference > 45_000L) return null

        val durationIsVeryClose = durationDifference <= CROSS_LANGUAGE_DURATION_TOLERANCE_MS
        val artistAndDurationMatch = !titleMatches &&
            artistMatches &&
            durationDifference <= TITLE_ONLY_DURATION_TOLERANCE_MS
        val crossLanguageTopResult = !titleMatches &&
            !artistMatches &&
            rank == 0 &&
            durationIsVeryClose
        val albumAndDurationMatch = albumMatches &&
            durationDifference <= ALBUM_MATCH_DURATION_TOLERANCE_MS
        if (!titleMatches && !artistAndDurationMatch &&
            !albumAndDurationMatch && !crossLanguageTopResult
        ) {
            return null
        }
        if (titleMatches && artistGroups.isNotEmpty() && !artistMatches &&
            durationDifference > TITLE_ONLY_DURATION_TOLERANCE_MS
        ) {
            return null
        }

        val score = (if (titleMatches) 100 else 0) +
            (if (artistMatches && artistGroups.isNotEmpty()) 45 else 0) +
            (if (albumMatches) 25 else 0) +
            when {
                durationDifference <= 1_500L -> 45
                durationDifference <= 5_000L -> 35
                durationDifference <= 15_000L -> 20
                durationDifference <= 45_000L -> 5
                else -> 0
            } +
            maxOf(0, 12 - rank * 2) -
            queryIndex * 5

        return RankedCandidate(
            song = candidate,
            score = score,
            durationDifferenceMs = durationDifference,
            rank = rank
        )
    }

    private fun normalizeMatchText(text: String): String =
        text.lowercase()
            .replace(Regex("""\([^)]*\)"""), "")
            .replace(Regex("""\[[\s\S]*?\]"""), "")
            .replace(Regex("""\{[^}]*\}"""), "")
            .replace(Regex("""[\p{P}\p{S}\s]+"""), "")

    private fun splitArtists(text: String): List<String> =
        Regex("[,/&、，;]+")
            .split(text)
            .map(::normalizeMatchText)
            .filter { it.length > 1 }

    private data class RankedCandidate(
        val song: SongHit,
        val score: Int,
        val durationDifferenceMs: Long,
        val rank: Int
    )

    private fun fetchModernLyrics(song: SongHit): FetchedLyrics? {
        fun base64(value: String): String =
            Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

        val param = JSONObject()
            .put("songMID", song.id)
            .put("songName", base64(song.title))
            .put("albumName", base64(song.album))
            .put("singerName", base64(song.artist))
            .put("crypt", 1)
            .put("qrc", 1)
            .put("trans", 1)
            .put("roma", 1)
            .put("cv", 2111)
            .put("ct", 19)
            .put("lrc_t", 0)
            .put("qrc_t", 0)
            .put("roma_t", 0)
            .put("trans_t", 0)
            .put("type", 0)
            .put("interval", song.durationMs / 1000L)
        song.providerSongId?.toLongOrNull()?.let { param.put("songID", it) }
        val request = JSONObject()
            .put(
                "comm",
                JSONObject()
                    .put("ct", 11)
                    .put("cv", 1003006)
                    .put("v", 1003006)
                    .put("format", "json")
                    .put("tmeAppID", "qqmusiclight")
                    .put("uin", "0")
            )
            .put(
                "req_0",
                JSONObject()
                    .put("module", "music.musichallSong.PlayLyricInfo")
                    .put("method", "GetPlayLyricInfo")
                    .put("param", param)
            )
        val root = JSONObject(postJson(request.toString()))
        val response = root.optJSONObject("req_0")
            ?: throw IllegalStateException("QQ 音乐歌词接口返回了无效数据")
        if (response.optInt("code", 0) != 0) {
            throw IllegalStateException("QQ 音乐暂时无法提供这首歌的歌词（错误码 ${response.optInt("code")}）")
        }
        val data = response.optJSONObject("data")
            ?: throw IllegalStateException("QQ 音乐歌词接口没有返回歌词数据")
        val originalPayload = firstPayload(data, "lyric") ?: return null
        val original = decryptQrc(originalPayload)
        val lyrics = parseLyrics(original, "qq-qrc") ?: return null

        var translationWarning: String? = null
        val translation = firstPayload(data, "trans")?.let { payload ->
            try {
                decryptQrc(payload)
            } catch (error: IllegalStateException) {
                translationWarning = error.message ?: "QQ 音乐译文无法解码"
                null
            }
        }
        val translatedLyrics = translation?.let { parseLyrics(it, "qq-translation") }
        val romanization = firstPayload(data, "roma")?.let { payload ->
            try {
                decryptQrc(payload)
            } catch (error: IllegalStateException) {
                null
            }
        }
        return FetchedLyrics(
            lyrics = mergeQqTranslations(lyrics, translatedLyrics),
            ttml = null,
            translationWarning = translationWarning,
            qqQrcDebugText = buildQrcDebugText(
                "musicu.fcg GetPlayLyricInfo",
                original,
                translation,
                romanization
            )
        )
    }

    private fun buildQrcDebugText(
        endpoint: String,
        original: String?,
        translation: String?,
        romanization: String?
    ): String? {
        if (original.isNullOrBlank() && translation.isNullOrBlank() && romanization.isNullOrBlank()) {
            return null
        }
        return buildString {
            appendLine("QQ Music decrypted lyric response")
            appendLine("Endpoint: $endpoint")
            appendLine("These are decoded source payloads, not generated LRC/TTML.")
            appendLine()
            appendLine("===== ORIGINAL QRC / LYRIC =====")
            appendLine(original.orEmpty())
            appendLine()
            appendLine("===== TRANSLATION QRC / TRANS =====")
            appendLine(translation.orEmpty())
            appendLine()
            appendLine("===== ROMANIZATION QRC / ROMA =====")
            appendLine(romanization.orEmpty())
        }
    }

    private fun mergeQqTranslations(original: Lyrics, translated: Lyrics?): Lyrics {
        if (translated == null) return original
        val translatedLines = translated.lines.sortedBy { it.startMs }
        val usedTranslations = BooleanArray(translatedLines.size)
        val mergedLines = original.lines.map { line ->
            val matchIndex = translatedLines.indices
                .asSequence()
                .filter { index ->
                    !usedTranslations[index] &&
                        kotlin.math.abs(translatedLines[index].startMs - line.startMs) <=
                        TRANSLATION_TIME_TOLERANCE_MS
                }
                .minWithOrNull(
                    compareBy<Int> { index ->
                        kotlin.math.abs(translatedLines[index].startMs - line.startMs)
                    }.thenBy { index -> translatedLines[index].startMs }
                )
            val translation = matchIndex?.let { index ->
                usedTranslations[index] = true
                translatedLines[index].text
            }
            LyricLine(
                line.startMs,
                line.endMs,
                line.text,
                line.words,
                translation = translation?.takeIf { it.isNotBlank() },
                key = line.key
            )
        }
        return Lyrics(mergedLines, original.synced, original.kind)
    }

    private fun firstPayload(data: JSONObject, vararg keys: String): String? =
        keys.asSequence()
            .map { data.optString(it).trim() }
            .firstOrNull { it.isNotEmpty() && it != "0" && it != "null" }

    private fun decryptQrc(hex: String): String {
        val digits = hex.filterNot(Char::isWhitespace)
        if (digits.isEmpty() || digits.length % 16 != 0 || digits.any { it.digitToIntOrNull(16) == null }) {
            throw IllegalStateException("QQ 音乐 QRC 密文格式无效")
        }
        val encrypted = ByteArray(digits.length / 2) { index ->
            digits.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
        val schedule = Array(3) { Array(16) { ByteArray(6) } }
        DESHelper.tripleDESKeySetup(
            "!@#)(*$%123ZXC!@!@#)(NHL".toByteArray(Charsets.US_ASCII),
            schedule,
            DESHelper.DECRYPT
        )
        val decrypted = ByteArray(encrypted.size)
        var offset = 0
        while (offset < encrypted.size) {
            val block = encrypted.copyOfRange(offset, offset + 8)
            DESHelper.tripleDESCrypt(block, block, schedule)
            block.copyInto(decrypted, offset)
            offset += 8
        }
        val output = ByteArrayOutputStream()
        try {
            InflaterInputStream(ByteArrayInputStream(decrypted)).use { inflater ->
                val buffer = ByteArray(4096)
                while (true) {
                    val count = inflater.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > MAX_DECOMPRESSED_BYTES) {
                        throw IllegalStateException("QQ 音乐歌词解压后过大")
                    }
                    output.write(buffer, 0, count)
                }
            }
        } catch (error: Exception) {
            throw IllegalStateException("QQ 音乐 QRC 歌词解密失败", error)
        }
        val decryptedText = output.toByteArray().toString(Charsets.UTF_8).trimStart('\uFEFF')
        val xmlLyric = Regex(
            """<Lyric_1\b[^>]*\bLyricContent="(.*?)"[^>]*/?>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).find(decryptedText)?.groupValues?.get(1)
        return xmlLyric?.let(::decodeXmlAttribute) ?: decryptedText
    }

    private fun parseLyrics(content: String, kind: String): Lyrics? {
        val linePattern = Regex("""^\[(\d+),(\d+)](.*)$""")
        if (content.lineSequence().none { linePattern.matches(it.trim()) }) {
            return LyricsParser.parsePlain(content, kind)
        }
        val wordPattern = Regex("""\((\d+),(\d+)\)""")
        val lines = content.lineSequence().mapNotNull { rawLine ->
            val match = linePattern.matchEntire(rawLine.trim()) ?: return@mapNotNull null
            val lineStart = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val lineDuration = match.groupValues[2].toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            val body = match.groupValues[3]
            val text = StringBuilder()
            val words = ArrayList<LyricWord>()
            var cursor = 0
            for (marker in wordPattern.findAll(body)) {
                val segment = body.substring(cursor, marker.range.first)
                val charStart = text.length
                text.append(segment)
                val trimmedStart = segment.indexOfFirst { !it.isWhitespace() }
                val trimmedEnd = segment.indexOfLast { !it.isWhitespace() }
                val startMs = marker.groupValues[1].toLongOrNull() ?: lineStart
                val durationMs = marker.groupValues[2].toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                if (trimmedStart >= 0) {
                    words.add(
                        LyricWord(
                            startMs,
                            startMs + durationMs,
                            charStart + trimmedStart,
                            charStart + trimmedEnd + 1
                        )
                    )
                }
                cursor = marker.range.last + 1
            }
            text.append(body.substring(cursor))
            val lyricText = text.toString().trim()
            if (lyricText.isBlank()) return@mapNotNull null
            val trimStart = text.indexOf(lyricText)
            val trimEnd = trimStart + lyricText.length
            val adjustedWords = words.mapNotNull { word ->
                val start = maxOf(word.charStart, trimStart)
                val end = minOf(word.charEnd, trimEnd)
                if (end <= start) null else {
                    LyricWord(word.startMs, word.endMs, start - trimStart, end - trimStart)
                }
            }
            LyricLine(
                lineStart,
                lineStart + lineDuration,
                lyricText,
                adjustedWords
            )
        }.toList()
        return if (lines.isEmpty()) LyricsParser.parsePlain(content, kind)
        else Lyrics(lines, synced = true, kind = kind)
    }

    private fun decodeXmlAttribute(value: String): String {
        val namedEntities = LyricsParser.unescape(value)
        return Regex("""&#(x[0-9a-fA-F]+|\d+);""").replace(namedEntities) { match ->
            val entity = match.groupValues[1]
            val codePoint = if (entity.startsWith('x', ignoreCase = true)) {
                entity.drop(1).toIntOrNull(16)
            } else {
                entity.toIntOrNull()
            }
            codePoint?.takeIf(Character::isValidCodePoint)?.let(Character::toChars)?.concatToString()
                ?: match.value
        }
    }

    private fun postJson(body: String): String {
        return request(API_URL, body)
    }

    private fun request(url: String, body: String? = null): String {
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            if (body == null) {
                connection.requestMethod = "GET"
            } else {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            connection.setRequestProperty("Referer", "https://y.qq.com/")
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
            )
            if (body != null) {
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IllegalStateException("QQ 音乐请求失败（HTTP $code）")
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val API_URL = "https://u.y.qq.com/cgi-bin/musicu.fcg"
        const val SEARCH_URL = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp"
        const val MAX_DECOMPRESSED_BYTES = 8 * 1024 * 1024
        const val TRANSLATION_TIME_TOLERANCE_MS = 1_000L
        const val CROSS_LANGUAGE_DURATION_TOLERANCE_MS = 2_500L
        const val ALBUM_MATCH_DURATION_TOLERANCE_MS = 5_000L
        const val TITLE_ONLY_DURATION_TOLERANCE_MS = 5_000L
    }
}
