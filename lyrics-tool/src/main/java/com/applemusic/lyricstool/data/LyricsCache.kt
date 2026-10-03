package com.applemusic.lyricstool.data

import android.content.Context
import com.applemusic.android.lyrics.LyricLine
import com.applemusic.android.lyrics.LyricWord
import com.applemusic.android.lyrics.Lyrics
import com.applemusic.android.util.Logx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

class LyricsCache(context: Context) {
    private val directory = File(context.filesDir, "lyrics-cache")

    data class CacheItem(
        val songId: String,
        val sources: Set<String>,
        val lastAccess: Long
    )

    suspend fun get(source: String, songId: String): FetchedLyrics? =
        withContext(Dispatchers.IO) {
            val file = cacheFile(source, songId)
            if (!file.isFile) return@withContext null

            val json: JSONObject
            val fetched: FetchedLyrics
            try {
                json = JSONObject(file.readText(Charsets.UTF_8))
                fetched = decode(json)
            } catch (error: Exception) {
                Logx.w(TAG, "歌词缓存读取失败 $source/$songId：${error.message}")
                if (!file.delete() && file.exists()) {
                    Logx.e(TAG, "无法删除损坏的歌词缓存：${file.name}")
                }
                return@withContext null
            }
            file.setLastModified(System.currentTimeMillis())
            if (json.optString("songId").isBlank() ||
                json.optString("source").isBlank()
            ) {
                try {
                    file.writeText(encode(source, songId, fetched), Charsets.UTF_8)
                } catch (error: Exception) {
                    Logx.w(TAG, "歌词缓存关联信息写入失败 $source/$songId：${error.message}")
                }
            }
            fetched
        }

    suspend fun put(source: String, songId: String, lyrics: FetchedLyrics) =
        withContext(Dispatchers.IO) {
            val target = cacheFile(source, songId)
            val temporary = File(directory, "${target.name}.${UUID.randomUUID()}.tmp")
            try {
                if (!directory.exists() && !directory.mkdirs()) {
                    throw java.io.IOException("无法创建歌词缓存目录")
                }
                FileOutputStream(temporary).use { output ->
                    output.write(encode(source, songId, lyrics).toByteArray(Charsets.UTF_8))
                    output.fd.sync()
                }
                if (!temporary.renameTo(target)) {
                    throw java.io.IOException("无法保存歌词缓存文件")
                }
                trimOldEntries()
            } catch (error: Exception) {
                Logx.w(TAG, "歌词缓存写入失败 $source/$songId：${error.message}")
                if (temporary.exists() && !temporary.delete()) {
                    Logx.e(TAG, "无法清理临时歌词缓存：${temporary.name}")
                }
            }
        }

