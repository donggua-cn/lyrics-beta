package com.applemusic.android.apple

/**
 * 一个「货架」（横向滚动的一排卡片）。
 *
 * Apple Music 的主页 / 新发现 / 广播 都是这个结构：
 * `/v1/me/recommendations` 返回一堆 shelf，每个 shelf 有标题 + 一排内容，
 * 内容可能是专辑、歌单、电台或歌曲。
 */
class Shelf(
    val id: String,
    /** 显示用标题，如「专属推荐歌单」「最新发行」「专属电台推荐」。 */
    val title: String,
    val subtitle: String,
    /** Apple 的货架样式标记（MusicCoverShelf / MusicNotesHeroShelf …），仅用于展示风格。 */
    val kind: String,
    val items: List<ShelfItem>
)

/** 货架上的一个卡片。 */
class ShelfItem(
    val id: String,
    /** `albums` / `playlists` / `stations` / `songs`。 */
    val type: String,
    val title: String,
    val subtitle: String,
    /** 已替换好尺寸的封面 URL（可能为空，如部分电台）。 */
    val artwork: String,
    /** Apple 给此条推荐附带的个性化原因，如 Made for You / New Release。 */
    val reason: String = "",
    /** 适用于主页精选竖版卡片的 editorial artwork。 */
    val heroArtwork: String = "",
    /** Apple 给可播放歌曲提供的目录 ID，资料库卡片可能与资源 ID 不同。 */
    val catalogId: String = "",
    /** 按曲目属性识别歌曲，用于 Apple 返回非标准歌曲资源类型时兜底。 */
    val songLike: Boolean = false,
    /** 资料库歌单可能同时关联其目录歌单资源。 */
    val relatedCatalogId: String = "",
    val collectionDescription: String = "",
    val creator: String = "",
    val trackCount: Int = 0,
    val updatedAt: String = "",
    val ambientVideoUrl: String = "",
    val ambientPreviewUrl: String = "",
    val ambientSquareVideoUrl: String = "",
    val ambientSquarePreviewUrl: String = "",
    val artworkAspectRatio: Float = 0.75f,
    val catalogStorefront: String = ""
) {
    val isAlbum get() = type.contains("album", ignoreCase = true)
    val isPlaylist get() = type.contains("playlist", ignoreCase = true)
    val isLibraryPlaylist get() = type.contains("library", ignoreCase = true) &&
        isPlaylist
    val isStation get() = type == "stations"
    val isArtist get() = type == "artists"
    val isSong get() =
        type.contains("song", ignoreCase = true) ||
            songLike ||
            catalogId.isNotBlank() ||
            id.startsWith("i.")
    val isLibrarySong get() =
        type.contains("library", ignoreCase = true) || id.startsWith("i.")

    /** 卡片右下角的小标签。 */
    val badge: String
        get() = when (type) {
            "albums" -> "专辑"
            "playlists", "library-playlists" -> "歌单"
            "stations" -> "电台"
            else -> if (isSong) "歌曲" else ""
        }

}

data class CollectionMetadata(
    val title: String = "",
    val catalogId: String = "",
    val creator: String = "",
    val creatorId: String = "",
    val description: String = "",
    val trackCount: Int = 0,
    val updatedAt: String = "",
    val artworkUrl: String = "",
    val ambientVideoUrl: String = "",
    val ambientPreviewUrl: String = "",
    val ambientSquareVideoUrl: String = "",
    val ambientSquarePreviewUrl: String = "",
    val artworkAspectRatio: Float = 0.75f
)
