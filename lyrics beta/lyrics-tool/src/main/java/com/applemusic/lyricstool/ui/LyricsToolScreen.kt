package com.applemusic.lyricstool.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import android.view.MotionEvent
import android.view.TextureView
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.recyclerview.widget.RecyclerView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.ErrorResult
import coil.request.SuccessResult
import androidx.palette.graphics.Palette
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CancellationException
import com.applemusic.lyricstool.data.AppleLyricsClient
import com.applemusic.lyricstool.data.FetchedLyrics
import com.applemusic.lyricstool.data.LyricsCache
import com.applemusic.lyricstool.data.QQMusicLyricsClient
import com.applemusic.lyricstool.data.SongHit
import org.json.JSONObject
import com.applemusic.android.apple.AppleLibrarySync
import com.applemusic.android.apple.AppleArtist
import com.applemusic.android.apple.AppleArtistPage
import com.applemusic.android.apple.AppleAlbum
import com.applemusic.android.apple.ApplePlaylist
import com.applemusic.android.apple.AppleSong
import com.applemusic.android.apple.CollectionMetadata
import com.applemusic.android.apple.AppleCache
import com.applemusic.android.apple.MusicRepository
import com.applemusic.android.apple.Shelf
import com.applemusic.android.apple.ShelfItem
import com.applemusic.android.player.MusicService
import com.applemusic.android.player.PlaybackEngine
import com.applemusic.android.player.LiveLyricsNotification
import com.applemusic.android.lyrics.LyricsParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

