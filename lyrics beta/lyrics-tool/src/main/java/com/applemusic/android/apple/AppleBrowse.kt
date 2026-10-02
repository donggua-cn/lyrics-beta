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
    val artwork: String
) {
    val isAlbum get() = type == "albums"
    val isPlaylist get() = type == "playlists"
    val isStation get() = type == "stations"
    val isSong get() = type == "songs"

    /** 卡片右下角的小标签。 */
    val badge: String
        get() = when (type) {
            "albums" -> "专辑"
            "playlists" -> "歌单"
            "stations" -> "电台"
            "songs" -> "歌曲"
            else -> ""
        }
}
