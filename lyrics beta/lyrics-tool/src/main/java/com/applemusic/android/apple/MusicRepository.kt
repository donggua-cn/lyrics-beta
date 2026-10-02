package com.applemusic.android.apple

import android.content.Context
import com.applemusic.android.util.Logx
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 资料库仓库：进程内的唯一数据源。
 *
 * 对应鸿蒙版里「MusicDatabase + LibrarySignal + 聚合重建」这一摊事的合并简化版：
 * 同步拿到 Apple 的数据 → 落 JSON 快照 → 在内存里算出
 * 歌曲 / 专辑 / 艺人 / 歌单 四个板块 + 播放队列映射。
 */
object MusicRepository {

    private const val TAG = "MusicRepository"

    private lateinit var appContext: Context
    private val io = Executors.newSingleThreadExecutor { r -> Thread(r, "repo-io").apply { isDaemon = true } }

    lateinit var api: AppleApi
        private set
    lateinit var cache: AppleCache
        private set

    /** 同步状态（UI 轮询/回调）。 */
    @Volatile
    var syncing = false
        private set

    @Volatile
    var syncNote = ""
        private set

    @Volatile
    var syncPercent = 0
        private set

    @Volatile
    var lastSyncAt = 0L
        private set

    /** 原始数据。 */
    private val songs = ArrayList<AppleLibrarySync.SongRow>()
    private val playlists = ArrayList<AppleLibrarySync.PlaylistRow>()
    private val playlistTracks = HashMap<String, List<String>>()
    private val avatars = HashMap<String, String>()
    private val songByUri = HashMap<String, AppleLibrarySync.SongRow>()

    /** 聚合结果（供 UI 直接读）。 */
    val albumList = ArrayList<AlbumAgg>()
    val artistList = ArrayList<ArtistAgg>()

    class AlbumAgg {
        var name = ""
        var artist = ""
        var year = 0
        var coverUrl = ""
        var trackCount = 0
        var songs = ArrayList<AppleLibrarySync.SongRow>()
    }

    class ArtistAgg {
        var name = ""
        var songCount = 0
        var albumCount = 0
        var genre = ""
        var coverUrl = ""
    }

