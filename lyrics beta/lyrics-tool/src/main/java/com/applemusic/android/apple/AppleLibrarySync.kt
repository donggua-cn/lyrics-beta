package com.applemusic.android.apple

import com.applemusic.android.util.Logx
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 资料库同步 —— 对应鸿蒙版 `AppleLibrarySync.ets`。
 *
 * 原版把数据落到 HarmonyOS 的 RDB（关系型数据库）并重建聚合表；
 * 安卓这版只有一个用户、一份资料库，没必要引入 Room + KSP，
 * 所以落成一份 JSON 快照（`filesDir/library.json`），同步后重建内存聚合。
 *
 * 同步内容：
 *   syncSongs()            分页拉 /v1/me/library/songs（include=albums,artists）
 *                          → 写快照 → 顺手刷新「艺人官方头像」
 *   syncPlaylists()        歌单元数据
 *   syncAllPlaylistTracks() 逐个歌单拉曲目（自动歌单会 404，按空歌单处理）
 */
object AppleLibrarySync {

    private const val TAG = "AppleLibrarySync"

    /** 单页大小（Apple 上限 100）。 */
    const val PAGE = 100

    class Result {
        var ok = false
        var songs = 0
        var message = ""
    }

    /** 本地歌曲行（对应鸿蒙版的 MusicSongRow，字段按需裁剪）。 */
    class SongRow {
        var appleId = ""
        var uri = ""
        var title = ""
        var artist = ""
        var album = ""
        var durationMs = 0L
        var year = 0
        var genre = ""
        var coverUrl = ""
        var trackNumber = 0
        var discNumber = 0
        var isLibrary = true
        /** 目录歌曲 id（换密钥必须用它） */
        var catalogId = ""
        var artistId = ""

        fun toJson(): JSONObject = JSONObject().apply {
            put("appleId", appleId)
            put("uri", uri)
            put("title", title)
            put("artist", artist)
            put("album", album)
            put("durationMs", durationMs)
            put("year", year)
            put("genre", genre)
            put("coverUrl", coverUrl)
            put("trackNumber", trackNumber)
            put("discNumber", discNumber)
            put("isLibrary", isLibrary)
            put("catalogId", catalogId)
            put("artistId", artistId)
        }

        companion object {
            fun fromJson(o: JSONObject): SongRow = SongRow().apply {
                appleId = o.optString("appleId", "")
                uri = o.optString("uri", "")
                title = o.optString("title", "")
                artist = o.optString("artist", "")
                album = o.optString("album", "")
                durationMs = o.optLong("durationMs", 0)
                year = o.optInt("year", 0)
                genre = o.optString("genre", "")
                coverUrl = o.optString("coverUrl", "")
                trackNumber = o.optInt("trackNumber", 0)
                discNumber = o.optInt("discNumber", 0)
                isLibrary = o.optBoolean("isLibrary", true)
                catalogId = o.optString("catalogId", "")
                artistId = o.optString("artistId", "")
            }
        }
    }

    class PlaylistRow {
        var id = ""
        var name = ""
        var description = ""
        var coverUrl = ""
        var trackCount = 0

        fun toJson(): JSONObject = JSONObject().apply {
            put("id", id)
            put("name", name)
            put("description", description)
            put("coverUrl", coverUrl)
            put("trackCount", trackCount)
        }

        companion object {
            fun fromJson(o: JSONObject): PlaylistRow = PlaylistRow().apply {
                id = o.optString("id", "")
                name = o.optString("name", "")
                description = o.optString("description", "")
                coverUrl = o.optString("coverUrl", "")
                trackCount = o.optInt("trackCount", 0)
            }
        }
    }

    /** AppleSong → SongRow（uri 用 `apple:<id>` 作为播放标识）。 */
    fun toRow(s: AppleSong): SongRow = SongRow().apply {
        appleId = s.id
        title = s.name.ifEmpty { "未知歌曲" }
        artist = s.artistName.ifEmpty { "未知艺人" }
        album = s.albumName.ifEmpty { "未知专辑" }
        durationMs = s.durationMs
        year = s.year
        uri = "apple:" + s.id
        coverUrl = s.artworkUrl
        trackNumber = s.trackNumber
        discNumber = s.discNumber
        genre = s.genre
        isLibrary = s.isLibrary
        catalogId = s.catalogId
        artistId = s.artistId
    }

