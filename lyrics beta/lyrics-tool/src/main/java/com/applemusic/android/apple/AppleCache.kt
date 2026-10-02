package com.applemusic.android.apple

import android.content.Context
import com.applemusic.android.util.Logx
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Apple Music 播放缓存 —— 对应鸿蒙版 `AppleCache.ets`。
 *
 * 策略（与原实现一致）：
 *   - 解密后的音频落在 App 私有 cacheDir/apple_music/<id>.m4a
 *   - 上限 3GB，超出按 LRU 淘汰到 90%（pinned 的不动）
 *   - 索引持久化到 SharedPreferences（JSON），重启后仍能命中
 *   - 小于 128KB 的文件视为损坏（半截文件）直接作废
 *
 * 【关键】`ensure()` 是 single-flight：同一首歌的并发调用共享同一个下载任务，
 * 从根上避免两个下载器同时写同一个文件写出残缺的 m4a —— 这是原项目
 * 「点了歌有时播、有时等很久都不播」的根因修复。
 */
class AppleCache(private val context: Context, private val api: AppleApi) {

    companion object {
        private const val TAG = "AppleCache"
        private const val PREF = "apple_music_cache"
        private const val KEY_INDEX = "index_json"
        private const val KEY_SCHEMA = "schema_version"
        /**
         * 缓存格式版本：不一致就把整个缓存清掉。
         *
         * v1 → v2：v1 的缓存文件里还留着 stsd 采样条目的 CENC 加密标识（sinf / enca），
         * 安卓的 MediaCodec 会因此拒绝播放（queuing secure buffer without mCrypto）。
         * v2 → v3：v2 那一版清理逻辑本身有 bug（偏移算错），写出来的文件**依然带
         * `enca`/`sinf`**，所以必须再清一次。
         * v3 → v4：v3 那一版删了 `sinf` 的字节，但**没有同步 `stsd`/`stbl`/`minf`/`mdia`/
         * `trak`/`moov` 的长度**，写出来的文件顶层看着连续、内部却多算了字节，
         * ExoPlayer 会报 `ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE`。
         * v4 → v5：v4 写出的还是**分片 MP4**，`mvhd.duration=0` 且没有 `mehd`/`sidx`，
         * ExoPlayer 报 `duration=TIME_UNSET`、`seekable=false` ——
         * 现象是「时长显示不出来、进度条一拖就跳回开头」。
         * v5 起统一重封装成普通 MP4。
         *
         * 光靠版本号不够 —— 见 [looksPlayable]：现在每次命中缓存都会递归校验
         * MP4 结构 + 检查 DRM 标识，不合格的一律作废重下。
         */
        private const val CACHE_SCHEMA = 5

        /**
         * 缓存文件的「健康检查」。
         *
         * 音频已经被我们解成明文，所以缓存文件必须满足：
         *  1. 是 MP4（有 `ftyp`），且**顶层 box 严格连续铺满整个文件** ——
         *     这一条能抓住「删了 sinf 字节但没同步祖先长度」这类结构损坏。
         *     损坏文件会让 ExoPlayer 报 `ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE`；
         *  2. 不含任何 CENC 加密标识（`enca`/`enct`/`sinf`）—— 否则 MediaCodec
         *     会要求安全缓冲区（`queuing secure buffer without mCrypto`）。
         *
         * 以前只靠「缓存格式版本号」来清理，但版本号跟着代码版本走：只要某一版
         * 清理逻辑写错、又恰好升过版本号，坏文件就会一直躺在缓存里被命中。
         * 加了这个自检之后，结构损坏或带 DRM 标识的缓存都会被丢掉重下。
         */
        private fun looksPlayable(file: java.io.File): Boolean {
            return try {
                val len = file.length()
                if (len < MIN_VALID_BYTES) return false
                // moov 一定在文件头部；读前 64KB 足够覆盖 ftyp + moov
                val head = ByteArray(minOf(64 * 1024L, len).toInt())
                java.io.RandomAccessFile(file, "r").use { raf -> raf.readFully(head) }
                val s = String(head, Charsets.ISO_8859_1)
                if (!s.contains("ftyp")) return false
                if (s.contains("enca") || s.contains("enct")) return false
                if (s.contains("sinf")) return false
                // 【v5 起】输出必须是**普通 MP4**。旧版写出的是分片 MP4（含 moof），
                // 它 mvhd.duration=0 且没有 mehd/sidx，ExoPlayer 会报
                // duration=TIME_UNSET / seekable=false —— 时长显示不出来、进度条拖不动。
                // 这里直接把分片文件判为不合格，保证旧缓存一定会被重下。
                if (s.contains("moof")) return false

                // 找到 moov 并**递归校验它内部每一层**的长度是否自洽。
                // 【关键】只检查顶层连续是不够的：真机上出过「删了 sinf 的 80 字节，
                // 但 stsd/stbl/minf/mdia/trak/moov 的长度没跟着减」这种损坏 ——
                // 顶层看着连续（moov 后面还有别的 box），但 moov 内部多算了 80 字节，
                // ExoPlayer 照样报 ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE。
                var off = 0
                var guard = 0
                while (off + 8 <= head.size && guard < 64) {
                    val size = be32(head, off)
                    if (size < 8) return false
                    val isMoov = head[off + 4] == 'm'.code.toByte() && head[off + 5] == 'o'.code.toByte() &&
                            head[off + 6] == 'o'.code.toByte() && head[off + 7] == 'v'.code.toByte()
                    if (isMoov) {
                        if (off + size > len) return false
                        return treeConsistent(head, off + 8, minOf(off + size, head.size), 0)
                    }
                    off += size
                    guard++
                }
                false
            } catch (_: Throwable) {
                false
            }
        }

        /** 递归校验一个范围内同级 box 是否严格铺满（返回 false 说明结构损坏）。 */
        private fun treeConsistent(d: ByteArray, start: Int, end: Int, depth: Int): Boolean {
            if (depth > 8) return true
            var off = start
            while (off + 8 <= end) {
                val size = be32(d, off)
                if (size < 8) return false
                if (off + size > end) return false
                val type = String(d, off + 4, 4, Charsets.ISO_8859_1)
                if (CONTAINER_TYPES.contains(type)) {
                    val childStart = if (type == "stsd") off + 16 else off + 8
                    if (!treeConsistent(d, childStart, off + size, depth + 1)) return false
                }
                off += size
            }
            return off == end
        }

        private val CONTAINER_TYPES = setOf(
            "moov", "trak", "mdia", "minf", "stbl", "mvex", "edts", "dinf", "udta", "stsd"
        )

        private fun be32(d: ByteArray, off: Int): Int {
            if (off < 0 || off + 4 > d.size) return -1
            return ((d[off].toInt() and 0xFF) shl 24) or
                    ((d[off + 1].toInt() and 0xFF) shl 16) or
                    ((d[off + 2].toInt() and 0xFF) shl 8) or
                    (d[off + 3].toInt() and 0xFF)
        }
        private const val MAX_BYTES = 3L * 1024 * 1024 * 1024
        private const val TARGET_RATIO = 0.9
        const val MIN_VALID_BYTES = 128L * 1024
    }