    var listener: (() -> Unit)? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        api = AppleApi(PrefTokenStore(appContext))
        api.restore()
        cache = AppleCache(appContext, api)
        cache.init()
        loadSnapshot()
    }

    private fun snapshotFile(): File = File(appContext.filesDir, "library.json")

    private fun loadSnapshot() {
        val snap = AppleLibrarySync.load(snapshotFile()) ?: return
        songs.clear()
        songs.addAll(snap.songs)
        playlists.clear()
        playlists.addAll(snap.playlists)
        playlistTracks.clear()
        playlistTracks.putAll(snap.playlistTracks)
        avatars.clear()
        avatars.putAll(snap.avatars)
        lastSyncAt = snap.syncedAt
        rebuild()
    }

    fun songCount(): Int = songs.size
    fun playlistCount(): Int = playlists.size

    fun allSongs(): List<AppleLibrarySync.SongRow> = songs

    fun songByUri(uri: String): AppleLibrarySync.SongRow? = songByUri[uri]

    /**
     * 「临时目录歌曲」索引。
     *
     * 主页/新发现/广播 浏览到的专辑、歌单里的歌，**不一定在用户资料库里**。
     * 而播放链路（`PlaybackEngine.prepareAndPlay` → `findSong`）原本只认资料库快照，
     * 于是浏览页点歌会全部落到「这首歌不在本地资料库里」。
     *
     * 这里单独放一份内存索引：只用于解析播放，**不写进资料库快照**，
     * 所以资料库列表不会被浏览到的歌污染。
     */
    private val transientSongs = HashMap<String, AppleLibrarySync.SongRow>()

    /** 登记一批「只是浏览到、不在资料库」的目录歌曲，使其可被 [findSong] 解析并播放。 */
    fun registerTransient(rows: List<AppleLibrarySync.SongRow>) {
        if (rows.isEmpty()) return
        synchronized(transientSongs) {
            for (r in rows) {
                if (r.uri.isEmpty()) continue
                transientSongs[r.uri] = r
            }
        }
    }

    /**
     * 查找一首歌：先按 uri（`apple:<id>`），再按裸 id 兜底。
     * 队列里存的是 uri，但目录歌曲可能只记了 catalogId。
     */
    fun findSong(uriOrId: String): AppleLibrarySync.SongRow? {
        songByUri[uriOrId]?.let { return it }
        val bare = uriOrId.removePrefix("apple:")
        // ① 资料库
        songs.firstOrNull { it.appleId == bare || it.catalogId == bare }?.let { return it }
        // ② 浏览到的目录歌曲（不写进资料库快照）
        synchronized(transientSongs) {
            transientSongs[uriOrId]?.let { return it }
            transientSongs["apple:$bare"]?.let { return it }
            return transientSongs.values.firstOrNull { it.appleId == bare || it.catalogId == bare }
        }
    }

    fun playlistById(id: String): AppleLibrarySync.PlaylistRow? = playlists.firstOrNull { it.id == id }

    /** 全部歌单（歌单页用）。 */
    fun allPlaylists(): List<AppleLibrarySync.PlaylistRow> = playlists

    /** 歌单曲目（uri 列表），未拉取过则返回本地已知的。 */
    fun playlistSongs(id: String): List<AppleLibrarySync.SongRow> {
        val uris = playlistTracks[id] ?: return emptyList()
        return uris.mapNotNull { findSong(it) }
    }

    fun hasPlaylistTracks(id: String): Boolean = playlistTracks.containsKey(id)

    /** 把某个歌单的曲目拉下来并缓存（打开歌单时调用）。 */
    fun loadPlaylistTracks(id: String): List<AppleLibrarySync.SongRow> {
        val rows = AppleLibrarySync.loadPlaylistSongs(api, id)
        val uris = ArrayList<String>(rows.size)
        synchronized(songs) {
            for (r in rows) {
                uris.add(r.uri)
                songByUri[r.uri] = r
                if (songs.none { it.uri == r.uri }) songs.add(r)
            }
        }
        playlistTracks[id] = uris
        persist()
        rebuild()
        notifyChanged()
        return rows
    }

    /** 全量同步（歌曲 + 歌单元数据）。 */
    fun syncAll(onDone: ((Boolean, String) -> Unit)? = null) {
        if (syncing) return
        syncing = true
        syncPercent = 0
        syncNote = "开始同步"
        io.execute {
            var ok = true
            var msg = ""
            try {
                api.getAccountInfo()
                val newSongs = AppleLibrarySync.fetchSongs(api) { p, n ->
                    syncPercent = p
                    syncNote = n
                }
                val probe = LinkedHashMap<String, String>()
                for (s in newSongs) {
                    if (s.catalogId.isNotEmpty() && s.artist.isNotEmpty() && !probe.containsKey(s.artist)) {
                        probe[s.artist] = s.catalogId
                    }
                }
                val newAvatars = AppleLibrarySync.fetchArtistAvatars(api, probe)
                val newPlaylists = AppleLibrarySync.fetchPlaylists(api) { p, n ->
                    syncPercent = minOf(99, p)
                    syncNote = n
                }
                synchronized(songs) {
                    songs.clear()
                    songs.addAll(newSongs)
                    rebuildLocked()
                }
                playlists.clear()
                playlists.addAll(newPlaylists)
                avatars.clear()
                avatars.putAll(newAvatars)
                lastSyncAt = System.currentTimeMillis()
                persist()
                msg = "同步完成：${newSongs.size} 首 / ${newPlaylists.size} 个歌单"
                Logx.i(TAG, msg)
            } catch (e: Throwable) {
                ok = false
                msg = e.message ?: e.toString()
                Logx.e(TAG, "同步失败: $msg", e)
            } finally {
                syncing = false
                syncPercent = 100
                syncNote = msg
                notifyChanged()
                onDone?.invoke(ok, msg)
            }
        }
    }

    /** 搜索（目录侧）：歌曲 / 专辑 / 艺人 / 歌单。 */
    fun searchCatalog(term: String, limit: Int = 25): AppleSearchResult = api.search(term, limit)

    /** 仅资料库本地过滤（零网络）。 */
    fun searchLocal(term: String): AppleSearchResult {
        val t = term.lowercase()
        val out = AppleSearchResult()
        if (t.isBlank()) return out
        for (s in songs) {
            if (s.title.lowercase().contains(t) || s.artist.lowercase().contains(t) ||
                s.album.lowercase().contains(t)
            ) {
                out.songs.add(songFromRow(s))
            }
        }
        for (a in albumList) {
            if (a.name.lowercase().contains(t) || a.artist.lowercase().contains(t)) {
                out.albums.add(AppleAlbum(name = a.name, artistName = a.artist, artworkUrl = a.coverUrl,
                    trackCount = a.trackCount, year = a.year, isLibrary = true))
            }
        }
        for (ar in artistList) {
            if (ar.name.lowercase().contains(t)) {
                out.artists.add(AppleArtist(name = ar.name, artworkUrl = ar.coverUrl,
                    songCount = ar.songCount, albumCount = ar.albumCount))
            }
        }
        for (p in playlists) {
            if (p.name.lowercase().contains(t)) {
                out.playlists.add(ApplePlaylist(id = p.id, name = p.name, artworkUrl = p.coverUrl,
                    trackCount = p.trackCount))
            }
        }
        return out
    }

    /** 用一列歌曲行当播放队列（uri 形式）。 */
    fun urisOf(rows: List<AppleLibrarySync.SongRow>): List<String> = rows.map { it.uri }

    /**
     * 同步后曲目 id 可能变化 → 修队列，避免播放条指向不存在的曲目。
     * （对应原版的 remapQueueAfterSync，这里队列很短，直接滤掉失效项。）
     */
    fun remapQueue(uris: List<String>): List<String> {
        val out = ArrayList<String>(uris.size)
        for (u in uris) {
            val s = findSong(u)
            if (s != null) out.add(s.uri)
        }
        return out
    }

    fun songsOfAlbum(name: String, artist: String): List<AppleLibrarySync.SongRow> {
        return albumList.firstOrNull { it.name == name && it.artist == artist }?.songs ?: emptyList()
    }

    fun songsOfArtist(name: String): List<AppleLibrarySync.SongRow> =
        songs.filter { it.artist == name }.sortedWith(
            compareBy({ it.album }, { it.discNumber }, { it.trackNumber })
        )

    // ─────────────────────── 内部 ───────────────────────

    private fun persist() {
        val snap = AppleLibrarySync.Snapshot()
        snap.songs = ArrayList(songs)
        snap.playlists = ArrayList(playlists)
        snap.playlistTracks = HashMap(playlistTracks)
        snap.avatars = HashMap(avatars)
        snap.syncedAt = if (lastSyncAt > 0) lastSyncAt else System.currentTimeMillis()
        AppleLibrarySync.save(snapshotFile(), snap)
    }

    @Synchronized
    private fun rebuild() = rebuildLocked()

    /** 重建专辑/艺人聚合（对应原版的 rebuildAggregates）。 */
    private fun rebuildLocked() {
        songByUri.clear()
        val albumMap = LinkedHashMap<String, AlbumAgg>()
        val artistMap = LinkedHashMap<String, ArtistAgg>()
        val genreVotes = HashMap<String, HashMap<String, Int>>()

        for (s in songs) {
            songByUri[s.uri] = s
            val key = s.album + "\u0000" + s.artist
            val agg = albumMap.getOrPut(key) {
                AlbumAgg().apply {
                    name = s.album
                    artist = s.artist
                    year = s.year
                    coverUrl = s.coverUrl
                }
            }
            agg.songs.add(s)
            if (agg.year == 0 && s.year > 0) agg.year = s.year
            if (agg.coverUrl.isEmpty() && s.coverUrl.isNotEmpty()) agg.coverUrl = s.coverUrl

            val ar = artistMap.getOrPut(s.artist) {
                ArtistAgg().apply {
                    name = s.artist
                    coverUrl = avatars[s.artist] ?: s.coverUrl
                }
            }
            ar.songCount++
            if (s.genre.isNotEmpty()) {
                val votes = genreVotes.getOrPut(s.artist) { HashMap() }
                for (g in s.genre.split("|")) {
                    if (g.isNotBlank()) votes[g] = (votes[g] ?: 0) + 1
                }
            }
        }

        for (agg in albumMap.values) {
            agg.trackCount = agg.songs.size
            agg.songs.sortWith(compareBy({ it.discNumber }, { it.trackNumber }))
            val ar = artistMap[agg.artist]
            if (ar != null) ar.albumCount++
        }
        for ((name, votes) in genreVotes) {
            val top = votes.maxByOrNull { it.value }?.key ?: ""
            artistMap[name]?.genre = top
        }
        // 头像缺失时用某首歌的封面兜底
        for (ar in artistMap.values) {
            if (ar.coverUrl.isEmpty()) {
                ar.coverUrl = songs.firstOrNull { it.artist == ar.name }?.coverUrl ?: ""
            }
        }

        albumList.clear()
        albumList.addAll(albumMap.values.sortedBy { it.name.lowercase() })
        artistList.clear()
        artistList.addAll(artistMap.values.sortedBy { it.name.lowercase() })
        Logx.i(TAG, "聚合完成：${songs.size} 首 / ${albumList.size} 张专辑 / ${artistList.size} 位艺人")
    }

    fun notifyChanged() {
        listener?.invoke()
    }

    fun syncTimeText(): String {
        if (lastSyncAt <= 0) return "尚未同步"
        val fmt = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
        return "上次同步 " + fmt.format(Date(lastSyncAt))
    }

    private fun songFromRow(s: AppleLibrarySync.SongRow): AppleSong = AppleSong(
        id = s.appleId, name = s.title, artistName = s.artist, albumName = s.album,
        durationMs = s.durationMs, trackNumber = s.trackNumber, discNumber = s.discNumber,
        year = s.year, artworkUrl = s.coverUrl, isLibrary = s.isLibrary, genre = s.genre,
        catalogId = s.catalogId
    )

    /** 把 AppleSong（目录搜索结果）转成可入库的行。 */
    fun rowOf(s: AppleSong): AppleLibrarySync.SongRow = AppleLibrarySync.toRow(s)

    /**
     * 用一个货架卡片（歌曲类型）临时造一条可播放的记录。
     * 主页/新发现里偶尔直接给歌曲卡片，那种情况下没有完整的 AppleSong 字段。
     */
    fun transientRow(
        catalogId: String,
        title: String,
        artist: String,
        artwork: String
    ): AppleLibrarySync.SongRow = AppleLibrarySync.SongRow().apply {
        appleId = catalogId
        this.title = title.ifEmpty { "未知歌曲" }
        this.artist = artist.ifEmpty { "未知艺人" }
        uri = "apple:$catalogId"
        coverUrl = artwork
        isLibrary = false
        this.catalogId = catalogId
    }
}

/** SharedPreferences 实现的 token 存储。 */
class PrefTokenStore(context: Context) : TokenStore {
    private val sp = context.getSharedPreferences("apple_music_auth", Context.MODE_PRIVATE)
    override fun getString(key: String, def: String): String = sp.getString(key, def) ?: def
    override fun putString(key: String, value: String) {
        sp.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        sp.edit().remove(key).apply()
    }
}