    /**
     * 全量同步资料库歌曲（自动分页）。
     *
     * @return 歌曲行列表；艺人头像在内部顺手刷新
     */
    fun fetchSongs(api: AppleApi, onProgress: ((Int, String) -> Unit)? = null): List<SongRow> {
        val report: (Int, String) -> Unit = { p, n -> onProgress?.invoke(p, n) }
        api.ensureReady()
        report(2, "读取资料库")
        val all = ArrayList<SongRow>()
        // 每位艺人只记一首歌的目录 id：艺人头像是按艺人取的，几次请求就能覆盖整库
        val avatarProbe = LinkedHashMap<String, String>()
        var offset = 0
        var page = api.getLibrarySongs(PAGE, offset)
        val declaredTotal = page.total
        var guard = 0
        while (page.items.isNotEmpty() && guard < 200) {
            for (song in page.items) {
                all.add(toRow(song))
                if (song.catalogId.isNotEmpty() && song.artistName.isNotEmpty() &&
                    !avatarProbe.containsKey(song.artistName)
                ) {
                    avatarProbe[song.artistName] = song.catalogId
                }
            }
            offset += page.items.size
            report(2 + minOf(80, offset / 10), "已读取 $offset 首")
            if (page.items.size < PAGE) break
            page = api.getLibrarySongs(PAGE, offset)
            guard++
        }
        Logx.i(TAG, "拉取完成，共 ${all.size} 首（Apple 声明 total=$declaredTotal）")
        if (declaredTotal > 0 && all.size != declaredTotal) {
            Logx.w(TAG, "资料库曲目数与 Apple 声明不一致：拉取=${all.size} total=$declaredTotal")
        }
        report(92, "更新艺人头像")
        val avatars = fetchArtistAvatars(api, avatarProbe)
        report(100, "同步完成，共 ${all.size} 首")
        return all
    }

    /**
     * 用「每位艺人一首歌」的目录 id 批量换取 Apple Music 官方艺人头像。
     *
     * 资料库接口本身不返回艺人头像，但目录歌曲接口的 `relationships.artists` 带
     * `attributes.artwork`；一次请求可带 100 个 id，整库通常只要两三次请求。
     *
     * @return 艺人名 → 头像 URL
     */
    fun fetchArtistAvatars(api: AppleApi, probe: Map<String, String>): Map<String, String> {
        val avatars = HashMap<String, String>()
        if (probe.isEmpty()) return avatars
        val songIds = ArrayList<String>(probe.keys.size)
        val bySongId = HashMap<String, String>()
        for ((artistName, songId) in probe) {
            songIds.add(songId)
            bySongId[songId] = artistName
        }
        var i = 0
        while (i < songIds.size) {
            val slice = songIds.subList(i, minOf(i + 100, songIds.size))
            val found = api.catalogArtistsForSongs(slice)
            for ((songId, artist) in found) {
                val name = bySongId[songId] ?: continue
                if (name.isNotEmpty() && artist.artworkUrl.isNotEmpty()) {
                    avatars[name] = artist.artworkUrl
                }
            }
            i += 100
        }
        Logx.i(TAG, "艺人官方头像已更新：${avatars.size} 位")
        return avatars
    }

    /** 全量同步资料库歌单（只同步元数据，曲目在打开歌单时按需拉取）。 */
    fun fetchPlaylists(api: AppleApi, onProgress: ((Int, String) -> Unit)? = null): List<PlaylistRow> {
        val report: (Int, String) -> Unit = { p, n -> onProgress?.invoke(p, n) }
        api.ensureReady()
        val all = ArrayList<PlaylistRow>()
        var offset = 0
        var page = api.getLibraryPlaylists(PAGE, offset)
        var guard = 0
        while (page.items.isNotEmpty() && guard < 50) {
            for (p in page.items) {
                val row = PlaylistRow()
                row.id = p.id
                row.name = p.name.ifEmpty { "未命名歌单" }
                row.description = p.description
                row.coverUrl = p.artworkUrl
                row.trackCount = p.trackCount
                all.add(row)
            }
            offset += page.items.size
            report(minOf(95, 10 + offset), "已读取 $offset 个歌单")
            if (page.items.size < PAGE) break
            page = api.getLibraryPlaylists(PAGE, offset)
            guard++
        }
        Logx.i(TAG, "歌单同步完成: ${all.size}")
        return all
    }