    class Entry {
        var songId = ""
        var fileName = ""
        var title = ""
        var artist = ""
        var album = ""
        var size = 0L
        var lastAccess = 0L
        var pinned = false
    }

    data class CacheItem(
        val songId: String,
        val title: String,
        val artist: String,
        val album: String,
        val size: Long,
        val lastAccess: Long,
        val pinned: Boolean,
        val downloading: Boolean
    )

    data class SongMetadata(
        val title: String,
        val artist: String,
        val album: String
    )

    /** 一次「下载 + 解密」任务的状态（single-flight 用）。 */
    private class Task {
        @Volatile
        var done = false

        @Volatile
        var path: String? = null

        @Volatile
        var error: Throwable? = null

        /** 当前正在执行下载的线程；换手由 synchronized(inflight) 保护。 */
        @Volatile
        var owner: Thread? = null
    }

    private val dir: File = File(context.cacheDir, "apple_music")
    private val entries = ConcurrentHashMap<String, Entry>()
    private val inflight = ConcurrentHashMap<String, Task>()
    private val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun init() {
        if (!dir.exists()) dir.mkdirs()
        loadIndex()
        val schema = prefs.getInt(KEY_SCHEMA, 0)
        if (schema != CACHE_SCHEMA) {
            clearAll(keepPinned = false)
            prefs.edit().putInt(KEY_SCHEMA, CACHE_SCHEMA).apply()
            Logx.w(TAG, "缓存格式升级 $schema → $CACHE_SCHEMA，已清空旧缓存")
        }
        sweepCorrupted()
        sweepTempFiles()
        Logx.i(TAG, "init: dir=${dir.absolutePath} count=${entries.size} bytes=${statsBytes()}")
    }

