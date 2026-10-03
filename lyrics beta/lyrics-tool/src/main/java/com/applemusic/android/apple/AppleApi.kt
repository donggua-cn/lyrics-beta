package com.applemusic.android.apple

import com.applemusic.android.net.Http
import com.applemusic.android.util.Logx
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

/**
 * Apple Music API 客户端 —— 对应鸿蒙版 `AppleApi.ets`（全项目最核心的一个文件）。
 *
 *  1) 认证：developer token（从 music.apple.com 首页的 index.js 里正则抓 JWT）
 *           + media-user-token（WebView 登录后从 cookie 抓，代表「你的账号身份」）
 *  2) 请求：统一走 ampGet()，头是 Origin + Chrome UA + Bearer + Cookie
 *  3) 资料库：/v1/me/library/{songs,albums,playlists,artists}（分页）
 *  4) 目录：/v1/catalog/{storefront}/{search,songs,albums,artists}
 *  5) 播放：getWebPlayback() 拿资产清单 → getPlayAssets() 拿 HLS + license 服务器
 *  6) 字节流：getBytes() 支持 Range（HLS 分片按字节区间拉）
 *
 * ⚠ dev token 靠正则抓前端资源，Apple 换前端结构就失效（表现为所有接口 401），
 *   此时更新 `refreshDeveloperToken()` 里的正则即可。
 */
class AppleApi(private val prefs: TokenStore) {

    companion object {
        private const val TAG = "AppleApi"
        private const val UA = AppleConst.UA
    }

    data class StationProbeResponse(
        val path: String,
        val statusCode: Int,
        val body: String
    )

    @Volatile
    var devToken: String = ""
        private set

    @Volatile
    var userToken: String = ""
        private set

    @Volatile
    var storefront: String = "cn"
        private set

    /** 是否已具备调用条件。 */
    fun isAuthorized(): Boolean = userToken.length > 50

    fun hasDeveloperToken(): Boolean = devToken.length > 50

    // ─────────────────────────── 认证 ───────────────────────────

    /** 从本地偏好恢复登录态。 */
    fun restore() {
        devToken = prefs.getString("developer_token", "")
        userToken = prefs.getString("media_user_token", "")
        storefront = prefs.getString("storefront", "cn")
        Logx.i(TAG, "restore: dev=${devToken.length} user=${userToken.length} sf=$storefront")
    }

    /** 持久化 token。 */
    fun persist(userToken: String) {
        this.userToken = userToken
        prefs.putString("developer_token", devToken)
        prefs.putString("media_user_token", userToken)
        prefs.putString("storefront", storefront)
    }

    /** 退出登录。 */
    fun logout() {
        userToken = ""
        prefs.putString("media_user_token", "")
        prefs.remove("media_user_token")
    }

    /**
     * 抓 developer token：首页 HTML → index.js → 正则取第一个 JWT。
     *
     * 这是最脆的一环：Apple 换前端结构就失效，所以每次都 try/catch + 落盘缓存。
     */
    fun refreshDeveloperToken(): String {
        val home = Http.getText(AppleConst.HOME + "/us/browse", headers(false), 25000)
        val m = Regex("/assets/index[~-][^/\"]+\\.js").find(home)
            ?: throw RuntimeException("首页未找到 index.js（Apple 可能改了结构）")
        val js = Http.getText(AppleConst.HOME + m.value, headers(false), 25000)
        val tok = Regex("\"(eyJ[A-Za-z0-9\\-_]+\\.eyJ[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]+)\"").find(js)
            ?: throw RuntimeException("index.js 中未找到 developer token")
        devToken = tok.groupValues[1]
        prefs.putString("developer_token", devToken)
        Logx.i(TAG, "developer token refreshed, len=${devToken.length}")
        return devToken
    }

    /** 确保 token 就绪（必要时刷新 developer token）。 */
    fun ensureReady() {
        if (!hasDeveloperToken()) refreshDeveloperToken()
    }

    /**
     * 从 WebView 现有 cookie 里取 media-user-token。
     *
     * 与原实现一致：cookie 的 Domain 可能是 `.music.apple.com` 也可能是 `.apple.com`，
     * 所以几个地址都试一遍；长度 ≥50 才认（避免抓到空值）。
     */
    fun extractUserToken(cookie: String): String {
        val m = Regex("media-user-token=([^;]+)").find(cookie) ?: return ""
        val v = m.groupValues[1]
        return if (v.length >= 50) v else ""
    }

    // ─────────────────────────── HTTP ───────────────────────────

    private fun headers(withAuth: Boolean): Map<String, String> {
        val h = LinkedHashMap<String, String>()
        h["Origin"] = AppleConst.HOME
        h["User-Agent"] = UA
        if (withAuth) {
            h["Authorization"] = "Bearer $devToken"
            h["Cookie"] = "media-user-token=$userToken"
        }
        return h
    }

    /** license 请求用的 headers（与开源实现对齐：小写名 + 不带 UA）。 */
    fun licenseHeaders(): Map<String, String> {
        val h = LinkedHashMap<String, String>()
        h["authorization"] = "Bearer $devToken"
        h["origin"] = "https://music.apple.com"
        h["cookie"] = "media-user-token=$userToken"
        h["content-type"] = "application/json"
        return h
    }

    private fun ampGet(path: String): String {
        ensureReady()
        val url = AppleConst.AMP + path
        val r = Http.request(url, "GET", headers(true), null, 15000, 30000)
        if (r.code != 200) {
            throw RuntimeException("HTTP ${r.code} @ $path body=${r.text.take(200)}")
        }
        return r.text
    }

    // ─────────────────────────── 账号 ───────────────────────────

    /** 账号信息（校验订阅，并取出 storefront）。 */
    fun getAccountInfo(): String {
        val txt = ampGet("/v1/me/account?meta=subscription")
        try {
            val sub = JSONObject(txt).optJSONObject("meta")?.optJSONObject("subscription")
            val sf = sub?.optString("storefront", "") ?: ""
            if (sf.isNotEmpty()) {
                storefront = sf
                prefs.putString("storefront", sf)
            }
        } catch (e: Throwable) {
            Logx.w(TAG, "解析 account 失败：${e.message}")
        }
        return txt
    }

    // ─────────────────────────── 资料库 ───────────────────────────

    /** 资料库歌曲（分页）。 */
    fun getLibrarySongs(limit: Int, offset: Int): PageResult<AppleSong> {
        val path = "/v1/me/library/songs?limit=$limit&offset=$offset" +
                "&extend=artistUrl&include=albums,artists&l=en-US"
        return parseSongs(ampGet(path), true)
    }

    /** 资料库专辑。 */
    fun getLibraryAlbums(limit: Int, offset: Int): PageResult<AppleAlbum> {
        return parseAlbums(ampGet("/v1/me/library/albums?limit=$limit&offset=$offset&l=en-US"), true)
    }

    /** 资料库歌单。 */
    fun getLibraryPlaylists(limit: Int, offset: Int): PageResult<ApplePlaylist> {
        return parsePlaylists(ampGet("/v1/me/library/playlists?limit=$limit&offset=$offset&l=en-US"))
    }

    fun getAllLibraryPlaylists(): List<ApplePlaylist> {
        val playlists = ArrayList<ApplePlaylist>()
        var offset = 0
        var guard = 0
        while (guard < 50) {
            val page = getLibraryPlaylists(100, offset)
            playlists.addAll(page.items)
            offset += page.items.size
            if (page.items.size < 100) break
            guard++
        }
        return playlists
    }

    /** 歌单内的歌曲（自动歌单会 404，属正常）。 */
    fun getPlaylistTracks(playlistId: String, limit: Int, offset: Int): PageResult<AppleSong> {
        val path = "/v1/me/library/playlists/$playlistId/tracks?limit=$limit&offset=$offset&l=en-US"
        return parseSongs(ampGet(path), true)
    }

    fun favoritePlaylistId(): String = prefs.getString("favorite_playlist_id", "")

    fun setFavoritePlaylistId(playlistId: String) {
        require(playlistId.matches(Regex("[A-Za-z0-9._-]+"))) { "无效的歌单 ID" }
        prefs.putString("favorite_playlist_id", playlistId)
    }

    /** 自动绑定 Favorite Songs 智能歌单，并读取其全部曲目目录 ID。 */
    fun getFavoriteCatalogSongIds(): Set<String> {
        val favoritePlaylistNames = setOf(
            "favorite songs",
            "favorites",
            "liked songs",
            "喜爱歌曲",
            "喜爱的歌曲",
            "喜欢的歌曲",
            "喜欢的音乐",
            "喜爱的音乐",
            "最爱歌曲",
            "最爱音乐",
            "最喜欢的歌曲",
            "最喜欢的音乐"
        ).mapTo(HashSet(), ::normalizeFavoritePlaylistName)

        fun findFavoritePlaylist(): ApplePlaylist? =
            getAllLibraryPlaylists().firstOrNull { playlist ->
                normalizeFavoritePlaylistName(playlist.name) in favoritePlaylistNames
            }

        val configuredPlaylistId = favoritePlaylistId()
        var favoritePlaylist = configuredPlaylistId.takeIf(String::isNotBlank)
            ?.let { ApplePlaylist(id = it) }
        if (favoritePlaylist == null) {
            favoritePlaylist = findFavoritePlaylist()
                ?: throw IllegalStateException(
                    "Apple Music 资料库中未找到 Favorite Songs 喜爱歌曲歌单"
                )
            setFavoritePlaylistId(favoritePlaylist.id)
            Logx.i(TAG, "已自动绑定 Favorite Songs 歌单：${favoritePlaylist.id}")
        }

        return try {
            readFavoritePlaylistCatalogSongIds(favoritePlaylist.id)
        } catch (configuredError: RuntimeException) {
            if (configuredPlaylistId.isBlank()) throw configuredError
            Logx.w(
                TAG,
                "已绑定喜爱歌单不可读取，重新查找 Favorite Songs：${configuredError.message}"
            )
            val discoveredPlaylist = findFavoritePlaylist()
                ?.takeIf { it.id != configuredPlaylistId }
                ?: throw configuredError
            setFavoritePlaylistId(discoveredPlaylist.id)
            Logx.i(TAG, "已重新绑定 Favorite Songs 歌单：${discoveredPlaylist.id}")
            readFavoritePlaylistCatalogSongIds(discoveredPlaylist.id)
        }
    }

