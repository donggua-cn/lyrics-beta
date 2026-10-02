package com.applemusic.android.lyrics

import org.json.JSONObject

/** 一个字/词的时间轴片段，以及它在整行文本里的字符区间（用于逐字染色）。 */
class LyricWord(
    val startMs: Long,
    val endMs: Long,
    val charStart: Int,
    val charEnd: Int
)

/** 一行歌词（带行级时间轴，可能还带逐字时间轴）。 */
class LyricLine(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    /** 逐字/逐词时间轴；为空表示这行只能整行高亮。 */
    val words: List<LyricWord> = emptyList(),
    /** Apple TTML 提供的对应翻译；没有时为空。 */
    val translation: String? = null,
    /** Apple TTML 行标识，可用于关联独立返回的译文。 */
    val key: String? = null,
    internal val isTranslationParagraph: Boolean = false
) {
    val isBlank: Boolean get() = text.isBlank()
    val hasWordTiming: Boolean get() = words.isNotEmpty()

    /**
     * [ms] 时刻这行已经「唱到」第几个字符 —— 逐字染色就靠它。
     *
     * 正在唱的那个字按时间比例推进，所以进度是连续增长而不是一格一格跳。
     */
    fun sungChars(ms: Long): Int {
        if (text.isEmpty()) return 0
        if (words.isEmpty()) {
            // 没有逐字信息：整行按行级时间轴处理（过半即视为已唱完）
            return when {
                ms >= endMs -> text.length
                ms <= startMs -> 0
                else -> if (ms - startMs >= (endMs - startMs) / 2) text.length else 0
            }
        }
        var n = 0
        for (w in words) {
            if (ms >= w.endMs) {
                n = w.charEnd
            } else if (ms >= w.startMs) {
                val span = (w.endMs - w.startMs).coerceAtLeast(1L)
                val frac = ((ms - w.startMs).toDouble() / span).coerceIn(0.0, 1.0)
                n = w.charStart + ((w.charEnd - w.charStart) * frac).toInt()
                break
            } else {
                break
            }
        }
        return n.coerceIn(0, text.length)
    }
}

