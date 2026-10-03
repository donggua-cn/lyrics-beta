package com.applemusic.lyricstool.ui

import android.content.Context
import android.util.Log
import com.applemusic.android.apple.Shelf
import com.applemusic.android.apple.ShelfItem
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

internal data class HomeRecommendationsSnapshot(
    val shelves: List<Shelf>,
    val savedAtMs: Long
)

internal object HomeRecommendationsCache {
    private const val TAG = "HomeRecommendations"
    private const val PREFERENCES = "home_recommendations"
    private const val SNAPSHOT_PREFIX = "snapshot_"
    private const val FRESHNESS_MS = 24L * 60L * 60L * 1000L

    fun read(context: Context, userToken: String): HomeRecommendationsSnapshot? {
        if (userToken.isBlank()) return null
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val key = snapshotKey(userToken)
        val encoded = preferences.getString(key, null) ?: return null
        return try {
            val snapshot = JSONObject(encoded)
            val shelfArray = snapshot.optJSONArray("shelves") ?: JSONArray()
            val shelves = buildList {
                for (index in 0 until shelfArray.length()) {
                    shelfArray.optJSONObject(index)?.let { add(it.toShelf()) }
                }
            }
            HomeRecommendationsSnapshot(shelves, snapshot.optLong("savedAtMs", 0L))
        } catch (error: Exception) {
            Log.e(TAG, "无法读取首页推荐缓存", error)
            preferences.edit().remove(key).apply()
            null
        }
    }

    fun write(context: Context, userToken: String, shelves: List<Shelf>) {
        if (userToken.isBlank()) return
        try {
            val shelfArray = JSONArray().apply {
                shelves.forEach { put(it.toJson()) }
            }
            val value = JSONObject()
                .put("savedAtMs", System.currentTimeMillis())
                .put("shelves", shelfArray)
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putString(snapshotKey(userToken), value.toString())
                .apply()
        } catch (error: Exception) {
            Log.e(TAG, "无法保存首页推荐缓存", error)
        }
    }

    fun isFresh(savedAtMs: Long, nowMs: Long = System.currentTimeMillis()): Boolean =
        savedAtMs > 0L && nowMs >= savedAtMs && nowMs - savedAtMs < FRESHNESS_MS

    private fun snapshotKey(userToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(userToken.toByteArray(Charsets.UTF_8))
        val fingerprint = digest.joinToString("") { "%02x".format(it) }
        return "$SNAPSHOT_PREFIX$fingerprint"
    }

    private fun Shelf.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("subtitle", subtitle)
        put("kind", kind)
        put("items", JSONArray().apply { items.forEach { put(it.toJson()) } })
    }

    private fun JSONObject.toShelf(): Shelf {
        val itemArray = optJSONArray("items") ?: JSONArray()
        val shelfItems = buildList {
            for (index in 0 until itemArray.length()) {
                itemArray.optJSONObject(index)?.let { add(it.toShelfItem()) }
            }
        }
        return Shelf(
            id = optString("id"),
            title = optString("title"),
            subtitle = optString("subtitle"),
            kind = optString("kind"),
            items = shelfItems
        )
    }

    private fun ShelfItem.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type)
        put("title", title)
        put("subtitle", subtitle)
        put("artwork", artwork)
        put("reason", reason)
        put("heroArtwork", heroArtwork)
        put("catalogId", catalogId)
        put("songLike", songLike)
        put("relatedCatalogId", relatedCatalogId)
        put("collectionDescription", collectionDescription)
        put("creator", creator)
        put("trackCount", trackCount)
        put("updatedAt", updatedAt)
        put("ambientVideoUrl", ambientVideoUrl)
        put("ambientPreviewUrl", ambientPreviewUrl)
        put("ambientSquareVideoUrl", ambientSquareVideoUrl)
        put("ambientSquarePreviewUrl", ambientSquarePreviewUrl)
        put("artworkAspectRatio", artworkAspectRatio)
    }

    private fun JSONObject.toShelfItem(): ShelfItem = ShelfItem(
        id = optString("id"),
        type = optString("type"),
        title = optString("title"),
        subtitle = optString("subtitle"),
        artwork = optString("artwork"),
        reason = optString("reason"),
        heroArtwork = optString("heroArtwork"),
        catalogId = optString("catalogId"),
        songLike = optBoolean("songLike"),
        relatedCatalogId = optString("relatedCatalogId"),
        collectionDescription = optString("collectionDescription"),
        creator = optString("creator"),
        trackCount = optInt("trackCount"),
        updatedAt = optString("updatedAt"),
        ambientVideoUrl = optString("ambientVideoUrl"),
        ambientPreviewUrl = optString("ambientPreviewUrl"),
        ambientSquareVideoUrl = optString("ambientSquareVideoUrl"),
        ambientSquarePreviewUrl = optString("ambientSquarePreviewUrl"),
        artworkAspectRatio = optDouble("artworkAspectRatio", 0.75).toFloat()
    )
}