    private fun readFavoritePlaylistCatalogSongIds(playlistId: String): Set<String> {
        val songIds = LinkedHashSet<String>()
        val seenPageSignatures = HashSet<String>()
        var offset = 0
        var songPage = getPlaylistTracks(playlistId, 100, offset)
        var guard = 0
        while (songPage.items.isNotEmpty() && guard < 100) {
            val pageIds = songPage.items.map { song ->
                song.catalogId.ifBlank { song.id.takeUnless { it.startsWith("i.") }.orEmpty() }
            }.filter(String::isNotBlank)
            if (!seenPageSignatures.add(pageIds.joinToString(","))) break
            pageIds.forEach { songIds.add(it) }
            offset += songPage.items.size
            if (songPage.next.isBlank() && songPage.total > 0 && offset >= songPage.total) break
            songPage = getPlaylistTracks(playlistId, 100, offset)
            guard++
        }
        return songIds
    }

    private fun normalizeFavoritePlaylistName(name: String): String =
        name.lowercase(java.util.Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}]"), "")

    /**
     * **目录歌单**的曲目。
     *
     * 主页/新发现里的「专属推荐歌单」「热门歌单」全是**目录歌单**（id 形如 `pl.xxx`），
     * 用 [getPlaylistTracks] 那条 `/v1/me/library/playlists/...` 路径取**永远是空的**
     * （资料库歌单才走那条）。必须用 `/v1/catalog/{sf}/playlists/...`。
     */
    fun getCatalogPlaylistTracks(playlistId: String, limit: Int, offset: Int): PageResult<AppleSong> {
        val sf = if (storefront.isNotEmpty()) storefront else "us"
        val path =
            "/v1/catalog/$sf/playlists/$playlistId/tracks?limit=$limit&offset=$offset&l=${langTag()}"
        return parseSongs(ampGet(path), false).also { page ->
            localizeCatalogSongs(page.items)
        }
    }

    /**
     * 按 id 前缀自动选路径取歌单曲目。
     *
     *  - `p.xxx`  → 资料库歌单 → `/v1/me/library/playlists/...`
     *  - `pl.xxx` → 目录歌单   → `/v1/catalog/{sf}/playlists/...`
     *
     * 注意判断顺序：`pl.` 也以 `p` 开头，所以必须用 `startsWith("p.")` 来区分。
     */
    fun getAnyPlaylistTracks(
        playlistId: String,
        limit: Int,
        offset: Int,
        isLibraryPlaylist: Boolean = playlistId.startsWith("p.")
    ): PageResult<AppleSong> {
        return if (isLibraryPlaylist) {
            getPlaylistTracks(playlistId, limit, offset)
        } else {
            getCatalogPlaylistTracks(playlistId, limit, offset)
        }
    }

    /** 从主页推荐资源读取歌单曲目，保留资料库歌单与目录歌单的真实资源类型。 */
    fun getRecommendedPlaylistTracks(
        playlistId: String,
        relatedCatalogId: String,
        isLibraryPlaylist: Boolean,
        limit: Int,
        offset: Int
    ): PageResult<AppleSong> {
        return when {
            !isLibraryPlaylist ->
                getCatalogPlaylistTracks(playlistId, limit, offset)
            relatedCatalogId.isNotBlank() ->
                getCatalogPlaylistTracks(relatedCatalogId, limit, offset)
            else ->
                getPlaylistTracks(playlistId, limit, offset)
        }
    }

    /** Read a recommended album's tracks from its catalog resource or library relationship. */
    fun getRecommendedAlbumTracks(
        albumId: String,
        relatedCatalogId: String,
        isLibraryAlbum: Boolean,
        resolvedCatalogId: String = "",
        catalogStorefront: String = storefront
    ): List<AppleSong> {
        val catalogId = resolvedCatalogId.ifBlank { relatedCatalogId }
        if (catalogId.isNotBlank()) {
            return getCatalogAlbumTracks(catalogId, catalogStorefront)
        }
        if (!isLibraryAlbum) return getCatalogAlbumTracks(albumId, catalogStorefront)
        require(albumId.matches(Regex("[A-Za-z0-9._-]+"))) { "无效的资料库专辑 ID" }
        val root = JSONObject(
            ampGet("/v1/me/library/albums/$albumId/tracks?limit=100&l=${langTag()}")
        )
        val tracks = root.optJSONArray("data")
            ?: throw IllegalStateException("Apple Music 没有返回资料库专辑曲目")
        return pickSongs(JSONObject().put("data", tracks), "data", true)
    }

    /** Load complete collection metadata, including the responsive portrait motion-art assets. */
    fun getRecommendedCollectionMetadata(
        item: ShelfItem,
        preferSquareArtwork: Boolean = false
    ): CollectionMetadata {
        require(item.isAlbum || item.isPlaylist) { "不支持获取此类型的详情：${item.type}" }

        val resourceType = if (item.isAlbum) "albums" else "playlists"
        val libraryType = if (item.isAlbum) "albums" else "playlists"
        var libraryAttributes = JSONObject()
        var catalogId = item.relatedCatalogId

        if (item.type.contains("library", ignoreCase = true) && catalogId.isBlank()) {
            require(item.id.matches(Regex("[A-Za-z0-9._-]+"))) { "无效的资料库内容 ID" }
            val libraryResource = JSONObject(
                ampGet(
                    "/v1/me/library/$libraryType/${item.id}" +
                        "?include=catalog&l=${langTag()}"
                )
            ).optJSONArray("data")?.optJSONObject(0)
                ?: throw IllegalStateException("Apple Music 没有返回《${item.title}》的详情")
            libraryAttributes = libraryResource.optJSONObject("attributes") ?: JSONObject()
            catalogId = libraryResource.optJSONObject("relationships")
                ?.optJSONObject("catalog")
                ?.optJSONArray("data")
                ?.optJSONObject(0)
                ?.optString("id", "")
                .orEmpty()
        }

        val catalogStorefront = item.catalogStorefront.ifBlank { storefront }
        val catalogLanguage = if (catalogStorefront == "cn") "zh-Hans-CN" else langTag()
        val catalogResource: JSONObject? =
            if (catalogId.isNotBlank() || !item.type.contains("library", true)) {
            val resolvedCatalogId = catalogId.ifBlank { item.id }
            require(resolvedCatalogId.matches(Regex("[A-Za-z0-9._-]+"))) {
                "无效的目录内容 ID"
            }
            catalogId = resolvedCatalogId
            val includeCurator = if (item.isPlaylist) "&include=curator" else "&include=artists"
            JSONObject(
                ampGet(
                    "/v1/catalog/$catalogStorefront/$resourceType/$resolvedCatalogId" +
                        "?extend=editorialArtwork%2CeditorialVideo$includeCurator" +
                        "&l=$catalogLanguage"
                )
            ).optJSONArray("data")?.optJSONObject(0)
                ?: throw IllegalStateException("Apple Music 没有返回《${item.title}》的目录详情")
        } else {
            null
        }
        val catalogAttributes = catalogResource?.optJSONObject("attributes") ?: JSONObject()

        val artwork = catalogAttributes.optJSONObject("editorialArtwork")
            ?.optJSONObject("staticDetailTall")
        val editorialVideo = catalogAttributes.optJSONObject("editorialVideo")
        val motion = if (preferSquareArtwork) {
            editorialVideo?.optJSONObject("motionDetailSquare")
                ?: editorialVideo?.optJSONObject("motionDetailTall")
                ?: editorialVideo?.optJSONObject("motionTallVideo3x4")
        } else {
            editorialVideo?.optJSONObject("motionDetailTall")
                ?: editorialVideo?.optJSONObject("motionTallVideo3x4")
                ?: editorialVideo?.optJSONObject("motionDetailSquare")
        }
        val preview = motion?.optJSONObject("previewFrame")
        val description = collectionDescription(catalogAttributes)
            .ifBlank { collectionDescription(libraryAttributes) }
            .ifBlank { item.collectionDescription }
        val creator = catalogAttributes.optString("curatorName", "")
            .ifBlank {
                catalogAttributes.optJSONObject("editorialNotes")
                    ?.optString("name", "")
                    .orEmpty()
            }
            .ifBlank { catalogAttributes.optString("artistName", "") }
            .ifBlank { libraryAttributes.optString("curatorName", "") }
            .ifBlank { item.creator }
        val creatorId = catalogResource?.optJSONObject("relationships")
            ?.optJSONObject("artists")
            ?.optJSONArray("data")
            ?.optJSONObject(0)
            ?.optString("id", "")
            .orEmpty()
        val ratioSource = preview ?: artwork
        val width = ratioSource?.optDouble("width", 0.0) ?: 0.0
        val height = ratioSource?.optDouble("height", 0.0) ?: 0.0
        val aspectRatio = if (width > 0.0 && height > 0.0) {
            (width / height).toFloat().coerceIn(0.5f, 2f)
        } else {
            item.artworkAspectRatio
        }

        return CollectionMetadata(
            title = catalogAttributes.optString("name", "").ifBlank { item.title },
            catalogId = catalogId,
            creator = creator,
            creatorId = creatorId,
            description = description,
            trackCount = catalogAttributes.optInt("trackCount", 0)
                .takeIf { it > 0 }
                ?: libraryAttributes.optInt("trackCount", 0)
                .takeIf { it > 0 }
                ?: item.trackCount,
            updatedAt = catalogAttributes.optString("lastModifiedDate", "")
                .ifBlank { catalogAttributes.optString("releaseDate", "") }
                .ifBlank { libraryAttributes.optString("lastModifiedDate", "") }
                .ifBlank { item.updatedAt },
            artworkUrl = motionArtworkUrl(
                preview,
                targetWidth = 600,
                targetHeight = if (preferSquareArtwork) 600 else 800
            )
                .ifBlank { artwork?.optString("url", "").orEmpty() }
                .ifBlank {
                    catalogAttributes.optJSONObject("artwork")?.optString("url", "").orEmpty()
                }
                .ifBlank { item.artwork },
            ambientVideoUrl = motion?.optString("video", "").orEmpty()
                .ifBlank { item.ambientVideoUrl },
            ambientPreviewUrl = if (preferSquareArtwork) {
                item.ambientPreviewUrl
            } else {
                motionArtworkUrl(preview, 600, 800)
            }
                .ifBlank { item.ambientPreviewUrl },
            ambientSquareVideoUrl = editorialVideo
                ?.optJSONObject("motionDetailSquare")
                ?.optString("video", "")
                .orEmpty()
                .ifBlank { item.ambientSquareVideoUrl },
            ambientSquarePreviewUrl = motionArtworkUrl(
                editorialVideo?.optJSONObject("motionDetailSquare")?.optJSONObject("previewFrame"),
                600,
                600
            ).ifBlank { item.ambientSquarePreviewUrl },
            artworkAspectRatio = aspectRatio
        )
    }

    private fun motionArtworkUrl(
        frame: JSONObject?,
        targetWidth: Int,
        targetHeight: Int
    ): String {
        val url = frame?.optString("url", "").orEmpty()
        if (url.isBlank()) return ""
        return url.replace("{w}", targetWidth.toString())
            .replace("{h}", targetHeight.toString())
            .replace("{f}", "jpg")
    }

    private fun collectionDescription(attributes: JSONObject): String {
        val description = attributes.opt("description")
        return when (description) {
            is JSONObject -> description.optString(
                "standard",
                description.optString("stringForDisplay", "")
            )
            is String -> description
            else -> attributes.optJSONObject("editorialNotes")
                ?.optString("standard", "")
                .orEmpty()
        }
    }

    /**
     * 当前商店对应的语言标签。
     *
     * 这个值直接影响搜索能否命中：写死 `l=en-US` 时 Apple 只在英文名/别名里匹配，
     * 搜「方大同」（目录规范名是 Khalil Fong）就搜不到本人。
     */
    fun langTag(): String = when (storefront) {
        "cn", "zh" -> "zh-Hans-CN"
        "jp" -> "ja-JP"
        "kr" -> "ko-KR"
        "tw", "hk" -> "zh-Hant-TW"
        "tr" -> "tr-TR"
        else -> "en-US"
    }

    // ─────────────────────────── 目录 / 搜索 ───────────────────────────

    private fun searchOnce(term: String, limit: Int, lang: String): AppleSearchResult {
        val enc = java.net.URLEncoder.encode(term, "UTF-8")
        val path = "/v1/catalog/$storefront/search?term=$enc" +
                "&types=songs,albums,artists,playlists&limit=$limit&l=$lang"
        val root = JSONObject(ampGet(path))
        val results = root.optJSONObject("results") ?: JSONObject()
        val out = AppleSearchResult()
        // 目录搜索的响应形状是 { results: { songs: { data: [...] }, ... } }：
        // 每个类型外面还包了一层对象（含 href/next/data），必须先把 data 那层取出来。
        out.songs = pickSongs(results.optJSONObject("songs") ?: JSONObject(), "data", false)
        out.albums = pickAlbums(results.optJSONObject("albums") ?: JSONObject(), "data", false)
        out.artists = pickArtists(results.optJSONObject("artists") ?: JSONObject(), "data")
        out.playlists = pickPlaylists(results.optJSONObject("playlists") ?: JSONObject(), "data")
        return out
    }

    /**
     * 搜索（歌曲/专辑/艺人/歌单）：**按商店语言 + 英文各搜一次后合并**，
     * 所以中文名（方大同）和英文名（Khalil Fong）都能命中。
     */
    fun search(term: String, limit: Int): AppleSearchResult {
        val lang = langTag()
        val merged = AppleSearchResult()
        val seenArtist = HashSet<String>()
        val seenAlbum = HashSet<String>()
        val seenSong = HashSet<String>()
        val langs = if (lang == "en-US") listOf("en-US") else listOf(lang, "en-US")
        for ((i, lg) in langs.withIndex()) {
            try {
                val part = searchOnce(term, limit, lg)
                for (a in part.artists) if (seenArtist.add(a.id)) merged.artists.add(a)
                for (a in part.albums) if (seenAlbum.add(a.id)) merged.albums.add(a)
                for (a in part.songs) if (seenSong.add(a.id)) merged.songs.add(a)
                if (i == 0 && merged.artists.isNotEmpty() && merged.albums.size >= 5) break
            } catch (e: Throwable) {
                Logx.w(TAG, "search with lang=$lg failed: ${e.message}")
            }
        }
        // 对照表兜底：Apple 公开搜索只索引规范名，命中对照表时把规范名也搜一次并置顶。
        val alias = ArtistAliasTable.lookup(term)
        if (alias.isNotEmpty() && !alias.equals(term, ignoreCase = true)) {
            try {
                val mapped = searchOnce(alias, limit, "en-US")
                val fa = mapped.artists.filter { seenArtist.add(it.id) }
                val fb = mapped.albums.filter { seenAlbum.add(it.id) }
                val fs = mapped.songs.filter { seenSong.add(it.id) }
                merged.artists.addAll(0, fa)
                merged.albums.addAll(0, fb)
                merged.songs.addAll(0, fs)
                Logx.i(TAG, "alias search: $term -> $alias (artists=${fa.size})")
            } catch (e: Throwable) {
                Logx.w(TAG, "alias search failed $alias: ${e.message}")
            }
        }
        merged.playlists = ArrayList()
        return merged
    }

    /** 只搜艺人（独立请求，limit 更大）。 */
    fun searchArtistsOnly(term: String, limit: Int): List<AppleArtist> {
        if (term.isEmpty()) return emptyList()
        val enc = java.net.URLEncoder.encode(term, "UTF-8")
        val path = "/v1/catalog/$storefront/search?term=$enc&types=artists&limit=$limit&l=${langTag()}"
        return try {
            val root = JSONObject(ampGet(path))
            val results = root.optJSONObject("results") ?: JSONObject()
            pickArtists(results.optJSONObject("artists") ?: JSONObject(), "data")
        } catch (e: Throwable) {
            Logx.w(TAG, "searchArtistsOnly failed $term: ${e.message}")
            emptyList()
        }
    }

    /** 目录艺人详情（补名字与头像）。 */
    fun getCatalogArtist(artistId: String): AppleArtist? {
        if (artistId.isEmpty()) return null
        return try {
            fun fetchArtistResource(
                language: String,
                includeMotionArtwork: Boolean = true
            ): JSONObject {
                val motionExtension = if (includeMotionArtwork) "%2CeditorialVideo" else ""
                return JSONObject(
                    ampGet(
                        "/v1/catalog/cn/artists/$artistId" +
                            "?extend=artistBio%2CbornOrFormed%2Corigin%2CeditorialArtwork%2C" +
                            "hero%2ChasBiographyContent%2CisGroup%2CplainEditorialNotes" +
                            motionExtension +
                            "&include=artist-biography-canvas" +
                            "&format%5Bresources%5D=map&l=$language&platform=web"
                    )
                )
            }

            fun textContent(value: Any?): String = when (value) {
                is String -> value.trim()
                is JSONObject -> sequenceOf(
                    "standard",
                    "text",
                    "value",
                    "content",
                    "description",
                    "paragraphs",
                    "data"
                )
                    .map { key -> textContent(value.opt(key)) }
                    .firstOrNull(String::isNotBlank)
                    .orEmpty()
                is JSONArray -> (0 until value.length())
                    .asSequence()
                    .map { index -> textContent(value.opt(index)) }
                    .filter(String::isNotBlank)
                    .joinToString("\n\n")
                else -> ""
            }

            fun editorialDescription(attributes: JSONObject): String {
                return sequenceOf("artistBio", "plainEditorialNotes", "editorialNotes")
                    .map { key -> textContent(attributes.opt(key)) }
                    .firstOrNull(String::isNotBlank)
                    .orEmpty()
            }

            fun canvasBiography(response: JSONObject): String {
                val paragraphs = LinkedHashSet<String>()
                fun collect(value: Any?) {
                    when (value) {
                        is JSONObject -> {
                            val attributes = value.optJSONObject("attributes")
                            val style = attributes?.optJSONObject("display")
                                ?.optString("style", "")
                            if (style == "artist-about-text") {
                                textContent(attributes.opt("text"))
                                    .takeIf(String::isNotBlank)
                                    ?.let(paragraphs::add)
                            }
                            val keys = value.keys()
                            while (keys.hasNext()) collect(value.opt(keys.next()))
                        }
                        is JSONArray -> {
                            for (index in 0 until value.length()) collect(value.opt(index))
                        }
                    }
                }
                collect(response)
                return paragraphs.joinToString("\n\n")
            }

            val root = fetchArtistResource("zh-Hans-CN")
            val data = root.optJSONArray("data") ?: return null
            if (data.length() == 0) return null
            val artistReference = data.getJSONObject(0)
            val artistResource = resolveResource(
                artistReference,
                root.optJSONObject("resources") ?: JSONObject()
            )
            val a = AppleArtist()
            a.id = artistResource.optString(
                "id",
                artistReference.optString("id", "")
            )
            val attrs = artistResource.optJSONObject("attributes") ?: JSONObject()
            a.name = attrs.optString("name", "")
            a.artworkUrl = bigArtwork(attrs.optJSONObject("artwork")?.optString("url", "") ?: "")
            a.bornOrFormed = attrs.optString("bornOrFormed", "")
            a.isGroup = attrs.optBoolean("isGroup", false)
            a.genreNames = attrs.optJSONArray("genreNames")?.let { genres ->
                (0 until genres.length())
                    .map { index -> genres.optString(index, "").trim() }
                    .filter(String::isNotBlank)
            }.orEmpty()
            val editorialVideo = attrs.optJSONObject("editorialVideo")
            val motion = editorialVideo?.optJSONObject("motionArtistFullscreen16x9")
                ?: editorialVideo?.optJSONObject("motionArtistWide16x9")
                ?: editorialVideo?.optJSONObject("motionArtistSquare1x1")
            val preview = motion?.optJSONObject("previewFrame")
            a.motionVideoUrl = motion?.optString("video", "").orEmpty()
            a.motionPreviewUrl = preview?.optString("url", "").orEmpty()
                .replace("{w}", "2400")
                .replace("{h}", "1350")
                .replace("{f}", "jpg")
            a.motionBackgroundColor = preview?.optString("bgColor", "").orEmpty()
            a.motionTextColor = preview?.optString("textColor1", "").orEmpty()
            a.editorialDescription = canvasBiography(root)
                .ifBlank { editorialDescription(attrs) }
            if (a.editorialDescription.isBlank()) {
                try {
                    val biographyRoot = fetchArtistResource(
                        "zh-Hans-CN",
                        includeMotionArtwork = false
                    )
                    val biographyReference =
                        biographyRoot.optJSONArray("data")?.optJSONObject(0)
                    val biographyArtist = biographyReference?.let {
                        resolveResource(
                            it,
                            biographyRoot.optJSONObject("resources") ?: JSONObject()
                        )
                    }
                    val biographyAttributes =
                        biographyArtist?.optJSONObject("attributes") ?: JSONObject()
                    a.editorialDescription = canvasBiography(biographyRoot)
                        .ifBlank { editorialDescription(biographyAttributes) }
                } catch (error: Exception) {
                    Logx.w(
                        TAG,
                        "getCatalogArtist biography request failed $artistId: ${error.message}"
                    )
                }
            }
            if (a.editorialDescription.isBlank()) {
                try {
                    val englishRoot = fetchArtistResource(
                        "en-US",
                        includeMotionArtwork = false
                    )
                    val englishReference =
                        englishRoot.optJSONArray("data")?.optJSONObject(0)
                    val englishArtist = englishReference?.let {
                        resolveResource(
                            it,
                            englishRoot.optJSONObject("resources") ?: JSONObject()
                        )
                    }
                    val englishAttributes =
                        englishArtist?.optJSONObject("attributes") ?: JSONObject()
                    a.editorialDescription = editorialDescription(englishAttributes)
                        .ifBlank { canvasBiography(englishRoot) }
                } catch (error: Exception) {
                    Logx.w(
                        TAG,
                        "getCatalogArtist editorial fallback failed $artistId: ${error.message}"
                    )
                }
            }
            if (a.name.isEmpty()) null else a
        } catch (e: Throwable) {
            Logx.w(TAG, "getCatalogArtist failed $artistId: ${e.message}")
            null
        }
    }

    /** 某位目录艺人的专辑列表（分页，用于「补齐资料库」）。 */
    fun getCatalogArtistAlbums(artistId: String, limit: Int, offset: Int): List<AppleAlbum> {
        val path =
            "/v1/catalog/cn/artists/$artistId/albums?limit=$limit&offset=$offset&l=zh-Hans-CN"
        return pickAlbums(JSONObject(ampGet(path)), "data", false).onEach {
            it.catalogStorefront = "cn"
        }
    }

    fun getCatalogArtistTopSongs(
        artistId: String,
        limit: Int = 20,
        offset: Int = 0
    ): List<AppleSong> {
        require(artistId.matches(Regex("[A-Za-z0-9._-]+"))) { "无效的目录艺人 ID" }
        val root = JSONObject(
            ampGet(
                "/v1/catalog/$storefront/artists/$artistId/view/top-songs" +
                    "?l=${langTag()}&limit=$limit&offset=$offset"
            )
        )
        return pickSongs(root, "data", false).onEach { it.artistId = artistId }
    }

    fun getFavoriteCatalogArtistIds(): Set<String>? {
        return try {
            val root = JSONObject(
                ampGet("/v1/me/favorites?types=artists&limit=100&l=${langTag()}&platform=web")
            )
            val data = root.optJSONArray("data") ?: return emptySet()
            buildSet {
                for (index in 0 until data.length()) {
                    data.optJSONObject(index)?.optString("id")
                        ?.takeIf(String::isNotBlank)
                        ?.let(::add)
                }
            }
        } catch (error: Exception) {
            Logx.w(TAG, "无法读取已收藏艺人（此视图可能不受 storefront 支持）：${error.message}")
            null
        }
    }

    fun setCatalogArtistFavorite(artistId: String, favorite: Boolean) {
        require(artistId.matches(Regex("[A-Za-z0-9._-]+"))) {
            "无效的目录艺人 ID"
        }
        ensureReady()
        val encodedId = java.net.URLEncoder.encode(artistId, "UTF-8")
        val path = "/v1/me/favorites?art%5Burl%5D=f&ids%5Bartists%5D=$encodedId" +
            "&l=${langTag()}&platform=web"
        val requestHeaders = LinkedHashMap(headers(true)).apply {
            put("media-user-token", userToken)
        }
        val response = Http.request(
            AppleConst.AMP + path,
            if (favorite) "POST" else "DELETE",
            requestHeaders,
            null,
            15000,
            30000
        )
        if (response.code !in 200..299) {
            throw IllegalStateException(
                "${if (favorite) "收藏" else "取消收藏"}艺人失败，HTTP " +
                    "${response.code}：${response.text.take(300)}"
            )
        }
        Logx.i(
            TAG,
            "catalog artist ${if (favorite) "favorited" else "unfavorited"} " +
                "(${response.code}): $artistId"
        )
    }

    fun getCatalogArtistPage(artistId: String): AppleArtistPage {
        require(artistId.matches(Regex("[A-Za-z0-9._-]+"))) { "无效的目录艺人 ID" }
        val artist = getCatalogArtist(artistId)
            ?: throw IllegalStateException("Apple Music 没有找到此艺人")
        fun view(viewName: String, limit: Int? = null): JSONObject {
            val limitQuery = limit?.let { "&limit=$it" }.orEmpty()
            return JSONObject(
                ampGet(
                    "/v1/catalog/cn/artists/$artistId/view/$viewName" +
                        "?l=zh-Hans-CN$limitQuery"
                )
            )
        }
        fun optionalView(viewName: String, limit: Int? = null): JSONObject? = try {
            view(viewName, limit)
        } catch (error: Exception) {
            Logx.w(TAG, "艺人 $artistId 的 $viewName 视图不可用：${error.message}")
            null
        }

        val topSongs = getCatalogArtistTopSongs(artistId, limit = 20)
        val albums = getCatalogArtistAlbums(artistId, limit = 20, offset = 0)
            .filterNot { it.isSingle }
        val singles = optionalView("singles", 20)
            ?.let { pickAlbums(it, "data", false) }
            .orEmpty()
            .onEach { it.catalogStorefront = "cn" }
        val latestRelease = (albums + singles)
            .maxByOrNull { it.releaseDate }
        val videos = optionalView("music-videos", 12)
            ?.let { pickAlbums(it, "data", false) }
            .orEmpty()
            .onEach { it.catalogStorefront = "cn" }
        val playlists = optionalView("artist-playlists", 12)
            ?.let { pickPlaylists(it, "data") }
            .orEmpty()
            .ifEmpty {
                optionalView("playlists", 12)
                    ?.let { pickPlaylists(it, "data") }
                    .orEmpty()
            }
        val similarArtists = optionalView("similar-artists", 12)
            ?.let { pickArtists(it, "data") }
            .orEmpty()

        topSongs.forEach { it.artistId = artist.id }
        albums.forEach { it.artistId = artist.id }
        singles.forEach { it.artistId = artist.id }
        videos.forEach { it.artistId = artist.id }
        return AppleArtistPage(
            artist = artist,
            topSongs = topSongs,
            latestRelease = latestRelease?.also { it.artistId = artist.id },
            albums = albums,
            singles = singles,
            musicVideos = videos,
            playlists = playlists,
            similarArtists = similarArtists
        )
    }

    /** 目录歌曲所属的专辑 id。 */
    fun getCatalogSongAlbumId(songId: String, catalogStorefront: String = storefront): String {
        if (songId.isEmpty()) return ""
        return try {
            val language = if (catalogStorefront == "cn") "zh-Hans-CN" else langTag()
            val data = JSONObject(
                ampGet(
                    "/v1/catalog/$catalogStorefront/songs/$songId" +
                        "?include=albums&l=$language"
                )
            )
                .optJSONArray("data") ?: return ""
            if (data.length() == 0) return ""
            data.getJSONObject(0).optJSONObject("relationships")
                ?.optJSONObject("albums")?.optJSONArray("data")?.optJSONObject(0)?.optString("id", "") ?: ""
        } catch (e: Throwable) {
            Logx.w(TAG, "getCatalogSongAlbumId failed $songId: ${e.message}")
            ""
        }
    }

    /** Fetch the catalog album represented by a song's albums relationship. */
    fun getCatalogAlbumForSong(
        songId: String,
        catalogStorefront: String = storefront
    ): AppleAlbum? {
        val albumId = getCatalogSongAlbumId(songId, catalogStorefront)
        if (albumId.isBlank()) return null
        return getCatalogAlbum(albumId, catalogStorefront)
    }

    fun getCatalogAlbum(albumId: String, catalogStorefront: String = storefront): AppleAlbum? {
        if (albumId.isBlank()) return null
        val language = if (catalogStorefront == "cn") "zh-Hans-CN" else langTag()
        val data = JSONObject(
            ampGet("/v1/catalog/$catalogStorefront/albums/$albumId?l=$language")
        ).optJSONArray("data") ?: return null
        return pickAlbums(JSONObject().put("data", data), "data", false).firstOrNull()
            ?.also { it.catalogStorefront = catalogStorefront }
    }

    /**
     * 目录专辑的曲目。
     * 注意：Apple 的 `albums/{id}?include=tracks` **不允许带 limit**
     * （会返回 400 "Limit may not be supplied on this request"）。
     */
    fun getCatalogAlbumTracks(
        albumId: String,
        catalogStorefront: String = storefront
    ): List<AppleSong> {
        if (albumId.isEmpty()) return emptyList()
        val language = if (catalogStorefront == "cn") "zh-Hans-CN" else langTag()
        val data = JSONObject(
            ampGet(
                "/v1/catalog/$catalogStorefront/albums/$albumId" +
                    "?include=tracks&l=$language"
            )
        ).optJSONArray("data") ?: error("Apple Music 没有返回专辑数据")
        check(data.length() > 0) { "Apple Music 没有找到专辑：$albumId" }
        val tracks = data.getJSONObject(0).optJSONObject("relationships")
            ?.optJSONObject("tracks") ?: error("Apple Music 没有返回专辑曲目")
        return pickSongs(tracks, "data", false).also {
            localizeCatalogSongs(it, catalogStorefront)
        }
    }

    private fun localizeCatalogSongs(
        songs: MutableList<AppleSong>,
        catalogStorefront: String = storefront
    ) {
        if (catalogStorefront != "cn" || songs.isEmpty()) return
        val catalogIds = songs.map { it.catalogId.ifBlank { it.id } }
            .filter(String::isNotBlank)
            .distinct()
        if (catalogIds.isEmpty()) return
        try {
            val data = JSONObject(
                ampGet(
                    "/v1/catalog/$catalogStorefront/songs?ids=${catalogIds.joinToString(",")}" +
                        "&include=artists" +
                        "&l=zh-Hans-CN"
                )
            ).optJSONArray("data") ?: return
            val localized = HashMap<String, AppleSong>(data.length())
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index) ?: continue
                val attributes = item.optJSONObject("attributes") ?: continue
                val id = item.optString("id", "")
                if (id.isBlank()) continue
                localized[id] = AppleSong(
                    id = id,
                    name = attributes.optString("name", ""),
                    artistName = attributes.optString("artistName", ""),
                    albumName = attributes.optString("albumName", ""),
                    artistId = item.optJSONObject("relationships")
                        ?.optJSONObject("artists")
                        ?.optJSONArray("data")
                        ?.optJSONObject(0)
                        ?.optString("id", "")
                        .orEmpty()
                )
            }
            songs.forEach { song ->
                val localizedSong = localized[song.catalogId.ifBlank { song.id }] ?: return@forEach
                if (localizedSong.name.isNotBlank()) song.name = localizedSong.name
                if (localizedSong.artistName.isNotBlank()) {
                    song.artistName = localizedSong.artistName
                }
                if (localizedSong.albumName.isNotBlank()) song.albumName = localizedSong.albumName
                if (song.artistId.isBlank()) song.artistId = localizedSong.artistId
            }
        } catch (error: Exception) {
            Logx.w(TAG, "catalog song localization failed: ${error.message}")
        }
    }

    /** 云资料库歌曲 id（i.xxxx）→ 曲库 catalogId。 */
    fun librarySongCatalogId(libraryId: String): String {
        if (libraryId.isEmpty()) return ""
        return try {
            val root = JSONObject(
                ampGet("/v1/me/library/songs/$libraryId?include=catalog&l=en-US")
            )
            val data = root.optJSONArray("data") ?: return ""
            if (data.length() == 0) return ""
            val song = data.getJSONObject(0)
            val attributes = song.optJSONObject("attributes")
            val playParamsId = attributes?.optJSONObject("playParams")
                ?.optString("catalogId", "")
                .orEmpty()
            if (playParamsId.isNotBlank()) return playParamsId

            val relationships = song.optJSONObject("relationships")
            for (key in listOf("catalog", "catalog-songs")) {
                val related = relationships?.optJSONObject(key)?.optJSONArray("data")
                    ?.optJSONObject(0)
                    ?.optString("id", "")
                    .orEmpty()
                if (related.isNotBlank()) return related
            }
            ""
        } catch (e: Throwable) {
            Logx.w(TAG, "librarySongCatalogId failed $libraryId: ${e.message}")
            ""
        }
    }

    /**
     * 取歌词原始 JSON（拿不到返回 null）。
     *
     * ## 逐字歌词是**独立的资源路径**，不是查询参数
     *
     * 这一点踩过坑：一直以为要靠 `l[lyrics]=...` 换取值，于是把
     * `ttml` / `syllable` / `syllable-lyrics` / `syllableLyrics` / 数组形式
     * 全试了一遍 —— 响应**逐字节完全一样**（服务端直接忽略这些值）。
     *
     * 真正的两个资源是：
     * ```
     * GET /v1/catalog/{storefront}/songs/{id}/syllable-lyrics   → 逐字
     *      type=syllable-lyrics、itunes:timing="Word"、<p> 里带 <span begin end>
     * GET /v1/catalog/{storefront}/songs/{id}/lyrics?l[lyrics]=ttml  → 逐行
     *      type=lyrics、itunes:timing="Line"、<p> 里只有整行文本
     * ```
     * 所以先请求 `syllable-lyrics`，它 404（该曲没有逐字）时再退回行级。
     *
     * 资料库歌曲（`i.xxx`）内部接口不认，先用 [librarySongCatalogId] 换成
     * catalog id；万一换不到，再退回 `/v1/me/library/songs/{id}/lyrics`。
     *
     * 纯音乐/未收录歌词会 404 —— 返回 null，不抛异常。
     */
    fun getLyricsJson(songId: String, isLibrary: Boolean): String? {
        if (songId.isEmpty()) return null
        val sf = if (storefront.isNotEmpty()) storefront else "us"

        val catalogId = if (isLibrary) librarySongCatalogId(songId) else songId
        if (catalogId.isNotEmpty()) {
            // ① 逐字（卡拉OK）—— 优先，能拿到就用它
            tryAmpGet("/v1/catalog/$sf/songs/$catalogId/syllable-lyrics")?.let { return it }
            // ② 退回逐行
            tryAmpGet("/v1/catalog/$sf/songs/$catalogId/lyrics?l%5Blyrics%5D=ttml")?.let { return it }
        }
        // 【不要再退回 /v1/me/library/songs/{id}/...】
        // 实测资料库资源根本没有这两个 relationship，只会白打两次 400：
        //   /v1/me/library/songs/{id}/syllable-lyrics → 400 No relationship found
        //   /v1/me/library/songs/{id}/lyrics          → 400 No relationship found
        // 拿不到 catalog id 就直接认为没有歌词，交给上层显示「暂无歌词」。
        return null
    }

    /** 和 [ampGet] 一样，但非 200 时返回 null 而不是抛异常（歌词缺失是常态）。 */
    private fun tryAmpGet(path: String): String? {
        return try {
            ensureReady()
            val r = Http.request(AppleConst.AMP + path, "GET", headers(true), null, 15000, 30000)
            if (r.code != 200) {
                Logx.w(TAG, "歌词接口 HTTP ${r.code} @ $path")
                null
            } else {
                r.text
            }
        } catch (e: Throwable) {
            Logx.w(TAG, "歌词接口异常 @ $path: ${e.message}")
            null
        }
    }

    /** 目录歌曲 id → 其主艺人（目录艺人 id）。 */
    fun getCatalogSongArtistId(catalogSongId: String): String {
        if (catalogSongId.isEmpty()) return ""
        return try {
            val data = JSONObject(ampGet("/v1/catalog/$storefront/songs/$catalogSongId?include=artists&l=en-US"))
                .optJSONArray("data") ?: return ""
            if (data.length() == 0) return ""
            data.getJSONObject(0).optJSONObject("relationships")?.optJSONObject("artists")
                ?.optJSONArray("data")?.optJSONObject(0)?.optString("id", "") ?: ""
        } catch (e: Throwable) {
            Logx.w(TAG, "getCatalogSongArtistId failed $catalogSongId: ${e.message}")
            ""
        }
    }

    /**
     * 批量：目录歌曲 id → 这首歌的主艺人（含官方头像）。
     *
     * 用途：资料库歌曲里有 `playParams.catalogId`，但资料库接口不返回艺人头像；
     * 一次请求最多 100 个目录歌曲 id，返回的 `relationships.artists` 就带
     * `attributes.artwork`（Apple Music 官方艺人图），整库只要二三十次请求。
     */
    fun catalogArtistsForSongs(songIds: List<String>): Map<String, AppleArtist> {
        val out = HashMap<String, AppleArtist>()
        if (songIds.isEmpty()) return out
        return try {
            val ids = songIds.joinToString(",")
            val data = JSONObject(
                ampGet("/v1/catalog/$storefront/songs?ids=$ids&include=artists&l=${langTag()}")
            ).optJSONArray("data") ?: return out
            for (i in 0 until data.length()) {
                val it = data.getJSONObject(i)
                val id = it.optString("id", "")
                if (id.isEmpty()) continue
                val a = it.optJSONObject("relationships")?.optJSONObject("artists")
                    ?.optJSONArray("data")?.optJSONObject(0) ?: continue
                val attrs = a.optJSONObject("attributes") ?: JSONObject()
                val artist = AppleArtist()
                artist.id = a.optString("id", "")
                artist.name = attrs.optString("name", "")
                artist.artworkUrl = bigArtwork(attrs.optJSONObject("artwork")?.optString("url", "") ?: "")
                out[id] = artist
            }
            out
        } catch (e: Throwable) {
            Logx.w(TAG, "catalogArtistsForSongs failed: ${e.message}")
            out
        }
    }

    /** 一次请求同时拿到专辑主艺人的 id / 名称 / 头像。 */
    fun catalogAlbumArtist(albumId: String): AppleArtist? {
        if (albumId.isEmpty()) return null
        return try {
            val data = JSONObject(ampGet("/v1/catalog/$storefront/albums/$albumId?include=artists&l=${langTag()}"))
                .optJSONArray("data") ?: return null
            if (data.length() == 0) return null
            val a = data.getJSONObject(0).optJSONObject("relationships")?.optJSONObject("artists")
                ?.optJSONArray("data")?.optJSONObject(0) ?: return null
            val attrs = a.optJSONObject("attributes") ?: JSONObject()
            val out = AppleArtist()
            out.id = a.optString("id", "")
            out.name = attrs.optString("name", "")
            out.artworkUrl = bigArtwork(attrs.optJSONObject("artwork")?.optString("url", "") ?: "")
            if (out.id.isEmpty()) null else out
        } catch (e: Throwable) {
            Logx.w(TAG, "catalogAlbumArtist failed $albumId: ${e.message}")
            null
        }
    }

    /** 目录歌曲详情（补全元数据）。 */
    fun getCatalogSong(songId: String): AppleSong? {
        val txt = ampGet("/v1/catalog/$storefront/songs/$songId?l=en-US&include=albums,artists")
        val page = parseSongs(txt, false)
        return page.items.firstOrNull()
    }

    /** 电台曲目显示元数据优先使用简体中文，没有本地化时退回当前 storefront 语言。 */
    fun getCatalogSongLocalized(songId: String): AppleSong? {
        if (songId.isBlank()) return null
        val languages = listOf("zh-Hans-CN", langTag()).distinct()
        var firstResult: AppleSong? = null
        for (language in languages) {
            val txt = ampGet(
                "/v1/catalog/$storefront/songs/$songId?l=$language&include=albums,artists"
            )
            val song = parseSongs(txt, false).items.firstOrNull() ?: continue
            if (firstResult == null) firstResult = song
            if (language == "zh-Hans-CN" && hasHanCharacters(song.name + song.artistName)) {
                return song
            }
            if (language == langTag()) return song
        }
        return firstResult
    }

    private fun hasHanCharacters(text: String): Boolean =
        text.any { it in '\u3400'..'\u9fff' }

    /** 把一首目录歌曲加入用户的 Apple Music 资料库。 */
    fun addCatalogSongToLibrary(songId: String): Boolean {
        if (songId.isEmpty()) return false
        val enc = java.net.URLEncoder.encode(songId, "UTF-8")
        val url = "${AppleConst.AMP}/v1/me/library?ids%5Bsongs%5D=$enc"
        return try {
            val h = LinkedHashMap(headers(true))
            h["Content-Type"] = "application/json"
            val r = Http.postRaw(url, "{}", h, 20000)
            if (r.code in 200..299) {
                Logx.i(TAG, "song added to library (${r.code}): $songId")
                true
            } else {
                Logx.w(TAG, "add song failed HTTP ${r.code}: ${r.text.take(200)}")
                false
            }
        } catch (e: Throwable) {
            Logx.w(TAG, "add song exception: ${e.message}")
            false
        }
    }

    /** 使用 Apple Music 网页端使用的接口切换目录歌曲的收藏状态。 */
    fun setCatalogSongFavorite(songId: String, favorite: Boolean) {
        require(songId.matches(Regex("[A-Za-z0-9._-]+"))) { "无效的歌曲 ID" }
        val encodedId = java.net.URLEncoder.encode(songId, "UTF-8")
        val path = "/v1/me/favorites?art%5Burl%5D=f&ids%5Bsongs%5D=$encodedId" +
            "&l=zh-Hans-CN&platform=web"
        ensureReady()
        val requestHeaders = LinkedHashMap(headers(true)).apply {
            put("media-user-token", userToken)
        }
        val response = Http.request(
            AppleConst.AMP + path,
            if (favorite) "POST" else "DELETE",
            requestHeaders,
            null,
            15000,
            30000
        )
        if (response.code !in 200..299) {
            throw IllegalStateException(
                "${if (favorite) "收藏" else "取消收藏"}失败，HTTP " +
                    "${response.code}：${response.text.take(300)}"
            )
        }
        Logx.i(
            TAG,
            "catalog song ${if (favorite) "favorited" else "unfavorited"} " +
                "(${response.code}): $songId"
        )
    }

    /**
     * 把某张「曲库专辑」加入用户的 Apple Music 资料库。
     *
     * 官方接口：`POST /v1/me/library?ids[albums]=<catalogAlbumId>`（幂等）。
     * 要点（原项目实测）：
     *  - 只用 amp-api 主机（api.music.apple.com 用网页版 token 会 403）
     *  - **不要带 l 参数**（storefront 代码 cn 不是合法语言标签，会 400）
     *  - 202 Accepted 是异步受理，也算成功
     */
    fun addCatalogAlbumToLibrary(albumId: String): Boolean {
        if (albumId.isEmpty()) return false
        val enc = java.net.URLEncoder.encode(albumId, "UTF-8")
        val url = "${AppleConst.AMP}/v1/me/library?ids%5Balbums%5D=$enc"
        return try {
            val h = LinkedHashMap(headers(true))
            h["Content-Type"] = "application/json"
            val r = Http.postRaw(url, "{}", h, 20000)
            if (r.code in 200..299) {
                Logx.i(TAG, "album added to library (${r.code}): $albumId")
                true
            } else {
                Logx.w(TAG, "add album failed HTTP ${r.code}: ${r.text.take(200)}")
                false
            }
        } catch (e: Throwable) {
            Logx.w(TAG, "add album exception: ${e.message}")
            false
        }
    }

    /** 资料库里已存在的曲库专辑 id 集合（用于「补齐」时去重）。 */
    fun libraryAlbumCatalogIds(): Set<String> {
        val ids = HashSet<String>()
        var offset = 0
        var guard = 0
        while (guard < 6) {
            val raw = try {
                ampGet("/v1/me/library/albums?limit=100&offset=$offset&include=tracks&l=en-US")
            } catch (e: Throwable) {
                break
            }
            val re = Regex("\"catalogId\":\"([0-9]+)\"")
            for (m in re.findAll(raw)) ids.add(m.groupValues[1])
            val items = Regex("\"type\":\"library-albums\"").findAll(raw).count()
            offset += items
            if (items < 100) break
            guard++
        }
        Logx.i(TAG, "library album catalogIds=${ids.size}")
        return ids
    }

    // ─────────────────────────── 播放资源 ───────────────────────────

    /**
     * webPlayback：拿到该歌的资产列表（含音频子清单 URL）。
     *
     * 资料库歌曲用 `universalLibraryId`，目录歌曲用 `salableAdamId`；
     * 返回的 `songList[0].songId` 是 **catalog id** —— 后面换密钥要用它。
     */
    fun getWebPlayback(songId: String, isLibrary: Boolean): WebPlaybackInfo {
        ensureReady()
        val body = if (isLibrary) {
            """{"universalLibraryId":"$songId","language":"en-US"}"""
        } else {
            """{"salableAdamId":"$songId","language":"en-US"}"""
        }
        val h = LinkedHashMap(headers(true))
        h["Content-Type"] = "application/json"
        val r = Http.request(AppleConst.WEBPLAYBACK, "POST", h, body.toByteArray(Charsets.UTF_8), 15000, 30000)
        if (r.code != 200) {
            throw RuntimeException("webPlayback HTTP ${r.code}: ${r.text.take(200)}")
        }
        val root = JSONObject(r.text)
        val list = root.optJSONArray("songList") ?: JSONArray()
        val info = WebPlaybackInfo()
        info.songId = songId

        // 【重要】webPlayback 在「商品已下架」这类失败时**依然返回 HTTP 200**，
        // 错误藏在 failureType / customerMessage 里。不看这两个字段的话，
        // 上层只会拿到一个空资产列表，然后报出「可能需要订阅」这种误导性提示。
        // 实测：已下架的歌返回
        //   failureType=3077, dialogId=MZCommerce.ItemNotFoundForFuse,
        //   customerMessage="The item you tried to download is no longer available."
        // 用 library id 冒充 catalog id 则返回 failureType=1010（NoSalableAdamId）。
        if (list.length() == 0) {
            val fail = root.optInt("failureType", 0)
            val msg = root.optString("customerMessage", "")
            val friendly = when (fail) {
                3077 -> "这首歌已从 Apple Music 下架，无法播放"
                1010 -> "这首歌没有可用的目录 ID（可能未在你所在区服上架）"
                0 -> msg.ifEmpty { "该歌曲没有可用的音频资源" }
                else -> msg.ifEmpty { "该歌曲没有可用的音频资源（failureType=$fail）" }
            }
            throw RuntimeException(friendly)
        }

        if (list.length() > 0) {
            val first = list.getJSONObject(0)
            info.songId = first.optString("songId", songId)
            val assets = first.optJSONArray("assets") ?: JSONArray()
            val out = ArrayList<AppleAsset>(assets.length())
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                out.add(AppleAsset(a.optString("flavor", ""), a.optString("URL", "")))
            }
            info.assets = out
        }
        return info
    }

    /**
     * `/v1/play/assets`：拿 HLS + license 服务器地址。
     *
     * `hlsEncryption=CBC&hlsProfile=enhancedHls` + `includeLicenseUrls=true`
     * 才会带 keyServerUrl / 证书地址（实测值）。
     */
    fun getPlayAssets(songId: String): PlayAssetInfo {
        val path = "/v1/play/assets?id=$songId&kind=song&includeLicenseUrls=true" +
                "&hlsEncryption=CBC&hlsProfile=enhancedHls"
        val info = PlayAssetInfo()
        try {
            val results = JSONObject(ampGet(path)).optJSONObject("results") ?: return info
            val arr = results.optJSONArray("assets") ?: return info
            if (arr.length() > 0) {
                val a = arr.getJSONObject(0)
                info.hlsUrl = a.optString("url", "")
                info.keyServerUrl = a.optString("keyServerUrl", "")
                info.widevineCertUrl = a.optString("widevineKeyCertificateUrl", "")
                info.fairPlayCertUrl = a.optString("fairPlayKeyCertificateUrl", "")
            }
        } catch (e: Throwable) {
            Logx.w(TAG, "getPlayAssets failed: ${e.message}")
        }
        return info
    }

    /** 下载任意 URL 为字节（HLS 清单 / 分片）。 */
    fun downloadBytes(url: String): ByteArray = downloadRange(url, -1, -1)

    /** 带 Range 的下载（HLS 分片按字节区间拉，服务端 206；少数回 200 也接受）。 */
    fun downloadRange(url: String, start: Int, end: Int): ByteArray {
        ensureReady()
        return Http.getBytes(url, headers(true), start, end, 60000)
    }

    /** 下载文本（m3u8）。 */
    fun downloadText(url: String): String = String(downloadBytes(url), Charsets.UTF_8)

    /**
     * 换 license（challenge 为 base64）。
     *
     * key-system 固定 `com.widevine.alpha`；adamId 必须是 catalog id。
     */
    fun getLicense(challengeB64: String, uri: String, adamId: String): String {
        val body = org.json.JSONObject().apply {
            put("challenge", challengeB64)
            put("key-system", "com.widevine.alpha")
            put("uri", uri)
            put("adamId", adamId)
            put("isLibrary", false)
            put("user-initiated", true)
        }.toString()
        val r = Http.request(
            AppleConst.LICENSE, "POST", licenseHeaders(), body.toByteArray(Charsets.UTF_8), 15000, 30000
        )
        Logx.i(TAG, "license http=${r.code} resp=${r.text.take(200)}")
        val lic = try {
            JSONObject(r.text).optString("license", "")
        } catch (e: Throwable) {
            ""
        }
        if (lic.isEmpty()) throw RuntimeException("license 响应异常: ${r.text.take(200)}")
        return lic
    }

    // ─────────────────────────── JSON → 模型 ───────────────────────────

    private fun parseSongs(txt: String, isLibrary: Boolean): PageResult<AppleSong> {
        val root = JSONObject(txt)
        val page = PageResult<AppleSong>()
        page.items = pickSongs(root, "data", isLibrary)
        val meta = root.optJSONObject("meta")
        if (meta != null) page.total = meta.optInt("total", 0)
        page.next = root.optString("next", "")
        return page
    }

    private fun parseAlbums(txt: String, isLibrary: Boolean): PageResult<AppleAlbum> {
        val page = PageResult<AppleAlbum>()
        page.items = pickAlbums(JSONObject(txt), "data", isLibrary)
        return page
    }

    private fun parsePlaylists(txt: String): PageResult<ApplePlaylist> {
        val page = PageResult<ApplePlaylist>()
        page.items = pickPlaylists(JSONObject(txt), "data")
        return page
    }

    private fun pickSongs(root: JSONObject, key: String, isLibrary: Boolean): MutableList<AppleSong> {
        val out = ArrayList<AppleSong>()
        val data = root.optJSONArray(key) ?: return out
        for (i in 0 until data.length()) {
            val it = data.getJSONObject(i)
            val attrs = it.optJSONObject("attributes") ?: JSONObject()
            val s = AppleSong()
            s.id = it.optString("id", "")
            s.name = attrs.optString("name", "")
            s.artistName = attrs.optString("artistName", "")
            s.albumName = attrs.optString("albumName", "")
            val relationships = it.optJSONObject("relationships")
            s.albumId = relationships?.optJSONObject("albums")
                ?.optJSONArray("data")?.optJSONObject(0)?.optString("id", "").orEmpty()
            s.artistId = relationships?.optJSONObject("artists")
                ?.optJSONArray("data")?.optJSONObject(0)?.optString("id", "").orEmpty()
            s.trackNumber = attrs.optInt("trackNumber", 0)
            s.discNumber = attrs.optInt("discNumber", 0)
            s.isLibrary = isLibrary
            s.durationMs = attrs.optLong("durationInMillis", 0)
            val rd = attrs.optString("releaseDate", "")
            if (rd.length >= 4) s.year = rd.substring(0, 4).toIntOrNull() ?: 0
            s.artworkUrl = bigArtwork(attrs.optJSONObject("artwork")?.optString("url", "") ?: "")
            val genres = attrs.optJSONArray("genreNames")
            if (genres != null && genres.length() > 0) {
                val parts = ArrayList<String>()
                for (g in 0 until genres.length()) {
                    val v = genres.optString(g, "")
                    if (v.isNotEmpty()) parts.add(v)
                }
                s.genre = parts.joinToString("|")
            }
            val traits = attrs.optJSONArray("audioTraits")
            if (traits != null) {
                val t = ArrayList<String>()
                for (g in 0 until traits.length()) t.add(traits.optString(g, ""))
                s.audioTraits = t
            }
            val pp = attrs.optJSONObject("playParams")
            if (pp != null) {
                val cid = pp.optString("catalogId", "")
                if (cid.isNotEmpty()) {
                    s.catalogId = cid
                    if (!isLibrary) s.id = cid
                }
            }
            out.add(s)
        }
        return out
    }

    private fun pickAlbums(root: JSONObject, key: String, isLibrary: Boolean): MutableList<AppleAlbum> {
        val out = ArrayList<AppleAlbum>()
        val data = root.optJSONArray(key) ?: return out
        for (i in 0 until data.length()) {
            val it = data.getJSONObject(i)
            val attrs = it.optJSONObject("attributes") ?: JSONObject()
            val a = AppleAlbum()
            a.id = it.optString("id", "")
            a.name = attrs.optString("name", "")
            a.artistName = attrs.optString("artistName", "")
            a.isLibrary = isLibrary
            a.trackCount = attrs.optInt("trackCount", 0)
            a.isSingle = attrs.optBoolean("isSingle", false)
            a.artworkUrl = bigArtwork(attrs.optJSONObject("artwork")?.optString("url", "") ?: "")
            val rd = attrs.optString("releaseDate", "")
            a.releaseDate = rd
            if (rd.length >= 4) a.year = rd.substring(0, 4).toIntOrNull() ?: 0
            out.add(a)
        }
        return out
    }

    private fun pickPlaylists(root: JSONObject, key: String): MutableList<ApplePlaylist> {
        val out = ArrayList<ApplePlaylist>()
        val data = root.optJSONArray(key) ?: return out
        for (i in 0 until data.length()) {
            val it = data.getJSONObject(i)
            val attrs = it.optJSONObject("attributes") ?: JSONObject()
            val p = ApplePlaylist()
            p.id = it.optString("id", "")
            p.name = attrs.optString("name", "")
            p.description = attrs.optJSONObject("description")?.optString("standard", "") ?: ""
            p.trackCount = attrs.optInt("trackCount", 0)
            p.artworkUrl = bigArtwork(attrs.optJSONObject("artwork")?.optString("url", "") ?: "")
            out.add(p)
        }
        return out
    }

    private fun pickArtists(root: JSONObject, key: String): MutableList<AppleArtist> {
        val out = ArrayList<AppleArtist>()
        val data = root.optJSONArray(key) ?: return out
        for (i in 0 until data.length()) {
            val it = data.getJSONObject(i)
            val attrs = it.optJSONObject("attributes") ?: JSONObject()
            val a = AppleArtist()
            a.id = it.optString("id", "")
            a.name = attrs.optString("name", "")
            a.artworkUrl = bigArtwork(attrs.optJSONObject("artwork")?.optString("url", "") ?: "")
            out.add(a)
        }
        return out
    }

    /** Apple 的封面 URL 模板替换成 600x600。 */
    fun bigArtwork(url: String): String {
        if (url.isEmpty()) return ""
        return url.replace("{w}", "600").replace("{h}", "600").replace("{f}", "jpg")
    }

    // ═══════════════════ 主页 / 新发现 / 广播 ═══════════════════

    /**
     * **主页**：个性化推荐货架。
     *
     * 参数与网页版 listen-now 请求保持一致；资源以 JSON:API resource map 返回，
     * 卡片内容及推荐理由由 [parseShelves] 从 resources 中解析。
     */
    fun getRecommendations(): List<Shelf> {
        val timezone = ZoneId.systemDefault().rules.getOffset(Instant.now()).id
        val types = listOf(
            "activities", "albums", "apple-curators", "artists", "concerts", "curators",
            "editorial-items", "library-albums", "library-playlists", "music-movies",
            "music-videos", "playlists", "social-profiles", "social-upsells", "songs",
            "stations", "tv-episodes", "tv-shows", "uploaded-audios", "uploaded-videos"
        ).joinToString("%2C")
        val path = "/v1/me/recommendations?art%5Burl%5D=f" +
                "&displayFilter%5Bkind%5D=" +
                "MusicCircleCoverShelf%2CMusicConcertsEmptyShelf%2CMusicCoverGrid%2C" +
                "MusicCoverShelf%2CMusicNotesHeroShelf%2CMusicSocialCardShelf%2C" +
                "MusicSuperHeroShelf" +
                "&extend=editorialArtwork%2CeditorialVideo%2CplainEditorialCard%2CplainEditorialNotes" +
                "&extend%5Bplaylists%5D=artistNames" +
                "&extend%5Bstations%5D=airTime%2CsupportsAirTimeUpdates" +
                "&fields%5Bartists%5D=name%2Cartwork%2Curl" +
                "&format%5Bresources%5D=map" +
                "&include%5Balbums%5D=artists" +
                "&include%5Blibrary-playlists%5D=catalog" +
                "&include%5Bpersonal-recommendation%5D=primary-content" +
                "&include%5Bstations%5D=radio-show" +
                "&l=zh-Hans-CN" +
                "&meta%5Bstations%5D=inflectionPoints" +
                "&name=listen-now" +
                "&omit%5Bresource%5D=autos" +
                "&platform=web" +
                "&timezone=${java.net.URLEncoder.encode(timezone, "UTF-8")}" +
                "&types=$types" +
                "&with=library"
        val txt = ampGet(path)
        return parseShelves(JSONObject(txt))
    }

    /** 请求网页版实际使用的个性电台 next-tracks 接口。 */
    fun probeStationNextTracks(stationId: String, limit: Int? = null): StationProbeResponse {
        require(stationId.matches(Regex("[A-Za-z0-9._-]+"))) { "无效的电台 ID" }
        require(limit == null || limit in 1..20) { "电台歌曲数量必须在 1 到 20 之间" }
        val path = "/v1/me/stations/next-tracks/$stationId" +
            (limit?.let { "?limit=$it" } ?: "")
        ensureReady()
        val requestHeaders = LinkedHashMap(headers(true)).apply {
            put("media-user-token", userToken)
        }
        val response = Http.request(
            AppleConst.AMP + path,
            "POST",
            requestHeaders,
            null,
            15000,
            30000
        )
        return StationProbeResponse(path, response.code, response.text)
    }

    /**
     * **新发现**：排行榜。
     *
     * `/v1/catalog/{sf}/charts?types=songs,albums,playlists` 返回三组榜单，
     * 每组可能是多个榜（如「Top 100: 土耳其」），这里各取第一个榜做成一个货架。
     */
    fun getCharts(): List<Shelf> {
        val sf = if (storefront.isNotEmpty()) storefront else "us"
        val txt = ampGet("/v1/catalog/$sf/charts?types=songs,albums,playlists&limit=20&l=zh-Hans")
        val results = JSONObject(txt).optJSONObject("results") ?: return emptyList()
        val out = ArrayList<Shelf>(3)
        for (type in listOf("songs", "albums", "playlists")) {
            val arr = results.optJSONArray(type) ?: continue
            if (arr.length() == 0) continue
            val chart = arr.optJSONObject(0) ?: continue
            val items = parseItems(chart.optJSONArray("data"))
            if (items.isEmpty()) continue
            out.add(
                Shelf(
                    id = "chart-$type",
                    title = chart.optString("name", "").ifEmpty { chartTitle(type) },
                    subtitle = "排行榜",
                    kind = "MusicCoverShelf",
                    items = items
                )
            )
        }
        return out
    }

    private fun chartTitle(type: String) = when (type) {
        "songs" -> "热门歌曲"
        "albums" -> "热门专辑"
        "playlists" -> "热门歌单"
        else -> "排行榜"
    }

    private fun parseShelves(root: JSONObject): List<Shelf> {
        val data = root.optJSONArray("data") ?: return emptyList()
        val resources = root.optJSONObject("resources") ?: JSONObject()
        val out = ArrayList<Shelf>(data.length())
        for (i in 0 until data.length()) {
            val reference = data.optJSONObject(i) ?: continue
            val s = resolveResource(reference, resources)
            val attrs = s.optJSONObject("attributes") ?: JSONObject()

            // 标题：网页版是 {"stringForDisplay":"..."}，有时直接是字符串。
            // 【坑】不能直接用 optString("title")：当 title 是对象时，
            // org.json 会把整个对象 toString() 出来，界面上就会显示
            // {"stringForDisplay":"专属推荐歌单"} 这种原始 JSON。
            val titleRaw = attrs.opt("title")
            val title = when (titleRaw) {
                is JSONObject -> titleRaw.optString("stringForDisplay", "")
                is String -> titleRaw
                else -> attrs.optString("name", "")
            }
            if (title.isEmpty()) continue

            val items = parseItems(
                s.optJSONObject("relationships")?.optJSONObject("contents")?.optJSONArray("data"),
                resources
            )
            if (items.isEmpty()) continue

            // display.kind（MusicCoverShelf / MusicNotesHeroShelf…）只是内部版式标记，
            // 不要当副标题显示给用户。副标题留空，界面会自动隐藏。
            val kind = attrs.optJSONObject("display")?.optString("kind", "") ?: ""
            out.add(
                Shelf(
                    id = s.optString("id", "shelf$i"),
                    title = title,
                    subtitle = "",
                    kind = kind,
                    items = items
                )
            )
        }
        return out
    }

    /** 把货架上的原始条目转成 [ShelfItem]（封面统一换成 600x600）。 */
    private fun parseItems(
        arr: JSONArray?,
        resources: JSONObject = JSONObject()
    ): List<ShelfItem> {
        if (arr == null) return emptyList()
        val out = ArrayList<ShelfItem>(arr.length())
        for (i in 0 until arr.length()) {
            val reference = arr.optJSONObject(i) ?: continue
            val it = resolveResource(reference, resources)
            val attrs = it.optJSONObject("attributes") ?: continue
            val type = it.optString("type", reference.optString("type", ""))
            val name = attrs.optString("name", "")
            if (name.isEmpty()) continue
            val songLike = attrs.has("artistName") &&
                (attrs.has("durationInMillis") || attrs.has("playParams") || attrs.has("albumName"))
            val catalogId = attrs.optJSONObject("playParams")
                ?.optString("catalogId", "")
                .orEmpty()
                .ifBlank {
                    it.optJSONObject("relationships")
                        ?.optJSONObject("catalog")
                        ?.optJSONArray("data")
                        ?.optJSONObject(0)
                        ?.optString("id", "")
                        .orEmpty()
                }
            val relatedCatalogId = it.optJSONObject("relationships")
                ?.optJSONObject("catalog")
                ?.optJSONArray("data")
                ?.optJSONObject(0)
                ?.optString("id", "")
                .orEmpty()
            val descriptionValue = attrs.opt("description")
            val collectionDescription = when (descriptionValue) {
                is JSONObject -> descriptionValue.optString(
                    "standard",
                    descriptionValue.optString("stringForDisplay", "")
                )
                is String -> descriptionValue
                else -> ""
            }
            val curatorId = it.optJSONObject("relationships")
                ?.optJSONObject("curator")
                ?.optJSONArray("data")
                ?.optJSONObject(0)
                ?.optString("id", "")
                .orEmpty()
            val relatedCuratorName = resources.optJSONObject("curators")
                ?.optJSONObject(curatorId)
                ?.optJSONObject("attributes")
                ?.optString("name", "")
                .orEmpty()
            val creator = attrs.optString("curatorName", "").ifBlank {
                relatedCuratorName
            }.ifBlank {
                attrs.optString("artistName", "")
            }
            val editorialVideo = attrs.optJSONObject("editorialVideo")
            val motionArtwork = editorialVideo?.optJSONObject("motionDetailTall")
                ?: editorialVideo?.optJSONObject("motionTallVideo3x4")
                ?: editorialVideo?.optJSONObject("motionDetailSquare")
            val squareMotionArtwork = editorialVideo?.optJSONObject("motionDetailSquare")
            val motionPreview = motionArtwork?.optJSONObject("previewFrame")
            val staticDetailArtwork = attrs.optJSONObject("editorialArtwork")
                ?.optJSONObject("staticDetailTall")
            val ratioSource = motionPreview ?: staticDetailArtwork
            val artworkAspectRatio = if (ratioSource != null) {
                val width = ratioSource.optDouble("width", 0.0)
                val height = ratioSource.optDouble("height", 0.0)
                if (width > 0.0 && height > 0.0) {
                    (width / height).toFloat().coerceIn(0.5f, 2f)
                } else {
                    0.75f
                }
            } else {
                0.75f
            }
            val reason = reference.optJSONObject("meta")
                ?.optJSONObject("reason")
                ?.optString("stringForDisplay", "")
                .orEmpty()
            val subtitle = when (type) {
                "albums", "songs", "library-songs" -> attrs.optString("artistName", "")
                "playlists", "library-playlists" -> attrs.optJSONArray("artistNames")
                    ?.let { names -> (0 until names.length()).map { names.optString(it) }.joinToString(", ") }
                    ?.ifBlank { attrs.optString("curatorName", "") }
                    ?: attrs.optString("curatorName", "")
                "stations" -> attrs.optString("kind", "").let { k ->
                    when (k) {
                        "user" -> "个性化电台"
                        "live" -> "直播电台"
                        "editorial" -> "编辑电台"
                        else -> "电台"
                    }
                }

                else -> ""
            }
            out.add(
                ShelfItem(
                    id = it.optString("id", ""),
                    type = type,
                    title = name,
                    subtitle = subtitle,
                    artwork = bigArtwork(attrs.optJSONObject("artwork")?.optString("url", "") ?: ""),
                    reason = reason,
                    heroArtwork = tallArtwork(
                        attrs.optJSONObject("editorialArtwork")
                            ?.optJSONObject("superHeroTall")
                            ?.optString("url", "")
                            .orEmpty()
                    ),
                    catalogId = catalogId,
                    songLike = songLike,
                    relatedCatalogId = relatedCatalogId,
                    collectionDescription = collectionDescription,
                    creator = creator,
                    trackCount = attrs.optInt("trackCount", 0),
                    updatedAt = attrs.optString("lastModifiedDate", "")
                        .ifBlank { attrs.optString("releaseDate", "") },
                    ambientVideoUrl = motionArtwork?.optString("video", "").orEmpty(),
                    ambientPreviewUrl = motionArtworkUrl(motionPreview, 600, 800),
                    ambientSquareVideoUrl = squareMotionArtwork
                        ?.optString("video", "")
                        .orEmpty(),
                    ambientSquarePreviewUrl = motionArtworkUrl(
                        squareMotionArtwork?.optJSONObject("previewFrame"),
                        600,
                        600
                    ),
                    artworkAspectRatio = artworkAspectRatio
                )
            )
        }
        return out
    }

    private fun resolveResource(reference: JSONObject, resources: JSONObject): JSONObject {
        val type = reference.optString("type", "")
        val id = reference.optString("id", "")
        return resources.optJSONObject(type)?.optJSONObject(id) ?: reference
    }

    private fun tallArtwork(url: String): String {
        if (url.isEmpty()) return ""
        return url.replace("{w}", "600")
            .replace("{h}", "800")
            .replace("{f}", "jpg")
    }

    /**
     * 把主页货架里的「电台」挑出来，给**广播**页用。
     *
     * 个性电台曲目由服务端按需生成，常规目录关系接口未必能读取。
     * 候选端点可从独立的电台诊断页按账号实测；此列表仍只用于展示。
     */
    fun stationsFrom(shelves: List<Shelf>): List<Shelf> =
        shelves.filter { sh -> sh.items.any { it.isStation } }
            .map { sh -> Shelf(sh.id, sh.title, sh.subtitle, sh.kind, sh.items.filter { it.isStation }) }
            .filter { it.items.isNotEmpty() }
}

/** token 持久化的最小抽象（实现见 TokenStore，用 SharedPreferences）。 */
interface TokenStore {
    fun getString(key: String, def: String): String
    fun putString(key: String, value: String)
    fun remove(key: String)
}