    suspend fun cachedItems(): List<CacheItem> = withContext(Dispatchers.IO) {
        directory.listFiles()
            ?.asSequence()
            ?.filter { it.isFile && it.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    val json = JSONObject(file.readText(Charsets.UTF_8))
                    val songId = json.optString("songId").takeIf(String::isNotBlank)
                        ?: return@mapNotNull null
                    val source = json.optString("source").takeIf(String::isNotBlank)
                        ?: return@mapNotNull null
                    CacheItem(songId, setOf(source), file.lastModified())
                } catch (error: Exception) {
                    Logx.w(TAG, "歌词缓存索引读取失败 ${file.name}：${error.message}")
                    null
                }
            }
            ?.groupBy(CacheItem::songId)
            ?.map { (songId, entries) ->
                CacheItem(
                    songId = songId,
                    sources = entries.flatMap(CacheItem::sources).toSet(),
                    lastAccess = entries.maxOf(CacheItem::lastAccess)
                )
            }
            ?.sortedByDescending(CacheItem::lastAccess)
            .orEmpty()
    }

    suspend fun removeForSong(songId: String): Int = withContext(Dispatchers.IO) {
        var removed = 0
        for (source in SOURCES) {
            val file = cacheFile(source, songId)
            if (file.exists()) {
                if (!file.delete()) throw java.io.IOException("无法删除歌词缓存文件：${file.name}")
                removed++
            }
        }
        removed
    }

    suspend fun clearAll(): Int = withContext(Dispatchers.IO) {
        var removed = 0
        var failure: java.io.IOException? = null
        directory.listFiles()?.forEach { file ->
            if (file.isFile) {
                if (file.delete() || !file.exists()) {
                    removed++
                } else {
                    val error = java.io.IOException("无法删除歌词缓存文件：${file.name}")
                    if (failure == null) failure = error else failure?.addSuppressed(error)
                }
            }
        }
        failure?.let { throw it }
        removed
    }

    private fun cacheFile(source: String, songId: String): File {
        require(source == "apple" || source == "qq") { "不支持的歌词缓存来源：$source" }
        require(songId.isNotBlank()) { "歌词缓存歌曲 ID 不能为空" }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$source:$songId".toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
        return File(directory, "$digest.json")
    }

    private fun encode(source: String, songId: String, fetched: FetchedLyrics): String {
        val lines = JSONArray()
        fetched.lyrics.lines.forEach { line ->
            val words = JSONArray()
            line.words.forEach { word ->
                words.put(
                    JSONObject()
                        .put("start", word.startMs)
                        .put("end", word.endMs)
                        .put("charStart", word.charStart)
                        .put("charEnd", word.charEnd)
                )
            }
            lines.put(
                JSONObject()
                    .put("start", line.startMs)
                    .put("end", line.endMs)
                    .put("text", line.text)
                    .put("translation", line.translation ?: JSONObject.NULL)
                    .put("key", line.key ?: JSONObject.NULL)
                    .put("words", words)
            )
        }
        return JSONObject()
            .put("version", SCHEMA_VERSION)
            .put("source", source)
            .put("songId", songId)
            .put("synced", fetched.lyrics.synced)
            .put("kind", fetched.lyrics.kind)
            .put("lines", lines)
            .put("ttml", fetched.ttml ?: JSONObject.NULL)
            .put("translationWarning", fetched.translationWarning ?: JSONObject.NULL)
            .put("qqQrcDebugText", fetched.qqQrcDebugText ?: JSONObject.NULL)
            .toString()
    }

    private fun decode(json: JSONObject): FetchedLyrics {
        require(json.optInt("version") == SCHEMA_VERSION) { "缓存格式版本不匹配" }
        val cachedLines = json.getJSONArray("lines")
        val lines = List(cachedLines.length()) { index ->
            val line = cachedLines.getJSONObject(index)
            val cachedWords = line.getJSONArray("words")
            val words = List(cachedWords.length()) { wordIndex ->
                val word = cachedWords.getJSONObject(wordIndex)
                LyricWord(
                    startMs = word.getLong("start"),
                    endMs = word.getLong("end"),
                    charStart = word.getInt("charStart"),
                    charEnd = word.getInt("charEnd")
                )
            }
            LyricLine(
                startMs = line.getLong("start"),
                endMs = line.getLong("end"),
                text = line.getString("text"),
                words = words,
                translation = line.optionalString("translation"),
                key = line.optionalString("key")
            )
        }
        return FetchedLyrics(
            lyrics = Lyrics(lines, json.getBoolean("synced"), json.optString("kind")),
            ttml = json.optionalString("ttml"),
            translationWarning = json.optionalString("translationWarning"),
            qqQrcDebugText = json.optionalString("qqQrcDebugText")
        )
    }

    private fun JSONObject.optionalString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)

    private fun trimOldEntries() {
        val entries = directory.listFiles()
            ?.filter { it.isFile && it.extension == "json" }
            ?.sortedByDescending(File::lastModified)
            ?: return
        entries.drop(MAX_ENTRIES).forEach { file ->
            if (!file.delete() && file.exists()) {
                Logx.w(TAG, "无法清理过期歌词缓存：${file.name}")
            }
        }
    }

    private companion object {
        const val TAG = "LyricsCache"
        const val SCHEMA_VERSION = 1
        const val MAX_ENTRIES = 500
        val SOURCES = listOf("apple", "qq")
    }
}