/** 一首歌的歌词。 */
class Lyrics(
    val lines: List<LyricLine>,
    /** 是否带时间轴（false = 纯文本，只能整段展示）。 */
    val synced: Boolean,
    /** Apple 的 data[0].type，例如 syllable-lyrics / lyrics。 */
    val kind: String = ""
) {
    /** 二分找出 [ms] 时刻应该高亮哪一行；没有匹配返回 -1。 */
    fun indexAt(ms: Long): Int {
        if (!synced || lines.isEmpty()) return -1
        var lo = 0
        var hi = lines.size - 1
        var best = -1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (lines[mid].startMs <= ms) {
                best = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return best
    }

    /** 有逐字时间轴的行数 / 总行数。 */
    fun wordTimedLines(): Int = lines.count { it.hasWordTiming }

    /** 逐字片段总数。 */
    fun wordCount(): Int = lines.sumOf { it.words.size }
}

/**
 * Apple Music 歌词解析（支持逐行与逐字时间轴）。
 *
 * Apple 用 `l[lyrics]=ttml` 返回 **TTML**（W3C Timed Text）。当 `data[0].type` 是
 * `syllable-lyrics` 时，`<p>` 里面还会有**逐字/逐词的 `<span begin end>`**：
 * ```xml
 * <tt xmlns="http://www.w3.org/ns/ttml">
 *   <body><div>
 *     <p begin="00:00:12.345" end="00:00:15.678" itunes:key="L1">
 *       <span begin="00:00:12.345" end="00:00:12.900">天</span>
 *       <span begin="00:00:12.900" end="00:00:13.400">若</span>
 *       <span begin="00:00:13.400" end="00:00:15.678">有情</span>
 *     </p>
 *     <p begin="00:00:15.678" end="00:00:18.000"/>          <!-- 间奏空行 -->
 *   </div></body>
 * </tt>
 * ```
 * 本项目**两级都用**：行级时间轴决定高亮哪一行，字级时间轴决定这一行染到第几个字
 * （卡拉 OK 效果）。若某行拿不到字级时间轴，就退回整行高亮，不会出错。
 *
 * 拿不到 TTML 时退回 `attributes.lyrics`，它通常是 LRC 风格：
 * ```
 * [00:12.34] Line one
 * [00:15.67] Line two
 * ```
 * 若连时间戳都没有，就当作纯文本（[Lyrics.synced] = false）。
 */
object LyricsParser {

    /**
     * 一个正则同时吃下两种 `<p>`：
     *   - `<p ...>文本</p>` —— 普通歌词行
     *   - `<p .../>`        —— Apple 用它表示间奏空行，**可以和普通行混排**
     * 早期版本只在「一个普通 `<p>` 都没匹配到」时才去看自闭合形式，
     * 于是混排时整段间奏会被漏掉（歌词时间轴就错位了）。
     */
    private val P_RE = Regex(
        "<p\\b([^>]*?)(?:/>|>(.*?)</p>)",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    private val SPAN_RE = Regex(
        "<span\\b([^>]*?)>(.*?)</span>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    private val BEGIN_RE = Regex("begin\\s*=\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
    private val END_RE = Regex("end\\s*=\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
    private val KEY_RE = Regex("(?:[\\w.-]+:)?key\\s*=\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
    private val AGENT_RE = Regex(
        "<(?:[\\w.-]+:)?agent\\b([^>]*)/?>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val ID_RE = Regex("(?:xml:)?id\\s*=\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
    private val ROLE_RE = Regex("(?:[\\w.-]+:)?role\\s*=\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
    private val AGENT_REF_RE = Regex("(?:[\\w.-]+:)?agent\\s*=\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
    private val TRANSLATION_BLOCK_RE = Regex(
        "<translation\\b([^>]*)>(.*?)</translation>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val TEXT_FOR_RE = Regex(
        "<text\\b([^>]*)>(.*?)</text>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val FOR_RE = Regex("(?:[\\w.-]+:)?for\\s*=\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
    private val TYPE_RE = Regex("(?:[\\w.-]+:)?type\\s*=\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
    private val TAG_RE = Regex("<[^>]*>")
    private val BR_RE = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val LRC_RE = Regex("^\\[(\\d+):(\\d+(?:\\.\\d+)?)]\\s*(.*)$")

    /** 从歌词接口的原始 JSON 里解析。解析不出返回 null。 */
    fun parse(json: String): Lyrics? {
        val ttml: String
        val plain: String
        val kind: String
        try {
            val data = JSONObject(json).optJSONArray("data")
            if (data == null || data.length() == 0) return null
            val first = data.getJSONObject(0)
            kind = first.optString("type", "")
            val attrs = first.optJSONObject("attributes") ?: return null
            ttml = attrs.optString("ttmlLocalizations")
                .takeIf { it.isNotBlank() }
                ?: attrs.optString("ttml", "")
            plain = attrs.optString("lyrics", "")
        } catch (e: Throwable) {
            return null
        }

        if (ttml.isNotEmpty()) {
            parseTtml(ttml, kind)?.let { if (it.lines.isNotEmpty()) return it }
        }
        if (plain.isNotEmpty()) {
            return parsePlain(plain, kind)
        }
        return null
    }

    /** 解析 TTML。 */
    fun parseTtml(ttml: String, kind: String = ""): Lyrics? {
        val paragraphs = ArrayList<LyricLine>(64)
        val metadataTranslations = parseMetadataTranslations(ttml)
        val translationAgents = AGENT_RE.findAll(ttml).mapNotNull { match ->
            val attrs = match.groupValues[1]
            val id = ID_RE.find(attrs)?.groupValues?.get(1) ?: return@mapNotNull null
            val role = ROLE_RE.find(attrs)?.groupValues?.get(1).orEmpty()
            if (role.contains("translation", ignoreCase = true)) id else null
        }.toSet()

        for (m in P_RE.findAll(ttml)) {
            val attrs = m.groupValues[1]
            // 自闭合形式没有 group[2]（返回空串）
            val body = m.groupValues.getOrNull(2) ?: ""
            val begin = BEGIN_RE.find(attrs)?.groupValues?.get(1)?.let { parseTime(it) } ?: continue
            if (begin < 0) continue
            val end = END_RE.find(attrs)?.groupValues?.get(1)?.let { parseTime(it) } ?: begin
            val inlineTranslations = SPAN_RE.findAll(body)
                .filter { it.groupValues[1].contains("x-translation", ignoreCase = true) }
                .map { cleanText(it.groupValues[2]) }
                .filter { it.isNotBlank() }
                .toList()
            val originalBody = SPAN_RE.replace(body) { match ->
                if (match.groupValues[1].contains("x-translation", ignoreCase = true)) {
                    ""
                } else {
                    match.value
                }
            }
            val text = cleanText(originalBody)
            val words = parseWords(originalBody, text)
            paragraphs.add(
                LyricLine(
                    begin,
                    if (end > begin) end else begin,
                    text,
                    words,
                    translation = inlineTranslations.joinToString("\n").ifBlank { null },
                    key = KEY_RE.find(attrs)?.groupValues?.get(1),
                    isTranslationParagraph = attrs.contains("x-translation", ignoreCase = true) ||
                        AGENT_REF_RE.find(attrs)?.groupValues?.get(1) in translationAgents
                )
            )
        }

        val inferredTranslationIndices = HashSet<Int>()
        val unmarked = paragraphs.withIndex().filterNot { it.value.isTranslationParagraph }
        val groups = unmarked.groupBy { indexed ->
            indexed.value.key?.let { "key:$it" } ?: "time:${indexed.value.startMs}"
        }
        for (group in groups.values) {
            if (group.size < 2) continue
            val primaryIndex = group.firstOrNull { it.value.hasWordTiming }?.index
                ?: group.first().index
            group.filter { it.index != primaryIndex }.forEach {
                inferredTranslationIndices.add(it.index)
            }
        }
        val translationIndices = paragraphs.indices.filter {
            paragraphs[it].isTranslationParagraph || it in inferredTranslationIndices
        }.toSet()
        val original = paragraphs.filterIndexed { index, _ -> index !in translationIndices }
        if (original.isEmpty()) return null
        val translationsByKey = paragraphs
            .filterIndexed { index, _ -> index in translationIndices }
            .filter { !it.key.isNullOrBlank() }
            .associate { it.key!! to it.text }
            .toMutableMap()
        translationsByKey.putAll(metadataTranslations)
        val unmatchedTranslations = paragraphs.withIndex()
            .filter { it.index in translationIndices }
            .map { it.value }
            .filter { it.key.isNullOrBlank() && it.text.isNotBlank() }
            .toMutableList()
        val out = original.map { line ->
            val translatedText = line.key?.let(translationsByKey::get)
                ?.takeIf { it != line.text }
                ?: unmatchedTranslations.firstOrNull { it.startMs == line.startMs }?.text
            if (translatedText == null) {
                line
            } else {
                unmatchedTranslations.removeAll {
                    it.startMs == line.startMs && it.text == translatedText
                }
                LyricLine(
                    line.startMs,
                    line.endMs,
                    line.text,
                    line.words,
                    translatedText,
                    line.key
                )
            }
        }.sortedBy { it.startMs }
        return Lyrics(out, true, kind)
    }

    /**
     * Merge translation data from Apple's separate lyrics resource into the
     * word-timed lyric resource. Apple may return translations only on /lyrics.
     */
    fun mergeTranslations(original: Lyrics, translationJson: String): Lyrics {
        val attributes = try {
            JSONObject(translationJson).optJSONArray("data")
                ?.optJSONObject(0)?.optJSONObject("attributes")
        } catch (_: Exception) {
            null
        } ?: return original
        val ttml = attributes.optString("ttmlLocalizations")
            .takeIf { it.isNotBlank() }
            ?: attributes.optString("ttml").takeIf { it.isNotBlank() }
        val translationByKey = ttml?.let(::parseMetadataTranslations).orEmpty().toMutableMap()
        val candidates = ttml
            ?.let(::parseTtml)
            ?.lines
            ?.mapNotNull { line ->
                line.translation?.takeIf { it.isNotBlank() }?.let { translation ->
                    if (!line.key.isNullOrBlank()) translationByKey[line.key] = translation
                    LyricLine(line.startMs, line.endMs, translation, key = line.key)
                }
            }
            .orEmpty()
        val plainCandidates = if (ttml == null) {
            attributes.optString("lyrics")
                .takeIf { it.isNotBlank() }
                ?.let(::parsePlain)
                ?.lines
                ?.filter { it.text.isNotBlank() }
                .orEmpty()
        } else {
            emptyList()
        }
        if (translationByKey.isEmpty() && candidates.isEmpty() && plainCandidates.isEmpty()) {
            return original
        }

        val merged = original.lines.map { line ->
            if (!line.translation.isNullOrBlank()) return@map line
            val byKey = line.key?.let(translationByKey::get)
            val candidate = if (byKey == null) {
                (candidates + plainCandidates).firstOrNull { candidate ->
                    candidate.text != line.text &&
                        kotlin.math.abs(candidate.startMs - line.startMs) <= 1_500L
                }?.text
            } else {
                byKey
            }
            val translatedText = candidate?.takeIf { it != line.text } ?: return@map line
            LyricLine(
                line.startMs,
                line.endMs,
                line.text,
                line.words,
                translatedText,
                line.key
            )
        }
        return Lyrics(merged, original.synced, original.kind)
    }

    private fun parseMetadataTranslations(ttml: String): Map<String, String> {
        val translations = LinkedHashMap<String, String>()
        for (block in TRANSLATION_BLOCK_RE.findAll(ttml)) {
            val type = TYPE_RE.find(block.groupValues[1])?.groupValues?.get(1)
            if (!type.equals("subtitle", ignoreCase = true)) continue
            for (text in TEXT_FOR_RE.findAll(block.groupValues[2])) {
                val key = FOR_RE.find(text.groupValues[1])?.groupValues?.get(1)
                    ?: continue
                val value = cleanText(text.groupValues[2])
                if (value.isNotBlank()) translations[key] = value
            }
        }
        return translations
    }

    /**
     * 把 `<p>` 里的 `<span begin end>` 转成字符区间。
     *
     * 做法：先把整行清洗成 [lineText]（这是真正显示出来的文本），再按顺序在
     * [lineText] 里查找每个 span 的文本，记下字符区间。用「按顺序查找」而不是
     * 自己拼接，是为了让 span 之间原有的空格/标点仍然算进正确的字符位置。
     *
     * 任何一个 span 找不到对应位置，就放弃这一行的字级时间轴（返回空），
     * 该行退回整行高亮 —— 宁可不逐字，也不能染错位置。
     */
    private fun parseWords(body: String, lineText: String): List<LyricWord> {
        if (lineText.isEmpty()) return emptyList()
        val spans = SPAN_RE.findAll(body).toList()
        if (spans.isEmpty()) return emptyList()

        val out = ArrayList<LyricWord>(spans.size)
        var cursor = 0
        for (sp in spans) {
            val attrs = sp.groupValues[1]
            val inner = sp.groupValues[2]
            val start = BEGIN_RE.find(attrs)?.groupValues?.get(1)?.let { parseTime(it) } ?: continue
            if (start < 0) continue
            val end = END_RE.find(attrs)?.groupValues?.get(1)?.let { parseTime(it) } ?: start
            val wordText = cleanText(inner)
            if (wordText.isEmpty()) continue

            val at = lineText.indexOf(wordText, cursor)
            if (at < 0) {
                // 对不上就整行放弃字级时间轴
                return emptyList()
            }
            out.add(
                LyricWord(
                    start,
                    if (end > start) end else start,
                    at,
                    at + wordText.length
                )
            )
            cursor = at + wordText.length
        }
        return out
    }

    /** 解析 `attributes.lyrics`：优先当 LRC，退化成纯文本。 */
    fun parsePlain(plain: String, kind: String = ""): Lyrics? {
        val raw = plain.replace("\\n", "\n")
        val lines = raw.split('\n')
        val synced = ArrayList<LyricLine>(lines.size)
        var anyTimestamp = false
        for (ln in lines) {
            val t = ln.trim()
            if (t.isEmpty()) continue
            val m = LRC_RE.find(t)
            if (m != null) {
                anyTimestamp = true
                val min = m.groupValues[1].toLongOrNull() ?: 0L
                val sec = m.groupValues[2].toDoubleOrNull() ?: 0.0
                val ms = (min * 60_000L + (sec * 1000).toLong())
                synced.add(LyricLine(ms, ms, m.groupValues[3].trim()))
            } else {
                synced.add(LyricLine(-1L, -1L, t))
            }
        }
        if (synced.isEmpty()) return null
        if (anyTimestamp) {
            // 结束时间用下一行的开始补齐
            val fixed = ArrayList<LyricLine>(synced.size)
            for (i in synced.indices) {
                val cur = synced[i]
                val next = synced.getOrNull(i + 1)?.startMs ?: (cur.startMs + 5000)
                fixed.add(
                    LyricLine(
                        cur.startMs,
                        if (next > cur.startMs) next else cur.startMs + 5000,
                        cur.text
                    )
                )
            }
            return Lyrics(fixed, true, kind)
        }
        // 完全没有时间戳：整段纯文本
        return Lyrics(synced.map { LyricLine(-1L, -1L, it.text) }, false, kind)
    }

    /**
     * 解析 TTML 的时间：支持 `HH:MM:SS.mmm`、`MM:SS.mmm`、`SS.mmm`、纯秒数。
     * 解析不出返回 -1。
     */
    fun parseTime(s: String): Long {
        val t = s.trim()
        if (t.isEmpty()) return -1L
        // 纯数字 = 秒（可带小数）
        t.toDoubleOrNull()?.let { return (it * 1000).toLong() }
        val parts = t.split(':')
        if (parts.isEmpty() || parts.size > 3) return -1L
        var total = 0.0
        for (p in parts) {
            val v = p.toDoubleOrNull() ?: return -1L
            total = total * 60.0 + v
        }
        return (total * 1000).toLong()
    }

    /** 去掉标签、还原实体、压掉多余空白。 */
    private fun cleanText(html: String): String {
        var s = BR_RE.replace(html, " ")
        s = TAG_RE.replace(s, "")
        s = unescape(s)
        return s.replace(Regex("\\s+"), " ").trim()
    }

    fun unescape(s: String): String {
        if (!s.contains('&')) return s
        var out = s
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
        // 数字实体 &#123; / &#x1F600;
        out = Regex("&#(x?[0-9A-Fa-f]+);").replace(out) { m ->
            val body = m.groupValues[1]
            val code = try {
                if (body.startsWith("x") || body.startsWith("X")) body.substring(1).toInt(16)
                else body.toInt()
            } catch (_: Throwable) {
                return@replace m.value
            }
            if (code in 1..0x10FFFF) String(Character.toChars(code)) else m.value
        }
        return out
    }
}
