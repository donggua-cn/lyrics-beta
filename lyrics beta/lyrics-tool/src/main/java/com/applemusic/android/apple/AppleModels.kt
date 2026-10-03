package com.applemusic.android.apple

/**
 * Apple Music 数据模型 —— 对应鸿蒙版 `AppleModels.ets`。
 *
 * 设计原则一致：只保留这个音乐 App 需要的字段，不把整个 API 结构搬进来。
 */

/** 一首歌（目录或资料库）。 */
data class AppleSong(
    /** Apple Music 歌曲 ID（catalog id / library id） */
    var id: String = "",
    var name: String = "",
    var artistName: String = "",
    var albumName: String = "",
    var albumId: String = "",
    var artistId: String = "",
    /** 时长（毫秒） */
    var durationMs: Long = 0,
    var trackNumber: Int = 0,
    var discNumber: Int = 0,
    var year: Int = 0,
    /** 封面 URL（已按需要的尺寸替换） */
    var artworkUrl: String = "",
    /** 是否来自资料库（true 表示 library song） */
    var isLibrary: Boolean = false,
    /** 音频特性（lossless / dolby-atmos / hi-res-lossless） */
    var audioTraits: List<String> = emptyList(),
    /** Apple 曲风（genreNames 用 `|` 连接） */
    var genre: String = "",
    /**
     * 目录歌曲 id（playParams.catalogId）。
     * 资料库歌曲的 id 是 `i.xxx`，但拿它才能批量向目录请求「这首歌的主艺人」。
     */
    var catalogId: String = ""
)

data class AppleAlbum(
    var id: String = "",
    var name: String = "",
    var artistName: String = "",
    var artistId: String = "",
    var artworkUrl: String = "",
    var trackCount: Int = 0,
    var year: Int = 0,
    var isLibrary: Boolean = false,
    /** Apple 标记：是否为单曲（用来分「专辑 / 单曲」两个板块） */
    var isSingle: Boolean = false,
    var releaseDate: String = "",
    var catalogStorefront: String = ""
)

data class AppleArtist(
    var id: String = "",
    var name: String = "",
    var artworkUrl: String = "",
    var albumCount: Int = 0,
    var songCount: Int = 0,
    var motionVideoUrl: String = "",
    var motionPreviewUrl: String = "",
    var motionBackgroundColor: String = "",
    var motionTextColor: String = "",
    var editorialDescription: String = "",
    var bornOrFormed: String = "",
    var isGroup: Boolean = false,
    var genreNames: List<String> = emptyList()
)

data class AppleArtistPage(
    val artist: AppleArtist,
    val topSongs: List<AppleSong> = emptyList(),
    val latestRelease: AppleAlbum? = null,
    val albums: List<AppleAlbum> = emptyList(),
    val singles: List<AppleAlbum> = emptyList(),
    val musicVideos: List<AppleAlbum> = emptyList(),
    val playlists: List<ApplePlaylist> = emptyList(),
    val similarArtists: List<AppleArtist> = emptyList()
)

data class ApplePlaylist(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var artworkUrl: String = "",
    var trackCount: Int = 0,
    var isLibrary: Boolean = true
)

/** webPlayback 返回的某个资产（audio asset）。 */
data class AppleAsset(
    /** 资产 flavor，如 `28:ctrp256` */
    var flavor: String = "",
    /** 子清单 m3u8 URL */
    var url: String = ""
)

data class WebPlaybackInfo(
    var songId: String = "",
    var assets: List<AppleAsset> = emptyList()
)

/** `/v1/play/assets` 结果。 */
data class PlayAssetInfo(
    var hlsUrl: String = "",
    var keyServerUrl: String = "",
    var widevineCertUrl: String = "",
    var fairPlayCertUrl: String = ""
)

data class AppleSearchResult(
    var songs: MutableList<AppleSong> = ArrayList(),
    var albums: MutableList<AppleAlbum> = ArrayList(),
    var artists: MutableList<AppleArtist> = ArrayList(),
    var playlists: MutableList<ApplePlaylist> = ArrayList()
)

/** 分页返回。 */
class PageResult<T> {
    var items: MutableList<T> = ArrayList()
    var total = 0
    var next = ""

    fun isEmpty(): Boolean = items.isEmpty()
    fun size(): Int = items.size
    operator fun get(i: Int): T = items[i]
    fun firstOrNull(): T? = if (items.isEmpty()) null else items[0]
}

/** Apple Music 常量（对应 `AppleConst`）。 */
object AppleConst {
    /** 网页版基址：抓 dev token / 取 cookie 用 */
    const val HOME = "https://music.apple.com"

    /** 内部 API：资料库、目录、搜索都走它（需要 Bearer + media-user-token） */
    const val AMP = "https://amp-api.music.apple.com"

    /** MusicKit 官方主机（资料库写入等接口） */
    const val API = "https://api.music.apple.com"

    const val WEBPLAYBACK = "https://play.itunes.apple.com/WebObjects/MZPlay.woa/wa/webPlayback"
    const val LICENSE = "https://play.itunes.apple.com/WebObjects/MZPlay.woa/wa/acquireWebPlaybackLicense"

    /** Widevine system id */
    const val WIDEVINE_UUID = "edef8ba9-79d6-4ace-a3c8-27dcd51d21ed"

    /** 默认音质：256kbps AAC（aac-web） */
    const val DEFAULT_FLAVOR = "28:ctrp256"

    /** 桌面 Chrome UA：能过风控的关键之一 */
    const val UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
}