    /**
     * 拉取某个歌单的全部曲目。
     *
     * Apple 的「自动歌单」（喜爱歌曲/睡眠/新歌…）走另一套内部服务，
     * 资料库接口对它们返回 404 —— 这里按空歌单处理并落日志。
     */
    fun loadPlaylistSongs(api: AppleApi, playlistId: String): List<SongRow> {
        val rows = ArrayList<SongRow>()
        var offset = 0
        var page: PageResult<AppleSong>
        try {
            page = api.getPlaylistTracks(playlistId, PAGE, offset)
        } catch (e: Throwable) {
            val msg = e.message ?: ""
            if (msg.contains("404")) {
                Logx.i(TAG, "loadPlaylistSongs $playlistId: 404（Apple 未开放该歌单曲目）")
                throw RuntimeException("Apple 没有开放这个歌单的曲目")
            }
            throw e
        }
        var guard = 0
        while (page.items.isNotEmpty() && guard < 100) {
            for (s in page.items) {
                val row = toRow(s)
                // 不在资料库里的曲目：标记为非资料库，避免污染三个板块
                row.isLibrary = false
                rows.add(row)
            }
            offset += page.items.size
            if (page.items.size < PAGE) break
            page = api.getPlaylistTracks(playlistId, PAGE, offset)
            guard++
        }
        Logx.i(TAG, "歌单 $playlistId 曲目 ${rows.size} 首")
        return rows
    }

    // ─────────────────────── 快照持久化 ───────────────────────

    class Snapshot {
        var songs = ArrayList<SongRow>()
        var playlists = ArrayList<PlaylistRow>()
        var playlistTracks = HashMap<String, List<String>>() // playlistId → uri 列表
        var avatars = HashMap<String, String>()
        var syncedAt = 0L
    }

    fun save(file: File, snap: Snapshot) {
        val root = JSONObject()
        root.put("syncedAt", snap.syncedAt)
        val songsArr = JSONArray()
        for (s in snap.songs) songsArr.put(s.toJson())
        root.put("songs", songsArr)
        val plArr = JSONArray()
        for (p in snap.playlists) plArr.put(p.toJson())
        root.put("playlists", plArr)
        val ptObj = JSONObject()
        for ((k, v) in snap.playlistTracks) ptObj.put(k, JSONArray(v))
        root.put("playlistTracks", ptObj)
        val avObj = JSONObject()
        for ((k, v) in snap.avatars) avObj.put(k, v)
        root.put("avatars", avObj)
        file.writeText(root.toString())
        Logx.i(TAG, "快照已保存：${snap.songs.size} 首 / ${snap.playlists.size} 个歌单 → ${file.absolutePath}")
    }

    fun load(file: File): Snapshot? {
        if (!file.exists()) return null
        return try {
            val root = JSONObject(file.readText())
            val snap = Snapshot()
            snap.syncedAt = root.optLong("syncedAt", 0)
            val songsArr = root.optJSONArray("songs") ?: JSONArray()
            for (i in 0 until songsArr.length()) snap.songs.add(SongRow.fromJson(songsArr.getJSONObject(i)))
            val plArr = root.optJSONArray("playlists") ?: JSONArray()
            for (i in 0 until plArr.length()) snap.playlists.add(PlaylistRow.fromJson(plArr.getJSONObject(i)))
            root.optJSONObject("playlistTracks")?.let { o ->
                for (k in o.keys()) {
                    val arr = o.optJSONArray(k) ?: continue
                    val list = ArrayList<String>(arr.length())
                    for (i in 0 until arr.length()) list.add(arr.optString(i, ""))
                    snap.playlistTracks[k] = list
                }
            }
            root.optJSONObject("avatars")?.let { o ->
                for (k in o.keys()) snap.avatars[k] = o.optString(k, "")
            }
            Logx.i(TAG, "快照已载入：${snap.songs.size} 首 / ${snap.playlists.size} 个歌单")
            snap
        } catch (e: Throwable) {
            Logx.e(TAG, "载入快照失败: ${e.message}")
            null
        }
    }
}