@Composable
private fun LyricsToolTheme(
    context: android.content.Context,
    themeMode: AppThemeMode,
    useDynamicColors: Boolean,
    manualSeed: Color,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val colorScheme = remember(context, themeMode, useDynamicColors, manualSeed, systemDark) {
        if (useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else if (darkTheme) {
            darkColorScheme(
                primary = lerp(manualSeed, Color.White, 0.28f),
                onPrimary = Color(0xFF211A2B),
                primaryContainer = lerp(manualSeed, Color.Black, 0.48f),
                onPrimaryContainer = lerp(manualSeed, Color.White, 0.72f),
                secondary = lerp(manualSeed, Color(0xFF8CC8BB), 0.42f),
                secondaryContainer = lerp(manualSeed, Color.Black, 0.38f),
                tertiary = lerp(manualSeed, Color(0xFFFFB4A8), 0.38f),
                background = Color(0xFF141218),
                surface = Color(0xFF141218),
                surfaceVariant = Color(0xFF302D35),
                onSurface = Color(0xFFE8E0EC),
                onSurfaceVariant = Color(0xFFCAC4D0)
            )
        } else {
            val background = Color(0xFFFFF8F7)
            lightColorScheme(
                primary = manualSeed,
                onPrimary = Color.White,
                primaryContainer = lerp(manualSeed, Color.White, 0.82f),
                onPrimaryContainer = lerp(manualSeed, Color.Black, 0.58f),
                secondary = lerp(manualSeed, Color(0xFF38756B), 0.45f),
                secondaryContainer = lerp(manualSeed, Color.White, 0.88f),
                tertiary = lerp(manualSeed, Color(0xFFB84F45), 0.45f),
                tertiaryContainer = lerp(manualSeed, Color.White, 0.88f),
                background = background,
                surface = background,
                surfaceVariant = lerp(manualSeed, Color.White, 0.93f),
                onSurface = Color(0xFF251D32),
                onSurfaceVariant = Color(0xFF514B5B)
            )
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

private enum class AppThemeMode(val preferenceValue: String, val label: String) {
    SYSTEM("system", "跟随系统"),
    LIGHT("light", "浅色"),
    DARK("dark", "深色");

    companion object {
        fun fromPreference(value: String?): AppThemeMode =
            entries.firstOrNull { it.preferenceValue == value } ?: SYSTEM
    }
}

private data class ThemeColorPreset(val name: String, val hex: String)

private val themeColorPresets = listOf(
    ThemeColorPreset("薰衣草紫", "#6750A4"),
    ThemeColorPreset("海蓝", "#0061A4"),
    ThemeColorPreset("青绿", "#006A60"),
    ThemeColorPreset("森林绿", "#386A20"),
    ThemeColorPreset("琥珀", "#825500"),
    ThemeColorPreset("玫瑰", "#9C4055")
)

private enum class ExportFormat(val extension: String) {
    Text("txt"),
    Lrc("lrc"),
    Ttml("ttml"),
    QqQrc("qq-qrc.txt")
}

private enum class ToolPage {
    Home,
    Collection,
    Artist,
    ArtistTopSongs,
    Search,
    Discovery,
    Radio,
    Library,
    Settings
}

private data class HomeCollection(
    val item: ShelfItem,
    val tracks: List<AppleLibrarySync.SongRow>,
    val metadata: CollectionMetadata? = null,
    val metadataError: String? = null,
    val nextTrackOffset: Int = tracks.size,
    val totalTrackCount: Int = 0,
    val hasMoreTracks: Boolean = false,
    val loadingMoreTracks: Boolean = false,
    val trackLoadError: String? = null
)

private data class LoadedCollectionTracks(
    val rows: List<AppleLibrarySync.SongRow>,
    val nextOffset: Int,
    val totalCount: Int,
    val hasMore: Boolean
)

private const val COLLECTION_TRACK_PAGE_SIZE = 20
private const val COLLECTION_TRACK_PREFETCH_REMAINING = 6
private const val FAVORITE_SONG_CACHE_TTL_MS = 30 * 60 * 1000L

private data class FavoriteSongCacheSnapshot(
    val songIds: Set<String>,
    val fetchedAtMs: Long
)

private fun favoriteSongCacheAccountKey(userToken: String): String? {
    if (userToken.isBlank()) return null
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(userToken.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { byte -> "%02x".format(byte) }
}

private fun isUnavailableCatalogPlaylistPage(
    error: RuntimeException,
    item: ShelfItem,
    offset: Int
): Boolean {
    val message = error.message ?: return false
    return !item.isLibraryPlaylist &&
        offset > 0 &&
        message.startsWith("HTTP 404 @ /v1/catalog/") &&
        message.contains("/playlists/") &&
        message.contains("/tracks?") &&
        message.contains("offset=$offset")
}

private fun loadRemainingPlaylistTracks(
    item: ShelfItem,
    initialRows: List<AppleLibrarySync.SongRow>,
    initialOffset: Int,
    initialTotalCount: Int
): LoadedCollectionTracks {
    val rows = initialRows.toMutableList()
    var nextOffset = initialOffset
    var totalCount = initialTotalCount
    var hasMore = true
    var pageCount = 0
    while (hasMore) {
        check(pageCount < 100) { "歌单曲目过多，暂时无法一次性加载完整列表" }
        val page = try {
            MusicRepository.api.getRecommendedPlaylistTracks(
                item.id,
                item.relatedCatalogId,
                item.isLibraryPlaylist,
                COLLECTION_TRACK_PAGE_SIZE,
                nextOffset
            )
        } catch (error: RuntimeException) {
            if (isUnavailableCatalogPlaylistPage(error, item, nextOffset)) {
                break
            }
            throw error
        }
        val newRows = page.items.map {
            MusicRepository.rowOf(it).also { row -> row.isLibrary = false }
        }
        if (newRows.isEmpty()) {
            break
        }
        rows += newRows
        nextOffset += page.size()
        totalCount = maxOf(totalCount, page.total)
        hasMore = page.next.isNotBlank() ||
            totalCount > nextOffset ||
            page.size() == COLLECTION_TRACK_PAGE_SIZE
        pageCount++
    }
    val distinctRows = rows.distinctBy { it.uri }
    MusicRepository.registerTransient(distinctRows)
    return LoadedCollectionTracks(
        rows = distinctRows,
        nextOffset = nextOffset,
        totalCount = totalCount,
        hasMore = false
    )
}

private data class HomeCollectionCardTransition(
    val item: ShelfItem,
    val sourceBounds: Rect
)

private data class PendingHomePlayback(
    val title: String,
    val tracks: List<AppleLibrarySync.SongRow> = emptyList(),
    val startIndex: Int = 0,
    val stationId: String? = null,
    val catalogArtistId: String? = null,
    val shuffleArtistContinuation: Boolean = false,
    val playlistContinuation: ShelfItem? = null,
    val playlistNextOffset: Int = 0,
    val playlistTotalCount: Int = 0
)

private data class LyricsAppearance(
    val highlightColor: String = "#6750A4",
    val darkTheme: Boolean = false,
    val useCoverAccent: Boolean = false,
    val glowRadius: Float = 5f,
    val fontSize: Float = 28f,
    val lineSpacing: Float = 18f,
    val currentLinePosition: Float = 18f,
    val autoScroll: Boolean = true,
    val interpolate: Boolean = true,
    val blur: Boolean = false,
    val reducedMotion: Boolean = false,
    val hidePlayedLines: Boolean = false,
    val traditionalToSimplified: Boolean = false,
    val customFontFileName: String? = null,
    val customFontDisplayName: String? = null
)

private data class LyricsFetchResult(
    val lyrics: FetchedLyrics?,
    val fallbackLyrics: FetchedLyrics? = null,
    val fallbackSourceName: String? = null,
    val searchOtherSources: Boolean = false,
    val notice: String? = null
)

private data class ManagedCacheItem(
    val songId: String,
    val playback: AppleCache.CacheItem?,
    val lyrics: LyricsCache.CacheItem?
)

private suspend fun fetchQqLyricsForAppleSong(
    appleClient: AppleLyricsClient,
    qqClient: QQMusicLyricsClient,
    lyricsCache: LyricsCache,
    song: SongHit
): FetchedLyrics? {
    lyricsCache.get("qq", song.id)?.let { return it }

    var metadataError: Exception? = null
    val localizedMetadata = try {
        appleClient.fetchChineseSongMetadata(song)
    } catch (error: Exception) {
        metadataError = error
        null
    }

    return try {
        val result = qqClient.fetchMatchingLyrics(
            title = song.title,
            artist = song.artist,
            durationMs = song.durationMs,
            album = song.album,
            localizedAliases = listOfNotNull(localizedMetadata)
        )
        if (result == null && metadataError != null) throw metadataError
        if (result != null) lyricsCache.put("qq", song.id, result)
        result
    } catch (error: Exception) {
        metadataError?.takeIf { it !== error }?.let(error::addSuppressed)
        throw error
    }
}

private suspend fun fetchAppleLyricsCached(
    appleClient: AppleLyricsClient,
    lyricsCache: LyricsCache,
    songId: String
): FetchedLyrics? {
    lyricsCache.get("apple", songId)?.let { return it }
    return appleClient.fetchLyrics(songId)?.also {
        lyricsCache.put("apple", songId, it)
    }
}

private suspend fun fetchAppleLyricsWithQqFallback(
    appleClient: AppleLyricsClient,
    qqClient: QQMusicLyricsClient,
    lyricsCache: LyricsCache,
    song: SongHit
): LyricsFetchResult {
    var appleError: Exception? = null
    val appleLyrics = try {
        fetchAppleLyricsCached(appleClient, lyricsCache, song.id)
    } catch (error: Exception) {
        appleError = error
        null
    }
    if ((appleLyrics?.lyrics?.wordTimedLines() ?: 0) > 0) {
        return LyricsFetchResult(appleLyrics)
    }

    var qqError: Exception? = null
    val qqLyrics = try {
        fetchQqLyricsForAppleSong(appleClient, qqClient, lyricsCache, song)
    } catch (error: Exception) {
        qqError = error
        null
    }
    if (qqLyrics != null) {
        return LyricsFetchResult(
            lyrics = qqLyrics,
            fallbackLyrics = appleLyrics,
            fallbackSourceName = "Apple Music",
            searchOtherSources = qqLyrics.lyrics.wordTimedLines() == 0,
            notice = if (qqLyrics.lyrics.wordTimedLines() == 0) {
                "QQ 音乐也没有逐字时间轴，正在尝试其他歌词来源"
            } else {
                "已自动切换到 QQ 音乐逐字歌词"
            }
        )
    }
    if (appleLyrics != null) {
        return LyricsFetchResult(
            lyrics = appleLyrics,
            searchOtherSources = true,
            notice = "正在尝试其他歌词来源"
        )
    }
    val error = qqError ?: appleError
    error?.let { qqError?.takeIf { qq -> qq !== it }?.let(it::addSuppressed) }
    return LyricsFetchResult(
        lyrics = null,
        searchOtherSources = true,
        notice = error?.message ?: "正在尝试其他歌词来源"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LyricsToolScreen(
    client: AppleLyricsClient,
    qqMusicClient: QQMusicLyricsClient,
    nowPlayingRequest: Long = 0L
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val lyricsCache = remember(context) { LyricsCache(context) }
    val themePreferences = remember(context) {
        context.getSharedPreferences("app_theme", android.content.Context.MODE_PRIVATE)
    }
    var themeMode by remember(context) {
        mutableStateOf(
            AppThemeMode.fromPreference(
                themePreferences.getString("theme_mode", null)
                    ?: if (themePreferences.getBoolean(
                            "use_system_colors",
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        )
                    ) {
                        "system"
                    } else {
                        "light"
                    }
            )
        )
    }
    var useDynamicColors by remember(context) {
        mutableStateOf(
            themePreferences.getBoolean(
                "use_system_colors",
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            )
        )
    }
    var manualThemeSeedHex by remember(context) {
        mutableStateOf(themePreferences.getString("manual_seed", "#6750A4") ?: "#6750A4")
    }
    val manualThemeSeed = remember(manualThemeSeedHex) {
        Color(android.graphics.Color.parseColor(manualThemeSeedHex))
    }
    LyricsToolTheme(context, themeMode, useDynamicColors, manualThemeSeed) {
        val appDarkTheme = when (themeMode) {
            AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            AppThemeMode.LIGHT -> false
            AppThemeMode.DARK -> true
        }
        val imageLoader = remember(context) { ImageLoader(context) }
        val scope = rememberCoroutineScope()
        val clipboard = LocalClipboardManager.current
        val snackbar = remember { SnackbarHostState() }
        val favoriteSongsPreferences = remember(context) {
            context.getSharedPreferences(
                "favorite_songs_cache",
                android.content.Context.MODE_PRIVATE
            )
        }
        val favoriteAccountKey = remember(client.userToken) {
            favoriteSongCacheAccountKey(client.userToken)
        }
        val cachedFavoriteSnapshot = remember(
            favoriteSongsPreferences,
            favoriteAccountKey
        ) {
            val accountMatches = favoriteAccountKey != null &&
                favoriteSongsPreferences.getString("account_key", null) == favoriteAccountKey
            val ids = favoriteSongsPreferences.getStringSet("song_ids", null)?.toSet()
            val fetchedAtMs = favoriteSongsPreferences.getLong("fetched_at_ms", 0L)
            if (accountMatches && ids != null && fetchedAtMs > 0L) {
                FavoriteSongCacheSnapshot(ids, fetchedAtMs)
            } else {
                null
            }
        }
        val profilePreferences = remember(context) {
            context.getSharedPreferences("user_profile", android.content.Context.MODE_PRIVATE)
        }
        var profileAvatarUri by remember(context) {
            mutableStateOf(
                profilePreferences.getString("avatar_uri", null)?.let(Uri::parse)
            )
        }
        var lyricsNotice by remember { mutableStateOf<String?>(null) }
        var query by remember { mutableStateOf("") }
        var songs by remember { mutableStateOf<List<SongHit>>(emptyList()) }
        var lyricSearchUsed by remember { mutableStateOf(false) }
        var selectedSong by remember { mutableStateOf<SongHit?>(null) }
        var lyricsCollapsed by remember { mutableStateOf(false) }
        var lyricsInteractionVersion by remember { mutableLongStateOf(0L) }
        var favoritedSongIds by remember(favoriteAccountKey) {
            mutableStateOf(cachedFavoriteSnapshot?.songIds)
        }
        var favoriteCacheFetchedAtMs by remember(favoriteAccountKey) {
            mutableLongStateOf(cachedFavoriteSnapshot?.fetchedAtMs ?: 0L)
        }
        var favoriteStatusLoading by remember { mutableStateOf(false) }
        var favoritePendingSongId by remember { mutableStateOf<String?>(null) }
        var selectedPage by remember { mutableStateOf(ToolPage.Home) }
        var lyricsReturnPage by remember { mutableStateOf(ToolPage.Home) }
        var selectedCollection by remember { mutableStateOf<HomeCollection?>(null) }
        var collectionReturnPage by remember { mutableStateOf(ToolPage.Home) }
        var artistPage by remember { mutableStateOf<AppleArtistPage?>(null) }
        val artistPageRecyclerState = rememberComposeRecyclerListState()
        val artistSectionStateHolder = rememberSaveableStateHolder()
        val artistTopSongsListState = rememberLazyListState()
        val collectionListState = rememberLazyListState()
        var artistPageHistory by remember { mutableStateOf<List<AppleArtistPage>>(emptyList()) }
        var artistReturnPage by remember { mutableStateOf(ToolPage.Home) }
        var artistPageLoading by remember { mutableStateOf(false) }
        var artistPageError by remember { mutableStateOf<String?>(null) }
        var artistTopSongs by remember { mutableStateOf<List<AppleSong>>(emptyList()) }
        var artistTopSongsLoading by remember { mutableStateOf(false) }
        var artistTopSongsHasMore by remember { mutableStateOf(false) }
        var favoriteArtistIds by remember { mutableStateOf<Set<String>?>(null) }
        var artistFavoriteOverride by remember { mutableStateOf<Boolean?>(null) }
        var artistSavePending by remember { mutableStateOf(false) }
        var collectionCardTransition by remember {
            mutableStateOf<HomeCollectionCardTransition?>(null)
        }
        var rootWindowOrigin by remember { mutableStateOf(Offset.Zero) }
        var stationStartPending by remember { mutableStateOf(false) }
        var pendingHomePlayback by remember { mutableStateOf<PendingHomePlayback?>(null) }
        var deferredRadio by remember { mutableStateOf<ShelfItem?>(null) }
        var deferredRadioPrompt by remember { mutableStateOf<ShelfItem?>(null) }
        var collectionLoading by remember { mutableStateOf(false) }
        var collectionError by remember { mutableStateOf<String?>(null) }
        val cachedHomeRecommendations = remember(context, client.userToken) {
            HomeRecommendationsCache.read(context, client.userToken)
        }
        var homeShelves by remember(context, client.userToken) {
            mutableStateOf(cachedHomeRecommendations?.shelves.orEmpty())
        }
        var homeLoading by remember { mutableStateOf(false) }
        var homeError by remember { mutableStateOf<String?>(null) }
        var homeRefreshKey by remember { mutableStateOf(0) }
        var handledHomeRefreshKey by remember { mutableStateOf(0) }
        var homeLastUpdatedAtMs by remember(context, client.userToken) {
            mutableLongStateOf(cachedHomeRecommendations?.savedAtMs ?: 0L)
        }
        var homeScrolled by remember { mutableStateOf(false) }
        var homeScrollStateInitialized by remember { mutableStateOf(false) }
        var artistNavigationCollapsed by remember { mutableStateOf(false) }
        val homeRecyclerState = rememberComposeRecyclerListState()
        val homeSectionStateHolder = rememberSaveableStateHolder()
        var fullscreenLyrics by remember { mutableStateOf(false) }
        var lyricsNavigationVisible by remember { mutableStateOf(false) }
        var fetchedLyrics by remember { mutableStateOf<FetchedLyrics?>(null) }
        var lyricsError by remember { mutableStateOf<String?>(null) }
        var searching by remember { mutableStateOf(false) }
        var loadingLyrics by remember { mutableStateOf(false) }
        var playbackPositionMs by remember { mutableStateOf(0L) }
        var playbackDurationMs by remember { mutableStateOf(0L) }
        var playbackIsPlaying by remember { mutableStateOf(false) }
        var nowPlayingSong by remember { mutableStateOf(PlaybackEngine.currentSong()?.toSongHitOrNull()) }
        var playbackQueue by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
        var playbackQueueIndex by remember { mutableStateOf(-1) }
        var queueDialogOpen by remember { mutableStateOf(false) }
        var coverQueueDragOffset by remember { mutableFloatStateOf(0f) }
        var playbackStatus by remember { mutableStateOf("") }
        var preparing by remember { mutableStateOf(true) }
        var signedIn by remember { mutableStateOf(client.isSignedIn) }
        var externalSourceMode by remember { mutableStateOf(false) }
        var externalSourceName by remember { mutableStateOf<String?>(null) }
        var sourceLookupInProgress by remember { mutableStateOf(false) }
        var sourceLookupRestoreOnFailure by remember { mutableStateOf(false) }
        var sourceLookupRequest by remember { mutableStateOf(0) }
        var externalFallbackLyrics by remember { mutableStateOf<FetchedLyrics?>(null) }
        var externalFallbackSourceName by remember { mutableStateOf<String?>(null) }
        var lyricsAppearanceOpen by remember { mutableStateOf(false) }
        val appearancePreferences = remember(context) {
            context.getSharedPreferences("lyrics_appearance", android.content.Context.MODE_PRIVATE)
        }
        val collapsedArtworkPreferences = remember(context) {
            context.getSharedPreferences("collapsed_lyrics_artwork", android.content.Context.MODE_PRIVATE)
        }
        var lyricsAppearance by remember(context) {
            mutableStateOf(
                LyricsAppearance(
                    highlightColor = appearancePreferences.getString("highlight_color", "#6750A4")
                        ?: "#6750A4",
                    useCoverAccent = appearancePreferences.getBoolean("use_cover_accent", false),
                    glowRadius = appearancePreferences.getFloat("glow_radius", 5f),
                    fontSize = appearancePreferences.getFloat("font_size", 28f),
                    lineSpacing = appearancePreferences.getFloat("line_spacing", 18f),
                    currentLinePosition = appearancePreferences.getFloat(
                        "current_line_position",
                        18f
                    ),
                    autoScroll = appearancePreferences.getBoolean("auto_scroll", true),
                    interpolate = appearancePreferences.getBoolean("interpolate", true),
                    blur = appearancePreferences.getBoolean("blur", false),
                    reducedMotion = appearancePreferences.getBoolean("reduced_motion", false),
                    hidePlayedLines = appearancePreferences.getBoolean("hide_played_lines", false),
                    traditionalToSimplified = appearancePreferences.getBoolean(
                        "traditional_to_simplified",
                        false
                    ),
                    customFontFileName = appearancePreferences
                        .getString("custom_font_file", null)
                        ?.takeIf { it.matches(Regex("lyrics-[a-f0-9-]+\\.(ttf|otf)")) },
                    customFontDisplayName = appearancePreferences
                        .getString("custom_font_name", null)
                )
            )
        }
        var coverAccentColor by remember { mutableStateOf<String?>(null) }
        var coverAccentResolvedFor by remember { mutableStateOf<String?>(null) }
        var coverAccentColors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
        var miniPlayerArtworkAccent by remember { mutableStateOf<Color?>(null) }
        var pendingExport by remember { mutableStateOf<Pair<String, ExportFormat>?>(null) }
        var cacheManagerOpen by remember { mutableStateOf(false) }
        var cachedItems by remember { mutableStateOf<List<AppleCache.CacheItem>>(emptyList()) }
        var cachedLyricsItems by remember { mutableStateOf<List<LyricsCache.CacheItem>>(emptyList()) }
        var playbackCacheLimitBytes by remember {
            mutableLongStateOf(MusicRepository.cache.maxCacheBytes())
        }
        var pendingCacheDelete by remember { mutableStateOf<AppleCache.CacheItem?>(null) }
        var statusBarLyricsEnabled by remember(context) {
            mutableStateOf(
                context.getSharedPreferences("lyrics_appearance", android.content.Context.MODE_PRIVATE)
                    .getBoolean("status_bar_lyrics_enabled", true)
            )
        }
        var lyricsFullscreenEnabled by remember(context) {
            mutableStateOf(
                appearancePreferences.getBoolean("lyrics_fullscreen_enabled", true)
            )
        }
        val immersiveLyrics = lyricsFullscreenEnabled &&
            selectedSong != null && selectedPage == ToolPage.Search
        val hideStatusBar = selectedPage != ToolPage.Search || immersiveLyrics
        val statusBarHeight = remember(context) {
            val resourceId = context.resources.getIdentifier(
                "status_bar_height",
                "dimen",
                "android"
            )
            if (resourceId != 0) {
                with(density) { context.resources.getDimensionPixelSize(resourceId).toDp() }
            } else {
                24.dp
            }
        }
        DisposableEffect(view) {
            val window = (context as? Activity)?.window
            val controller = window?.let { WindowInsetsControllerCompat(it, view) }
            val previousStatusBarColor = window?.statusBarColor
            val previousLightStatusBar = controller?.isAppearanceLightStatusBars
            val wasFullscreen = window?.let {
                it.attributes.flags and
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN != 0
            } ?: false
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.clearFlags(
                    android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                )
                window.addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                )
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                controller?.isAppearanceLightStatusBars = !appDarkTheme
                controller?.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (hideStatusBar) {
                    controller?.hide(WindowInsetsCompat.Type.statusBars())
                } else {
                    controller?.show(WindowInsetsCompat.Type.statusBars())
                }
            }
            onDispose {
                if (window != null) {
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                    previousStatusBarColor?.let { window.statusBarColor = it }
                    previousLightStatusBar?.let {
                        controller?.isAppearanceLightStatusBars = it
                    }
                    if (wasFullscreen) {
                        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    } else {
                        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    }
                    controller?.show(WindowInsetsCompat.Type.statusBars())
                }
            }
        }
        val playbackCardTopInset = remember(view, immersiveLyrics, density) {
            if (!immersiveLyrics) {
                0.dp
            } else {
                val rootInsets = view.rootWindowInsets
                val safeTopPx = if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    rootInsets != null
                ) {
                    maxOf(
                        rootInsets.getInsetsIgnoringVisibility(
                            android.view.WindowInsets.Type.statusBars()
                        ).top,
                        rootInsets.displayCutout?.safeInsetTop ?: 0
                    )
                } else {
                    rootInsets?.systemWindowInsetTop ?: 0
                }
                val fallbackTopPx = if (safeTopPx == 0) {
                    val resourceId = context.resources.getIdentifier(
                        "status_bar_height",
                        "dimen",
                        "android"
                    )
                    if (resourceId != 0) {
                        context.resources.getDimensionPixelSize(resourceId)
                    } else {
                        0
                    }
                } else {
                    safeTopPx
                }
                with(density) { fallbackTopPx.toDp() }
            }
        }
        SideEffect {
            val window = (context as? Activity)?.window
            val controller = window?.let { WindowInsetsControllerCompat(it, view) }
            window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }
            if (hideStatusBar) {
                window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
            } else {
                window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (hideStatusBar) {
                controller?.hide(WindowInsetsCompat.Type.statusBars())
            } else {
                controller?.isAppearanceLightStatusBars = !appDarkTheme
                controller?.show(WindowInsetsCompat.Type.statusBars())
            }
        }
        LaunchedEffect(selectedSong?.id, selectedPage, fullscreenLyrics, lyricsNavigationVisible) {
            if (selectedSong == null || selectedPage != ToolPage.Search) {
                lyricsNavigationVisible = true
            } else if (fullscreenLyrics) {
                lyricsNavigationVisible = false
            } else if (lyricsNavigationVisible) {
                delay(2_500L)
                lyricsNavigationVisible = false
            }
        }
        LaunchedEffect(lyricsNotice) {
            if (lyricsNotice != null) {
                delay(3_000L)
                lyricsNotice = null
            }
        }

        val loginLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                client.reloadSavedSession()
                signedIn = client.isSignedIn
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            MusicRepository.api.persist(client.userToken)
                            MusicRepository.api.getAccountInfo()
                        }
                        client.reloadSavedSession()
                        snackbar.showSnackbar("Apple Music 已连接")
                    } catch (error: Exception) {
                        snackbar.showSnackbar(
                            "登录凭据已保存，但账号区域读取失败：${error.message ?: "请重试"}"
                        )
                    }
                }
            }
        }
        val openLogin = {
            loginLauncher.launch(Intent(context, AppleMusicLoginActivity::class.java))
        }
        val refreshFavoriteStatus: (Boolean) -> Unit = { force ->
            val cacheIsFresh = favoritedSongIds != null &&
                System.currentTimeMillis() - favoriteCacheFetchedAtMs <
                FAVORITE_SONG_CACHE_TTL_MS
            if (
                signedIn &&
                favoriteAccountKey != null &&
                !favoriteStatusLoading &&
                (force || !cacheIsFresh)
            ) {
                scope.launch {
                    favoriteStatusLoading = true
                    try {
                        val favoriteIds = withContext(Dispatchers.IO) {
                            MusicRepository.api.getFavoriteCatalogSongIds()
                        }
                        val fetchedAtMs = System.currentTimeMillis()
                        favoritedSongIds = favoriteIds
                        favoriteCacheFetchedAtMs = fetchedAtMs
                        favoriteSongsPreferences.edit()
                            .putString("account_key", favoriteAccountKey)
                            .putStringSet("song_ids", favoriteIds.toSet())
                            .putLong("fetched_at_ms", fetchedAtMs)
                            .apply()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        snackbar.showSnackbar(
                            "无法读取 Apple Music 喜爱歌曲列表：" +
                                (error.message ?: "请稍后重试")
                        )
                    } finally {
                        favoriteStatusLoading = false
                    }
                }
            }
        }
        LaunchedEffect(signedIn, selectedSong?.id) {
            if (signedIn && selectedSong != null) refreshFavoriteStatus(false)
        }
        LaunchedEffect(signedIn, favoriteAccountKey) {
            if (!signedIn || favoriteAccountKey == null) {
                favoritedSongIds = null
                favoriteCacheFetchedAtMs = 0L
                return@LaunchedEffect
            }
            refreshFavoriteStatus(false)
            while (true) {
                delay(FAVORITE_SONG_CACHE_TTL_MS)
                refreshFavoriteStatus(true)
            }
        }
        val toggleSongFavorite: (SongHit) -> Unit = { song ->
            val catalogId = song.favoriteCatalogId()
            if (catalogId.isBlank()) {
                scope.launch { snackbar.showSnackbar("无法确定这首歌的 Apple Music 曲库 ID") }
            } else if (favoritePendingSongId == null) {
                val knownFavoriteIds = favoritedSongIds
                if (knownFavoriteIds == null) {
                    refreshFavoriteStatus(true)
                } else {
                    val favorite = catalogId !in knownFavoriteIds
                    scope.launch {
                        favoritePendingSongId = catalogId
                        try {
                            withContext(Dispatchers.IO) {
                                MusicRepository.api.setCatalogSongFavorite(catalogId, favorite)
                            }
                            favoritedSongIds = if (favorite) {
                                knownFavoriteIds + catalogId
                            } else {
                                knownFavoriteIds - catalogId
                            }
                            val updatedFavoriteIds = favoritedSongIds.orEmpty()
                            val updatedAtMs = System.currentTimeMillis()
                            favoriteCacheFetchedAtMs = updatedAtMs
                            favoriteAccountKey?.let { accountKey ->
                                favoriteSongsPreferences.edit()
                                    .putString("account_key", accountKey)
                                    .putStringSet("song_ids", updatedFavoriteIds.toSet())
                                    .putLong("fetched_at_ms", updatedAtMs)
                                    .apply()
                            }
                            snackbar.showSnackbar(
                                if (favorite) "已收藏到 Apple Music 资料库"
                                else "已从 Apple Music 收藏中移除"
                            )
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            snackbar.showSnackbar(
                                "${if (favorite) "收藏" else "取消收藏"}失败：" +
                                    (error.message ?: "请稍后重试")
                            )
                        } finally {
                            favoritePendingSongId = null
                        }
                    }
                }
            }
        }
        LaunchedEffect(selectedPage) {
            if (selectedPage == ToolPage.Settings) {
                cachedItems = MusicRepository.cache.cachedItems()
            }
        }
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                scope.launch {
                    snackbar.showSnackbar("允许通知后，系统才能显示正在播放的歌词")
                }
            }
        }

        fun openLyricsForSong(song: SongHit) {
            if (selectedSong == null) lyricsReturnPage = selectedPage
            selectedPage = ToolPage.Search
            selectedSong = song
            lyricsCollapsed = false
            sourceLookupRequest = 0
            fullscreenLyrics = false
            fetchedLyrics = null
            lyricsError = null
            loadingLyrics = true
            playbackStatus = PlaybackEngine.preparingNote
            scope.launch {
                try {
                    val result = fetchAppleLyricsWithQqFallback(
                        client,
                        qqMusicClient,
                        lyricsCache,
                        song
                    )
                    if (selectedSong == song) {
                        fetchedLyrics = result.lyrics
                        externalFallbackLyrics = result.fallbackLyrics
                        externalFallbackSourceName = result.fallbackSourceName
                        externalSourceName = result.fallbackSourceName?.let { "QQ 音乐" }
                        externalSourceMode = result.fallbackSourceName != null
                        LiveLyricsNotification.setLyrics(song.id, result.lyrics?.lyrics)
                        if (result.lyrics == null) lyricsNotice = "这首歌暂时没有可用歌词"
                    }
                } catch (error: Exception) {
                    if (selectedSong == song) {
                        lyricsError = error.message ?: "获取歌词失败，请稍后重试"
                    }
                } finally {
                    if (selectedSong == song) loadingLyrics = false
                }
            }
        }

        fun resetCollectionScrollFor(item: ShelfItem) {
            if (
                selectedPage != ToolPage.Collection ||
                selectedCollection?.item?.id != item.id
            ) {
                scope.launch { collectionListState.scrollToItem(0) }
            }
        }

        fun openHomeCollection(item: ShelfItem) {
            resetCollectionScrollFor(item)
            if (selectedPage != ToolPage.Collection) {
                collectionReturnPage = selectedPage
            }
            selectedPage = ToolPage.Collection
            selectedCollection = HomeCollection(item = item, tracks = emptyList())
            collectionError = null
            collectionLoading = true
            scope.launch {
                try {
                    val metadataResult = try {
                        withContext(Dispatchers.IO) {
                            MusicRepository.api.getRecommendedCollectionMetadata(item)
                        } to null
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        Log.w("CatalogCollectionPage", "无法读取《${item.title}》的完整详情", error)
                        null to (error.message ?: "完整详情暂不可用")
                    }
                    val loadedTracks = withContext(Dispatchers.IO) {
                        if (item.isAlbum) {
                            val rows = MusicRepository.api.getRecommendedAlbumTracks(
                                albumId = item.id,
                                relatedCatalogId = item.relatedCatalogId,
                                isLibraryAlbum = item.type.contains("library", ignoreCase = true),
                                resolvedCatalogId = metadataResult.first?.catalogId.orEmpty(),
                                catalogStorefront = item.catalogStorefront.ifBlank {
                                    MusicRepository.api.storefront
                                }
                            ).map { MusicRepository.rowOf(it).also { row -> row.isLibrary = false } }
                            LoadedCollectionTracks(
                                rows = rows,
                                nextOffset = rows.size,
                                totalCount = rows.size,
                                hasMore = false
                            )
                        } else if (item.isPlaylist) {
                            val page = MusicRepository.api.getRecommendedPlaylistTracks(
                                item.id,
                                item.relatedCatalogId,
                                item.isLibraryPlaylist,
                                COLLECTION_TRACK_PAGE_SIZE,
                                0
                            )
                            val rows = page.items.map {
                                MusicRepository.rowOf(it).also { row ->
                                    row.isLibrary = false
                                }
                            }
                            val totalCount = maxOf(page.total, item.trackCount)
                            val hasMore = page.next.isNotBlank() ||
                                totalCount > page.size() ||
                                page.size() == COLLECTION_TRACK_PAGE_SIZE
                            LoadedCollectionTracks(
                                rows = rows,
                                nextOffset = page.size(),
                                totalCount = totalCount,
                                hasMore = hasMore
                            )
                        } else {
                            error("不支持打开此类型的内容：${item.type}")
                        }
                    }
                    check(loadedTracks.rows.isNotEmpty()) {
                        "Apple Music 没有返回《${item.title}》的曲目"
                    }
                    MusicRepository.registerTransient(loadedTracks.rows)
                    if (selectedCollection?.item?.id != item.id) return@launch
                    selectedCollection = HomeCollection(
                        item = item,
                        tracks = loadedTracks.rows,
                        metadata = metadataResult.first,
                        metadataError = metadataResult.second,
                        nextTrackOffset = loadedTracks.nextOffset,
                        totalTrackCount = loadedTracks.totalCount,
                        hasMoreTracks = loadedTracks.hasMore
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    collectionError = error.message ?: "无法加载曲目，请稍后重试"
                } finally {
                    collectionLoading = false
                }
            }
        }

        fun loadMoreCollectionTracks() {
            val collection = selectedCollection ?: return
            if (
                !collection.item.isPlaylist ||
                !collection.hasMoreTracks ||
                collection.loadingMoreTracks
            ) {
                return
            }
            val item = collection.item
            val offset = collection.nextTrackOffset
            selectedCollection = collection.copy(
                loadingMoreTracks = true,
                trackLoadError = null
            )
            scope.launch {
                try {
                    val page = withContext(Dispatchers.IO) {
                        MusicRepository.api.getRecommendedPlaylistTracks(
                            item.id,
                            item.relatedCatalogId,
                            item.isLibraryPlaylist,
                            COLLECTION_TRACK_PAGE_SIZE,
                            offset
                        )
                    }
                    val current = selectedCollection
                        ?.takeIf { it.item.id == item.id && it.item.type == item.type }
                        ?: return@launch
                    val newRows = page.items.map {
                        MusicRepository.rowOf(it).also { row ->
                            row.isLibrary = false
                        }
                    }
                    MusicRepository.registerTransient(newRows)
                    val nextOffset = offset + page.size()
                    val totalCount = maxOf(current.totalTrackCount, page.total)
                    selectedCollection = current.copy(
                        tracks = (current.tracks + newRows).distinctBy { it.uri },
                        nextTrackOffset = nextOffset,
                        totalTrackCount = totalCount,
                        hasMoreTracks = newRows.isNotEmpty() && (
                            page.next.isNotBlank() ||
                                totalCount > nextOffset ||
                                page.size() == COLLECTION_TRACK_PAGE_SIZE
                            ),
                        loadingMoreTracks = false
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: RuntimeException) {
                    val current = selectedCollection
                        ?.takeIf { it.item.id == item.id && it.item.type == item.type }
                        ?: return@launch
                    if (isUnavailableCatalogPlaylistPage(error, item, offset)) {
                        selectedCollection = current.copy(
                            hasMoreTracks = false,
                            loadingMoreTracks = false
                        )
                    } else {
                        Log.e(
                            "CatalogCollectionPage",
                            "加载《${item.title}》后续曲目失败",
                            error
                        )
                        selectedCollection = current.copy(
                            loadingMoreTracks = false,
                            trackLoadError = error.message ?: "加载更多曲目失败"
                        )
                    }
                } catch (error: Exception) {
                    val current = selectedCollection
                        ?.takeIf { it.item.id == item.id && it.item.type == item.type }
                        ?: return@launch
                    Log.e(
                        "CatalogCollectionPage",
                        "加载《${item.title}》后续曲目失败",
                        error
                    )
                    selectedCollection = current.copy(
                        loadingMoreTracks = false,
                        trackLoadError = error.message ?: "加载更多曲目失败"
                    )
                }
            }
        }

        suspend fun loadAllCollectionTracks(
            collection: HomeCollection
        ): List<AppleLibrarySync.SongRow> {
            if (!collection.item.isPlaylist || !collection.hasMoreTracks) {
                return collection.tracks
            }
            check(!collection.loadingMoreTracks) { "曲目正在加载，请稍后重试" }
            selectedCollection = collection.copy(
                loadingMoreTracks = true,
                trackLoadError = null
            )
            return try {
                val loaded = withContext(Dispatchers.IO) {
                    loadRemainingPlaylistTracks(
                        collection.item,
                        collection.tracks,
                        collection.nextTrackOffset,
                        collection.totalTrackCount
                    )
                }
                selectedCollection
                    ?.takeIf { it.item.id == collection.item.id }
                    ?.let { current ->
                        selectedCollection = current.copy(
                            tracks = loaded.rows,
                            nextTrackOffset = loaded.nextOffset,
                            totalTrackCount = loaded.totalCount,
                            hasMoreTracks = loaded.hasMore,
                            loadingMoreTracks = false,
                            trackLoadError = null
                        )
                    }
                loaded.rows
            } catch (error: CancellationException) {
                selectedCollection
                    ?.takeIf { it.item.id == collection.item.id }
                    ?.let { selectedCollection = it.copy(loadingMoreTracks = false) }
                throw error
            } catch (error: Exception) {
                selectedCollection
                    ?.takeIf { it.item.id == collection.item.id }
                    ?.let {
                        selectedCollection = it.copy(
                            loadingMoreTracks = false,
                            trackLoadError = error.message ?: "加载完整歌单失败"
                        )
                    }
                throw error
            }
        }

        fun openHomeSongAlbum(item: ShelfItem) {
            resetCollectionScrollFor(item)
            collectionReturnPage = selectedPage
            selectedPage = ToolPage.Collection
            selectedCollection = HomeCollection(item = item, tracks = emptyList())
            collectionError = null
            collectionLoading = true
            scope.launch {
                try {
                    val album = withContext(Dispatchers.IO) {
                        val api = MusicRepository.api
                        var catalogSongId = item.catalogId
                        if (catalogSongId.isBlank() && item.isLibrarySong) {
                            catalogSongId = api.librarySongCatalogId(item.id)
                        }
                        if (catalogSongId.isBlank() && !item.isLibrarySong) {
                            catalogSongId = item.id
                        }
                        var matchedSong = if (catalogSongId.isNotBlank()) {
                            api.getCatalogSongLocalized(catalogSongId)
                        } else {
                            null
                        }
                        if (matchedSong == null) {
                            val search = api.search(item.title, 30)
                            matchedSong = search.songs.firstOrNull { candidate ->
                                candidate.name.equals(item.title, ignoreCase = true) &&
                                    (item.subtitle.isBlank() ||
                                        candidate.artistName.equals(
                                            item.subtitle,
                                            ignoreCase = true
                                        ))
                            }
                        }
                        val song = matchedSong
                            ?: error("Apple Music 没有找到《${item.title}》对应的歌曲")
                        api.getCatalogAlbumForSong(song.id)
                            ?: error("Apple Music 没有返回《${item.title}》所属专辑")
                    }
                    openHomeCollection(
                        ShelfItem(
                            id = album.id,
                            type = "albums",
                            title = album.name,
                            subtitle = album.artistName,
                            artwork = album.artworkUrl.ifBlank { item.artwork }
                        )
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    collectionError = error.message ?: "无法打开歌曲所属专辑"
                    collectionLoading = false
                }
            }
        }

        fun openCatalogArtist(
            artistId: String,
            artistName: String = "",
            forceReload: Boolean = false
        ) {
            if (
                !forceReload &&
                selectedPage == ToolPage.Artist &&
                artistPage?.artist?.id == artistId
            ) {
                return
            }
            if (
                selectedPage == ToolPage.Artist &&
                artistPage != null &&
                (artistId.isBlank() || artistPage?.artist?.id != artistId)
            ) {
                artistPage?.let { artistPageHistory = artistPageHistory + it }
            } else {
                if (selectedPage != ToolPage.Artist) {
                    artistPageHistory = emptyList()
                    artistReturnPage = selectedPage
                }
            }
            selectedPage = ToolPage.Artist
            artistPage = if (artistId.isNotBlank()) {
                artistPage?.takeIf { it.artist.id == artistId }
            } else {
                null
            }
            artistPageError = null
            artistPageLoading = true
            scope.launch {
                try {
                    val (loaded, savedArtists) = withContext(Dispatchers.IO) {
                        val api = MusicRepository.api
                        val resolvedId = artistId.ifBlank {
                            api.searchArtistsOnly(artistName, 10)
                                .firstOrNull {
                                    it.name.equals(artistName, ignoreCase = true)
                                }?.id.orEmpty()
                        }
                        require(resolvedId.isNotBlank()) {
                            "Apple Music 没有找到艺人：$artistName"
                        }
                        val artistPage = api.getCatalogArtistPage(resolvedId)
                        val favorites = api.getFavoriteCatalogArtistIds()
                        artistPage to favorites
                    }
                    artistPage = loaded
                    favoriteArtistIds = savedArtists
                    artistFavoriteOverride = null
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    artistPageError = error.message ?: "无法加载艺人主页"
                    Log.w("CatalogArtistPage", "加载艺人主页失败：$artistId", error)
                } finally {
                    artistPageLoading = false
                }
            }
        }

        fun loadMoreArtistTopSongs() {
            val currentArtistPage = artistPage ?: return
            if (
                artistTopSongsLoading ||
                !artistTopSongsHasMore ||
                selectedPage != ToolPage.ArtistTopSongs
            ) {
                return
            }
            artistTopSongsLoading = true
            scope.launch {
                try {
                    val offset = artistTopSongs.size
                    val nextPage = withContext(Dispatchers.IO) {
                        MusicRepository.api.getCatalogArtistTopSongs(
                            currentArtistPage.artist.id,
                            limit = 20,
                            offset = offset
                        )
                    }
                    artistTopSongs = (artistTopSongs + nextPage).distinctBy { it.id }
                    artistTopSongsHasMore = nextPage.size >= 20
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    artistTopSongsHasMore = false
                    snackbar.showSnackbar(
                        error.message ?: "无法加载艺人热门歌曲"
                    )
                } finally {
                    artistTopSongsLoading = false
                }
            }
        }

        fun openArtistTopSongs() {
            val currentArtistPage = artistPage ?: return
            selectedPage = ToolPage.ArtistTopSongs
            artistTopSongs = currentArtistPage.topSongs
            artistTopSongsHasMore = currentArtistPage.topSongs.size >= 20
            artistTopSongsLoading = false
        }

        fun toggleArtistFavorite() {
            val artist = artistPage?.artist ?: return
            if (artistSavePending) return
            val isCurrentlyFavorite = artistFavoriteOverride
                ?: favoriteArtistIds?.contains(artist.id)
                ?: false
            val favorite = !isCurrentlyFavorite
            artistSavePending = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        MusicRepository.api.setCatalogArtistFavorite(artist.id, favorite)
                    }
                    artistFavoriteOverride = favorite
                    favoriteArtistIds = favoriteArtistIds?.let { current ->
                        if (favorite) current + artist.id else current - artist.id
                    }
                    snackbar.showSnackbar(
                        if (favorite) "已收藏艺人：${artist.name}"
                        else "已取消收藏：${artist.name}"
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    snackbar.showSnackbar(error.message ?: "无法更新艺人收藏状态")
                } finally {
                    artistSavePending = false
                }
            }
        }

        fun backFromArtistPage() {
            val previous = artistPageHistory.lastOrNull()
            if (previous == null) {
                selectedPage = artistReturnPage
            } else {
                artistPageHistory = artistPageHistory.dropLast(1)
                artistPage = previous
                artistPageError = null
                artistPageLoading = false
                selectedPage = ToolPage.Artist
            }
        }

        fun openArtistSongAlbum(song: AppleSong) {
            resetCollectionScrollFor(
                ShelfItem(
                    id = song.albumId.ifBlank { song.id },
                    type = "albums",
                    title = song.albumName.ifBlank { song.name },
                    subtitle = song.artistName,
                    artwork = song.artworkUrl,
                    catalogStorefront = "cn"
                )
            )
            collectionReturnPage = selectedPage
            selectedPage = ToolPage.Collection
            selectedCollection = HomeCollection(
                item = ShelfItem(
                    id = song.albumId.ifBlank { song.id },
                    type = "albums",
                    title = song.albumName.ifBlank { song.name },
                    subtitle = song.artistName,
                    artwork = song.artworkUrl,
                    catalogStorefront = "cn"
                ),
                tracks = emptyList()
            )
            collectionError = null
            collectionLoading = true
            scope.launch {
                try {
                    val album = withContext(Dispatchers.IO) {
                        val api = MusicRepository.api
                        if (song.albumId.isNotBlank()) {
                            api.getCatalogAlbum(song.albumId, "cn")
                        } else {
                            api.getCatalogAlbumForSong(song.id, "cn")
                        } ?: error("Apple Music 没有返回《${song.name}》所属专辑")
                    }
                    openHomeCollection(
                        ShelfItem(
                            id = album.id,
                            type = "albums",
                            title = album.name,
                            subtitle = album.artistName,
                            artwork = album.artworkUrl.ifBlank { song.artworkUrl },
                            catalogStorefront = album.catalogStorefront.ifBlank {
                                "cn"
                            }
                        )
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    collectionError = error.message ?: "无法打开歌曲所属专辑"
                    collectionLoading = false
                }
            }
        }

        fun openArtistAlbum(album: AppleAlbum) {
            openHomeCollection(
                ShelfItem(
                    id = album.id,
                    type = "albums",
                    title = album.name,
                    subtitle = album.artistName,
                    artwork = album.artworkUrl,
                    catalogStorefront = album.catalogStorefront
                )
            )
        }

        fun openArtistPlaylist(playlist: ApplePlaylist) {
            openHomeCollection(
                ShelfItem(
                    id = playlist.id,
                    type = "playlists",
                    title = playlist.name,
                    subtitle = "Apple Music",
                    artwork = playlist.artworkUrl,
                    trackCount = playlist.trackCount
                )
            )
        }

        fun openSongAlbumByRow(row: AppleLibrarySync.SongRow) {
            scope.launch {
                try {
                    val album = withContext(Dispatchers.IO) {
                        val api = MusicRepository.api
                        val catalogSongId = row.catalogId.ifBlank {
                            if (row.isLibrary) api.librarySongCatalogId(row.appleId)
                            else row.appleId.ifBlank { row.uri.removePrefix("apple:") }
                        }
                        check(catalogSongId.isNotBlank()) { "当前歌曲缺少目录 ID" }
                        api.getCatalogAlbumForSong(catalogSongId)
                            ?: error("Apple Music 没有返回《${row.title}》所属专辑")
                    }
                    openArtistAlbum(album)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    snackbar.showSnackbar(error.message ?: "无法打开歌曲所属专辑")
                }
            }
        }

        fun openPlayingSongAlbum() {
            PlaybackEngine.currentSong()?.let(::openSongAlbumByRow)
        }

        fun openPlayingSongArtist() {
            val row = PlaybackEngine.currentSong() ?: return
            scope.launch {
                try {
                    val (artistId, artistName) = withContext(Dispatchers.IO) {
                        val api = MusicRepository.api
                        val catalogSongId = row.catalogId.ifBlank {
                            if (row.isLibrary) api.librarySongCatalogId(row.appleId)
                            else row.appleId.ifBlank { row.uri.removePrefix("apple:") }
                        }
                        val localizedSong = if (row.artistId.isBlank() && catalogSongId.isNotBlank()) {
                            api.getCatalogSongLocalized(catalogSongId)
                        } else {
                            null
                        }
                        val searchArtistName = localizedSong?.artistName
                            ?.takeIf(String::isNotBlank)
                            ?: row.artist
                        val artistId = row.artistId
                            .ifBlank { localizedSong?.artistId.orEmpty() }
                            .ifBlank {
                                catalogSongId.takeIf(String::isNotBlank)
                                    ?.let(api::getCatalogSongArtistId)
                                    .orEmpty()
                            }
                            .ifBlank {
                                api.searchArtistsOnly(searchArtistName, 10)
                                .firstOrNull {
                                    it.name.equals(searchArtistName, ignoreCase = true)
                                }?.id.orEmpty()
                            }
                        artistId to searchArtistName
                    }
                    check(artistId.isNotBlank()) { "Apple Music 没有找到艺人：${row.artist}" }
                    openCatalogArtist(artistId, artistName)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    snackbar.showSnackbar(error.message ?: "无法打开艺人主页")
                }
            }
        }

        fun openCollectionTrackArtist(row: AppleLibrarySync.SongRow) {
            scope.launch {
                try {
                    val (artistId, artistName) = withContext(Dispatchers.IO) {
                        val api = MusicRepository.api
                        val catalogSongId = row.catalogId.ifBlank {
                            if (row.isLibrary) api.librarySongCatalogId(row.appleId)
                            else row.appleId.ifBlank { row.uri.removePrefix("apple:") }
                        }
                        val localizedSong = if (row.artistId.isBlank() && catalogSongId.isNotBlank()) {
                            api.getCatalogSongLocalized(catalogSongId)
                        } else {
                            null
                        }
                        val searchArtistName = localizedSong?.artistName
                            ?.takeIf(String::isNotBlank)
                            ?: row.artist
                        val artistId = row.artistId
                            .ifBlank { localizedSong?.artistId.orEmpty() }
                            .ifBlank {
                                catalogSongId.takeIf(String::isNotBlank)
                                    ?.let(api::getCatalogSongArtistId)
                                    .orEmpty()
                            }
                            .ifBlank {
                                api.searchArtistsOnly(searchArtistName, 10)
                                .firstOrNull {
                                    it.name.equals(searchArtistName, ignoreCase = true)
                                }?.id.orEmpty()
                            }
                        artistId to searchArtistName
                    }
                    check(artistId.isNotBlank()) { "Apple Music 没有找到艺人：${row.artist}" }
                    openCatalogArtist(artistId, artistName)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    snackbar.showSnackbar(error.message ?: "无法打开艺人主页")
                }
            }
        }

        fun openSongHitAlbum(song: SongHit) {
            val knownRow = MusicRepository.findSong(song.id)
                ?: MusicRepository.findSong("apple:${song.id}")
            if (knownRow != null) {
                openSongAlbumByRow(knownRow)
                return
            }
            scope.launch {
                try {
                    val album = withContext(Dispatchers.IO) {
                        val catalogSongId = song.providerSongId
                            ?.takeIf(String::isNotBlank)
                            ?: song.id
                        MusicRepository.api.getCatalogAlbumForSong(catalogSongId)
                            ?: error("Apple Music 没有返回《${song.title}》所属专辑")
                    }
                    openArtistAlbum(album)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    snackbar.showSnackbar(error.message ?: "无法打开歌曲所属专辑")
                }
            }
        }

        fun openSongHitArtist(song: SongHit) {
            val knownRow = MusicRepository.findSong(song.id)
                ?: MusicRepository.findSong("apple:${song.id}")
            if (knownRow != null) {
                openCollectionTrackArtist(knownRow)
                return
            }
            scope.launch {
                try {
                    val artistId = withContext(Dispatchers.IO) {
                        val api = MusicRepository.api
                        val catalogSongId = song.providerSongId
                            ?.takeIf(String::isNotBlank)
                            ?: song.id
                        api.getCatalogSongArtistId(catalogSongId).ifBlank {
                            api.searchArtistsOnly(song.artist, 10)
                                .firstOrNull {
                                    it.name.equals(song.artist, ignoreCase = true)
                                }?.id.orEmpty()
                        }
                    }
                    check(artistId.isNotBlank()) {
                        "Apple Music 没有找到艺人：${song.artist}"
                    }
                    openCatalogArtist(artistId, song.artist)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    snackbar.showSnackbar(error.message ?: "无法打开艺人主页")
                }
            }
        }

        fun startHomePlayback(request: PendingHomePlayback, replaceQueue: Boolean) {
            if (stationStartPending) return
            scope.launch {
                try {
                    if (request.stationId != null) {
                        stationStartPending = true
                        if (replaceQueue) {
                            PlaybackEngine.onQueueLow = null
                            PlaybackEngine.onQueueEnded = null
                            deferredRadio = null
                        }
                        context.startService(Intent(context, MusicService::class.java))
                        startRecommendedRadio(
                            context.applicationContext,
                            MusicRepository.api,
                            request.stationId
                        )
                        snackbar.showSnackbar("已开始播放电台：${request.title}")
                    } else {
                        val tracks = request.tracks.drop(request.startIndex)
                        check(tracks.isNotEmpty()) { "没有可播放的歌曲" }
                        MusicRepository.registerTransient(tracks)
                        context.startService(Intent(context, MusicService::class.java))
                        if (replaceQueue) {
                            PlaybackEngine.onQueueLow = null
                            PlaybackEngine.onQueueEnded = null
                            deferredRadio = null
                            request.catalogArtistId?.let { artistId ->
                                var nextOffset = request.tracks.size
                                var requestInFlight = false
                                var hasMore = true
                                lateinit var artistQueueContinuation: (Int) -> Unit
                                artistQueueContinuation = { remaining ->
                                    if (remaining < 5 && hasMore && !requestInFlight) {
                                        requestInFlight = true
                                        scope.launch {
                                            try {
                                                val nextPage = withContext(Dispatchers.IO) {
                                                    MusicRepository.api.getCatalogArtistTopSongs(
                                                        artistId,
                                                        limit = 20,
                                                        offset = nextOffset
                                                    )
                                                }
                                                if (
                                                    PlaybackEngine.onQueueLow !==
                                                    artistQueueContinuation
                                                ) {
                                                    return@launch
                                                }
                                                if (nextPage.isEmpty()) {
                                                    hasMore = false
                                                    PlaybackEngine.onQueueLow = null
                                                    return@launch
                                                }
                                                val songs = if (
                                                    request.shuffleArtistContinuation
                                                ) {
                                                    nextPage.shuffled()
                                                } else {
                                                    nextPage
                                                }
                                                val nextTracks = songs.map {
                                                    MusicRepository.rowOf(it).also { row ->
                                                        row.isLibrary = false
                                                    }
                                                }
                                                MusicRepository.registerTransient(nextTracks)
                                                PlaybackEngine.appendToQueue(
                                                    nextTracks.map { it.uri }
                                                )
                                                nextOffset += nextPage.size
                                                hasMore = nextPage.size >= 20
                                                if (!hasMore) {
                                                    PlaybackEngine.onQueueLow = null
                                                }
                                            } catch (error: CancellationException) {
                                                throw error
                                            } catch (error: Exception) {
                                                if (
                                                    PlaybackEngine.onQueueLow ===
                                                    artistQueueContinuation
                                                ) {
                                                    snackbar.showSnackbar(
                                                        error.message
                                                            ?: "无法继续加载艺人热门歌曲"
                                                    )
                                                }
                                            } finally {
                                                requestInFlight = false
                                            }
                                        }
                                    }
                                }
                                PlaybackEngine.onQueueLow = artistQueueContinuation
                            }
                            request.playlistContinuation?.let { playlist ->
                                var nextOffset = request.playlistNextOffset
                                var expectedTotal = request.playlistTotalCount
                                var requestInFlight = false
                                var hasMore = true
                                lateinit var playlistQueueContinuation: (Int) -> Unit
                                playlistQueueContinuation = { remaining ->
                                    if (remaining < 5 && hasMore && !requestInFlight) {
                                        requestInFlight = true
                                        scope.launch {
                                            try {
                                                val nextPage = withContext(Dispatchers.IO) {
                                                    MusicRepository.api.getRecommendedPlaylistTracks(
                                                        playlist.id,
                                                        playlist.relatedCatalogId,
                                                        playlist.isLibraryPlaylist,
                                                        COLLECTION_TRACK_PAGE_SIZE,
                                                        nextOffset
                                                    )
                                                }
                                                if (
                                                    PlaybackEngine.onQueueLow !==
                                                    playlistQueueContinuation
                                                ) {
                                                    return@launch
                                                }
                                                val nextTracks = nextPage.items.map {
                                                    MusicRepository.rowOf(it).also { row ->
                                                        row.isLibrary = false
                                                    }
                                                }
                                                if (nextTracks.isEmpty()) {
                                                    hasMore = false
                                                    PlaybackEngine.onQueueLow = null
                                                    return@launch
                                                }
                                                MusicRepository.registerTransient(nextTracks)
                                                PlaybackEngine.appendToQueue(
                                                    nextTracks.map { it.uri }
                                                )
                                                nextOffset += nextPage.size()
                                                expectedTotal = maxOf(
                                                    expectedTotal,
                                                    nextPage.total
                                                )
                                                hasMore = nextPage.next.isNotBlank() ||
                                                    expectedTotal > nextOffset ||
                                                    nextPage.size() == COLLECTION_TRACK_PAGE_SIZE
                                                if (!hasMore) {
                                                    PlaybackEngine.onQueueLow = null
                                                }
                                            } catch (error: CancellationException) {
                                                throw error
                                            } catch (error: RuntimeException) {
                                                if (
                                                    PlaybackEngine.onQueueLow ===
                                                    playlistQueueContinuation &&
                                                    isUnavailableCatalogPlaylistPage(
                                                        error,
                                                        playlist,
                                                        nextOffset
                                                    )
                                                ) {
                                                    hasMore = false
                                                    PlaybackEngine.onQueueLow = null
                                                } else if (
                                                    PlaybackEngine.onQueueLow ===
                                                    playlistQueueContinuation
                                                ) {
                                                    snackbar.showSnackbar(
                                                        error.message
                                                            ?: "无法继续加载歌单曲目"
                                                    )
                                                }
                                            } catch (error: Exception) {
                                                if (
                                                    PlaybackEngine.onQueueLow ===
                                                    playlistQueueContinuation
                                                ) {
                                                    snackbar.showSnackbar(
                                                        error.message
                                                            ?: "无法继续加载歌单曲目"
                                                    )
                                                }
                                            } finally {
                                                requestInFlight = false
                                            }
                                        }
                                    }
                                }
                                PlaybackEngine.onQueueLow = playlistQueueContinuation
                            }
                            PlaybackEngine.playQueue(tracks.map { it.uri }, 0)
                        } else {
                            PlaybackEngine.appendToQueue(tracks.map { it.uri })
                        }
                        snackbar.showSnackbar(
                            if (replaceQueue) "开始播放：${request.title}"
                            else "已加入当前队列末尾：${request.title}"
                        )
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    snackbar.showSnackbar(
                        "无法播放${request.title}：${error.message ?: error.javaClass.simpleName}"
                    )
                } finally {
                    stationStartPending = false
                }
            }
        }

        fun requestHomePlayback(request: PendingHomePlayback) {
            if (stationStartPending) return
            if (PlaybackEngine.queueSnapshot().isNotEmpty() &&
                PlaybackEngine.currentSong() != null
            ) {
                pendingHomePlayback = request
            } else {
                startHomePlayback(request, replaceQueue = true)
            }
        }

        LaunchedEffect(deferredRadio?.id) {
            val station = deferredRadio
            PlaybackEngine.onQueueEnded = if (station == null) {
                null
            } else {
                {
                    deferredRadioPrompt = station
                    true
                }
            }
        }

        LaunchedEffect(deferredRadioPrompt?.id) {
            val station = deferredRadioPrompt ?: return@LaunchedEffect
            val result = snackbar.showSnackbar(
                message = "当前播放列表已结束，要开始播放电台“${station.title}”吗？",
                actionLabel = "开始播放",
                withDismissAction = true
            )
            deferredRadioPrompt = null
            deferredRadio = null
            PlaybackEngine.onQueueEnded = null
            if (result == SnackbarResult.ActionPerformed) {
                startHomePlayback(
                    PendingHomePlayback(title = station.title, stationId = station.id),
                    replaceQueue = true
                )
            }
        }

        val createDocument = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("text/plain")
        ) { uri ->
            val export = pendingExport
            pendingExport = null
            if (uri == null || export == null) return@rememberLauncherForActivityResult
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)
                            ?.use { it.write(export.second.render(fetchedLyrics)) }
                            ?: error("无法创建文件")
                    }
                    lyricsNotice = "歌词已保存"
                } catch (error: Exception) {
                    lyricsNotice = "保存失败：${error.message ?: "请检查存储权限"}"
                }
            }
        }

        val importLyricsDocument = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            val song = selectedSong
            if (uri == null || song == null) return@rememberLauncherForActivityResult
            scope.launch {
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)
                            ?.bufferedReader(Charsets.UTF_8)
                            ?.use { it.readText() }
                            ?: error("无法读取所选歌词文件")
                    }
                    val isTtml = Regex("<(?:[A-Za-z_][\\w.-]*:)?tt\\b", RegexOption.IGNORE_CASE)
                        .containsMatchIn(content)
                    val parsed = if (isTtml) {
                        LyricsParser.parseTtml(content, "imported-ttml")
                    } else {
                        LyricsParser.parsePlain(content, "imported-lrc")
                    } ?: error("无法识别歌词内容，请选择有效的 TTML 或 LRC 文件")
                    fetchedLyrics = FetchedLyrics(
                        lyrics = parsed,
                        ttml = content.takeIf { isTtml }
                    )
                    externalFallbackLyrics = fetchedLyrics
                    externalFallbackSourceName = null
                    externalSourceName = null
                    externalSourceMode = false
                    LiveLyricsNotification.setLyrics(song.id, parsed)
                    lyricsNotice = (
                        if (parsed.wordTimedLines() > 0) "已导入逐字歌词文件"
                        else "歌词文件已导入（该文件不含逐字时间轴）"
                    )
                } catch (error: Exception) {
                    lyricsNotice = "导入失败：${error.message ?: "请检查文件格式"}"
                }
            }

        }

        val selectProfileAvatar = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val previousAvatarUri = profileAvatarUri
                profilePreferences.edit().putString("avatar_uri", uri.toString()).apply()
                profileAvatarUri = uri
                if (previousAvatarUri != null && previousAvatarUri != uri) {
                    try {
                        context.contentResolver.releasePersistableUriPermission(
                            previousAvatarUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (error: SecurityException) {
                        Log.w("LyricsToolScreen", "释放旧头像图片权限失败", error)
                    }
                }
            } catch (error: Exception) {
                Log.e("LyricsToolScreen", "保存个人头像访问权限失败", error)
                scope.launch {
                    snackbar.showSnackbar(
                        "无法保存这张图片作为头像：${error.message ?: "请重新选择图片"}"
                    )
                }
            }
        }

        val updateLyricsAppearance: (LyricsAppearance) -> Unit = { appearance ->
            lyricsAppearance = appearance
            appearancePreferences.edit()
                .putString("highlight_color", appearance.highlightColor)
                .putBoolean("use_cover_accent", appearance.useCoverAccent)
                .putFloat("glow_radius", appearance.glowRadius)
                .putFloat("font_size", appearance.fontSize)
                .putFloat("line_spacing", appearance.lineSpacing)
                .putFloat("current_line_position", appearance.currentLinePosition)
                .putBoolean("auto_scroll", appearance.autoScroll)
                .putBoolean("interpolate", appearance.interpolate)
                .putBoolean("blur", appearance.blur)
                .putBoolean("reduced_motion", appearance.reducedMotion)
                .putBoolean("hide_played_lines", appearance.hidePlayedLines)
                .putBoolean("traditional_to_simplified", appearance.traditionalToSimplified)
                .putString("custom_font_file", appearance.customFontFileName)
                .putString("custom_font_name", appearance.customFontDisplayName)
                .apply()
        }

        val selectLyricsFont = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                var temporaryFile: File? = null
                try {
                    val displayName = withContext(Dispatchers.IO) {
                        context.contentResolver.query(
                            uri,
                            arrayOf(OpenableColumns.DISPLAY_NAME),
                            null,
                            null,
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                            } else {
                                null
                            }
                        }
                    } ?: "custom-font"
                    val extension = displayName.substringAfterLast('.', "").lowercase(Locale.US)
                    require(extension == "ttf" || extension == "otf") {
                        "请选择 .ttf 或 .otf 格式的字体文件"
                    }
                    val fileName = "lyrics-${UUID.randomUUID()}.$extension"
                    val fontDirectory = File(context.filesDir, "lyrics-fonts")
                    val savedFile = File(fontDirectory, fileName)
                    val tempFile = File(fontDirectory, "$fileName.tmp")
                    temporaryFile = tempFile
                    withContext(Dispatchers.IO) {
                        check(fontDirectory.exists() || fontDirectory.mkdirs()) {
                            "无法创建字体存储目录"
                        }
                        val source = context.contentResolver.openInputStream(uri)
                            ?: error("无法读取所选字体文件")
                        source.use { input ->
                            tempFile.outputStream().buffered().use { output ->
                                val buffer = ByteArray(8192)
                                var totalBytes = 0L
                                while (true) {
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    totalBytes += count
                                    require(totalBytes <= MAX_CUSTOM_FONT_BYTES) {
                                        "字体文件不能超过 20 MB"
                                    }
                                    output.write(buffer, 0, count)
                                }
                            }
                        }
                        Typeface.createFromFile(tempFile)
                        check(tempFile.renameTo(savedFile)) {
                            "无法保存字体文件"
                        }
                    }
                    val oldFile = lyricsAppearance.customFontFileName
                        ?.let { File(fontDirectory, it) }
                    updateLyricsAppearance(
                        lyricsAppearance.copy(
                            customFontFileName = fileName,
                            customFontDisplayName = displayName
                        )
                    )
                    if (oldFile != null && oldFile != savedFile) oldFile.delete()
                    snackbar.showSnackbar("已应用字体：$displayName")
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    temporaryFile?.delete()
                    snackbar.showSnackbar(
                        "字体导入失败：${error.message ?: "请确认文件是有效的 TTF/OTF 字体"}"
                    )
                }
            }
        }
        val clearLyricsFont: () -> Unit = {
            lyricsAppearance.customFontFileName?.let {
                File(File(context.filesDir, "lyrics-fonts"), it).delete()
            }
            updateLyricsAppearance(
                lyricsAppearance.copy(
                    customFontFileName = null,
                    customFontDisplayName = null
                )
            )
            scope.launch { snackbar.showSnackbar("已恢复默认歌词字体") }
        }

        LaunchedEffect(selectedSong?.artworkUrl) {
            val artworkUrl = selectedSong?.artworkUrl
            val cachedAccent = artworkUrl?.let(coverAccentColors::get)
            coverAccentColor = cachedAccent
            coverAccentResolvedFor = if (artworkUrl.isNullOrBlank() || cachedAccent != null) {
                artworkUrl
            } else {
                null
            }
            if (artworkUrl.isNullOrBlank()) {
                return@LaunchedEffect
            }
            if (cachedAccent != null) return@LaunchedEffect
            val request = ImageRequest.Builder(context)
                .data(artworkUrl)
                .size(256)
                .allowHardware(false)
                .build()
            try {
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.drawable.toBitmap()
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap).generate()
                    }
                    val accent = palette.vibrantSwatch
                        ?: palette.darkVibrantSwatch
                        ?: palette.lightVibrantSwatch
                        ?: palette.mutedSwatch
                        ?: palette.dominantSwatch
                    val extractedAccent = accent?.rgb?.let {
                        "#%06X".format(Locale.US, 0xFFFFFF and it)
                    }
                    coverAccentColor = extractedAccent
                    coverAccentResolvedFor = artworkUrl
                    if (extractedAccent != null) {
                        coverAccentColors = coverAccentColors + (artworkUrl to extractedAccent)
                    }
                    if (accent == null) {
                        lyricsNotice = "无法从封面提取强调色，继续使用默认颜色"
                    }
                } else if (result is ErrorResult) {
                    coverAccentResolvedFor = artworkUrl
                    lyricsNotice = "封面加载失败，无法提取强调色"
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                coverAccentResolvedFor = artworkUrl
                lyricsNotice = "封面加载失败：${error.message ?: "无法提取强调色"}"
            }
        }
        val coverAccentReady = !lyricsAppearance.useCoverAccent ||
            selectedSong?.artworkUrl.isNullOrBlank() ||
            coverAccentResolvedFor == selectedSong?.artworkUrl

        val effectiveLyricsAppearance = remember(
            lyricsAppearance,
            coverAccentColor,
            appDarkTheme
        ) {
            coverAccentColor?.takeIf { lyricsAppearance.useCoverAccent }
                ?.let {
                    lyricsAppearance.copy(highlightColor = it, darkTheme = appDarkTheme)
                }
                ?: lyricsAppearance.copy(darkTheme = appDarkTheme)
        }

        val playbackAccentColor = coverAccentColor
            ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
            ?: MaterialTheme.colorScheme.primary
        LaunchedEffect(nowPlayingSong?.artworkUrl) {
            val artworkUrl = nowPlayingSong?.artworkUrl.orEmpty()
            miniPlayerArtworkAccent = null
            if (artworkUrl.isBlank()) return@LaunchedEffect

            val cachedAccent = coverAccentColors[artworkUrl]
                ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
            if (cachedAccent != null) {
                miniPlayerArtworkAccent = cachedAccent
                return@LaunchedEffect
            }

            try {
                val request = ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .size(256)
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.drawable.toBitmap()
                    val swatch = withContext(Dispatchers.Default) {
                        val palette = Palette.from(bitmap).generate()
                        palette.vibrantSwatch
                            ?: palette.darkVibrantSwatch
                            ?: palette.lightVibrantSwatch
                            ?: palette.mutedSwatch
                            ?: palette.dominantSwatch
                    }
                    swatch?.let {
                        val color = Color(it.rgb)
                        miniPlayerArtworkAccent = color
                        val encoded = "#%06X".format(Locale.US, 0xFFFFFF and it.rgb)
                        coverAccentColors = coverAccentColors + (artworkUrl to encoded)
                    } ?: Log.w(
                        "MiniPlayerCapsule",
                        "无法从正在播放歌曲的封面提取强调色"
                    )
                } else if (result is ErrorResult) {
                    Log.w(
                        "MiniPlayerCapsule",
                        "无法加载正在播放歌曲的封面以提取强调色",
                        result.throwable
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w("MiniPlayerCapsule", "提取播放胶囊强调色失败", error)
            }
        }

        fun applyLyricsResult(song: SongHit, result: LyricsFetchResult) {
            if (selectedSong != song) return
            fetchedLyrics = result.lyrics
            externalFallbackLyrics = result.fallbackLyrics
            externalFallbackSourceName = result.fallbackSourceName
            externalSourceName = result.fallbackSourceName?.let { "QQ 音乐" }
            externalSourceMode = result.fallbackSourceName != null
            LiveLyricsNotification.setLyrics(song.id, result.lyrics?.lyrics)
            result.notice?.let { lyricsNotice = it }
            if (result.searchOtherSources) {
                externalFallbackLyrics = result.lyrics
                externalFallbackSourceName = if (externalSourceMode) externalSourceName else "Apple Music"
                sourceLookupRestoreOnFailure = true
                sourceLookupInProgress = true
                sourceLookupRequest += 1
            } else if (result.lyrics == null) {
                lyricsNotice = "这首歌暂时没有可用歌词"
            }
        }

        fun searchOtherSources(automatic: Boolean) {
            externalFallbackLyrics = fetchedLyrics
            externalFallbackSourceName =
                if (externalSourceMode) externalSourceName else "Apple Music"
            sourceLookupRestoreOnFailure = automatic
            sourceLookupInProgress = true
            sourceLookupRequest += 1
        }

        fun currentLyricSourceName(): String =
            if (externalSourceMode) externalSourceName ?: "外部来源" else "Apple Music"

        fun search() {
            val term = query.trim()
            if (term.isEmpty() || searching) return
            searching = true
            songs = emptyList()
            lyricSearchUsed = false
            selectedSong = null
            fullscreenLyrics = false
            fetchedLyrics = null
            lyricsError = null
            scope.launch {
                try {
                    val titleMatches = client.searchSongs(term)
                    if (titleMatches.isNotEmpty()) {
                        songs = titleMatches
                    } else {
                        lyricSearchUsed = true
                        songs = client.searchSongsByLyrics(term, qqMusicClient)
                    }
                    if (songs.isEmpty()) snackbar.showSnackbar("没有找到歌曲，试试歌名、艺人名或歌词内容")
                } catch (error: Exception) {
                    snackbar.showSnackbar(error.message ?: "搜索失败，请稍后重试")
                } finally {
                    searching = false
                }
            }
        }

        LaunchedEffect(client) {
            try {
                client.prepare()
                PlaybackEngine.resumeRestoredPlayback()
                restoreNativeRadioPrefetch(
                    context.applicationContext,
                    MusicRepository.api
                )
            } catch (error: Exception) {
                snackbar.showSnackbar(error.message ?: "连接 Apple Music 失败")
            } finally {
                preparing = false
            }
        }

        LaunchedEffect(signedIn, selectedPage, homeRefreshKey, client.userToken) {
            if (selectedPage != ToolPage.Home) return@LaunchedEffect
            if (!signedIn) {
                homeShelves = emptyList()
                homeError = null
                return@LaunchedEffect
            }
            val explicitlyRequested = homeRefreshKey != handledHomeRefreshKey
            if (
                !explicitlyRequested &&
                homeShelves.isNotEmpty() &&
                HomeRecommendationsCache.isFresh(homeLastUpdatedAtMs)
            ) {
                return@LaunchedEffect
            }
            handledHomeRefreshKey = homeRefreshKey

            homeLoading = true
            homeError = null
            try {
                val recommendations = withContext(Dispatchers.IO) {
                    MusicRepository.api.getRecommendations().also {
                        HomeRecommendationsCache.write(
                            context.applicationContext,
                            client.userToken,
                            it
                        )
                    }
                }
                homeShelves = recommendations
                homeLastUpdatedAtMs = System.currentTimeMillis()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                homeError = error.message ?: "暂时无法加载个性化推荐"
            } finally {
                homeLoading = false
            }
        }

        LaunchedEffect(nowPlayingRequest) {
            if (nowPlayingRequest == 0L) return@LaunchedEffect

            val row = PlaybackEngine.currentSong()
            if (row == null || row.toSongHitOrNull() == null) {
                snackbar.showSnackbar("无法恢复当前播放歌曲")
                return@LaunchedEffect
            }
            val song = row.toSongHitOrNull() ?: return@LaunchedEffect
            openLyricsForSong(song)
        }

        LaunchedEffect(selectedSong?.id) {
            externalSourceMode = false
            externalSourceName = null
            sourceLookupInProgress = false
            sourceLookupRestoreOnFailure = false
            sourceLookupRequest = 0
            lyricsNotice = null
            externalFallbackLyrics = null
            externalFallbackSourceName = null
        }

        LaunchedEffect(selectedSong?.id) {
            if (selectedSong == null) return@LaunchedEffect
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        LaunchedEffect(Unit) {
            while (true) {
                val row = PlaybackEngine.currentSong()
                val currentSong = row?.toSongHitOrNull()
                if (nowPlayingSong != currentSong) {
                    nowPlayingSong = currentSong
                    if (
                        currentSong != null &&
                        selectedSong != null &&
                        selectedSong?.id != currentSong.id &&
                        !PlaybackEngine.preparing
                    ) {
                        openLyricsForSong(currentSong)
                    }
                }
                val queueEntries = PlaybackEngine.queueSnapshot().mapIndexed { index, uri ->
                    index to uri
                }
                if (playbackQueue != queueEntries) playbackQueue = queueEntries
                val currentQueueIndex = PlaybackEngine.currentQueueIndex()
                if (playbackQueueIndex != currentQueueIndex) {
                    playbackQueueIndex = currentQueueIndex
                }
                playbackPositionMs = PlaybackEngine.positionMs()
                playbackDurationMs = maxOf(
                    PlaybackEngine.durationMs(),
                    selectedSong?.durationMs ?: currentSong?.durationMs ?: 0L
                )
                playbackIsPlaying = PlaybackEngine.isPlaying()
                playbackStatus = PlaybackEngine.preparingNote
                delay(250L)
            }
        }

        LaunchedEffect(playbackQueue, playbackQueueIndex) {
            val nextUri = playbackQueue
                .firstOrNull { (index, _) -> index == playbackQueueIndex + 1 }
                ?.second ?: return@LaunchedEffect
            val row = MusicRepository.findSong(nextUri) ?: return@LaunchedEffect
            val song = row.toSongHitOrNull() ?: return@LaunchedEffect
            val songId = row.appleId.ifBlank { song.id }
            MusicRepository.cache.preload(
                songId = songId,
                isLibrary = row.isLibrary,
                metadata = com.applemusic.android.apple.AppleCache.SongMetadata(
                    title = row.title,
                    artist = row.artist,
                    album = row.album
                )
            )
            scope.launch {
                try {
                    fetchAppleLyricsWithQqFallback(
                        client,
                        qqMusicClient,
                        lyricsCache,
                        song
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.w("LyricsPrefetch", "预加载下一首歌词失败 (${song.id})", error)
                }
            }
        }

        LaunchedEffect(
            selectedSong?.id,
            selectedPage,
            lyricsCollapsed,
            lyricsInteractionVersion,
            queueDialogOpen,
            lyricsAppearanceOpen,
            cacheManagerOpen
        ) {
            if (
                selectedSong == null ||
                selectedPage != ToolPage.Search ||
                lyricsCollapsed ||
                queueDialogOpen ||
                lyricsAppearanceOpen ||
                cacheManagerOpen
            ) {
                return@LaunchedEffect
            }
            delay(8_000L)
            lyricsCollapsed = true
            fullscreenLyrics = false
            lyricsNavigationVisible = false
        }

        BackHandler(enabled = selectedSong != null || selectedPage != ToolPage.Home) {
            if (fullscreenLyrics) {
                fullscreenLyrics = false
            } else if (lyricsCollapsed) {
                lyricsCollapsed = false
            } else if (selectedSong != null) {
                selectedSong = null
                selectedPage = lyricsReturnPage
                fetchedLyrics = null
                lyricsError = null
            } else if (selectedPage == ToolPage.Artist) {
                backFromArtistPage()
            } else if (selectedPage == ToolPage.ArtistTopSongs) {
                selectedPage = ToolPage.Artist
            } else if (selectedPage == ToolPage.Collection) {
                selectedPage = collectionReturnPage
            } else if (selectedPage != ToolPage.Home) {
                selectedPage = ToolPage.Home
            }
        }

        Scaffold(
            containerColor = if (selectedPage == ToolPage.Home && appDarkTheme) {
                Color(0xFF1C1C1E)
            }
            else MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbar) }
        ) { padding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (selectedPage == ToolPage.Home && appDarkTheme) Color(0xFF1C1C1E)
                        else MaterialTheme.colorScheme.background
                    )
                    .padding(padding)
                    .onGloballyPositioned { rootWindowOrigin = it.positionInWindow() }
            ) {
                val originalPlayerCapsuleWidth = (maxWidth - 32.dp)
                    .coerceAtMost(360.dp)
                    .coerceAtLeast(0.dp)
                val collectionTransitionProgress = remember { Animatable(0f) }
                LaunchedEffect(collectionCardTransition, selectedPage) {
                    if (collectionCardTransition != null &&
                        selectedPage == ToolPage.Collection
                    ) {
                        collectionTransitionProgress.snapTo(0f)
                        collectionTransitionProgress.animateTo(
                            1f,
                            animationSpec = tween(durationMillis = 720)
                        )
                        while (collectionLoading) delay(16)
                        delay(260)
                        collectionCardTransition = null
                    } else if (selectedPage != ToolPage.Collection) {
                        collectionCardTransition = null
                    }
                }
                if (selectedPage != ToolPage.Collection && !immersiveLyrics) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(statusBarHeight + 10.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        if (appDarkTheme) Color(0xAA151517)
                                        else Color(0x99FFFFFF),
                                        if (appDarkTheme) Color(0x66151517)
                                        else Color(0x55FFFFFF),
                                        Color.Transparent
                                    )
                                )
                            )
                            .blur(6.dp)
                    )
                }
                Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = if (hideStatusBar) 0.dp else statusBarHeight
                            )
                            .padding(
                                bottom = WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding()
                            )
                ) {
                        AnimatedContent(
                            targetState = selectedPage,
                            transitionSpec = {
                                fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                            },
                            label = "pageNavigation",
                            modifier = Modifier.weight(1f)
                        ) { page ->
                            Column(Modifier.fillMaxSize()) {
                            AnimatedVisibility(
                                visible = page == ToolPage.Search &&
                                    !fullscreenLyrics &&
                                    selectedSong == null,
                            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = query,
                                        onValueChange = { query = it },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(28.dp),
                                        singleLine = true,
                                        placeholder = { Text("歌名、艺人或歌词") },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Search, contentDescription = null)
                                        },
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(onSearch = { search() })
                                    )
                                    Spacer(Modifier.size(10.dp))
                                    Button(
                                        onClick = { search() },
                                        enabled = query.isNotBlank() && !searching && !preparing,
                                        modifier = Modifier.height(54.dp),
                                        contentPadding = PaddingValues(horizontal = 19.dp),
                                        shape = RoundedCornerShape(22.dp)
                                    ) {
                                        if (searching) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        } else {
                                            Text("搜索", fontWeight = FontWeight.Bold)
                                        }

                                }
                            }
                        }
                        }

                        if (cacheManagerOpen) {
                            CacheManagerDialog(
                                items = remember(cachedItems, cachedLyricsItems) {
                                    (cachedItems.map { it.songId } + cachedLyricsItems.map { it.songId })
                                        .distinct()
                                        .map { songId ->
                                            ManagedCacheItem(
                                                songId = songId,
                                                playback = cachedItems.firstOrNull {
                                                    it.songId == songId
                                                },
                                                lyrics = cachedLyricsItems.firstOrNull {
                                                    it.songId == songId
                                                }
                                            )
                                        }
                                },
                                playbackCacheCount = cachedItems.size,
                                playbackCacheBytes = cachedItems.sumOf { it.size },
                                playbackCacheLimitBytes = playbackCacheLimitBytes,
                                currentSongId = PlaybackEngine.currentSong()?.let { song ->
                                    song.appleId.ifBlank { song.catalogId.ifBlank { song.uri.removePrefix("apple:") } }
                                },
                                pendingDelete = pendingCacheDelete,
                                onDismiss = {
                                    cacheManagerOpen = false
                                    pendingCacheDelete = null
                                },
                                onRequestDelete = { pendingCacheDelete = it },
                                onCancelDelete = { pendingCacheDelete = null },
                                onConfirmDelete = { item ->
                                    scope.launch {
                                        try {
                                            val removed = withContext(Dispatchers.IO) {
                                                MusicRepository.cache.removeCached(item.songId)
                                            }
                                            cachedItems = MusicRepository.cache.cachedItems()
                                            cachedLyricsItems = lyricsCache.cachedItems()
                                            pendingCacheDelete = null
                                            snackbar.showSnackbar(
                                                if (removed) "已删除这首歌的播放缓存"
                                                else "缓存正在下载或已被移除，未执行删除"
                                            )
                                        } catch (error: Exception) {
                                            snackbar.showSnackbar("删除缓存失败：${error.message ?: "未知错误"}")
                                        }
                                    }
                                },
                                onClearAll = {
                                    scope.launch {
                                        var playbackRemoved = 0
                                        var lyricsRemoved = 0
                                        val failures = mutableListOf<String>()
                                        try {
                                            playbackRemoved = withContext(Dispatchers.IO) {
                                                MusicRepository.cache.clearAllCached()
                                            }
                                        } catch (error: Exception) {
                                            failures += error.message ?: "清除播放缓存失败"
                                        }
                                        try {
                                            lyricsRemoved = lyricsCache.clearAll()
                                        } catch (error: Exception) {
                                            failures += error.message ?: "清除歌词缓存失败"
                                        }
                                        cachedItems = MusicRepository.cache.cachedItems()
                                        cachedLyricsItems = lyricsCache.cachedItems()
                                        if (failures.isEmpty()) {
                                            snackbar.showSnackbar(
                                                "已清除全部缓存（$playbackRemoved 个歌曲缓存文件，$lyricsRemoved 个歌词缓存文件）"
                                            )
                                        } else {
                                            snackbar.showSnackbar(
                                                "缓存未能全部清除：${failures.joinToString("；")}"
                                            )
                                        }
                                    }
                                },
                                onTogglePinned = { item ->
                                    MusicRepository.cache.setPinned(item.songId, !item.pinned)
                                    cachedItems = MusicRepository.cache.cachedItems()
                                },
                                onLimitChange = { bytes ->
                                    playbackCacheLimitBytes = bytes
                                    scope.launch {
                                        val removed = withContext(Dispatchers.IO) {
                                            MusicRepository.cache.setMaxCacheBytes(bytes)
                                        }
                                        cachedItems = MusicRepository.cache.cachedItems()
                                        if (removed > 0) {
                                            snackbar.showSnackbar("已按新上限清理 $removed 首较早的缓存")
                                        }
                                    }
                                },
                                onDeleteLyrics = { songId ->
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                lyricsCache.removeForSong(songId)
                                            }
                                            cachedLyricsItems = lyricsCache.cachedItems()
                                            snackbar.showSnackbar("已删除这首歌的歌词缓存")
                                        } catch (error: Exception) {
                                            snackbar.showSnackbar(
                                                "删除歌词缓存失败：${error.message ?: "未知错误"}"
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        pendingHomePlayback?.let { request ->
                            AlertDialog(
                                onDismissRequest = { pendingHomePlayback = null },
                                title = { Text("更改播放队列？") },
                                text = {
                                    Text(
                                        "当前还有歌曲待播放。要现在播放“${request.title}”，" +
                                            "还是等当前队列结束后再播放？"
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            pendingHomePlayback = null
                                            startHomePlayback(request, replaceQueue = true)
                                        }
                                    ) {
                                        Text("清空队列并播放")
                                    }
                                },
                                dismissButton = {
                                    Row {
                                        TextButton(
                                            onClick = { pendingHomePlayback = null }
                                        ) {
                                            Text("取消")
                                        }
                                        TextButton(
                                            onClick = {
                                                pendingHomePlayback = null
                                                if (request.stationId != null) {
                                                    deferredRadio = ShelfItem(
                                                        id = request.stationId,
                                                        type = "stations",
                                                        title = request.title,
                                                        subtitle = "电台",
                                                        artwork = ""
                                                    )
                                                    scope.launch {
                                                        snackbar.showSnackbar(
                                                            "电台将在当前队列结束后询问是否开始；暂不预取歌曲"
                                                        )
                                                    }
                                                } else {
                                                    startHomePlayback(
                                                        request,
                                                        replaceQueue = false
                                                    )
                                                }
                                            }
                                        ) {
                                            Text("当前队列后播放")
                                        }
                                    }
                                }
                            )
                        }

                        if (lyricsAppearanceOpen) {
                            LyricsAppearanceDialog(
                                appearance = lyricsAppearance,
                                coverAccentColor = coverAccentColor,
                                onAppearanceChange = updateLyricsAppearance,
                                onSelectFont = {
                                    selectLyricsFont.launch(arrayOf("*/*"))
                                },
                                onClearFont = clearLyricsFont,
                                onDismiss = { lyricsAppearanceOpen = false }
                            )
                        }

                        if (queueDialogOpen) {
                            PlaybackQueueDialog(
                                entries = playbackQueue,
                                currentIndex = playbackQueueIndex,
                                anchorArtworkUrl = selectedSong?.artworkUrl.orEmpty(),
                                onDismiss = { queueDialogOpen = false },
                                onSelect = { index ->
                                    queueDialogOpen = false
                                    PlaybackEngine.playAt(index)
                                }
                            )
                        }

                        if (page == ToolPage.Settings && !fullscreenLyrics) {
                            SettingsPage(
                                signedIn = signedIn,
                                profileAvatarUri = profileAvatarUri,
                                cacheCount = cachedItems.size,
                                cacheBytes = cachedItems.sumOf { it.size },
                                statusBarLyricsEnabled = statusBarLyricsEnabled,
                                lyricsFullscreenEnabled = lyricsFullscreenEnabled,
                                themeMode = themeMode,
                                useDynamicColors = useDynamicColors,
                                manualThemeSeedHex = manualThemeSeedHex,
                                modifier = Modifier.weight(1f),
                                onLogin = openLogin,
                                onSelectProfileAvatar = {
                                    selectProfileAvatar.launch(arrayOf("image/*"))
                                },
                                onClearProfileAvatar = {
                                    profileAvatarUri?.let { uri ->
                                        try {
                                            context.contentResolver.releasePersistableUriPermission(
                                                uri,
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            )
                                        } catch (error: SecurityException) {
                                            Log.w(
                                                "LyricsToolScreen",
                                                "释放头像图片权限失败",
                                                error
                                            )
                                        }
                                    }
                                    profileAvatarUri = null
                                    profilePreferences.edit().remove("avatar_uri").apply()
                                },
                                onThemeModeChange = { mode ->
                                    themeMode = mode
                                    themePreferences.edit()
                                        .putString("theme_mode", mode.preferenceValue)
                                        .apply()
                                },
                                onDynamicColorsChange = { enabled ->
                                    useDynamicColors = enabled
                                    themePreferences.edit()
                                        .putBoolean("use_system_colors", enabled)
                                        .apply()
                                },
                                onManualThemeSeedChange = { hex ->
                                    manualThemeSeedHex = hex
                                    themePreferences.edit().putString("manual_seed", hex).apply()
                                },
                                onStatusBarLyricsEnabledChange = { enabled ->
                                    statusBarLyricsEnabled = enabled
                                    LiveLyricsNotification.setEnabled(context, enabled)
                                },
                                onLyricsFullscreenEnabledChange = { enabled ->
                                    lyricsFullscreenEnabled = enabled
                                    appearancePreferences.edit()
                                        .putBoolean("lyrics_fullscreen_enabled", enabled)
                                        .apply()
                                },
                                onOpenCache = {
                                    scope.launch {
                                        val playback = MusicRepository.cache.cachedItems()
                                        withContext(Dispatchers.IO) {
                                            playback.forEach { item ->
                                                lyricsCache.get("apple", item.songId)
                                                lyricsCache.get("qq", item.songId)
                                            }
                                        }
                                        cachedItems = playback
                                        cachedLyricsItems = lyricsCache.cachedItems()
                                        cacheManagerOpen = true
                                    }
                                }
                            )
                        } else if (page == ToolPage.Home && selectedSong == null) {
                            MusicHomePage(
                                shelves = homeShelves,
                                profileAvatarUri = profileAvatarUri,
                                darkTheme = appDarkTheme,
                                statusBarHeight = statusBarHeight,
                                isSignedIn = signedIn,
                                isLoading = homeLoading,
                                error = homeError,
                                onSettings = { selectedPage = ToolPage.Settings },
                                onRefresh = { homeRefreshKey++ },
                                recyclerState = homeRecyclerState,
                                sectionStateHolder = homeSectionStateHolder,
                                onScrollChanged = { isCollapsed, isInitial ->
                                    if (!isInitial || !homeScrollStateInitialized) {
                                        if (homeScrolled != isCollapsed) {
                                            homeScrolled = isCollapsed
                                        }
                                    }
                                    if (!homeScrollStateInitialized) {
                                        homeScrollStateInitialized = true
                                    }
                                },
                                onOpenItem = { item, cardBounds ->
                                    when {
                                        item.isStation -> requestHomePlayback(
                                            PendingHomePlayback(
                                                title = item.title,
                                                stationId = item.id
                                            )
                                        )
                                        item.isArtist -> openCatalogArtist(item.id, item.title)
                                        item.isAlbum || item.isPlaylist -> {
                                            collectionCardTransition =
                                                HomeCollectionCardTransition(
                                                    item,
                                                    cardBounds
                                                )
                                            openHomeCollection(item)
                                        }
                                        item.isSong -> {
                                            collectionCardTransition =
                                                HomeCollectionCardTransition(
                                                    item,
                                                    cardBounds
                                                )
                                            openHomeSongAlbum(item)
                                        }
                                        else -> scope.launch {
                                            snackbar.showSnackbar(
                                                "暂不支持打开此类型的内容：${item.title}（类型：${item.type}）"
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else if (page == ToolPage.Artist) {
                            CatalogArtistPage(
                                page = artistPage,
                                recyclerState = artistPageRecyclerState,
                                sectionStateHolder = artistSectionStateHolder,
                                isLoading = artistPageLoading,
                                error = artistPageError,
                                statusBarHeight = statusBarHeight,
                                bottomPadding = WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding(),
                                onBack = ::backFromArtistPage,
                                onScrollChanged = { isCollapsed, isInitial ->
                                    if (isInitial || artistNavigationCollapsed != isCollapsed) {
                                        artistNavigationCollapsed = isCollapsed
                                    }
                                },
                                onShare = {
                                    artistPage?.artist?.let { artist ->
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "https://music.apple.com/${MusicRepository.api.storefront}/artist/${artist.id}"
                                            )
                                        }
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, artist.name)
                                        )
                                    }
                                },
                                onRetry = {
                                    artistPage?.artist?.let {
                                        openCatalogArtist(it.id, it.name, forceReload = true)
                                    }
                                },
                                onOpenTopSongs = ::openArtistTopSongs,
                                isArtistFavorite = artistFavoriteOverride
                                    ?: artistPage?.artist?.id?.let {
                                        favoriteArtistIds?.contains(it)
                                    },
                                artistSavePending = artistSavePending,
                                onToggleArtistFavorite = ::toggleArtistFavorite,
                                onPlay = {
                                    val artist = artistPage ?: return@CatalogArtistPage
                                    val rows = artist.topSongs.map {
                                        MusicRepository.rowOf(it).also { row ->
                                            row.isLibrary = false
                                        }
                                    }
                                    if (rows.isNotEmpty()) {
                                        startHomePlayback(
                                            PendingHomePlayback(
                                                title = artist.artist.name,
                                                tracks = rows,
                                                catalogArtistId = artist.artist.id
                                            ),
                                            replaceQueue = true
                                        )
                                    }
                                },
                                onShuffle = {
                                    artistPage?.let { artist ->
                                        val rows = artist.topSongs.shuffled().map {
                                            MusicRepository.rowOf(it).also { row ->
                                                row.isLibrary = false
                                            }
                                        }
                                        if (rows.isNotEmpty()) {
                                            startHomePlayback(
                                                PendingHomePlayback(
                                                    title = artist.artist.name,
                                                    tracks = rows,
                                                    catalogArtistId = artist.artist.id,
                                                    shuffleArtistContinuation = true
                                                ),
                                                replaceQueue = true
                                            )
                                        }
                                    }
                                },
                                onPlaySong = { song ->
                                    startHomePlayback(
                                        PendingHomePlayback(
                                            title = song.name,
                                            tracks = listOf(
                                                MusicRepository.rowOf(song).also {
                                                    it.isLibrary = false
                                                }
                                            )
                                        ),
                                        replaceQueue = true
                                    )
                                },
                                onOpenSongAlbum = ::openArtistSongAlbum,
                                onOpenArtist = { artist ->
                                    openCatalogArtist(artist.id, artist.name)
                                },
                                onOpenAlbum = ::openArtistAlbum,
                                onAddAlbum = { album ->
                                    scope.launch {
                                        try {
                                            val added = withContext(Dispatchers.IO) {
                                                MusicRepository.api.addCatalogAlbumToLibrary(album.id)
                                            }
                                            snackbar.showSnackbar(
                                                if (added) "已添加《${album.name}》到资料库"
                                                else "添加《${album.name}》到资料库失败"
                                            )
                                        } catch (error: CancellationException) {
                                            throw error
                                        } catch (error: Exception) {
                                            snackbar.showSnackbar(
                                                error.message ?: "添加专辑到资料库失败"
                                            )
                                        }
                                    }
                                },
                                onOpenPlaylist = ::openArtistPlaylist,
                                modifier = Modifier.weight(1f)
                            )
                        } else if (page == ToolPage.ArtistTopSongs) {
                            CatalogArtistTopSongsPage(
                                artist = artistPage?.artist,
                                listState = artistTopSongsListState,
                                songs = artistTopSongs,
                                isLoading = artistTopSongsLoading,
                                hasMore = artistTopSongsHasMore,
                                onLoadMore = ::loadMoreArtistTopSongs,
                                onBack = { selectedPage = ToolPage.Artist },
                                onPlaySong = { song ->
                                    startHomePlayback(
                                        PendingHomePlayback(
                                            title = song.name,
                                            tracks = listOf(
                                                MusicRepository.rowOf(song).also {
                                                    it.isLibrary = false
                                                }
                                            )
                                        ),
                                        replaceQueue = true
                                    )
                                },
                                onOpenSongAlbum = ::openArtistSongAlbum,
                                onOpenArtist = { artist ->
                                    openCatalogArtist(artist.id, artist.name)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else if (page == ToolPage.Collection) {
                            AnimatedContent(
                                targetState = collectionLoading,
                                transitionSpec = {
                                    fadeIn(tween(260)) togetherWith fadeOut(tween(180))
                                },
                                label = "collectionContentLoading"
                            ) { showingCollectionLoading ->
                                CatalogCollectionPage(
                                collection = selectedCollection,
                                listState = collectionListState,
                                isLoading = showingCollectionLoading,
                                error = collectionError,
                                onBack = { selectedPage = collectionReturnPage },
                                onSearch = { selectedPage = ToolPage.Search },
                                onPlayTrack = { index ->
                                    val collection = selectedCollection ?: return@CatalogCollectionPage
                                    requestHomePlayback(
                                        PendingHomePlayback(
                                            title = collection.item.title,
                                            tracks = collection.tracks,
                                            startIndex = index,
                                            playlistContinuation = collection.item.takeIf {
                                                it.isPlaylist && collection.hasMoreTracks
                                            },
                                            playlistNextOffset = collection.nextTrackOffset,
                                            playlistTotalCount = collection.totalTrackCount
                                        )
                                    )
                                },
                                onShuffle = {
                                    val collection = selectedCollection ?: return@CatalogCollectionPage
                                    scope.launch {
                                        try {
                                            val tracks = loadAllCollectionTracks(collection)
                                            requestHomePlayback(
                                                PendingHomePlayback(
                                                    title = collection.item.title,
                                                    tracks = tracks.shuffled()
                                                )
                                            )
                                        } catch (error: CancellationException) {
                                            throw error
                                        } catch (error: Exception) {
                                            snackbar.showSnackbar(
                                                error.message ?: "无法完整加载歌单曲目"
                                            )
                                        }
                                    }
                                },
                                onLoadMoreTracks = ::loadMoreCollectionTracks,
                                onDownload = {
                                    val collection = selectedCollection ?: return@CatalogCollectionPage
                                    scope.launch {
                                        val tracks = try {
                                            loadAllCollectionTracks(collection)
                                        } catch (error: CancellationException) {
                                            throw error
                                        } catch (error: Exception) {
                                            snackbar.showSnackbar(
                                                error.message ?: "无法完整加载歌单曲目"
                                            )
                                            return@launch
                                        }
                                        var completed = 0
                                        val failures = mutableListOf<String>()
                                        tracks.forEach { row ->
                                            try {
                                                withContext(Dispatchers.IO) {
                                                    val songId = row.appleId.ifBlank { row.catalogId }
                                                        .ifBlank { row.uri }
                                                    require(songId.isNotBlank()) {
                                                        "《${row.title}》缺少可下载的歌曲 ID"
                                                    }
                                                    MusicRepository.cache.ensure(
                                                        songId = songId,
                                                        isLibrary = row.isLibrary,
                                                        metadata = com.applemusic.android.apple.AppleCache.SongMetadata(
                                                            title = row.title,
                                                            artist = row.artist,
                                                            album = row.album
                                                        )
                                                    )
                                                }
                                                completed++
                                            } catch (error: CancellationException) {
                                                throw error
                                            } catch (error: Exception) {
                                                failures += "${row.title}: ${error.message ?: "下载失败"}"
                                            }
                                        }
                                        val message = if (failures.isEmpty()) {
                                            "已下载 ${completed} 首歌曲"
                                        } else {
                                            "已下载 ${completed} 首，${failures.size} 首失败：${failures.take(2).joinToString("；")}"
                                        }
                                        snackbar.showSnackbar(message)
                                    }
                                },
                                onOpenAlbum = ::openSongAlbumByRow,
                                onOpenArtist = ::openCollectionTrackArtist,
                                playingSong = nowPlayingSong,
                                isPlaybackPlaying = playbackIsPlaying
                            )
                            }
                        } else if (
                            page == ToolPage.Discovery ||
                            page == ToolPage.Radio ||
                            page == ToolPage.Library
                        ) {
                            val title = when (page) {
                                ToolPage.Discovery -> "新发现"
                                ToolPage.Radio -> "电台"
                                else -> "资料库"
                            }
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$title · 即将推出",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.56f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            AnimatedContent(
                                targetState = selectedSong != null,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "lyricsContent",
                                modifier = Modifier.weight(1f)
                            ) { showingLyrics ->
                                if (showingLyrics) {
                                    selectedSong?.let { song ->
                                        key(song.id) {
                                            LyricsDetail(
                                                song = song,
                                                fetchedLyrics = fetchedLyrics,
                                                lyricsNotice = lyricsNotice,
                                                playbackControlsVisible = !lyricsCollapsed,
                                                error = lyricsError,
                                                loading = loadingLyrics,
                                                signedIn = signedIn,
                                                positionMs = playbackPositionMs,
                                                durationMs = playbackDurationMs,
                                                isPlaying = playbackIsPlaying,
                                                externalSourceMode = externalSourceMode,
                                                externalSourceName = externalSourceName,
                                                externalFallbackSourceName = externalFallbackSourceName,
                                                sourceLookupInProgress = sourceLookupInProgress,
                                                sourceLookupRequest = sourceLookupRequest,
                                                lyricsAppearance = effectiveLyricsAppearance,
                                                traditionalToSimplified =
                                                    lyricsAppearance.traditionalToSimplified,
                                                coverAccentReady = coverAccentReady,
                                                playbackCardTopInset = playbackCardTopInset,
                                                accentColor = playbackAccentColor,
                                                preparing = PlaybackEngine.preparing,
                                                playbackStatus = playbackStatus,
                                                fullscreen = fullscreenLyrics,
                                                onToggleFullscreen = {
                                                    fullscreenLyrics = !fullscreenLyrics
                                                },
                                                onCollapseLyrics = {
                                                    lyricsCollapsed = true
                                                    fullscreenLyrics = false
                                                    lyricsNavigationVisible = false
                                                },
                                                onLogin = openLogin,
                                                onOpenQueue = { queueDialogOpen = true },
                                                onOpenAlbum = { openSongHitAlbum(song) },
                                                onOpenArtist = { openSongHitArtist(song) },
                                                onQueueDragOffsetChange = {
                                                    coverQueueDragOffset = it
                                                },
                                                coverQueueDragOffset = coverQueueDragOffset,
                                                isFavorited = song.favoriteCatalogId().let { catalogId ->
                                                    favoritedSongIds?.let { ids ->
                                                        catalogId.takeIf(String::isNotBlank)?.let(ids::contains)
                                                    }
                                                },
                                                favoritePending = song.favoriteCatalogId()
                                                        .let { it.isNotBlank() && it == favoritePendingSongId },
                                                onToggleFavorite = { toggleSongFavorite(song) },
                                                onTogglePlayback = {
                                                    if (PlaybackEngine.preparingNote.startsWith("无法播放")) {
                                                        val row = AppleLibrarySync.SongRow().apply {
                                                            appleId = song.id
                                                            uri = "apple:${song.id}"
                                                            title = song.title
                                                            artist = song.artist
                                                            album = song.album
                                                            durationMs = song.durationMs
                                                            coverUrl = song.artworkUrl
                                                            isLibrary = false
                                                            catalogId = song.id
                                                        }
                                                        MusicRepository.registerTransient(listOf(row))
                                                        PlaybackEngine.playSingle(row.uri)
                                                    } else {
                                                        PlaybackEngine.toggle()
                                                    }
                                                },
                                                onSeekTo = { position ->
                                                    PlaybackEngine.seekTo(position)
                                                    if (!PlaybackEngine.isPlaying()) PlaybackEngine.toggle()
                                                },
                                                onRetry = {
                                                    loadingLyrics = true
                                                    lyricsError = null
                                                    scope.launch {
                                                        try {
                                                            val result = fetchAppleLyricsWithQqFallback(
                                                                client,
                                                                qqMusicClient,
                                                                lyricsCache,
                                                                song
                                                            )
                                                            applyLyricsResult(song, result)
                                                        } catch (error: Exception) {
                                                            val message = error.message ?: "获取歌词失败，请稍后重试"
                                                            lyricsError = message
                                                            lyricsNotice = message
                                                        } finally {
                                                            loadingLyrics = false
                                                        }
                                                    }
                                                },
                                                onCopy = {
                                                    val text = fetchedLyrics?.lyrics?.lines
                                                        ?.renderWithTranslations().orEmpty()
                                                    clipboard.setText(AnnotatedString(text))
                                                    lyricsNotice = "歌词已复制"
                                                },
                                                onExport = { format ->
                                                    if (fetchedLyrics == null) return@LyricsDetail
                                                    pendingExport = song.title to format
                                                    val safeName = song.title
                                                        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                                                        .take(80)
                                                        .ifBlank { "歌词" }
                                                    createDocument.launch("$safeName.${format.extension}")
                                                },
                                                onImportLyrics = {
                                                    importLyricsDocument.launch(
                                                        arrayOf(
                                                            "text/*",
                                                            "application/xml",
                                                            "application/ttml+xml"
                                                        )
                                                    )
                                                },
                                                onSearchOtherSources = {
                                                    searchOtherSources(automatic = false)
                                                },
                                                onSelectAppleLyrics = {
                                                    sourceLookupInProgress = false
                                                    sourceLookupRestoreOnFailure = false
                                                    externalFallbackLyrics = fetchedLyrics
                                                    externalFallbackSourceName = currentLyricSourceName()
                                                    loadingLyrics = true
                                                    scope.launch {
                                                        try {
                                                            val result = fetchAppleLyricsCached(
                                                                client,
                                                                lyricsCache,
                                                                song.id
                                                            )
                                                            fetchedLyrics = result
                                                            externalSourceMode = false
                                                            externalSourceName = null
                                                            LiveLyricsNotification.setLyrics(
                                                                song.id,
                                                                result?.lyrics
                                                            )
                                                            lyricsNotice = if (result == null) {
                                                                "Apple Music 没有提供这首歌的歌词"
                                                            } else {
                                                                "已切换到 Apple Music 歌词"
                                                            }
                                                        } catch (error: Exception) {
                                                            lyricsNotice =
                                                                error.message ?: "获取 Apple Music 歌词失败"
                                                        } finally {
                                                            loadingLyrics = false
                                                        }
                                                    }
                                                },
                                                onSelectQqLyrics = {
                                                    sourceLookupInProgress = false
                                                    sourceLookupRestoreOnFailure = false
                                                    externalFallbackLyrics = fetchedLyrics
                                                    externalFallbackSourceName = currentLyricSourceName()
                                                    loadingLyrics = true
                                                    scope.launch {
                                                        try {
                                                            val result = fetchQqLyricsForAppleSong(
                                                                client,
                                                                qqMusicClient,
                                                                lyricsCache,
                                                                song
                                                            )
                                                            if (result == null) {
                                                                lyricsNotice =
                                                                    "QQ 音乐没有找到匹配的歌词"
                                                            } else {
                                                                fetchedLyrics = result
                                                                externalSourceMode = true
                                                                externalSourceName = "QQ 音乐"
                                                                LiveLyricsNotification.setLyrics(
                                                                    song.id,
                                                                    result.lyrics
                                                                )
                                                                lyricsNotice = if (
                                                                    result.lyrics.wordTimedLines() > 0
                                                                ) {
                                                                    "已切换到 QQ 音乐逐字歌词"
                                                                } else {
                                                                    "QQ 音乐只提供逐行歌词，没有找到这首歌的逐字 QRC 版本"
                                                                }
                                                            }
                                                        } catch (error: Exception) {
                                                            lyricsNotice =
                                                                error.message ?: "获取 QQ 音乐歌词失败"
                                                        } finally {
                                                            loadingLyrics = false
                                                        }
                                                    }
                                                },
                                                onRestoreAppleLyrics = {
                                                    sourceLookupInProgress = false
                                                    sourceLookupRestoreOnFailure = false
                                                    externalFallbackLyrics?.let { fetchedLyrics = it }
                                                    externalSourceName = externalFallbackSourceName
                                                    externalSourceMode =
                                                        externalFallbackSourceName != null &&
                                                            externalFallbackSourceName != "Apple Music"
                                                    externalFallbackLyrics?.let {
                                                        LiveLyricsNotification.setLyrics(
                                                            song.id,
                                                            it.lyrics
                                                        )
                                                    }
                                                    externalFallbackLyrics = null
                                                    externalFallbackSourceName = null
                                                },
                                                onOpenAppearance = { lyricsAppearanceOpen = true },
                                                onSourceLookupSucceeded = { source, ttml ->
                                                    if (selectedSong == song && sourceLookupInProgress) {
                                                        val parsed = LyricsParser.parseTtml(
                                                            ttml,
                                                            "external-source"
                                                        )
                                                        if (parsed == null || parsed.lines.isEmpty()) {
                                                            sourceLookupInProgress = false
                                                            lyricsNotice =
                                                                "$source 返回的歌词格式无法识别，继续使用当前歌词"
                                                        } else {
                                                            fetchedLyrics = FetchedLyrics(parsed, ttml)
                                                            LiveLyricsNotification.setLyrics(
                                                                song.id,
                                                                parsed
                                                            )
                                                            externalSourceMode = true
                                                            externalSourceName = source
                                                            sourceLookupInProgress = false
                                                            sourceLookupRestoreOnFailure = false
                                                            lyricsNotice = "已从 $source 找到歌词"
                                                        }
                                                    }
                                                },
                                                onSourceLookupFailed = {
                                                    if (selectedSong == song && sourceLookupInProgress) {
                                                        sourceLookupInProgress = false
                                                        if (sourceLookupRestoreOnFailure) {
                                                            externalFallbackLyrics?.let {
                                                                fetchedLyrics = it
                                                                LiveLyricsNotification.setLyrics(
                                                                    song.id,
                                                                    it.lyrics
                                                                )
                                                            }
                                                            externalSourceName =
                                                                externalFallbackSourceName
                                                            externalSourceMode =
                                                                externalFallbackSourceName != null &&
                                                                    externalFallbackSourceName != "Apple Music"
                                                            sourceLookupRestoreOnFailure = false
                                                            lyricsNotice =
                                                                "其他来源没有匹配到歌词，继续使用${externalSourceName ?: "Apple Music"}歌词"
                                                        } else {
                                                            lyricsNotice =
                                                                "其他来源没有匹配到歌词，继续使用当前歌词"
                                                        }
                                                    }
                                                },
                                                onLyricsScroll = {
                                                    lyricsNavigationVisible = true
                                                    lyricsInteractionVersion++
                                                },
                                                onUserInteraction = {
                                                    lyricsInteractionVersion++
                                                },
                                                onTranslationUnavailable = {
                                                    lyricsNotice = "这份歌词没有译文"
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    SongResults(
                                        songs = songs,
                                        searching = searching,
                                        query = query,
                                        lyricMatches = lyricSearchUsed,
                                        onOpenAlbum = ::openSongHitAlbum,
                                        onOpenArtist = ::openSongHitArtist,
                                        onSongClick = { song ->
                                            sourceLookupRequest = 0
                                            fullscreenLyrics = false
                                            lyricsCollapsed = false
                                            if (selectedSong == null) {
                                                lyricsReturnPage = selectedPage
                                            }
                                            selectedSong = song
                                            fetchedLyrics = null
                                            loadingLyrics = true
                                            lyricsError = null
                                            playbackStatus = "正在准备播放…"
                                            val row = AppleLibrarySync.SongRow().apply {
                                                appleId = song.id
                                                uri = "apple:${song.id}"
                                                title = song.title
                                                artist = song.artist
                                                album = song.album
                                                durationMs = song.durationMs
                                                coverUrl = song.artworkUrl
                                                isLibrary = false
                                                catalogId = song.id
                                            }
                                            MusicRepository.registerTransient(listOf(row))
                                            runCatching {
                                                context.startService(
                                                    Intent(context, MusicService::class.java)
                                                )
                                                PlaybackEngine.playSingle(row.uri)
                                            }.onFailure { error ->
                                                val message = error.message ?: "无法启动播放器"
                                                playbackStatus = "无法播放：$message"
                                                scope.launch { snackbar.showSnackbar(message) }
                                            }
                                            scope.launch {
                                                try {
                                                    val lyricsResult = fetchAppleLyricsWithQqFallback(
                                                        client,
                                                        qqMusicClient,
                                                        lyricsCache,
                                                        song
                                                    )
                                                    if (selectedSong == song) {
                                                        fetchedLyrics = lyricsResult.lyrics
                                                        externalFallbackLyrics =
                                                            lyricsResult.fallbackLyrics
                                                        externalFallbackSourceName =
                                                            lyricsResult.fallbackSourceName
                                                        externalSourceName =
                                                            lyricsResult.fallbackSourceName?.let {
                                                                "QQ 音乐"
                                                            }
                                                        externalSourceMode =
                                                            lyricsResult.fallbackSourceName != null
                                                        LiveLyricsNotification.setLyrics(
                                                            song.id,
                                                            lyricsResult.lyrics?.lyrics
                                                        )
                                                    }
                                                    if (lyricsResult.lyrics == null &&
                                                        selectedSong == song
                                                    ) {
                                                        lyricsNotice = "这首歌暂时没有可用歌词"
                                                    } else if (selectedSong == song) {
                                                        lyricsResult.lyrics?.translationWarning?.let {
                                                            lyricsNotice = "翻译歌词获取失败：$it"
                                                        }
                                                    }
                                                } catch (error: Exception) {
                                                    val message = error.message ?: "获取歌词失败，请稍后重试"
                                                    if (selectedSong == song) {
                                                        lyricsError = message
                                                    }
                                                } finally {
                                                    if (selectedSong == song) loadingLyrics = false
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        }
                    }
                }

                val navigationShouldCollapse = nowPlayingSong != null &&
                    ((selectedPage == ToolPage.Home && homeScrolled) ||
                        (selectedPage == ToolPage.Artist && artistNavigationCollapsed))
                val navigationCollapseProgress = animateFloatAsState(
                    targetValue = if (navigationShouldCollapse) 1f else 0f,
                    animationSpec = spring(dampingRatio = 0.88f, stiffness = 220f),
                    label = "navigationCollapseProgress"
                )
                CollapsingMiniPlayer(
                    song = nowPlayingSong,
                    visible = selectedSong == null && !fullscreenLyrics,
                    isPlaying = playbackIsPlaying,
                    positionMs = playbackPositionMs,
                    durationMs = playbackDurationMs,
                    progress = navigationCollapseProgress,
                    expandedWidth = originalPlayerCapsuleWidth,
                    artworkAccent = miniPlayerArtworkAccent,
                    onOpen = { song -> openLyricsForSong(song) },
                    onOpenAlbum = ::openPlayingSongAlbum,
                    onOpenArtist = ::openPlayingSongArtist,
                    onTogglePlayback = { PlaybackEngine.toggle() },
                    onNext = { PlaybackEngine.next() },
                    onPrevious = { PlaybackEngine.prev() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                AnimatedVisibility(
                    visible = lyricsCollapsed && selectedSong != null,
                    enter = fadeIn(animationSpec = tween(180)),
                    exit = fadeOut(animationSpec = tween(130)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val overlayDensity = androidx.compose.ui.platform.LocalDensity.current
                        var artworkX by remember(collapsedArtworkPreferences) {
                            mutableFloatStateOf(
                                collapsedArtworkPreferences.getFloat("x_dp", 16f)
                            )
                        }
                        var artworkY by remember(collapsedArtworkPreferences) {
                            mutableFloatStateOf(
                                collapsedArtworkPreferences.getFloat("y_dp", 12f)
                            )
                        }
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(180)) +
                                scaleIn(initialScale = 0.72f, animationSpec = tween(180)),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset {
                                    val maxX = (maxWidth - 64.dp).coerceAtLeast(0.dp)
                                    val maxY = (maxHeight - 64.dp).coerceAtLeast(0.dp)
                                    IntOffset(
                                        with(overlayDensity) {
                                            artworkX.dp.coerceIn(0.dp, maxX).roundToPx()
                                        },
                                        with(overlayDensity) {
                                            artworkY.dp.coerceIn(0.dp, maxY).roundToPx()
                                        }
                                    )
                                }
                        ) {
                            selectedSong?.let { song ->
                            CollapsedLyricsArtwork(
                                song = song,
                                positionMs = playbackPositionMs,
                                durationMs = playbackDurationMs,
                                accentColor = playbackAccentColor,
                                onDrag = { delta ->
                                    val maxX = with(overlayDensity) { (maxWidth - 64.dp).toPx() }
                                        .coerceAtLeast(0f)
                                    val maxY = with(overlayDensity) { (maxHeight - 64.dp).toPx() }
                                        .coerceAtLeast(0f)
                                    artworkX = (
                                        with(overlayDensity) { artworkX.dp.toPx() } + delta.x
                                        ).coerceIn(0f, maxX).let {
                                            with(overlayDensity) { it.toDp() }.value
                                        }
                                    artworkY = (
                                        with(overlayDensity) { artworkY.dp.toPx() } + delta.y
                                        ).coerceIn(0f, maxY).let {
                                            with(overlayDensity) { it.toDp() }.value
                                        }
                                },
                                onDragFinished = {
                                    collapsedArtworkPreferences.edit()
                                        .putFloat("x_dp", artworkX)
                                        .putFloat("y_dp", artworkY)
                                        .apply()
                                },
                                onExpand = { lyricsCollapsed = false }
                            )
                            }
                        }
                    }

                collectionCardTransition?.let { transition ->
                        val progress = collectionTransitionProgress.value
                        val artworkAlpha by animateFloatAsState(
                            targetValue = if (progress >= 1f && !collectionLoading) 0f else 1f,
                            animationSpec = tween(260),
                            label = "collectionArtworkTransitionFade"
                        )
                        val targetWidthPx = with(density) { maxWidth.toPx() }
                        val targetHeightPx = with(density) { maxHeight.toPx() }
                        val sourceBounds = transition.sourceBounds
                        val sourceLeft = sourceBounds.left - rootWindowOrigin.x
                        val sourceTop = sourceBounds.top - rootWindowOrigin.y
                        val currentWidth = sourceBounds.width +
                            (targetWidthPx - sourceBounds.width) * progress
                        val currentHeight = sourceBounds.height +
                            (targetHeightPx - sourceBounds.height) * progress
                        val currentLeft = sourceLeft * (1f - progress)
                        val currentTop = sourceTop * (1f - progress)
                        val artwork = transition.item.heroArtwork
                            .ifBlank { transition.item.artwork }
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(currentLeft.roundToInt(), currentTop.roundToInt())
                                }
                                .size(
                                    with(density) { currentWidth.toDp() },
                                    with(density) { currentHeight.toDp() }
                                )
                                .clip(RoundedCornerShape(lerp(20.dp, 0.dp, progress)))
                                .graphicsLayer {
                                    alpha = artworkAlpha
                                }
                                .zIndex(2f)
                        ) {
                            AsyncImage(
                                model = artwork,
                                contentDescription = transition.item.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = !fullscreenLyrics &&
                        !lyricsCollapsed &&
                        (selectedSong == null || selectedPage != ToolPage.Search ||
                            lyricsNavigationVisible),
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            bottom = WindowInsets.navigationBars.asPaddingValues()
                                .calculateBottomPadding() + 12.dp
                        )
                ) {
                    AnimatedFloatingNavigationBar(
                        selectedPage = if (selectedSong != null) lyricsReturnPage else selectedPage,
                        collectionTheme = selectedPage == ToolPage.Collection ||
                            selectedPage == ToolPage.Artist ||
                            selectedPage == ToolPage.ArtistTopSongs,
                        collapseProgress = navigationCollapseProgress,
                        darkTheme = appDarkTheme,
                        expandedWidth = originalPlayerCapsuleWidth,
                        onSelect = { page ->
                            selectedPage = page
                            if (page == ToolPage.Home) {
                                scope.launch {
                                    homeRecyclerState.scrollToPosition(0)
                                }
                            }
                            if (page != ToolPage.Settings) {
                                selectedSong = null
                                lyricsCollapsed = false
                                fullscreenLyrics = false
                                fetchedLyrics = null
                                lyricsError = null
                            }
                        }
                    )
                }
            }
        }

    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun CatalogCollectionPage(
    collection: HomeCollection?,
    listState: LazyListState,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onLoadMoreTracks: () -> Unit,
    onPlayTrack: (Int) -> Unit,
    onShuffle: () -> Unit,
    onDownload: () -> Unit,
    onOpenAlbum: (AppleLibrarySync.SongRow) -> Unit,
    onOpenArtist: (AppleLibrarySync.SongRow) -> Unit,
    playingSong: SongHit?,
    isPlaybackPlaying: Boolean
) {
    val context = LocalContext.current
    val localView = LocalView.current
    val imageLoader = remember(context) { ImageLoader(context) }
    val metadata = collection?.metadata
    val artworkAspectRatio = (metadata?.artworkAspectRatio
        ?: collection?.item?.artworkAspectRatio
        ?: 0.75f).coerceIn(0.5f, 2f)
    val artworkWidthPx = context.resources.displayMetrics.widthPixels.coerceAtLeast(1)
        .coerceAtMost(2048)
    val artworkHeightPx = (artworkWidthPx / artworkAspectRatio).toInt().coerceAtLeast(1)
    val statusBarInsetPx = remember(context) {
        val rootInsets = localView.rootWindowInsets
        val systemTop = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && rootInsets != null
        ) {
            rootInsets.getInsetsIgnoringVisibility(
                android.view.WindowInsets.Type.statusBars()
            ).top
        } else {
            rootInsets?.systemWindowInsetTop ?: 0
        }
        val cutoutTop = rootInsets?.displayCutout?.safeInsetTop ?: 0
        maxOf(systemTop, cutoutTop).takeIf { it > 0 } ?: context.resources.getIdentifier(
            "status_bar_height",
            "dimen",
            "android"
        ).let { resourceId ->
            if (resourceId != 0) context.resources.getDimensionPixelSize(resourceId) else 0
        }
    }
    val collectionControlsTopInset =
        with(LocalDensity.current) { statusBarInsetPx.toDp() } + 8.dp
    val artworkTemplate = metadata?.artworkUrl.orEmpty()
        .ifBlank { collection?.item?.ambientPreviewUrl.orEmpty() }
        .ifBlank { collection?.item?.artwork.orEmpty() }
    val artworkUrl = resolveResponsiveArtwork(
        artworkTemplate,
        artworkWidthPx,
        artworkHeightPx
    )
    val ambientVideoUrl = metadata?.ambientVideoUrl.orEmpty()
        .ifBlank { collection?.item?.ambientVideoUrl.orEmpty() }
    var pageBackground by remember(artworkUrl) { mutableStateOf(Color(0xFF18181A)) }
    var accentColor by remember(artworkUrl) { mutableStateOf(Color(0xFFD1D1D6)) }
    var sortedByTitle by remember(artworkUrl) { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    val animatedPageBackground by animateColorAsState(
        targetValue = pageBackground,
        animationSpec = tween(500),
        label = "collectionArtworkColor"
    )
    val animatedAccentColor by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(500),
        label = "collectionAccentColor"
    )

    LaunchedEffect(artworkUrl) {
        if (artworkUrl.isBlank()) return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(artworkUrl)
            .size(artworkWidthPx, artworkHeightPx)
            .allowHardware(false)
            .build()
        try {
            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = result.drawable.toBitmap()
                val palette = withContext(Dispatchers.Default) {
                    Palette.from(bitmap).generate()
                }
                val baseSwatch = palette.darkMutedSwatch
                    ?: palette.darkVibrantSwatch
                    ?: palette.mutedSwatch
                    ?: palette.dominantSwatch
                val accentSwatch = palette.vibrantSwatch
                    ?: palette.lightVibrantSwatch
                    ?: palette.darkVibrantSwatch
                    ?: palette.dominantSwatch
                baseSwatch?.let { swatch ->
                    val extracted = Color(swatch.rgb)
                    val darkening = if (extracted.luminance() > 0.3f) 0.58f else 0.26f
                    pageBackground = lerp(extracted, Color.Black, darkening)
                }
                accentSwatch?.let { swatch ->
                    val extracted = Color(swatch.rgb)
                    accentColor = if (extracted.luminance() < 0.28f) {
                        lerp(extracted, Color.White, 0.55f)
                    } else {
                        extracted
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w("CatalogCollectionPage", "封面取色失败：${error.message}")
        }
    }

    val trackCardColor = lerp(animatedPageBackground, Color.White, 0.08f)
    val secondaryTextColor = Color.White.copy(alpha = 0.82f)
    when {
        isLoading -> Box(
            Modifier.fillMaxSize().background(animatedPageBackground),
            contentAlignment = Alignment.Center
        ) {
            if (artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().blur(32.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to animatedPageBackground.copy(alpha = 0.52f),
                        0.5f to animatedPageBackground.copy(alpha = 0.82f),
                        1f to animatedPageBackground.copy(alpha = 0.96f)
                    )
                )
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (artworkUrl.isNotBlank()) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = collection?.item?.title,
                        modifier = Modifier
                            .size(208.dp)
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                CircularProgressIndicator(
                    modifier = Modifier.padding(top = 26.dp),
                    color = animatedAccentColor
                )
            }
        }
        error != null -> Column(
            modifier = Modifier.fillMaxSize()
                .background(animatedPageBackground)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            TextButton(onClick = onBack) { Text("‹ 主页", color = animatedAccentColor) }
            Text("无法加载曲目", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(error, color = Color(0xFFB7B7BC), modifier = Modifier.padding(top = 8.dp))
        }
        collection != null -> {
            val description = metadata?.description.orEmpty()
                .ifBlank { collection.item.collectionDescription }
            val displayedTracks = remember(collection.tracks, sortedByTitle) {
                if (sortedByTitle) collection.tracks.sortedBy { it.title.lowercase() }
                else collection.tracks
            }
            val latestOnLoadMoreTracks = rememberUpdatedState(onLoadMoreTracks)
            LaunchedEffect(
                collection.item.id,
                displayedTracks.size,
                collection.hasMoreTracks,
                collection.loadingMoreTracks,
                collection.trackLoadError
            ) {
                if (
                    !collection.hasMoreTracks ||
                    collection.loadingMoreTracks ||
                    collection.trackLoadError != null ||
                    displayedTracks.isEmpty()
                ) {
                    return@LaunchedEffect
                }
                snapshotFlow {
                    listState.layoutInfo.visibleItemsInfo
                        .mapNotNull { it.key as? String }
                        .lastOrNull { it.startsWith("collection-track:") }
                }.collectLatest { lastVisibleTrackKey ->
                    val lastVisibleTrackIndex = lastVisibleTrackKey?.let { visibleKey ->
                        displayedTracks.indexOfFirst {
                            "collection-track:${it.uri}" == visibleKey
                        }
                    } ?: -1
                    if (
                        lastVisibleTrackIndex >=
                        displayedTracks.size - COLLECTION_TRACK_PREFETCH_REMAINING
                    ) {
                        latestOnLoadMoreTracks.value()
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize().background(animatedPageBackground)) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().blur(32.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to animatedPageBackground.copy(alpha = 0.6f),
                            0.35f to animatedPageBackground.copy(alpha = 0.87f),
                            1f to animatedPageBackground.copy(alpha = 0.96f)
                        )
                    )
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 160.dp)
                ) {
                    item(key = "collection-hero") {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val artworkHeight = (maxWidth / artworkAspectRatio)
                            .coerceIn(220.dp, 680.dp)
                        val heroHeight = artworkHeight + 106.dp
                        Box(modifier = Modifier.fillMaxWidth().height(heroHeight)) {
                            val blurTailHeight = 176.dp
                            val blurOverlap = 88.dp
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .height(blurTailHeight)
                                    .offset(y = artworkHeight - blurOverlap)
                            ) {
                                AsyncImage(
                                    model = artworkUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().blur(36.dp),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    alignment = Alignment.BottomCenter
                                )
                                Box(
                                    modifier = Modifier.fillMaxSize().background(
                                        Brush.verticalGradient(
                                            0f to Color.Transparent,
                                            0.38f to animatedPageBackground.copy(alpha = 0.14f),
                                            0.76f to animatedPageBackground.copy(alpha = 0.72f),
                                            1f to animatedPageBackground
                                        )
                                    )
                                )
                            }
                            AmbientCollectionArtwork(
                                artworkUrl = artworkUrl,
                                videoUrl = ambientVideoUrl,
                                modifier = Modifier.fillMaxWidth().height(artworkHeight)
                            )
                            Box(
                                Modifier.fillMaxWidth().height(heroHeight).background(
                                    Brush.verticalGradient(
                                        0f to Color.Black.copy(alpha = 0.18f),
                                        0.54f to animatedPageBackground.copy(alpha = 0.04f),
                                        0.83f to animatedPageBackground.copy(alpha = 0.7f),
                                        1f to animatedPageBackground
                                    )
                                )
                            )
                            Column(
                                modifier = Modifier.align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    metadata?.title?.ifBlank { collection.item.title }
                                        ?: collection.item.title,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                val creator = metadata?.creator.orEmpty()
                                    .ifBlank { collection.item.creator }
                                    .ifBlank {
                                        collection.item.subtitle
                                    }
                                if (creator.isNotBlank()) {
                                    Text(
                                        creator,
                                        color = Color(0xFFE3F4ED),
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(top = 3.dp).then(
                                            if (collection.item.isAlbum) {
                                                Modifier.clickable {
                                                    onOpenArtist(
                                                        AppleLibrarySync.SongRow().apply {
                                                            artist = creator
                                                            artistId = metadata?.creatorId.orEmpty()
                                                        }
                                                    )
                                                }
                                            } else {
                                                Modifier
                                            }
                                        )
                                    )
                                }
                                val count = collection.totalTrackCount.takeIf { it > 0 }
                                    ?: metadata?.trackCount?.takeIf { it > 0 }
                                    ?: collection.item.trackCount.takeIf { it > 0 }
                                    ?: collection.tracks.size
                                val date = metadata?.updatedAt.orEmpty()
                                    .ifBlank { collection.item.updatedAt }
                                    .takeIf(String::isNotBlank)
                                    ?.let(::formatCollectionDate)
                                val dateLabel = date?.let {
                                    if (collection.item.isAlbum) "发行于 $it" else "更新于 $it"
                                }
                                Text(
                                    listOfNotNull("$count 首歌曲", dateLabel).joinToString(" · "),
                                    color = secondaryTextColor,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                        }
                        stickyHeader(key = "collection-actions") {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(animatedPageBackground.copy(alpha = 0.96f))
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onShuffle,
                                enabled = collection.tracks.isNotEmpty(),
                                modifier = Modifier.size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(trackCardColor)
                            ) {
                                Icon(
                                    Icons.Filled.Shuffle,
                                    contentDescription = "随机播放",
                                    tint = Color.White
                                )
                            }
                            Button(
                                onClick = { onPlayTrack(0) },
                                modifier = Modifier.weight(1f).height(56.dp),
                                enabled = collection.tracks.isNotEmpty(),
                                shape = RoundedCornerShape(10.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = animatedPageBackground
                                )
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Text(
                                    "播放",
                                    modifier = Modifier.padding(start = 4.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(
                                onClick = onDownload,
                                enabled = collection.tracks.isNotEmpty(),
                                modifier = Modifier.size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(trackCardColor)
                            ) {
                                Icon(
                                    Icons.Filled.CloudDownload,
                                    contentDescription = "下载",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    if (description.isNotBlank()) {
                        item(key = "collection-description") {
                            Text(
                                description,
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                color = secondaryTextColor,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    collection.metadataError?.let { detailsError ->
                        item(key = "collection-metadata-warning") {
                            Text(
                                "部分详情暂不可用：$detailsError",
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 4.dp),
                                color = Color(0xFFFFD18A),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    itemsIndexed(
                        displayedTracks,
                        key = { _, row -> "collection-track:${row.uri}" }
                    ) { index, row ->
                        HomeCollectionTrackRow(
                            track = row,
                            index = index,
                            cardColor = trackCardColor,
                            secondaryTextColor = secondaryTextColor,
                            isPlaying = isPlaybackPlaying &&
                                playingSong?.let {
                                    it.title == row.title && it.artist == row.artist
                                } == true,
                            onOpenAlbum = { onOpenAlbum(row) },
                            onOpenArtist = { onOpenArtist(row) },
                            onClick = {
                                val actualIndex = collection.tracks.indexOf(row)
                                onPlayTrack(actualIndex.coerceAtLeast(0))
                            }
                        )
                    }
                    if (collection.hasMoreTracks) {
                        item(key = "collection-track-pagination") {
                            when {
                                collection.loadingMoreTracks -> Box(
                                    Modifier.fillMaxWidth().padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = animatedAccentColor)
                                }
                                collection.trackLoadError != null -> TextButton(
                                    onClick = onLoadMoreTracks,
                                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                                ) {
                                    Text(
                                        "加载失败，点按重试：${collection.trackLoadError}",
                                        color = Color(0xFFFFD18A)
                                    )
                                }
                                else -> Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            top = collectionControlsTopInset,
                            end = 16.dp,
                            bottom = 12.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = animatedPageBackground.copy(alpha = 0.76f)
                    ) {
                        IconButton(onClick = onBack) {
                            Text("‹", color = Color.White, fontSize = 30.sp, lineHeight = 30.sp)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = animatedPageBackground.copy(alpha = 0.76f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onSearch) {
                                Icon(Icons.Filled.Search, contentDescription = "搜索", tint = Color.White)
                            }
                            Box {
                                IconButton(onClick = { sortedByTitle = !sortedByTitle }) {
                                    Icon(Icons.Filled.Sort, contentDescription = "排序", tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(if (sortedByTitle) "按曲目顺序" else "按标题排序")
                                        },
                                        onClick = {
                                            sortedByTitle = !sortedByTitle
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                            IconButton(onClick = { showSortMenu = !showSortMenu }) {
                                Icon(Icons.Filled.MoreHoriz, contentDescription = "更多", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AmbientCollectionArtwork(
    artworkUrl: String,
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(modifier = modifier.clipToBounds()) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
        if (videoUrl.startsWith("https://", ignoreCase = true)) {
            val player = remember(context, videoUrl) {
                ExoPlayer.Builder(context.applicationContext).build().apply {
                    volume = 0f
                    repeatMode = Player.REPEAT_MODE_ONE
                    videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                }
            }
            var textureView by remember(player) { mutableStateOf<TextureView?>(null) }
            var renderedFirstFrame by remember(videoUrl) { mutableStateOf(false) }
            DisposableEffect(player, videoUrl) {
                val listener = object : Player.Listener {
                    override fun onRenderedFirstFrame() {
                        renderedFirstFrame = true
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        renderedFirstFrame = false
                        Log.w(
                            "CatalogCollectionPage",
                            "动态封面播放失败，改用静态预览图",
                            error
                        )
                    }
                }
                player.addListener(listener)
                player.setMediaItem(MediaItem.fromUri(videoUrl))
                player.prepare()
                player.playWhenReady = true
                onDispose {
                    player.removeListener(listener)
                    textureView?.let(player::clearVideoTextureView)
                    player.release()
                    textureView = null
                }
            }
            AndroidView(
                factory = { viewContext ->
                    TextureView(viewContext).also { view ->
                        textureView = view
                        player.setVideoTextureView(view)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (!renderedFirstFrame) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
        }
    }
}

private fun resolveResponsiveArtwork(
    template: String,
    width: Int,
    height: Int
): String = template
    .replace("{w}", width.toString())
    .replace("{h}", height.toString())
    .replace("{f}", "jpg")

private fun formatCollectionDate(rawDate: String): String {
    val datePart = rawDate.take(10)
    return try {
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .parse(datePart)
            ?: return datePart
        java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US).format(date)
    } catch (_: java.text.ParseException) {
        datePart
    }
}

@Composable
private fun HomeCollectionTrackRow(
    track: AppleLibrarySync.SongRow,
    index: Int,
    cardColor: Color,
    secondaryTextColor: Color,
    isPlaying: Boolean,
    onOpenAlbum: () -> Unit,
    onOpenArtist: () -> Unit,
    onClick: () -> Unit
) {
    var showTrackMenu by remember(track.uri) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPlaying) {
            Icon(
                Icons.Filled.GraphicEq,
                contentDescription = "正在播放",
                tint = Color(0xFFA4E6C8),
                modifier = Modifier.width(44.dp).height(44.dp).padding(10.dp)
            )
        } else {
            AsyncImage(
                model = track.coverUrl.ifBlank { null },
                contentDescription = null,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            Text(
                track.title,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onOpenAlbum)
            )
            Text(
                track.artist,
                color = secondaryTextColor,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onOpenArtist)
            )
        }
        val durationSeconds = track.durationMs / 1000L
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (durationSeconds > 0) {
                    "%d:%02d".format(durationSeconds / 60, durationSeconds % 60)
                } else {
                    (index + 1).toString()
                },
                color = secondaryTextColor,
                style = MaterialTheme.typography.labelMedium
            )
            Box {
                IconButton(
                    onClick = { showTrackMenu = true },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Filled.MoreHoriz,
                        contentDescription = "更多：${track.title}",
                        tint = secondaryTextColor
                    )
                }
                DropdownMenu(
                    expanded = showTrackMenu,
                    onDismissRequest = { showTrackMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("播放") },
                        onClick = {
                            showTrackMenu = false
                            onClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicHomePage(
    shelves: List<Shelf>,
    profileAvatarUri: Uri?,
    darkTheme: Boolean,
    statusBarHeight: Dp,
    isSignedIn: Boolean,
    isLoading: Boolean,
    error: String?,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
    recyclerState: ComposeRecyclerListState,
    sectionStateHolder: androidx.compose.runtime.saveable.SaveableStateHolder,
    onScrollChanged: (Boolean, Boolean) -> Unit,
    onOpenItem: (ShelfItem, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val returnToTop: () -> Unit = {
        recyclerState.scrollToPosition(0)
    }
    val featuredShelf = shelves.firstOrNull()
    val featuredItems = featuredShelf?.items.orEmpty()
    val remainingShelves = remember(shelves) { shelves.drop(1) }
    val appleRed = Color(0xFFFF375F)
    val background = if (darkTheme) Color(0xFF1C1C1E)
    else MaterialTheme.colorScheme.background
    val primaryTextColor = if (darkTheme) Color.White else MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = if (darkTheme) Color(0xFFAAAAAF)
    else MaterialTheme.colorScheme.onSurfaceVariant
    val cardColor = if (darkTheme) Color(0xFF303034)
    else MaterialTheme.colorScheme.surfaceVariant
    val density = LocalDensity.current
    val scrollCollapseDistancePx = with(density) { 56.dp.toPx() }
    val headerCollapseProgress = remember(recyclerState, scrollCollapseDistancePx) {
        derivedStateOf {
            if (recyclerState.firstVisibleItemPosition > 0) {
                1f
            } else {
                (recyclerState.scrollOffsetPx / scrollCollapseDistancePx)
                    .coerceIn(0f, 1f)
            }
        }
    }
    var previousScrollY by remember(recyclerState) { mutableIntStateOf(-1) }
    var candidateDirection by remember(recyclerState) { mutableStateOf<Boolean?>(null) }
    var accumulatedDistance by remember(recyclerState) { mutableIntStateOf(0) }
    val directionThresholdPx = with(density) { 10.dp.toPx() }
    val onRecyclerScroll: (RecyclerView, Int, Int) -> Unit = { recyclerView, _, dy ->
        val currentScrollY = recyclerView.computeVerticalScrollOffset()
        if (previousScrollY < 0) {
            onScrollChanged(currentScrollY > 24, true)
            previousScrollY = currentScrollY
        } else if (dy != 0) {
            val scrollingDown = dy > 0
            if (candidateDirection != scrollingDown) {
                candidateDirection = scrollingDown
                accumulatedDistance = 0
            }
            accumulatedDistance += kotlin.math.abs(dy)
            if (currentScrollY == 0) {
                onScrollChanged(false, false)
                accumulatedDistance = 0
            } else if (accumulatedDistance >= directionThresholdPx) {
                onScrollChanged(scrollingDown, false)
                accumulatedDistance = 0
            }
            previousScrollY = currentScrollY
        }
    }
    var focusedShelfKey by remember(recyclerState) { mutableStateOf<String?>(null) }
    LaunchedEffect(recyclerState) {
        snapshotFlow { recyclerState.isScrollInProgress }.collectLatest { scrolling ->
            if (scrolling) {
                focusedShelfKey = null
            } else {
                delay(220)
                focusedShelfKey = recyclerState.focusedItemKey
            }
        }
    }
    val latestOnOpenItem = rememberUpdatedState(onOpenItem)
    val latestOnSettings = rememberUpdatedState(onSettings)
    val latestIsLoading = rememberUpdatedState(isLoading)
    val sections = buildList {
        if (!isSignedIn) {
            add(
                ComposeRecyclerSection(
                    key = "home:connect",
                    viewType = 1,
                    revision = cardColor
                ) {
                    Card(
                        onClick = { latestOnSettings.value() },
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor)
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text(
                                "连接 Apple Music",
                                color = primaryTextColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "登录后查看为你精选的歌单、专辑和个性电台。",
                                color = secondaryTextColor,
                                modifier = Modifier.padding(top = 5.dp)
                            )
                        }
                    }
                }
            )
        } else if (error != null) {
            add(
                ComposeRecyclerSection(
                    key = "home:error",
                    viewType = 2,
                    revision = error
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(error, color = secondaryTextColor)
                    }
                }
            )
        }

        val featuredShelfFocused = focusedShelfKey == "home:featured"
        add(
            ComposeRecyclerSection(
                key = "home:featured",
                viewType = 3,
                revision = listOf(featuredShelf, isLoading, featuredShelfFocused, darkTheme)
            ) {
                Column {
                    Text(
                        featuredShelf?.title ?: "为你精选",
                        modifier = Modifier.padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 18.dp,
                            bottom = 12.dp
                        ),
                        color = primaryTextColor,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (featuredItems.isEmpty()) {
                        Text(
                            if (latestIsLoading.value) {
                                "正在准备你的推荐…"
                            } else {
                                "登录 Apple Music 后，这里会出现专属推荐。"
                            },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            color = secondaryTextColor
                        )
                    } else {
                        val featuredRowState = rememberLazyListState()
                        val featuredFocusIndex = rememberSettledFocusIndex(
                            rowState = featuredRowState,
                            isShelfFocused = featuredShelfFocused
                        )
                        LazyRow(
                            state = featuredRowState,
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            itemsIndexed(
                                featuredItems,
                                key = { _, item -> "featured:${item.id}" }
                            ) { index, item ->
                                HomeFeatureCard(
                                    item = item,
                                    isFocused = index == featuredFocusIndex,
                                    onClick = { bounds ->
                                        latestOnOpenItem.value(item, bounds)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        )

        remainingShelves.forEach { shelf ->
            val shelfFocused = focusedShelfKey == "home:${shelf.id}"
            add(
                ComposeRecyclerSection(
                    key = "home:${shelf.id}",
                    viewType = 4,
                    revision = listOf(shelf, darkTheme, shelfFocused)
                ) {
                    HomeShelfSection(
                        shelf = shelf,
                        darkTheme = darkTheme,
                        isShelfFocused = shelfFocused,
                        onOpenItem = { item, bounds ->
                            latestOnOpenItem.value(item, bounds)
                        }
                    )
                }
            )
        }
    }
    val headerSurfaceBaseColor =
        if (darkTheme) Color(0xE52C2C2E) else Color(0xEFFFFFFF)
    val headerSurfaceModifier = Modifier.drawBehind {
        val progress = headerCollapseProgress.value
        drawRoundRect(
            color = headerSurfaceBaseColor.copy(alpha = headerSurfaceBaseColor.alpha * progress),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
        )
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
    ) {
        ComposeRecyclerSections(
            sections = sections,
            state = recyclerState,
            saveableStateHolder = sectionStateHolder,
            contentPadding = PaddingValues(top = 90.dp, bottom = 160.dp),
            onScroll = onRecyclerScroll,
            onPullToRefresh = { if (!isLoading) onRefresh() },
            pullToRefreshThresholdPx = with(density) { 64.dp.toPx() },
            modifier = Modifier.fillMaxSize()
        )
        Row(
            modifier = Modifier.align(Alignment.TopStart)
                .offset(y = statusBarHeight)
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = headerSurfaceModifier.clickable(onClick = returnToTop),
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent
            ) {
                Text(
                    "主页",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = primaryTextColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                modifier = headerSurfaceModifier,
                shape = RoundedCornerShape(28.dp),
                color = Color.Transparent
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSettings) {
                        if (profileAvatarUri == null) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = "账户与设置",
                                tint = appleRed
                            )
                        } else {
                            AsyncImage(
                                model = profileAvatarUri,
                                contentDescription = "账户与设置",
                                modifier = Modifier.size(46.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeFeatureCard(
    item: ShelfItem,
    isFocused: Boolean,
    onClick: (Rect) -> Unit
) {
    val cardCoordinates = remember { arrayOfNulls<LayoutCoordinates>(1) }
    Card(
        onClick = { onClick(cardCoordinates[0]?.boundsInWindow() ?: Rect.Zero) },
        modifier = Modifier.width(240.dp).aspectRatio(0.75f)
            .onGloballyPositioned { cardCoordinates[0] = it },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF39393D))
    ) {
        Box(Modifier.fillMaxSize()) {
            HomeMotionArtwork(
                item = item,
                artworkUrl = item.ambientPreviewUrl
                    .ifBlank { item.heroArtwork.ifBlank { item.artwork } },
                isFocused = isFocused,
                preferSquareArtwork = false,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Box(
                Modifier.fillMaxSize()
                    .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC111114)),
                        startY = 110f
                    )
                )
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            ) {
                Text(
                    item.reason.ifBlank { item.badge.ifBlank { "为你推荐" } },
                    color = Color(0xFFE5E5E7),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    item.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp)
                )
                if (item.subtitle.isNotBlank()) {
                    Text(
                        item.subtitle,
                        color = Color(0xFFD1D1D6),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeShelfSection(
    shelf: Shelf,
    darkTheme: Boolean,
    isShelfFocused: Boolean,
    onOpenItem: (ShelfItem, Rect) -> Unit
) {
    if (shelf.items.isEmpty()) return
    val primaryTextColor = if (darkTheme) Color.White else MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = if (darkTheme) Color(0xFFAAAAAF)
    else MaterialTheme.colorScheme.onSurfaceVariant
    val rowState = rememberLazyListState()
    val focusedIndex = rememberSettledFocusIndex(rowState, isShelfFocused)
    Column(Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 4.dp)) {
        Text(
            shelf.title,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            color = primaryTextColor,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(shelf.items, key = { _, item -> item.id }) { index, item ->
                val itemCoordinates = remember(item.id) {
                    arrayOfNulls<LayoutCoordinates>(1)
                }
                Column(
                    modifier = Modifier.width(156.dp)
                        .onGloballyPositioned { itemCoordinates[0] = it }
                        .clickable {
                            onOpenItem(item, itemCoordinates[0]?.boundsInWindow() ?: Rect.Zero)
                        }
                ) {
                    if (item.artwork.isNotBlank() || item.ambientPreviewUrl.isNotBlank()) {
                        HomeMotionArtwork(
                            item = item,
                            artworkUrl = item.ambientSquarePreviewUrl
                                .ifBlank { item.artwork },
                            isFocused = isShelfFocused && index == focusedIndex,
                            preferSquareArtwork = true,
                            modifier = Modifier.fillMaxWidth().height(156.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(156.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (darkTheme) Color(0xFF39393D)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = Color(0xFFFF375F)
                            )
                        }
                    }

                    Text(
                        item.title,
                        modifier = Modifier.padding(top = 7.dp),
                        color = primaryTextColor,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.subtitle.isNotBlank()) {
                        Text(
                            item.subtitle,
                            color = secondaryTextColor,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListState.focusedItemIndex(focusFraction: Float = 0.36f): Int {
    val layout = layoutInfo
    val viewportStart = layout.viewportStartOffset
    val viewportEnd = layout.viewportEndOffset
    val focusX = viewportStart + (viewportEnd - viewportStart) * focusFraction
    return layout.visibleItemsInfo
        .filter { item ->
            val visibleStart = maxOf(item.offset, viewportStart)
            val visibleEnd = minOf(item.offset + item.size, viewportEnd)
            visibleEnd - visibleStart >= item.size / 2
        }
        .minByOrNull { item ->
            kotlin.math.abs(item.offset + item.size / 2f - focusX)
        }
        ?.index ?: -1
}

@Composable
private fun rememberSettledFocusIndex(
    rowState: LazyListState,
    isShelfFocused: Boolean
): Int {
    var focusedIndex by remember(rowState) { mutableStateOf(-1) }
    LaunchedEffect(rowState, isShelfFocused) {
        if (!isShelfFocused) {
            focusedIndex = -1
            return@LaunchedEffect
        }
        snapshotFlow { rowState.isScrollInProgress }.collectLatest { scrolling ->
            if (scrolling) {
                focusedIndex = -1
            } else {
                delay(180)
                focusedIndex = rowState.focusedItemIndex()
            }
        }
    }
    return focusedIndex
}

private fun LazyListState.focusedItemKey(focusFraction: Float = 0.42f): Any? {
    val layout = layoutInfo
    val viewportStart = layout.viewportStartOffset
    val viewportEnd = layout.viewportEndOffset
    val focusY = viewportStart + (viewportEnd - viewportStart) * focusFraction
    return layout.visibleItemsInfo
        .filter { item ->
            val visibleStart = maxOf(item.offset, viewportStart)
            val visibleEnd = minOf(item.offset + item.size, viewportEnd)
            visibleEnd - visibleStart >= item.size / 2
        }
        .minByOrNull { item ->
            kotlin.math.abs(item.offset + item.size / 2f - focusY)
        }
        ?.key
}

private data class HomeMotionAssets(
    val videoUrl: String,
    val previewUrl: String
)

private val homeMotionAssetCache = ConcurrentHashMap<String, HomeMotionAssets>()

@Composable
private fun HomeMotionArtwork(
    item: ShelfItem,
    artworkUrl: String,
    isFocused: Boolean,
    preferSquareArtwork: Boolean,
    modifier: Modifier = Modifier,
    contentScale: androidx.compose.ui.layout.ContentScale
) {
    val context = LocalContext.current
    val resourceType = if (item.isAlbum) "albums" else "playlists"
    val resourceId = item.relatedCatalogId.ifBlank {
        item.catalogId.ifBlank { item.id }
    }
    val cacheKey = "$resourceType:$resourceId:${if (preferSquareArtwork) "square" else "tall"}"
    val initialVideoUrl = if (preferSquareArtwork) {
        item.ambientSquareVideoUrl
    } else {
        item.ambientVideoUrl
    }
    val initialPreviewUrl = if (preferSquareArtwork) {
        item.ambientSquarePreviewUrl.ifBlank { item.ambientPreviewUrl }
    } else {
        item.ambientPreviewUrl
    }
    var resolvedAssets by remember(cacheKey) {
        mutableStateOf(
            homeMotionAssetCache[cacheKey] ?: HomeMotionAssets(
                initialVideoUrl,
                initialPreviewUrl
            )
        )
    }
    var playbackEnabled by remember(cacheKey) { mutableStateOf(false) }
    LaunchedEffect(item.id, isFocused) {
        if (!isFocused || resolvedAssets.videoUrl.isNotBlank() ||
            homeMotionAssetCache.containsKey(cacheKey) ||
            !(item.isPlaylist || item.isAlbum)
        ) {
            return@LaunchedEffect
        }
        try {
            val metadata = withContext(Dispatchers.IO) {
                MusicRepository.api.getRecommendedCollectionMetadata(
                    item,
                    preferSquareArtwork = preferSquareArtwork
                )
            }
            resolvedAssets = HomeMotionAssets(
                videoUrl = if (preferSquareArtwork) {
                    metadata.ambientSquareVideoUrl.ifBlank { metadata.ambientVideoUrl }
                } else {
                    metadata.ambientVideoUrl
                },
                previewUrl = if (preferSquareArtwork) {
                    metadata.ambientSquarePreviewUrl
                        .ifBlank { metadata.ambientPreviewUrl }
                } else {
                    metadata.ambientPreviewUrl
                }
            )
            homeMotionAssetCache[cacheKey] = resolvedAssets
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w("MusicHomePage", "无法获取《${item.title}》的动态封面信息", error)
            homeMotionAssetCache.putIfAbsent(
                cacheKey,
                HomeMotionAssets(initialVideoUrl, initialPreviewUrl)
            )
        }
    }
    LaunchedEffect(isFocused, resolvedAssets.videoUrl) {
        playbackEnabled = false
        if (isFocused && resolvedAssets.videoUrl.startsWith("https://", ignoreCase = true)) {
            delay(300)
            playbackEnabled = true
        }
    }

    Box(modifier = modifier) {
        val staticArtworkUrl = resolvedAssets.previewUrl.ifBlank { artworkUrl }
        if (staticArtworkUrl.isNotBlank()) {
            AsyncImage(
                model = staticArtworkUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }
        if (playbackEnabled) {
            val player = remember(context, resolvedAssets.videoUrl) {
                ExoPlayer.Builder(context.applicationContext).build().apply {
                    volume = 0f
                    repeatMode = Player.REPEAT_MODE_ONE
                    videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                }
            }
            var textureView by remember(player) { mutableStateOf<TextureView?>(null) }
            var renderedFirstFrame by remember(resolvedAssets.videoUrl) {
                mutableStateOf(false)
            }
            DisposableEffect(player, resolvedAssets.videoUrl) {
                val listener = object : Player.Listener {
                    override fun onRenderedFirstFrame() {
                        renderedFirstFrame = true
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.w("MusicHomePage", "主页动态封面播放失败", error)
                    }
                }
                player.addListener(listener)
                player.setMediaItem(MediaItem.fromUri(resolvedAssets.videoUrl))
                player.prepare()
                player.playWhenReady = true
                onDispose {
                    player.removeListener(listener)
                    textureView?.let(player::clearVideoTextureView)
                    player.release()
                    textureView = null
                }
            }
            AndroidView(
                factory = { viewContext ->
                    TextureView(viewContext).also { view ->
                        textureView = view
                        player.setVideoTextureView(view)
                    }
                },
                update = { view ->
                    view.alpha = if (renderedFirstFrame) 1f else 0f
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SongResults(
    songs: List<SongHit>,
    searching: Boolean,
    query: String,
    lyricMatches: Boolean,
    onOpenAlbum: (SongHit) -> Unit,
    onOpenArtist: (SongHit) -> Unit,
    onSongClick: (SongHit) -> Unit
) {
    if (searching && songs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (songs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(78.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(21.dp)
                    )
                }
                Text(
                    if (query.isBlank()) "从一首歌开始" else "还没有搜索结果",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 18.dp)
                )
                Text(
                    if (query.isBlank()) "输入歌名或艺人，找到你想要的歌词。"
                    else "换个关键词再试试，也可以输入歌词中的一句。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (lyricMatches) "歌词匹配结果" else "搜索结果",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${songs.size} 首",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(songs, key = { it.id }) { song ->
            SongResultCard(
                song = song,
                onClick = { onSongClick(song) },
                onOpenAlbum = { onOpenAlbum(song) },
                onOpenArtist = { onOpenArtist(song) }
            )
        }
    }
}

@Composable
private fun SongResultCard(
    song: SongHit,
    onClick: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenArtist: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.artworkUrl,
                contentDescription = "${song.title} 专辑封面",
                modifier = Modifier.size(58.dp).clip(RoundedCornerShape(18.dp))
            )
            Column(
                modifier = Modifier.weight(1f).padding(start = 14.dp, end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    song.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onOpenArtist)
                )
                if (song.album.isNotBlank()) {
                    Text(
                        song.album,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(onClick = onOpenAlbum)
                    )
                }
            }
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = "查看歌词",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 3.dp).size(21.dp)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun LyricsDetail(
    song: SongHit,
    fetchedLyrics: FetchedLyrics?,
    lyricsNotice: String?,
    playbackControlsVisible: Boolean,
    accentColor: Color,
    error: String?,
    loading: Boolean,
    signedIn: Boolean,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isFavorited: Boolean?,
    favoritePending: Boolean,
    onToggleFavorite: () -> Unit,
    externalSourceMode: Boolean,
    externalSourceName: String?,
    externalFallbackSourceName: String?,
    sourceLookupInProgress: Boolean,
    sourceLookupRequest: Int,
    lyricsAppearance: LyricsAppearance,
    traditionalToSimplified: Boolean,
    coverAccentReady: Boolean,
    playbackCardTopInset: androidx.compose.ui.unit.Dp,
    preparing: Boolean,
    playbackStatus: String,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onCollapseLyrics: () -> Unit,
    onLogin: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenArtist: () -> Unit,
    onQueueDragOffsetChange: (Float) -> Unit,
    coverQueueDragOffset: Float,
    onTogglePlayback: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onRetry: () -> Unit,
    onCopy: () -> Unit,
    onExport: (ExportFormat) -> Unit,
    onImportLyrics: () -> Unit,
    onSearchOtherSources: () -> Unit,
    onSelectAppleLyrics: () -> Unit,
    onSelectQqLyrics: () -> Unit,
    onRestoreAppleLyrics: () -> Unit,
    onOpenAppearance: () -> Unit,
    onSourceLookupSucceeded: (String, String) -> Unit,
    onSourceLookupFailed: () -> Unit,
    onLyricsScroll: () -> Unit,
    onUserInteraction: () -> Unit,
    onTranslationUnavailable: () -> Unit
) {
    var actionsMenuExpanded by remember { mutableStateOf(false) }
    val currentOnUserInteraction by rememberUpdatedState(onUserInteraction)
    var dragPositionMs by remember(song.id) { mutableStateOf<Long?>(null) }
    val hasTranslations = fetchedLyrics?.lyrics?.lines
        ?.any { !it.translation.isNullOrBlank() } == true
    var showTranslations by remember(song.id, externalSourceName, fetchedLyrics) {
        mutableStateOf(
            externalSourceMode &&
                externalSourceName == "QQ 音乐" &&
                hasTranslations
        )
    }
    val displayPositionMs = dragPositionMs ?: positionMs
    val lyricSourceName = if (externalSourceMode) externalSourceName ?: "外部来源"
    else "Apple Music"
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    currentOnUserInteraction()
                }
            }
    ) {
        Column(Modifier.fillMaxSize()) {
            if (sourceLookupInProgress) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        "正在查找其他歌词来源，暂时继续显示当前版本…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            AnimatedVisibility(
                visible = lyricsNotice != null && !sourceLookupInProgress,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Text(
                    text = lyricsNotice.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            if (
                showTranslations &&
                fetchedLyrics != null &&
                fetchedLyrics.lyrics.lines.none { !it.translation.isNullOrBlank() }
            ) {
                Text(
                    "这次获取到的歌词数据未包含译文",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            when {
            loading && fetchedLyrics == null -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("正在取回逐字歌词…", modifier = Modifier.padding(top = 14.dp))
                }
            }

            fetchedLyrics != null && !coverAccentReady -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            fetchedLyrics != null -> {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AmLyricsWebView(
                        modifier = Modifier.fillMaxSize(),
                        song = song,
                        fetchedLyrics = fetchedLyrics,
                        positionMs = displayPositionMs,
                        showTranslations = showTranslations,
                        externalSourceMode = externalSourceMode,
                        appearance = lyricsAppearance,
                        traditionalToSimplified = traditionalToSimplified,
                        sourceLookupRequest = sourceLookupRequest,
                        onSourceLookupSucceeded = onSourceLookupSucceeded,
                        onSourceLookupFailed = onSourceLookupFailed,
                        onUserScroll = onLyricsScroll,
                        onTranslationUnavailable = {
                            if (showTranslations) showTranslations = false
                            onTranslationUnavailable()
                        },
                        onLongPress = { actionsMenuExpanded = true },
                        onSeekTo = onSeekTo
                    )
                    DropdownMenu(
                        expanded = actionsMenuExpanded,
                        onDismissRequest = { actionsMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("复制 $lyricSourceName 歌词") },
                            onClick = {
                                actionsMenuExpanded = false
                                onCopy()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("下载 $lyricSourceName TXT") },
                            onClick = {
                                actionsMenuExpanded = false
                                onExport(ExportFormat.Text)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (fetchedLyrics.lyrics.synced) {
                                        "下载 $lyricSourceName LRC 时间轴"
                                    } else {
                                        "LRC 时间轴不可用（无同步时间）"
                                    }
                                )
                            },
                            enabled = fetchedLyrics.lyrics.synced,
                            onClick = {
                                actionsMenuExpanded = false
                                onExport(ExportFormat.Lrc)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (fetchedLyrics.ttml == null) "TTML 暂不可用"
                                    else "下载 $lyricSourceName TTML 逐字时间轴"
                                )
                            },
                            enabled = fetchedLyrics.ttml != null,
                            onClick = {
                                actionsMenuExpanded = false
                                onExport(ExportFormat.Ttml)
                            }
                        )
                        if (fetchedLyrics.qqQrcDebugText != null) {
                            DropdownMenuItem(
                                text = { Text("下载 QQ QRC 解密结构（供排查）") },
                                onClick = {
                                    actionsMenuExpanded = false
                                    onExport(ExportFormat.QqQrc)
                                }
                            )
                        }
                        if (
                            externalFallbackSourceName != null &&
                            externalFallbackSourceName != lyricSourceName
                        ) {
                            DropdownMenuItem(
                                text = { Text("返回 $externalFallbackSourceName 歌词") },
                                onClick = {
                                    actionsMenuExpanded = false
                                    onRestoreAppleLyrics()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (!externalSourceMode) "Apple Music 歌词（当前）"
                                    else "使用 Apple Music 歌词"
                                )
                            },
                            enabled = externalSourceMode || !loading,
                            onClick = {
                                actionsMenuExpanded = false
                                onSelectAppleLyrics()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (externalSourceMode && externalSourceName == "QQ 音乐") {
                                        "QQ 音乐歌词（当前）"
                                    } else {
                                        "使用 QQ 音乐歌词"
                                    }
                                )
                            },
                            enabled = !(externalSourceMode && externalSourceName == "QQ 音乐") &&
                                !loading,
                            onClick = {
                                actionsMenuExpanded = false
                                onSelectQqLyrics()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (externalSourceMode && externalSourceName != "QQ 音乐") {
                                        "其他在线来源（当前：$lyricSourceName）"
                                    } else {
                                        "从其他在线来源查找"
                                    }
                                )
                            },
                            enabled = !sourceLookupInProgress && !loading,
                            onClick = {
                                actionsMenuExpanded = false
                                onSearchOtherSources()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("导入 TTML / LRC 文件") },
                            onClick = {
                                actionsMenuExpanded = false
                                onImportLyrics()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("歌词效果与显示") },
                            onClick = {
                                actionsMenuExpanded = false
                                onOpenAppearance()
                            }
                        )
                    }
                }
            }

            loading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            error != null && !signedIn -> EmptyLyrics(
                title = "连接 Apple Music 后获取歌词",
                detail = error,
                action = "连接 Apple Music",
                onAction = onLogin
            )

            error != null -> EmptyLyrics(
                title = "歌词获取失败",
                detail = error,
                action = "重新获取",
                onAction = onRetry
            )

            signedIn.not() -> EmptyLyrics(
                title = "连接 Apple Music 后获取歌词",
                detail = "登录账号以读取这首歌的歌词内容。",
                action = "连接 Apple Music",
                onAction = onLogin
            )

            else -> EmptyLyrics(
                title = "这首歌暂时没有歌词",
                detail = "$lyricSourceName 可能尚未提供这首歌的歌词。",
                action = null,
                onAction = {}
            )
        }
        }
        AnimatedVisibility(
            visible = playbackControlsVisible,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(150)) +
                slideOutVertically(targetOffsetY = { -it / 4 }, animationSpec = tween(180)) +
                scaleOut(targetScale = 0.96f, animationSpec = tween(180))
        ) {
            KaraokePlaybackControls(
            song = song,
            fetchedLyrics = fetchedLyrics,
            accentColor = accentColor,
            isPlaying = isPlaying,
            preparing = preparing,
            playbackStatus = playbackStatus,
            positionMs = displayPositionMs,
            durationMs = durationMs,
            onLogin = onLogin,
            onTogglePlayback = onTogglePlayback,
            isFavorited = isFavorited,
            favoritePending = favoritePending,
            onToggleFavorite = onToggleFavorite,
            fullscreen = fullscreen,
            onToggleFullscreen = onToggleFullscreen,
            onCollapseLyrics = onCollapseLyrics,
            showTranslations = showTranslations,
            onToggleTranslations = {
                if (!showTranslations && !hasTranslations) {
                    onTranslationUnavailable()
                } else {
                    showTranslations = !showTranslations
                }
            },
            onOpenQueue = onOpenQueue,
            onOpenAlbum = onOpenAlbum,
            onOpenArtist = onOpenArtist,
            onNext = { PlaybackEngine.next() },
            onPrevious = { PlaybackEngine.prev() },
            onQueueDragOffsetChange = onQueueDragOffsetChange,
            coverQueueDragOffset = coverQueueDragOffset,
            onSeekStart = { dragPositionMs = it },
            onSeekChange = { dragPositionMs = it },
            onSeekFinished = {
                dragPositionMs?.let(onSeekTo)
                dragPositionMs = null
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = playbackCardTopInset)
        )
        }
    }
}

@Composable
private fun PlaybackQueueDialog(
    entries: List<Pair<Int, String>>,
    currentIndex: Int,
    anchorArtworkUrl: String,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visible = true,
            enter = expandVertically(expandFrom = Alignment.Top) +
                fadeIn() +
                scaleIn(
                    initialScale = 0.84f,
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.08f, 0f)
                ) +
                slideInVertically { -it / 5 },
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut() +
                scaleOut(
                    targetScale = 0.9f,
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.08f, 0f)
                )
        ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "接下来播放",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "队列共 ${entries.size} 首 · ${entries.count { it.first > currentIndex }} 首在后",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (anchorArtworkUrl.isNotBlank()) {
                        AsyncImage(
                            model = anchorArtworkUrl,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp))
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
                if (entries.isEmpty()) {
                    Text(
                        "当前没有可显示的播放队列",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(entries, key = { it.first }) { (index, uri) ->
                            val row = MusicRepository.findSong(uri)
                            val title = row?.title?.ifBlank { null }
                                ?: "歌曲信息暂不可用 · ${uri.removePrefix("apple:")}"
                            val artist = row?.artist.orEmpty()
                            val isCurrent = index == currentIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isCurrent) MaterialTheme.colorScheme.secondaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable { onSelect(index) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (isCurrent) "正在播放" else "%02d".format(index + 1),
                                    modifier = Modifier.width(64.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (isCurrent) FontWeight.SemiBold
                                        else FontWeight.Normal
                                    )
                                    Text(
                                        artist,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsAppearanceDialog(
    appearance: LyricsAppearance,
    coverAccentColor: String?,
    onAppearanceChange: (LyricsAppearance) -> Unit,
    onSelectFont: () -> Unit,
    onClearFont: () -> Unit,
    onDismiss: () -> Unit
) {
    val positionSliderInteractionSource = remember { MutableInteractionSource() }
    val isPositionSliderDragged by positionSliderInteractionSource.collectIsDraggedAsState()
    val dialogView = LocalView.current
    SideEffect {
        val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
        dialogWindow?.setDimAmount(if (isPositionSliderDragged) 0f else 0.32f)
    }
    val highlightColors = listOf(
        "#6750A4" to "薰衣草紫",
        "#007AFF" to "亮蓝",
        "#E35D6A" to "珊瑚红",
        "#2E9D73" to "薄荷绿"
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = if (isPositionSliderDragged) Color.Transparent else MaterialTheme.colorScheme.surface,
            tonalElevation = if (isPositionSliderDragged) 0.dp else 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .heightIn(max = 650.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.graphicsLayer {
                        alpha = if (isPositionSliderDragged) 0f else 1f
                    },
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                Text("歌词效果", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "逐字发光需要歌词文件本身带有字级时间轴。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("歌词字体", style = MaterialTheme.typography.titleSmall)
                Text(
                    appearance.customFontDisplayName ?: "默认字体",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onSelectFont) {
                        Text("上传字体")
                    }
                    if (appearance.customFontFileName != null) {
                        TextButton(onClick = onClearFont) {
                            Text("恢复默认")
                        }
                    }
                }
                AppearanceSwitch(
                    label = "从歌曲封面提取强调色",
                    checked = appearance.useCoverAccent,
                    onCheckedChange = {
                        onAppearanceChange(appearance.copy(useCoverAccent = it))
                    }
                )
                AppearanceSwitch(
                    label = "繁体歌词转为简体",
                    checked = appearance.traditionalToSimplified,
                    onCheckedChange = {
                        onAppearanceChange(appearance.copy(traditionalToSimplified = it))
                    }
                )
                Text(
                    "仅影响歌词页显示，不会修改原歌词或导出内容。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(18.dp),
                        shape = CircleShape,
                        color = coverAccentColor?.let {
                            runCatching {
                                Color(android.graphics.Color.parseColor(it))
                            }.getOrNull()
                        } ?: MaterialTheme.colorScheme.surfaceVariant
                    ) {}
                    Text(
                        when {
                            !appearance.useCoverAccent -> "使用下方手动颜色"
                            coverAccentColor != null -> "封面强调色 · $coverAccentColor"
                            else -> "打开歌曲后提取封面颜色"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("高亮颜色", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    highlightColors.forEach { (hex, _) ->
                        Surface(
                            onClick = {
                                onAppearanceChange(
                                    appearance.copy(highlightColor = hex, useCoverAccent = false)
                                )
                            },
                            modifier = Modifier.size(42.dp),
                            shape = CircleShape,
                            color = Color(android.graphics.Color.parseColor(hex)),
                            border = if (!appearance.useCoverAccent && appearance.highlightColor == hex) {
                                BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
                            } else {
                                null
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (!appearance.useCoverAccent && appearance.highlightColor == hex) {
                                    Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                }
                Column(
                    modifier = Modifier.graphicsLayer {
                        alpha = if (isPositionSliderDragged) 0f else 1f
                    },
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                AppearanceSlider(
                    label = "字级发光 · ${appearance.glowRadius.toInt()} px",
                    value = appearance.glowRadius,
                    range = 0f..16f,
                    onValueChange = { onAppearanceChange(appearance.copy(glowRadius = it)) }
                )
                AppearanceSlider(
                    label = "歌词字号 · ${appearance.fontSize.toInt()} px",
                    value = appearance.fontSize,
                    range = 20f..44f,
                    onValueChange = { onAppearanceChange(appearance.copy(fontSize = it)) }
                )
                AppearanceSlider(
                    label = "行间距 · ${appearance.lineSpacing.toInt()} px",
                    value = appearance.lineSpacing,
                    range = 8f..36f,
                    onValueChange = { onAppearanceChange(appearance.copy(lineSpacing = it)) }
                )
                }
                AppearanceSlider(
                    label = "当前播放歌词位置 · ${appearance.currentLinePosition.toInt()}%",
                    value = appearance.currentLinePosition,
                    range = 5f..75f,
                    onValueChange = {
                        onAppearanceChange(appearance.copy(currentLinePosition = it))
                    },
                    interactionSource = positionSliderInteractionSource
                )
                Column(
                    modifier = Modifier.graphicsLayer {
                        alpha = if (isPositionSliderDragged) 0f else 1f
                    },
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                AppearanceSwitch(
                    label = "逐字渐进高亮",
                    checked = appearance.interpolate,
                    onCheckedChange = { onAppearanceChange(appearance.copy(interpolate = it)) }
                )
                AppearanceSwitch(
                    label = "自动滚动",
                    checked = appearance.autoScroll,
                    onCheckedChange = { onAppearanceChange(appearance.copy(autoScroll = it)) }
                )
                AppearanceSwitch(
                    label = "歌词景深模糊",
                    checked = appearance.blur,
                    onCheckedChange = { onAppearanceChange(appearance.copy(blur = it)) }
                )
                AppearanceSwitch(
                    label = "隐藏已播放歌词",
                    checked = appearance.hidePlayedLines,
                    onCheckedChange = { onAppearanceChange(appearance.copy(hidePlayedLines = it)) }
                )
                AppearanceSwitch(
                    label = "减少动态效果",
                    checked = appearance.reducedMotion,
                    onCheckedChange = { onAppearanceChange(appearance.copy(reducedMotion = it)) }
                )
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("完成")
                }
                }
            }
        }
    }
}

@Composable
private fun AppearanceSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            interactionSource = interactionSource
        )
    }
}

@Composable
private fun AppearanceSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private class AmLyricsViewState(
    var songId: String,
    var ttml: String,
    var qqQrc: Boolean,
    var title: String,
    var artist: String,
    var album: String,
    var durationMs: Long,
    var positionMs: Long,
    var showTranslations: Boolean,
    var externalSourceMode: Boolean,
    var appearance: LyricsAppearance,
    var sourceLookupRequest: Int,
    var onSourceLookupSucceeded: (String, String) -> Unit,
    var onSourceLookupFailed: () -> Unit,
    var onTranslationUnavailable: () -> Unit,
    var onLongPress: () -> Unit,
    var onUserScroll: () -> Unit,
    var seekTo: (Long) -> Unit
) {
    var pageReady = false
    var sentTtml: String? = null
    var sentSongId: String? = null
    var sentQqQrc: Boolean? = null
    var sentAppearance: LyricsAppearance? = null
    var sentTranslations: Boolean? = null
    var sentExternalSourceMode: Boolean? = null
    var sentSourceLookupRequest: Int? = null
}

private class AmLyricsJavascriptBridge(
    private val webView: WebView,
    private val state: AmLyricsViewState
) {
    @JavascriptInterface
    fun seekTo(positionMs: Double) {
        if (!positionMs.isFinite()) return
        webView.post {
            val upperBound = state.durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
            state.seekTo(positionMs.toLong().coerceIn(0L, upperBound))
        }
    }

    @JavascriptInterface
    fun sourceLookupSucceeded(source: String, ttml: String) {
        webView.post {
            state.onSourceLookupSucceeded(source.take(80), ttml)
        }
    }

    @JavascriptInterface
    fun sourceLookupFailed() {
        webView.post {
            state.onSourceLookupFailed()
        }
    }

    @JavascriptInterface
    fun translationUnavailable() {
        webView.post {
            state.onTranslationUnavailable()
        }
    }
}

@Composable
private fun AmLyricsWebView(
    modifier: Modifier,
    song: SongHit,
    fetchedLyrics: FetchedLyrics,
    positionMs: Long,
    showTranslations: Boolean,
    externalSourceMode: Boolean,
    appearance: LyricsAppearance,
    traditionalToSimplified: Boolean,
    sourceLookupRequest: Int,
    onSourceLookupSucceeded: (String, String) -> Unit,
    onSourceLookupFailed: () -> Unit,
    onTranslationUnavailable: () -> Unit,
    onUserScroll: () -> Unit,
    onLongPress: () -> Unit,
    onSeekTo: (Long) -> Unit
) {
    val context = LocalContext.current
    val ttml = remember(song.id, fetchedLyrics, traditionalToSimplified) {
        fetchedLyrics.lyrics.toAmLyricsTtml(traditionalToSimplified)
    }
    val state = remember(song.id) {
        AmLyricsViewState(
            songId = song.id,
            ttml = ttml,
            qqQrc = fetchedLyrics.qqQrcDebugText != null,
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationMs = song.durationMs,
            positionMs = positionMs,
            showTranslations = showTranslations,
            externalSourceMode = externalSourceMode,
            appearance = appearance,
            sourceLookupRequest = sourceLookupRequest,
            onSourceLookupSucceeded = onSourceLookupSucceeded,
            onSourceLookupFailed = onSourceLookupFailed,
            onTranslationUnavailable = onTranslationUnavailable,
            onLongPress = onLongPress,
            onUserScroll = onUserScroll,
            seekTo = onSeekTo
        )
    }
    state.songId = song.id
    state.ttml = ttml
    state.qqQrc = fetchedLyrics.qqQrcDebugText != null
    state.title = song.title
    state.artist = song.artist
    state.album = song.album
    state.durationMs = song.durationMs
    state.positionMs = positionMs
    state.showTranslations = showTranslations
    state.externalSourceMode = externalSourceMode
    state.appearance = appearance
    state.sourceLookupRequest = sourceLookupRequest
    state.onSourceLookupSucceeded = onSourceLookupSucceeded
    state.onSourceLookupFailed = onSourceLookupFailed
    state.onTranslationUnavailable = onTranslationUnavailable
    state.onLongPress = onLongPress
    state.onUserScroll = onUserScroll
    state.seekTo = onSeekTo
    val assetLoader = remember(context) {
        val fontDirectory = File(context.filesDir, "lyrics-fonts")
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .addPathHandler(
                "/custom-font/",
                WebViewAssetLoader.PathHandler { path ->
                    if (!path.matches(Regex("lyrics-[a-f0-9-]+\\.(ttf|otf)"))) {
                        null
                    } else {
                        val fontFile = File(fontDirectory, path)
                        if (!fontFile.isFile) {
                            null
                        } else {
                            WebResourceResponse(
                                if (path.endsWith(".otf")) "font/otf" else "font/ttf",
                                null,
                                FileInputStream(fontFile)
                            )
                        }
                    }
                }
            )
            .build()
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            WebView(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                tag = state
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = WebView.OVER_SCROLL_NEVER
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                var touchDownX = 0f
                var touchDownY = 0f
                var scrollReported = false
                val touchSlop = ViewConfiguration.get(viewContext).scaledTouchSlop
                setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            touchDownX = event.x
                            touchDownY = event.y
                            scrollReported = false
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (!scrollReported &&
                                (kotlin.math.abs(event.x - touchDownX) > touchSlop ||
                                    kotlin.math.abs(event.y - touchDownY) > touchSlop)
                            ) {
                                scrollReported = true
                                state.onUserScroll()
                            }
                        }
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> scrollReported = false
                    }
                    false
                }
                setOnLongClickListener {
                    state.onLongPress()
                    true
                }
                addJavascriptInterface(AmLyricsJavascriptBridge(this, state), "AndroidLyrics")
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                        if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                            Log.e("AmLyricsWebView", message.message())
                        }
                        return true
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        assetLoader.shouldInterceptRequest(request.url)?.let { return it }
                        if (isAllowedLyricsSourceUrl(request.url)) return null
                        return WebResourceResponse(
                            "text/plain",
                            "UTF-8",
                            ByteArrayInputStream(ByteArray(0))
                        )
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean = request.url.host != "appassets.androidplatform.net"

                    override fun onPageFinished(view: WebView, url: String) {
                        state.pageReady = true
                        view.applyAmLyricsState(state, forceContent = true)
                        view.evaluateJavascript(
                            "JSON.stringify({setter:typeof window.amLyricsSetContent," +
                                "registered:Boolean(customElements.get('am-lyrics'))})"
                        ) { result ->
                            val initialization = result.orEmpty()
                            if (!initialization.contains("\"function\"") ||
                                !initialization.contains("\"registered\":true")) {
                                Log.e("AmLyricsWebView", "Lyrics renderer failed to initialize")
                            }
                        }
                    }
                }
                val initialAppearance = state.appearance
                val initialColor = android.net.Uri.encode(initialAppearance.highlightColor)
                loadUrl(
                    "https://appassets.androidplatform.net/assets/am-lyrics/index.html" +
                        "?highlightColor=$initialColor&darkTheme=${initialAppearance.darkTheme}"
                )
            }
        },
        update = { webView ->
            webView.tag = state
            if (state.pageReady) webView.applyAmLyricsState(state)
        },
        onRelease = { webView ->
            state.pageReady = false
            webView.removeJavascriptInterface("AndroidLyrics")
            webView.stopLoading()
            webView.destroy()
        }
    )
}

private fun WebView.applyAmLyricsState(
    state: AmLyricsViewState,
    forceContent: Boolean = false
) {
    if (state.sentAppearance != state.appearance) {
        val appearance = state.appearance
        val appearanceJson = JSONObject()
            .put("highlightColor", appearance.highlightColor)
            .put("glowRadius", appearance.glowRadius)
            .put("fontSize", appearance.fontSize)
            .put("lineSpacing", appearance.lineSpacing)
            .put("currentLinePosition", appearance.currentLinePosition)
            .put("autoScroll", appearance.autoScroll)
            .put("interpolate", appearance.interpolate)
            .put("blur", appearance.blur)
            .put("reducedMotion", appearance.reducedMotion)
            .put("hidePlayedLines", appearance.hidePlayedLines)
            .put("darkTheme", appearance.darkTheme)
            .put("customFontFileName", appearance.customFontFileName)
        evaluateJavascript(
            "window.amLyricsSetAppearance(${JSONObject.quote(appearanceJson.toString())})",
            null
        )
        state.sentAppearance = appearance
    }

    if (forceContent ||
        state.sentTtml != state.ttml ||
        state.sentSongId != state.songId ||
        state.sentQqQrc != state.qqQrc
    ) {
        evaluateJavascript(
            "window.amLyricsSetContent(" +
                "${JSONObject.quote(state.ttml)}," +
                "${JSONObject.quote(state.title)}," +
                "${JSONObject.quote(state.artist)}," +
                "${JSONObject.quote(state.album)}," +
                "${state.durationMs}," +
                "${JSONObject.quote(state.appearance.highlightColor)}," +
                "${JSONObject.quote(state.songId)}," +
                "${state.qqQrc})",
            null
        )
        state.sentTtml = state.ttml
        state.sentSongId = state.songId
        state.sentQqQrc = state.qqQrc
    }

    if (state.sentExternalSourceMode != state.externalSourceMode) {
        evaluateJavascript(
            "window.amLyricsSetSourceMode(${state.externalSourceMode},${state.sourceLookupRequest})",
            null
        )
        state.sentExternalSourceMode = state.externalSourceMode
        state.sentSourceLookupRequest = state.sourceLookupRequest
    } else if (state.sentSourceLookupRequest != state.sourceLookupRequest) {
        evaluateJavascript(
            "window.amLyricsSetSourceMode(${state.externalSourceMode},${state.sourceLookupRequest})",
            null
        )
        state.sentSourceLookupRequest = state.sourceLookupRequest
    }

    if (state.sentTranslations != state.showTranslations) {
        evaluateJavascript(
            "window.amLyricsSetTranslations(${state.showTranslations})",
            null
        )
        state.sentTranslations = state.showTranslations
    }
    evaluateJavascript("window.amLyricsSetTime(${state.positionMs})", null)
}

private fun isAllowedLyricsSourceUrl(uri: android.net.Uri): Boolean {
    if (uri.scheme != "https") return false
    return uri.host?.lowercase(Locale.US) in setOf(
        "lrc.red",
        "lyrics-api.binimum.org",
        "lyricsplus.binimum.org",
        "lyricsplus-seven.vercel.app",
        "lyricsplus.prjktla.workers.dev",
        "lyrics-plus-backend.vercel.app",
        "unison.boidu.dev",
        "lrclib.net",
        "fetch-genius.samidy.workers.dev",
        "translate.googleapis.com",
        "amp-api.music.apple.com",
        "itunes.apple.com"
    )
}

private fun com.applemusic.android.lyrics.Lyrics.toAmLyricsTtml(
    traditionalToSimplified: Boolean = false
): String {
    val simplify: (String) -> String = if (traditionalToSimplified) {
        val transliterator =
            android.icu.text.Transliterator.getInstance("Traditional-Simplified")
        transliterator::transliterate
    } else {
        { value -> value }
    }
    val usedKeys = mutableSetOf<String>()
    val lineKeys = lines.mapIndexed { index, line ->
        val originalKey = line.key?.takeIf { it.isNotBlank() && usedKeys.add(it) }
        originalKey ?: "L${index + 1}".also { usedKeys.add(it) }
    }

    return buildString {
        append("""<?xml version="1.0" encoding="UTF-8"?>""")
        append(
            """<tt xmlns="http://www.w3.org/ns/ttml" """ +
                """xmlns:itunes="http://music.apple.com/lyrics"><head><metadata>"""
        )
        val translatedLines = lines.indices.filter { !lines[it].translation.isNullOrBlank() }
        if (translatedLines.isNotEmpty()) {
            append("<translations><translation type=\"subtitle\">")
            translatedLines.forEach { index ->
                append("<text for=\"")
                append(lineKeys[index].xmlEscaped())
                append("\">")
                append(simplify(lines[index].translation.orEmpty()).xmlEscaped())
                append("</text>")
            }
            append("</translation></translations>")
        }
        append("</metadata></head><body><div>")

        lines.forEachIndexed { index, line ->
            val start = line.startMs.coerceAtLeast(0L)
            val end = line.endMs.coerceAtLeast(start)
            append("<p begin=\"")
            append(start.toTtmlTime())
            append("\" end=\"")
            append(end.toTtmlTime())
            append("\" itunes:key=\"")
            append(lineKeys[index].xmlEscaped())
            append('"')

            if (line.isBlank) {
                append("/>")
                return@forEachIndexed
            }

            append('>')
            if (line.words.isEmpty()) {
                append(simplify(line.text).xmlEscaped())
            } else {
                var cursor = 0
                var cursorTimeMs = start
                line.words.forEach { word ->
                    val wordStart = word.charStart.coerceIn(cursor, line.text.length)
                    val wordEnd = word.charEnd.coerceIn(wordStart, line.text.length)
                    if (wordStart > cursor) {
                        appendTtmlSpan(
                            simplify(line.text.substring(cursor, wordStart)),
                            cursorTimeMs,
                            word.startMs
                        )
                    }
                    if (wordEnd > wordStart) {
                        appendTtmlSpan(
                            simplify(line.text.substring(wordStart, wordEnd)),
                            word.startMs,
                            word.endMs
                        )
                    }
                    cursor = maxOf(cursor, wordEnd)
                    cursorTimeMs = word.endMs
                }
                if (cursor < line.text.length) {
                    appendTtmlSpan(
                        simplify(line.text.substring(cursor)),
                        cursorTimeMs,
                        end
                    )
                }
            }
            append("</p>")
        }
        append("</div></body></tt>")
    }
}

private fun StringBuilder.appendTtmlSpan(text: String, startMs: Long, endMs: Long) {
    if (text.isEmpty()) return
    append("<span begin=\"")
    append(startMs.coerceAtLeast(0L).toTtmlTime())
    append("\" end=\"")
    append(endMs.coerceAtLeast(startMs.coerceAtLeast(0L)).toTtmlTime())
    append("\">")
    append(text.xmlEscaped())
    append("</span>")
}

private fun Long.toTtmlTime(): String {
    val value = coerceAtLeast(0L)
    val hours = value / 3_600_000
    val minutes = value / 60_000 % 60
    val seconds = value / 1_000 % 60
    val milliseconds = value % 1_000
    return "%02d:%02d:%02d.%03d".format(Locale.US, hours, minutes, seconds, milliseconds)
}

private fun String.xmlEscaped(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

private fun ensureDarkSurfaceContrast(color: Color, background: Color): Color {
    if (background.luminance() >= 0.4f) return color

    fun contrastRatio(foreground: Color): Float =
        (foreground.luminance() + 0.05f) / (background.luminance() + 0.05f)

    if (contrastRatio(color) >= 4.5f) return color

    var low = 0f
    var high = 1f
    repeat(20) {
        val amount = (low + high) / 2f
        if (contrastRatio(lerp(color, Color.White, amount)) >= 4.5f) {
            high = amount
        } else {
            low = amount
        }
    }
    return lerp(color, Color.White, high)
}

private fun AppleLibrarySync.SongRow.toSongHitOrNull(): SongHit? {
    val songId = appleId.ifBlank { catalogId }.ifBlank { uri.removePrefix("apple:") }
    if (songId.isBlank()) return null
    return SongHit(
        id = songId,
        title = title,
        artist = artist,
        album = album,
        artworkUrl = coverUrl,
        durationMs = durationMs
    )
}

private fun SongHit.favoriteCatalogId(): String {
    val row = MusicRepository.findSong("apple:$id")
    return row?.catalogId?.takeIf(String::isNotBlank)
        ?: id.takeUnless { it.startsWith("i.") }.orEmpty()
}

@Composable
private fun KaraokePlaybackControls(
    song: SongHit,
    fetchedLyrics: FetchedLyrics?,
    accentColor: Color,
    isPlaying: Boolean,
    preparing: Boolean,
    playbackStatus: String,
    positionMs: Long,
    durationMs: Long,
    onLogin: () -> Unit,
    onTogglePlayback: () -> Unit,
    isFavorited: Boolean?,
    favoritePending: Boolean,
    onToggleFavorite: () -> Unit,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onCollapseLyrics: () -> Unit,
    showTranslations: Boolean,
    onToggleTranslations: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenArtist: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onQueueDragOffsetChange: (Float) -> Unit,
    coverQueueDragOffset: Float,
    onSeekStart: (Long) -> Unit,
    onSeekChange: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (positionMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat())
        .coerceIn(0f, 1f)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val frameHitSlop = with(density) { 24.dp.toPx() }
    val frameCornerRadius = with(density) { 28.dp.toPx() }
    val frameStrokeWidth = with(density) { 2.dp.toPx() }
    val cardShape = RoundedCornerShape(28.dp)
    val backgroundColor = MaterialTheme.colorScheme.background
    val controlColor = ensureDarkSurfaceContrast(accentColor, backgroundColor)
    val outlineColor = controlColor.toArgb()
    val containerColor = backgroundColor.copy(alpha = 0.86f)
    Card(
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
            .pointerInput(durationMs, fullscreen) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun seekAt(position: Offset): Long? =
                        cardFrameProgress(
                            position = position,
                            width = size.width.toFloat(),
                            height = size.height.toFloat(),
                            cornerRadius = frameCornerRadius,
                            strokeWidth = frameStrokeWidth,
                            hitSlop = frameHitSlop
                        )?.let { (it * durationMs).toLong().coerceIn(0L, durationMs) }

                    val initialSeekPosition = if (durationMs > 0L) seekAt(down.position) else null
                    val seekGesture = initialSeekPosition != null
                    if (initialSeekPosition != null) onSeekStart(initialSeekPosition)
                    var totalDrag = Offset.Zero
                    var previousPosition = down.position
                    var handledSwipe = false
                    var pointerPressed = true
                    while (pointerPressed) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.first()
                        if (seekGesture) {
                            seekAt(pointer.position)?.let(onSeekChange)
                        } else {
                            val movement = pointer.position - previousPosition
                            totalDrag += movement
                            previousPosition = pointer.position
                            val swipeThreshold = 72.dp.toPx()
                            val horizontalSwipe = kotlin.math.abs(totalDrag.x) >= swipeThreshold &&
                                kotlin.math.abs(totalDrag.x) > kotlin.math.abs(totalDrag.y)
                            val verticalSwipe = kotlin.math.abs(totalDrag.y) >= swipeThreshold &&
                                kotlin.math.abs(totalDrag.y) > kotlin.math.abs(totalDrag.x)
                            if (horizontalSwipe && !handledSwipe) {
                                handledSwipe = true
                                if (totalDrag.x > 0f) onNext()
                                else onPrevious()
                            } else if (verticalSwipe && !handledSwipe) {
                                handledSwipe = true
                                if (!fullscreen && totalDrag.y < 0f) {
                                    onCollapseLyrics()
                                } else if (fullscreen && totalDrag.y > 0f) {
                                    onToggleFullscreen()
                                }
                            }
                            if (handledSwipe) pointer.consume()
                        }
                        if (seekGesture) pointer.consume()
                        pointerPressed = pointer.pressed
                    }
                    if (seekGesture) onSeekFinished()
                }
            }
            .drawWithContent {
                drawContent()
                val lineWidth = 2.dp.toPx()
                val inset = lineWidth / 2
                val path = android.graphics.Path().apply {
                    addRoundRect(
                        android.graphics.RectF(
                            inset,
                            inset,
                            size.width - inset,
                            size.height - inset
                        ),
                        28.dp.toPx(),
                        28.dp.toPx(),
                        android.graphics.Path.Direction.CW
                    )
                }
                val measure = android.graphics.PathMeasure(path, false)
                val length = measure.length
                if (length > 0f) {
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = lineWidth
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        color = controlColor.copy(alpha = 0.32f).toArgb()
                    }
                    drawIntoCanvas { it.nativeCanvas.drawPath(path, paint) }
                    if (progress > 0f) {
                        val progressPath = android.graphics.Path()
                        measure.getSegment(0f, length * progress, progressPath, true)
                        paint.color = outlineColor
                        drawIntoCanvas { it.nativeCanvas.drawPath(progressPath, paint) }
                    }
                }
            },
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(
                start = 14.dp,
                top = 12.dp,
                end = 14.dp,
                bottom = 8.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .graphicsLayer { translationY = coverQueueDragOffset }
                        .clickable(onClick = onTogglePlayback)
                        .pointerInput(onOpenQueue) {
                            var downwardDrag = 0f
                            detectDragGesturesAfterLongPress(
                                onDragEnd = {
                                    if (downwardDrag >= 48.dp.toPx()) onOpenQueue()
                                    downwardDrag = 0f
                                    onQueueDragOffsetChange(0f)
                                },
                                onDragCancel = {
                                    downwardDrag = 0f
                                    onQueueDragOffsetChange(0f)
                                },
                                onDragStart = {
                                    downwardDrag = 0f
                                    onQueueDragOffsetChange(0f)
                                },
                                onDrag = { change, dragAmount ->
                                    downwardDrag = (downwardDrag + dragAmount.y).coerceAtLeast(0f)
                                    onQueueDragOffsetChange(downwardDrag.coerceAtMost(96.dp.toPx()))
                                    change.consume()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (song.artworkUrl.isNotBlank()) {
                        AsyncImage(
                            model = song.artworkUrl,
                            contentDescription = "点击${if (isPlaying) "暂停" else "继续"}播放",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = "点击${if (isPlaying) "暂停" else "继续"}播放",
                            tint = controlColor,
                            modifier = Modifier.fillMaxSize().background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(19.dp)
                            ).padding(14.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            song.title,
                            modifier = Modifier.weight(1f).clickable(onClick = onOpenAlbum),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        song.artist,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(onClick = onOpenArtist)
                    )
                    if (fetchedLyrics != null) {
                        val wordTimed = fetchedLyrics.lyrics.wordTimedLines()
                        Text(
                            when {
                                wordTimed > 0 -> "逐字歌词 · $wordTimed 行带字级时间轴"
                                fetchedLyrics.lyrics.synced -> "同步歌词 · ${fetchedLyrics.lyrics.lines.size} 行"
                                else -> "纯文本歌词 · ${fetchedLyrics.lyrics.lines.size} 行"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = controlColor,
                            modifier = Modifier.padding(top = 3.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = onToggleFavorite,
                    enabled = !favoritePending,
                    modifier = Modifier.size(48.dp)
                ) {
                    AnimatedContent(
                        targetState = isFavorited == true,
                        transitionSpec = {
                            (scaleIn(initialScale = 0.65f) + fadeIn()) togetherWith
                                (scaleOut(targetScale = 0.65f) + fadeOut())
                        },
                        label = "favoriteStar"
                    ) { favorited ->
                        ArtistFavoriteStar(
                            filled = favorited,
                            color = if (favorited) {
                                controlColor
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(30.dp).semantics {
                                contentDescription = when (isFavorited) {
                                    true -> "已收藏，点击取消收藏"
                                    false -> "未收藏，点击收藏"
                                    null -> "收藏状态尚未确认，点击重试查询"
                                }
                            }
                        )
                    }
                }
                if (fetchedLyrics?.lyrics?.lines?.any { !it.translation.isNullOrBlank() } == true) {
                    IconButton(
                        onClick = onToggleTranslations,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Filled.Translate,
                            contentDescription = if (showTranslations) "隐藏歌词翻译" else "显示歌词翻译",
                            tint = controlColor,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
            }
            if (playbackStatus.contains("session has ended", ignoreCase = true)) {
                TextButton(
                    onClick = onLogin,
                    modifier = Modifier.align(Alignment.End),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = controlColor
                    )
                ) {
                    Text("重新登录 Apple Music")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    when {
                        preparing -> playbackStatus.ifBlank { "正在准备播放…" }
                        playbackStatus.startsWith("无法播放") -> playbackStatus
                        isPlaying -> "正在播放 · 卡拉 OK"
                        else -> "已暂停"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        playbackStatus.startsWith("无法播放") -> MaterialTheme.colorScheme.error
                        else -> controlColor
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${formatPlaybackTime(positionMs)} / ${formatPlaybackTime(durationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = controlColor
                )
            }
        }
    }
}

private fun cardFrameProgress(
    position: Offset,
    width: Float,
    height: Float,
    cornerRadius: Float,
    strokeWidth: Float,
    hitSlop: Float
): Float? {
    val inset = strokeWidth / 2f
    val path = android.graphics.Path().apply {
        addRoundRect(
            android.graphics.RectF(inset, inset, width - inset, height - inset),
            cornerRadius,
            cornerRadius,
            android.graphics.Path.Direction.CW
        )
    }
    val measure = android.graphics.PathMeasure(path, false)
    val length = measure.length
    if (length <= 0f) return null

    val point = FloatArray(2)
    val tangent = FloatArray(2)
    val samples = 240
    var nearestDistanceSquared = Float.MAX_VALUE
    var nearestDistance = 0f
    for (sample in 0..samples) {
        val distance = length * sample / samples
        if (!measure.getPosTan(distance, point, tangent)) continue
        val dx = point[0] - position.x
        val dy = point[1] - position.y
        val distanceSquared = dx * dx + dy * dy
        if (distanceSquared < nearestDistanceSquared) {
            nearestDistanceSquared = distanceSquared
            nearestDistance = distance
        }
    }
    if (nearestDistanceSquared > hitSlop * hitSlop) return null
    return (nearestDistance / length).coerceIn(0f, 1f)
}

@Composable
private fun EmptyLyrics(
    title: String,
    detail: String,
    action: String?,
    onAction: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            if (action != null) {
                Button(onClick = onAction, modifier = Modifier.padding(top = 16.dp)) {
                    Text(action)
                }
            }
        }
    }
}

@Composable
private fun SettingsPage(
    signedIn: Boolean,
    profileAvatarUri: Uri?,
    cacheCount: Int,
    cacheBytes: Long,
    statusBarLyricsEnabled: Boolean,
    lyricsFullscreenEnabled: Boolean,
    themeMode: AppThemeMode,
    useDynamicColors: Boolean,
    manualThemeSeedHex: String,
    modifier: Modifier = Modifier,
    onLogin: () -> Unit,
    onSelectProfileAvatar: () -> Unit,
    onClearProfileAvatar: () -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onDynamicColorsChange: (Boolean) -> Unit,
    onManualThemeSeedChange: (String) -> Unit,
    onStatusBarLyricsEnabledChange: (Boolean) -> Unit,
    onLyricsFullscreenEnabledChange: (Boolean) -> Unit,
    onOpenCache: () -> Unit
) {
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val manualThemeActive = !useDynamicColors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val systemDark = isSystemInDarkTheme()
    val fallbackMonetColor = MaterialTheme.colorScheme.primary
    val monetColor = remember(
        context,
        themeMode,
        systemDark,
        supportsDynamicColor,
        fallbackMonetColor
    ) {
        if (!supportsDynamicColor) {
            fallbackMonetColor
        } else {
            val darkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            if (darkTheme) {
                dynamicDarkColorScheme(context).primary
            } else {
                dynamicLightColorScheme(context).primary
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "设置",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    if (profileAvatarUri != null) {
                        AsyncImage(
                            model = profileAvatarUri,
                            contentDescription = "个人头像",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 14.dp)
                ) {
                    Text("个人头像", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (profileAvatarUri == null) "上传一张图片作为应用头像"
                        else "点击更换本地头像图片",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = onSelectProfileAvatar) {
                        Text(if (profileAvatarUri == null) "上传" else "更换")
                    }
                    if (profileAvatarUri != null) {
                        TextButton(onClick = onClearProfileAvatar) {
                            Text("移除")
                        }
                    }
                }
            }
        }
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("应用主题色与深色模式", fontWeight = FontWeight.SemiBold)
                Text(
                    if (supportsDynamicColor) {
                        "选择外观与主题色；莫奈使用设备壁纸的主色调"
                    } else {
                        "选择跟随系统、浅色或深色外观；莫奈动态取色需要 Android 12 及以上"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }
                Text(
                    when {
                        useDynamicColors && supportsDynamicColor ->
                            "${themeMode.label} · 莫奈动态色"
                        else -> "${themeMode.label} · 手动主题色"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { onDynamicColorsChange(true) },
                            enabled = supportsDynamicColor,
                            modifier = Modifier.size(42.dp),
                            shape = CircleShape,
                            color = monetColor,
                            border = if (useDynamicColors && supportsDynamicColor) {
                                BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
                            } else {
                                null
                            }
                        ) {
                            if (useDynamicColors && supportsDynamicColor) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            "莫奈",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    themeColorPresets.forEach { preset ->
                        val color = Color(android.graphics.Color.parseColor(preset.hex))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                onClick = {
                                    if (useDynamicColors) onDynamicColorsChange(false)
                                    onManualThemeSeedChange(preset.hex)
                                },
                                modifier = Modifier.size(42.dp),
                                shape = CircleShape,
                                color = color,
                                border = if (
                                    manualThemeActive && manualThemeSeedHex.equals(
                                        preset.hex,
                                        ignoreCase = true
                                    )
                                ) {
                                    BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
                                } else {
                                    null
                                }
                            ) {
                                if (
                                    manualThemeActive && manualThemeSeedHex.equals(
                                        preset.hex,
                                        ignoreCase = true
                                    )
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(
                                preset.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("状态栏歌词", fontWeight = FontWeight.SemiBold)
                        Text(
                            "在系统状态栏实时显示当前播放歌词",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    Switch(
                        checked = statusBarLyricsEnabled,
                        onCheckedChange = onStatusBarLyricsEnabledChange
                    )
                }
            }
        }
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("歌词页全屏", fontWeight = FontWeight.SemiBold)
                        Text(
                            "隐藏状态栏并让歌词内容延伸到屏幕顶部；关闭后为摄像头区域留出空间",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    Switch(
                        checked = lyricsFullscreenEnabled,
                        onCheckedChange = onLyricsFullscreenEnabledChange
                    )
                }
            }
        }
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onLogin),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        if (signedIn) Icons.Filled.CheckCircle else Icons.Filled.Person,
                        contentDescription = null,
                        tint = if (signedIn) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp)
                ) {
                    Text("Apple Music", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (signedIn) "已连接，点按管理账号" else "登录以获取完整歌词",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Text(
                    if (signedIn) "管理" else "登录",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenCache),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        Icons.Filled.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp)
                ) {
                    Text("播放缓存", fontWeight = FontWeight.SemiBold)
                    Text(
                        "$cacheCount 首 · ${formatCacheSize(cacheBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Text("管理", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun PlaybackCacheLimitCard(
    cacheBytes: Long,
    limitBytes: Long,
    onLimitChange: (Long) -> Unit
) {
    val minGigabytes = 0.1f
    val maxGigabytes = 20f
    var sliderValue by remember(limitBytes) {
        mutableFloatStateOf(limitBytes.toFloat() / BYTES_PER_GIGABYTE)
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "歌曲缓存上限",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${formatGigabytes(sliderValue)} GB",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Text(
                "已用 ${formatCacheSize(cacheBytes)} · 超限时清理最旧的未固定歌曲缓存",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    val selectedGigabytes = (sliderValue * 10).roundToInt() / 10f
                    sliderValue = selectedGigabytes
                    onLimitChange((selectedGigabytes * BYTES_PER_GIGABYTE).toLong())
                },
                modifier = Modifier.height(34.dp),
                valueRange = minGigabytes..maxGigabytes,
                steps = 198
            )
        }
    }
}

private const val BYTES_PER_GIGABYTE = 1024f * 1024f * 1024f
private const val MAX_CUSTOM_FONT_BYTES = 20L * 1024L * 1024L

private fun formatGigabytes(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(Locale.US, value)

@Composable
private fun CollapsedLyricsArtwork(
    song: SongHit,
    positionMs: Long,
    durationMs: Long,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onDrag: (Offset) -> Unit,
    onDragFinished: () -> Unit,
    onExpand: () -> Unit,
) {
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragFinished by rememberUpdatedState(onDragFinished)
    val currentOnExpand by rememberUpdatedState(onExpand)
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    Box(
        modifier = modifier
            .size(64.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { currentOnExpand() })
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragEnd = { currentOnDragFinished() },
                    onDragCancel = { currentOnDragFinished() },
                    onDrag = { change, dragAmount ->
                        currentOnDrag(dragAmount)
                        change.consume()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 2.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
            )
            if (progress > 0f) {
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                )
            }
        }
        if (song.artworkUrl.isNotBlank()) {
            AsyncImage(
                model = song.artworkUrl,
                contentDescription = "${song.title}，点击展开歌词",
                modifier = Modifier.size(58.dp).clip(CircleShape)
            )
        } else {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = "${song.title}，点击展开歌词",
                tint = accentColor,
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(14.dp)
            )
        }
    }
}

@Composable
private fun MiniPlayerCapsule(
    song: SongHit,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onOpen: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenArtist: () -> Unit,
    onTogglePlayback: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    surfaceColor: Color? = null,
    contentColor: Color? = null,
    secondaryTextColor: Color? = null,
    accentColorOverride: Color? = null
) {
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val playerAccentColor by animateColorAsState(
        targetValue = accentColorOverride ?: MaterialTheme.colorScheme.primary,
        animationSpec = tween(450),
        label = "miniPlayerArtworkAccent"
    )
    val playerContentColor = contentColor ?: MaterialTheme.colorScheme.onSurface
    val playerSecondaryColor = secondaryTextColor ?: MaterialTheme.colorScheme.onSurfaceVariant
    val targetSurfaceColor = surfaceColor ?: MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    val playerSurfaceColor by animateColorAsState(
        targetValue = targetSurfaceColor,
        animationSpec = tween(450),
        label = "miniPlayerSurfaceColor"
    )
    val playerSurfaceVariant = surfaceColor ?: MaterialTheme.colorScheme.surfaceVariant
    val trackColor = playerContentColor.copy(alpha = 0.18f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp)
            .drawWithCache {
                val strokeWidth = 2.dp.toPx()
                val inset = strokeWidth / 2f
                val cornerRadius = 32.dp.toPx()
                val path = android.graphics.Path().apply {
                    addRoundRect(
                        android.graphics.RectF(
                            inset,
                            inset,
                            size.width - inset,
                            size.height - inset
                        ),
                        cornerRadius,
                        cornerRadius,
                        android.graphics.Path.Direction.CW
                    )
                }
                val pathMeasure = android.graphics.PathMeasure(path, false)
                val pathLength = pathMeasure.length
                val progressPath = android.graphics.Path()
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    style = android.graphics.Paint.Style.STROKE
                    this.strokeWidth = strokeWidth
                    strokeCap = android.graphics.Paint.Cap.ROUND
                }
                onDrawWithContent {
                    drawContent()
                    if (pathLength > 0f) {
                        paint.color = trackColor.toArgb()
                        drawIntoCanvas { it.nativeCanvas.drawPath(path, paint) }
                        if (progress > 0f) {
                            progressPath.reset()
                            pathMeasure.getSegment(
                                0f,
                                pathLength * progress,
                                progressPath,
                                true
                            )
                            paint.color = playerAccentColor.toArgb()
                            drawIntoCanvas {
                                it.nativeCanvas.drawPath(progressPath, paint)
                            }
                        }
                    }
                }
            }
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(32.dp),
        color = playerSurfaceColor,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(
                start = if (compact) 6.dp else 10.dp,
                top = if (compact) 5.dp else 8.dp,
                end = if (compact) 5.dp else 8.dp,
                bottom = if (compact) 5.dp else 8.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(if (compact) 36.dp else 48.dp)
                        .clip(CircleShape)
                        .background(playerSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (song.artworkUrl.isNotBlank()) {
                        AsyncImage(
                            model = song.artworkUrl,
                            contentDescription = "${song.title} 专辑封面，向右滑动切换下一首",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .pointerInput(onNext) {
                                    var horizontalDrag = 0f
                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { change, dragAmount ->
                                            horizontalDrag += dragAmount
                                            change.consume()
                                        },
                                        onDragEnd = {
                                            if (horizontalDrag >= 48.dp.toPx()) onNext()
                                            else if (horizontalDrag <= -48.dp.toPx()) onPrevious()
                                            horizontalDrag = 0f
                                        },
                                        onDragCancel = { horizontalDrag = 0f }
                                    )
                                }
                        )
                    } else {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = playerSecondaryColor
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 4.dp)
                ) {
                    Text(
                        song.title,
                        color = playerContentColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(onClick = onOpenAlbum)
                    )
                    Text(
                        song.artist,
                        color = playerSecondaryColor,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(onClick = onOpenArtist)
                    )
                }
                IconButton(
                    onClick = onTogglePlayback,
                    modifier = Modifier.size(if (compact) 34.dp else 42.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "暂停播放" else "继续播放",
                        tint = playerContentColor,
                        modifier = Modifier.size(if (compact) 21.dp else 27.dp)
                    )
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(if (compact) 34.dp else 42.dp)
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "下一首",
                        tint = playerContentColor,
                        modifier = Modifier.size(if (compact) 20.dp else 25.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsingMiniPlayer(
    song: SongHit?,
    visible: Boolean,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    progress: State<Float>,
    expandedWidth: Dp,
    artworkAccent: Color?,
    onOpen: (SongHit) -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenArtist: () -> Unit,
    onTogglePlayback: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    AnimatedVisibility(
        visible = visible && song != null,
        enter = fadeIn(tween(220)) +
            slideInVertically(
                animationSpec = spring(dampingRatio = 0.84f, stiffness = 360f)
            ) { it / 2 },
        exit = fadeOut(tween(180)) +
            slideOutVertically(
                animationSpec = spring(dampingRatio = 0.88f, stiffness = 420f)
            ) { it },
        modifier = modifier
            .graphicsLayer {
                translationY = with(density) {
                    -56.dp.toPx() * (
                        1f - ((progress.value - 0.42f) / 0.58f).coerceIn(0f, 1f)
                    )
                }
            }
            .padding(
                bottom = WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding() + 12.dp
            )
    ) {
        song?.let { currentSong ->
            val collapseProgress = progress.value.coerceIn(0f, 1f)
            val capsuleWidthProgress = (collapseProgress / 0.42f).coerceIn(0f, 1f)
            val songAccent = artworkAccent ?: MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier.fillMaxWidth().height(62.dp),
                contentAlignment = Alignment.Center
            ) {
                MiniPlayerCapsule(
                    song = currentSong,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onOpen = { onOpen(currentSong) },
                    onOpenAlbum = onOpenAlbum,
                    onOpenArtist = onOpenArtist,
                    onTogglePlayback = onTogglePlayback,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    modifier = Modifier.width(
                        lerp(expandedWidth, 250.dp, capsuleWidthProgress)
                    ),
                    compact = true,
                    surfaceColor = lerp(Color(0xFF252527), songAccent, 0.28f)
                        .copy(alpha = 0.94f),
                    contentColor = Color.White,
                    secondaryTextColor = Color.White.copy(alpha = 0.76f),
                    accentColorOverride = songAccent
                )
            }
        }
    }
}

@Composable
private fun AnimatedFloatingNavigationBar(
    selectedPage: ToolPage,
    onSelect: (ToolPage) -> Unit,
    collapseProgress: State<Float>,
    darkTheme: Boolean,
    expandedWidth: Dp,
    collectionTheme: Boolean,
    modifier: Modifier = Modifier
) {
    FloatingNavigationBar(
        selectedPage = selectedPage,
        onSelect = onSelect,
        collapseProgress = collapseProgress.value,
        darkTheme = darkTheme,
        expandedWidth = expandedWidth,
        collectionTheme = collectionTheme,
        modifier = modifier
    )
}

@Composable
private fun FloatingNavigationBar(
    selectedPage: ToolPage,
    onSelect: (ToolPage) -> Unit,
    collapseProgress: Float,
    darkTheme: Boolean,
    expandedWidth: Dp,
    collectionTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val darkSurface = collectionTheme || (darkTheme && selectedPage != ToolPage.Settings)
    val surfaceColor = if (collectionTheme) Color(0xD91C1C1E)
    else if (darkSurface) Color(0xED2C2C2E)
    else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    val activePage = if (
        selectedPage == ToolPage.Settings ||
            selectedPage == ToolPage.Collection ||
            selectedPage == ToolPage.Artist ||
            selectedPage == ToolPage.ArtistTopSongs
    ) ToolPage.Home else selectedPage
    val progress = collapseProgress.coerceIn(0f, 1f)
    val searchSize = lerp(54.dp, 50.dp, progress)
    val spacing = lerp(8.dp, 6.dp, progress)
    val dockWidth = lerp(expandedWidth, 362.dp, progress)
    val navWidth = dockWidth - searchSize - spacing
    val navHeight = lerp(54.dp, 50.dp, progress)
    val navFade = (1f - progress / 0.78f).coerceIn(0f, 1f)
    val collapsedHomeFade = ((progress - 0.62f) / 0.38f).coerceIn(0f, 1f)

    Row(
        modifier = modifier.width(dockWidth).height(62.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(searchSize).clickable { onSelect(ToolPage.Search) },
            shape = CircleShape,
            color = if (selectedPage == ToolPage.Search) {
                when {
                    collectionTheme -> Color.White.copy(alpha = 0.2f)
                    darkSurface -> Color(0xFF48484A)
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
            } else surfaceColor,
            tonalElevation = 2.dp,
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "搜索",
                    tint = if (darkSurface) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Box(modifier = Modifier.width(navWidth).height(62.dp)) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(lerp(290.dp, 50.dp, progress))
                    .height(navHeight),
                shape = RoundedCornerShape(lerp(27.dp, 25.dp, progress)),
                color = surfaceColor,
                tonalElevation = 2.dp,
                shadowElevation = 6.dp
            ) {
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .graphicsLayer {
                                alpha = navFade
                                translationX = 54.dp.toPx() * progress
                            },
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MusicNavigationItem(
                            label = "主页",
                            icon = Icons.Filled.Home,
                            selected = activePage == ToolPage.Home,
                            darkSurface = darkSurface,
                            collectionTheme = collectionTheme,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect(ToolPage.Home) }
                        )
                        MusicNavigationItem(
                            label = "新发现",
                            icon = Icons.Filled.Explore,
                            selected = activePage == ToolPage.Discovery,
                            darkSurface = darkSurface,
                            collectionTheme = collectionTheme,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect(ToolPage.Discovery) }
                        )
                        MusicNavigationItem(
                            label = "电台",
                            icon = Icons.Filled.GraphicEq,
                            selected = activePage == ToolPage.Radio,
                            darkSurface = darkSurface,
                            collectionTheme = collectionTheme,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect(ToolPage.Radio) }
                        )
                        MusicNavigationItem(
                            label = "资料库",
                            icon = Icons.Filled.Storage,
                            selected = activePage == ToolPage.Library,
                            darkSurface = darkSurface,
                            collectionTheme = collectionTheme,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect(ToolPage.Library) }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(50.dp)
                            .graphicsLayer { alpha = collapsedHomeFade }
                            .clickable { onSelect(ToolPage.Home) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = "主页",
                            tint = if (darkSurface) Color.White
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicNavigationItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    darkSurface: Boolean,
    collectionTheme: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        targetValue = when {
            darkSurface && selected -> Color.White
            darkSurface -> Color(0xFFB0B0B5)
            selected -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "navigationItemColor"
    )
    Box(
        modifier = Modifier
            .then(modifier)
            .height(54.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (selected) {
                    if (collectionTheme) Color.White.copy(alpha = 0.2f)
                    else if (darkSurface) Color(0xFF48484A)
                    else MaterialTheme.colorScheme.secondaryContainer
                } else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                label,
                color = contentColor,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun CacheManagerDialog(
    items: List<ManagedCacheItem>,
    playbackCacheCount: Int,
    playbackCacheBytes: Long,
    playbackCacheLimitBytes: Long,
    currentSongId: String?,
    pendingDelete: AppleCache.CacheItem?,
    onDismiss: () -> Unit,
    onRequestDelete: (AppleCache.CacheItem) -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: (AppleCache.CacheItem) -> Unit,
    onClearAll: () -> Unit,
    onTogglePinned: (AppleCache.CacheItem) -> Unit,
    onLimitChange: (Long) -> Unit,
    onDeleteLyrics: (String) -> Unit
) {
    var confirmClearAll by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(220)) +
                scaleIn(initialScale = 0.9f, animationSpec = tween(220)),
            exit = fadeOut(tween(150)) +
                scaleOut(targetScale = 0.96f, animationSpec = tween(150))
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(Modifier.padding(20.dp)) {
                if (pendingDelete != null) {
                    Text(
                        "删除这首歌的缓存？",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    val song = MusicRepository.findSong(pendingDelete.songId)
                    Text(
                        pendingDelete.title.ifBlank { song?.title ?: pendingDelete.songId },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        "下次播放时需要重新下载。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onCancelDelete) { Text("取消") }
                        TextButton(onClick = { onConfirmDelete(pendingDelete) }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else if (confirmClearAll) {
                    Text(
                        "清除所有缓存？",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "歌曲播放缓存和所有歌词来源的缓存都会删除。之后播放或打开歌曲时需要重新下载或获取。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { confirmClearAll = false }) { Text("取消") }
                        TextButton(
                            onClick = {
                                confirmClearAll = false
                                onClearAll()
                            }
                        ) {
                            Text("全部清除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    Text(
                        "播放缓存",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$playbackCacheCount 首歌曲 · ${items.count { it.lyrics != null }} 首有歌词缓存 · ${formatCacheSize(playbackCacheBytes)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                    PlaybackCacheLimitCard(
                        cacheBytes = playbackCacheBytes,
                        limitBytes = playbackCacheLimitBytes,
                        onLimitChange = onLimitChange
                    )
                    Spacer(Modifier.height(14.dp))
                    if (items.isEmpty()) {
                        Text(
                            "还没有歌曲或歌词缓存。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items, key = { it.songId }) { managed ->
                                val item = managed.playback
                                val song = MusicRepository.findSong(managed.songId)
                                val isCurrentSong = managed.songId == currentSongId
                                val title = item?.title?.takeIf { it.isNotBlank() }
                                    ?: song?.title
                                    ?: managed.songId
                                val artist = item?.artist?.takeIf { it.isNotBlank() }
                                    ?: song?.artist.orEmpty()
                                val lyricsSources = managed.lyrics?.sources.orEmpty()
                                    .sorted()
                                    .joinToString(" / ") {
                                        if (it == "qq") "QQ" else "Apple Music"
                                    }
                                ElevatedCard {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                title,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (artist.isNotBlank()) {
                                                Text(
                                                    artist,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                buildString {
                                                    if (item != null) {
                                                        append("歌曲 · ${formatCacheSize(item.size)}")
                                                        when {
                                                            item.downloading -> append(" · 下载中")
                                                            isCurrentSong -> append(" · 当前歌曲")
                                                            item.pinned -> append(" · 已固定")
                                                        }
                                                    } else {
                                                        append("无歌曲缓存")
                                                    }
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                if (lyricsSources.isBlank()) "无歌词缓存"
                                                else "歌词 · $lyricsSources",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (lyricsSources.isBlank()) {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                } else {
                                                    MaterialTheme.colorScheme.primary
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (item != null) {
                                            IconButton(onClick = { onTogglePinned(item) }) {
                                                Icon(
                                                    Icons.Filled.PushPin,
                                                    contentDescription = if (item.pinned) {
                                                        "取消固定缓存"
                                                    } else {
                                                        "固定缓存"
                                                    },
                                                    tint = if (item.pinned) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        }
                                        if (managed.lyrics != null) {
                                            IconButton(onClick = {
                                                onDeleteLyrics(managed.songId)
                                            }) {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription = "删除歌词缓存",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                        if (item != null) {
                                            IconButton(
                                                onClick = { onRequestDelete(item) },
                                                enabled = !item.downloading && !isCurrentSong
                                            ) {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription = "删除歌曲缓存",
                                                    tint = if (item.downloading || isCurrentSong) {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    } else {
                                                        MaterialTheme.colorScheme.error
                                                    }
                                                )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { confirmClearAll = true },
                            enabled = items.isNotEmpty()
                        ) {
                            Text(
                                "一键清除所有缓存",
                                color = if (items.isNotEmpty()) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        TextButton(onClick = onDismiss) {
                            Text("完成")
                        }
                    }
                }
            }
              }
            }
        }
    }
}

private fun formatCacheSize(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}

private fun formatPlaybackTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val seconds = ms / 1000L
    return String.format(Locale.US, "%d:%02d", seconds / 60L, seconds % 60L)
}

private fun ExportFormat.render(fetched: FetchedLyrics?): String {
    if (this == ExportFormat.QqQrc) return fetched?.qqQrcDebugText.orEmpty()
    val lyrics = fetched?.lyrics ?: return ""
    return when (this) {
        ExportFormat.Text -> lyrics.lines.renderWithTranslations()
        ExportFormat.Lrc -> lyrics.lines.joinToString("\n") { line ->
            val timestamp = "[${line.startMs.toLrcTimestamp()}]"
            line.translation
                ?.takeIf { it.isNotBlank() }
                ?.let { "$timestamp${line.text}\n$timestamp$it" }
                ?: "$timestamp${line.text}"
        }
        ExportFormat.Ttml -> fetched.ttml ?: lyrics.lines.joinToString("\n") { it.text }
        ExportFormat.QqQrc -> error("Handled before rendering lyrics")
    }
}

private fun List<com.applemusic.android.lyrics.LyricLine>.renderWithTranslations(): String =
    joinToString("\n") { line ->
        line.translation
            ?.takeIf { it.isNotBlank() }
            ?.let { "${line.text}\n$it" }
            ?: line.text
    }

private fun Long.toLrcTimestamp(): String {
    val centiseconds = (this.coerceAtLeast(0L) / 10L) % 100L
    val totalSeconds = this.coerceAtLeast(0L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centiseconds)
}