    private fun loadIndex() {
        try {
            val json = prefs.getString(KEY_INDEX, "[]") ?: "[]"
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val e = Entry()
                e.songId = o.optString("songId", "")
                e.fileName = o.optString("fileName", "")
                e.title = o.optString("title", "")
                e.artist = o.optString("artist", "")
                e.album = o.optString("album", "")
                e.size = o.optLong("size", 0)
                e.lastAccess = o.optLong("lastAccess", 0)
                e.pinned = o.optBoolean("pinned", false)
                if (e.songId.isNotEmpty()) entries[e.songId] = e
            }
        } catch (e: Throwable) {
            Logx.e(TAG, "loadIndex failed: ${e.message}")
        }
    }

    @Synchronized
    private fun persist() {
        val arr = JSONArray()
        for (e in entries.values) {
            arr.put(
                JSONObject().apply {
                    put("songId", e.songId)
                    put("fileName", e.fileName)
                    put("title", e.title)
                    put("artist", e.artist)
                    put("album", e.album)
                    put("size", e.size)
                    put("lastAccess", e.lastAccess)
                    put("pinned", e.pinned)
                }
            )
        }
        prefs.edit().putString(KEY_INDEX, arr.toString()).apply()
    }

    /** 启动时扫一遍索引，把体积异常（半截）的缓存条目清掉。 */
    private fun sweepCorrupted() {
        val bad = ArrayList<String>()
        for ((songId, e) in entries) {
            val f = File(dir, e.fileName)
            if (!f.exists() || f.length() < MIN_VALID_BYTES) {
                bad.add(songId)
                try {
                    f.delete()
                } catch (_: Throwable) {
                }
            }
        }
        if (bad.isNotEmpty()) {
            for (id in bad) entries.remove(id)
            persist()
            Logx.w(TAG, "sweepCorrupted: 清理 ${bad.size} 个损坏缓存")
        }
    }

    /** 清掉下载中断留下的 .part 临时文件。 */
    private fun sweepTempFiles() {
        val children = dir.listFiles() ?: return
        var removed = 0
        for (f in children) {
            if (f.name.endsWith(".part")) {
                if (f.delete()) removed++
            }
        }
        if (removed > 0) Logx.w(TAG, "sweepTempFiles: 清理 $removed 个 .part")
    }

    /** 命中则返回本地文件绝对路径，未命中/损坏返回 null（顺带刷新 LRU 时间）。 */
    fun getCachedPath(songId: String): String? {
        val e = entries[songId] ?: return null
        val f = File(dir, e.fileName)
        if (!f.exists()) {
            entries.remove(songId)
            persist()
            return null
        }
        if (f.length() < MIN_VALID_BYTES) {
            Logx.w(TAG, "缓存已损坏 $songId size=${f.length()}，作废")
            f.delete()
            entries.remove(songId)
            persist()
            return null
        }
        // 自检：带 CENC 加密标识的缓存一律作废（否则安卓播不出来）
        if (!looksPlayable(f)) {
            Logx.w(TAG, "缓存仍带 DRM 标识（enca/sinf），作废重下：$songId")
            f.delete()
            entries.remove(songId)
            persist()
            return null
        }
        e.lastAccess = System.currentTimeMillis()
        return f.absolutePath
    }

    fun has(songId: String): Boolean = getCachedPath(songId) != null

    fun isPreloading(songId: String): Boolean = inflight.containsKey(songId)

    fun isPinned(songId: String): Boolean = entries[songId]?.pinned == true

    fun setPinned(songId: String, pinned: Boolean) {
        entries[songId]?.let {
            it.pinned = pinned
            persist()
        }
    }

    @Synchronized
    private fun rememberMetadata(songId: String, metadata: SongMetadata?) {
        if (metadata == null) return
        val entry = entries[songId] ?: return
        var changed = false
        if (metadata.title.isNotBlank() && entry.title != metadata.title) {
            entry.title = metadata.title
            changed = true
        }
        if (metadata.artist.isNotBlank() && entry.artist != metadata.artist) {
            entry.artist = metadata.artist
            changed = true
        }
        if (metadata.album.isNotBlank() && entry.album != metadata.album) {
            entry.album = metadata.album
            changed = true
        }
        if (changed) persist()
    }

    fun cachedItems(): List<CacheItem> = entries.values.mapNotNull { entry ->
        val file = File(dir, entry.fileName)
        if (!file.isFile) {
            entries.remove(entry.songId, entry)
            persist()
            null
        } else {
            CacheItem(
                songId = entry.songId,
                title = entry.title,
                artist = entry.artist,
                album = entry.album,
                size = file.length(),
                lastAccess = entry.lastAccess,
                pinned = entry.pinned,
                downloading = inflight.containsKey(entry.songId)
            )
        }
    }.sortedByDescending { it.lastAccess }

    fun removeCached(songId: String): Boolean {
        if (inflight.containsKey(songId)) return false
        val entry = entries[songId] ?: return false
        val file = File(dir, entry.fileName)
        if (file.exists() && !file.delete()) {
            throw java.io.IOException("无法删除缓存文件：${file.name}")
        }
        if (!entries.remove(songId, entry)) return false
        persist()
        return true
    }

    /** 目标写入路径。 */
    fun targetPathFor(songId: String): String = File(dir, fileNameFor(songId)).absolutePath

    private fun fileNameFor(songId: String): String {
        val s = songId.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
        return (if (s.isNotEmpty()) s else "song") + ".m4a"
    }

    /**
     * 确保某首歌已经在缓存里，返回可直接交给播放器的本地文件路径。
     *
     * Apple 音源**唯一**的取文件入口：命中缓存立即返回；有在飞的任务就等待；
     * 否则发起一次下载并落索引后返回路径。失败时抛异常，调用方负责提示。
     */
    fun ensure(
        songId: String,
        isLibrary: Boolean,
        onProgress: ((AppleDownloader.Stage, Int, String) -> Unit)? = null,
        metadata: SongMetadata? = null
    ): String {
        require(songId.isNotEmpty()) { "歌曲 ID 为空" }
        getCachedPath(songId)?.let {
            rememberMetadata(songId, metadata)
            return it
        }

        // single-flight：同一首歌的并发调用共享同一个任务，谁接到谁当 owner。
        // 这里**不使用**「先抢锁再 join」的写法（容易因为换手顺序而互相等死），
        // 而是让非 owner 在任务状态上自旋等待 —— 结构简单，且不可能死锁。
        var mine = false
        val task: Task
        synchronized(inflight) {
            val existing = inflight[songId]
            if (existing != null) {
                task = existing
            } else {
                task = Task()
                task.owner = Thread.currentThread()
                inflight[songId] = task
                mine = true
            }
        }

        if (!mine) {
            Logx.i(TAG, "ensure: 复用进行中的任务 $songId")
            while (!task.done) {
                try {
                    Thread.sleep(120)
                } catch (_: InterruptedException) {
                }
                getCachedPath(songId)?.let {
                    rememberMetadata(songId, metadata)
                    return it
                }
            }
            getCachedPath(songId)?.let {
                rememberMetadata(songId, metadata)
                return it
            }
            task.error?.let { throw RuntimeException(it.message ?: "下载失败", it) }
            task.path?.let { return it }
            throw RuntimeException("下载失败（无结果）")
        }

        try {
            Logx.i(TAG, "ensure: 发起下载 $songId isLibrary=$isLibrary")
            val path = runDownload(songId, isLibrary, onProgress, metadata)
            task.path = path
            return path
        } catch (t: Throwable) {
            task.error = t
            throw t
        } finally {
            task.done = true
            synchronized(inflight) {
                if (inflight[songId] === task) inflight.remove(songId)
            }
        }
    }

    private fun runDownload(
        songId: String,
        isLibrary: Boolean,
        onProgress: ((AppleDownloader.Stage, Int, String) -> Unit)?,
        metadata: SongMetadata?
    ): String {
        val fileName = fileNameFor(songId)
        val target = File(dir, fileName).absolutePath
        var res = AppleDownloader.downloadSong(api, songId, isLibrary, target, onProgress)
        if (!res.ok) {
            // 【参数自愈】webPlayback 只有两种标识：
            //   isLibrary=true  → body 里是 universalLibraryId = "i.xxx"
            //   isLibrary=false → body 里是 salableAdamId    = 纯数字 catalogId
            //
            // 原来的「换标识重试」是拿**同一个 id** 换布尔值再发一次 ——
            // 对资料库歌曲来说第二次发的是 salableAdamId="i.xxx"，**永远不可能成功**
            // （实测日志：两次都是「该歌曲没有可用的音频资源」）。
            //
            // 正确做法：资料库歌曲先解析出真正的 catalogId，再用它当 salableAdamId 重试。
            val altId = if (isLibrary) api.librarySongCatalogId(songId) else ""
            if (altId.isNotEmpty() && altId != songId) {
                Logx.w(
                    TAG,
                    "ensure: universalLibraryId 失败（${res.message}），" +
                            "改用 catalogId=$altId 重试 $songId"
                )
                res = AppleDownloader.downloadSong(api, altId, false, target, onProgress)
            } else if (!isLibrary) {
                // 目录 id 失败：这首歌可能已经在资料库里，换 library 标识再试一次
                Logx.w(TAG, "ensure: salableAdamId 失败（${res.message}），改用资料库标识重试 $songId")
                res = AppleDownloader.downloadSong(api, songId, true, target, onProgress)
            }
        }
        if (!res.ok) throw RuntimeException(res.message.ifEmpty { "下载失败" })

        // 【重要】索引必须按**调用方传进来的 songId** 记账，
        // 而不是重试时用的 altId —— 否则 getCachedPath(songId) 永远命中不了，
        // 每次播放都会重新下载一遍。
        val e = Entry()
        e.songId = songId
        e.fileName = fileName
        e.title = metadata?.title.orEmpty()
        e.artist = metadata?.artist.orEmpty()
        e.album = metadata?.album.orEmpty()
        e.size = res.bytes.toLong()
        e.lastAccess = System.currentTimeMillis()
        entries[songId] = e
        persist()
        evictIfNeeded()
        Logx.i(TAG, "ensure: 就绪 $songId (${res.bytes} bytes)")
        return target
    }

    /** 后台预取（fire-and-forget）。 */
    fun preload(songId: String, isLibrary: Boolean) {
        if (songId.isEmpty() || entries.containsKey(songId) || inflight.containsKey(songId)) return
        Thread {
            try {
                ensure(songId, isLibrary)
            } catch (e: Throwable) {
                Logx.w(TAG, "preload 失败 $songId: ${e.message}")
            }
        }.start()
    }

    fun invalidate(songId: String) {
        remove(songId)
    }

    fun remove(songId: String) {
        val e = entries.remove(songId) ?: return
        File(dir, e.fileName).delete()
        persist()
    }

    fun clearAll(keepPinned: Boolean = true): Int {
        val ids = entries.values.filter { !keepPinned || !it.pinned }.map { it.songId }
        for (id in ids) remove(id)
        return ids.size
    }

    fun statsBytes(): Long = entries.values.sumOf { it.size }
    fun statsCount(): Int = entries.size

    /** 超出上限则按 LRU 清理（跳过 pinned）。 */
    @Synchronized
    fun evictIfNeeded(): Int {
        var total = statsBytes()
        if (total <= MAX_BYTES) return 0
        val target = (MAX_BYTES * TARGET_RATIO).toLong()
        val list = entries.values.filter { !it.pinned }.sortedBy { it.lastAccess }
        var removed = 0
        for (e in list) {
            if (total <= target) break
            File(dir, e.fileName).delete()
            entries.remove(e.songId)
            total -= e.size
            removed++
        }
        if (removed > 0) {
            persist()
            Logx.i(TAG, "evicted $removed files, now ${statsBytes()} bytes")
        }
        return removed
    }

    fun cacheDir(): File = dir
}
