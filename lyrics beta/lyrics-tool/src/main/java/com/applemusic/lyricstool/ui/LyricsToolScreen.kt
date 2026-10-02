package com.applemusic.lyricstool.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import android.view.MotionEvent
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
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.CancellationException
import com.applemusic.lyricstool.data.AppleLyricsClient
import com.applemusic.lyricstool.data.FetchedLyrics
import com.applemusic.lyricstool.data.LyricsCache
import com.applemusic.lyricstool.data.QQMusicLyricsClient
import com.applemusic.lyricstool.data.SongHit
import org.json.JSONObject
import com.applemusic.android.apple.AppleLibrarySync
import com.applemusic.android.apple.AppleCache
import com.applemusic.android.apple.MusicRepository
import com.applemusic.android.player.MusicService
import com.applemusic.android.player.PlaybackEngine
import com.applemusic.android.player.LiveLyricsNotification
import com.applemusic.android.lyrics.LyricsParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import java.util.Locale
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
    Search,
    Settings
}

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
        var lyricsNotice by remember { mutableStateOf<String?>(null) }
        var query by remember { mutableStateOf("") }
        var songs by remember { mutableStateOf<List<SongHit>>(emptyList()) }
        var lyricSearchUsed by remember { mutableStateOf(false) }
        var selectedSong by remember { mutableStateOf<SongHit?>(null) }
        var lyricsCollapsed by remember { mutableStateOf(false) }
        var lyricsInteractionVersion by remember { mutableLongStateOf(0L) }
        var favoritedSongIds by remember { mutableStateOf<Set<String>?>(null) }
        var favoriteStatusLoading by remember { mutableStateOf(false) }
        var favoritePendingSongId by remember { mutableStateOf<String?>(null) }
        var selectedPage by remember { mutableStateOf(ToolPage.Search) }
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
        DisposableEffect(view, immersiveLyrics) {
            val window = (context as? Activity)?.window
            val controller = window?.let { WindowInsetsControllerCompat(it, view) }
            val wasFullscreen = window?.let {
                it.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN != 0
            } ?: false
            if (immersiveLyrics) {
                window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                controller?.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller?.hide(WindowInsetsCompat.Type.statusBars())
            } else {
                window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                controller?.show(WindowInsetsCompat.Type.statusBars())
            }
            onDispose {
                if (wasFullscreen) {
                    window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                } else {
                    window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                }
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
        val refreshFavoriteStatus: () -> Unit = {
            if (!favoriteStatusLoading) {
                scope.launch {
                    favoriteStatusLoading = true
                    try {
                        favoritedSongIds = withContext(Dispatchers.IO) {
                            MusicRepository.api.getFavoriteCatalogSongIds()
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        favoritedSongIds = null
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
            favoritedSongIds = null
            if (signedIn && selectedSong != null) refreshFavoriteStatus()
        }
        val toggleSongFavorite: (SongHit) -> Unit = { song ->
            val catalogId = song.favoriteCatalogId()
            if (catalogId.isBlank()) {
                scope.launch { snackbar.showSnackbar("无法确定这首歌的 Apple Music 曲库 ID") }
            } else if (favoritePendingSongId == null) {
                val knownFavoriteIds = favoritedSongIds
                if (knownFavoriteIds == null) {
                    refreshFavoriteStatus()
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
            } catch (error: Exception) {
                snackbar.showSnackbar(error.message ?: "连接 Apple Music 失败")
            } finally {
                preparing = false
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

        BackHandler(enabled = selectedSong != null || selectedPage != ToolPage.Search) {
            if (fullscreenLyrics) {
                fullscreenLyrics = false
            } else if (lyricsCollapsed) {
                lyricsCollapsed = false
            } else if (selectedPage != ToolPage.Search) {
                selectedPage = ToolPage.Search
            } else {
                selectedSong = null
                fetchedLyrics = null
                lyricsError = null
            }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbar) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(
                        top = if (immersiveLyrics) playbackCardTopInset
                        else WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    )
            ) {
                Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                bottom = WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding()
                            )
                ) {
                        AnimatedContent(
                            targetState = selectedPage,
                            transitionSpec = {
                                val enteringFromRight = targetState == ToolPage.Settings
                                (
                                    slideInHorizontally(
                                        animationSpec = spring(
                                            dampingRatio = 0.88f,
                                            stiffness = 420f
                                        )
                                    ) { width -> if (enteringFromRight) width else -width } +
                                        fadeIn(animationSpec = tween(220)) +
                                        scaleIn(
                                            initialScale = 0.985f,
                                            animationSpec = spring(
                                                dampingRatio = 0.9f,
                                                stiffness = 420f
                                            )
                                        )
                                    ).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = spring(
                                                dampingRatio = 0.9f,
                                                stiffness = 420f
                                            )
                                        ) { width -> if (enteringFromRight) -width else width } +
                                            fadeOut(animationSpec = tween(170)) +
                                            scaleOut(
                                                targetScale = 0.99f,
                                                animationSpec = tween(170)
                                            )
                                    )
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
                                cacheCount = cachedItems.size,
                                cacheBytes = cachedItems.sumOf { it.size },
                                statusBarLyricsEnabled = statusBarLyricsEnabled,
                                lyricsFullscreenEnabled = lyricsFullscreenEnabled,
                                themeMode = themeMode,
                                useDynamicColors = useDynamicColors,
                                manualThemeSeedHex = manualThemeSeedHex,
                                modifier = Modifier.weight(1f),
                                onLogin = openLogin,
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
                                },
                                onOpenFavoriteSongsProbe = {
                                    context.startActivity(
                                        Intent(context, FavoriteSongsProbeActivity::class.java)
                                    )
                                }
                            )
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
                                                coverAccentReady = coverAccentReady,
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
                                        onSongClick = { song ->
                                            sourceLookupRequest = 0
                                            fullscreenLyrics = false
                                            lyricsCollapsed = false
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

                AnimatedVisibility(
                    visible = nowPlayingSong != null &&
                        selectedSong == null &&
                        !fullscreenLyrics,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp)
                        .padding(
                            bottom = WindowInsets.navigationBars.asPaddingValues()
                                .calculateBottomPadding() + 84.dp
                        )
                ) {
                    nowPlayingSong?.let { song ->
                        MiniPlayerCapsule(
                            song = song,
                            isPlaying = playbackIsPlaying,
                            positionMs = playbackPositionMs,
                            durationMs = playbackDurationMs,
                            onOpen = { openLyricsForSong(song) },
                            onTogglePlayback = { PlaybackEngine.toggle() },
                            onNext = { PlaybackEngine.next() },
                            onPrevious = { PlaybackEngine.prev() }
                        )
                    }
                }

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
                    FloatingNavigationBar(
                        selectedPage = selectedPage,
                        onSelect = { selectedPage = it }
                    )
                }
            }
        }

    }
}

@Composable
private fun SongResults(
    songs: List<SongHit>,
    searching: Boolean,
    query: String,
    lyricMatches: Boolean,
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 26.dp),
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
            SongResultCard(song, onClick = { onSongClick(song) })
        }
    }
}

@Composable
private fun SongResultCard(song: SongHit, onClick: () -> Unit) {
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
                    overflow = TextOverflow.Ellipsis
                )
                if (song.album.isNotBlank()) {
                    Text(
                        song.album,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
    coverAccentReady: Boolean,
    preparing: Boolean,
    playbackStatus: String,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onCollapseLyrics: () -> Unit,
    onLogin: () -> Unit,
    onOpenQueue: () -> Unit,
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
            modifier = Modifier.align(Alignment.TopCenter)
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
    sourceLookupRequest: Int,
    onSourceLookupSucceeded: (String, String) -> Unit,
    onSourceLookupFailed: () -> Unit,
    onTranslationUnavailable: () -> Unit,
    onUserScroll: () -> Unit,
    onLongPress: () -> Unit,
    onSeekTo: (Long) -> Unit
) {
    val context = LocalContext.current
    val ttml = remember(song.id, fetchedLyrics) {
        fetchedLyrics.lyrics.toAmLyricsTtml()
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

private fun com.applemusic.android.lyrics.Lyrics.toAmLyricsTtml(): String {
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
                append(lines[index].translation.orEmpty().xmlEscaped())
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
                append(line.text.xmlEscaped())
            } else {
                var cursor = 0
                var cursorTimeMs = start
                line.words.forEach { word ->
                    val wordStart = word.charStart.coerceIn(cursor, line.text.length)
                    val wordEnd = word.charEnd.coerceIn(wordStart, line.text.length)
                    if (wordStart > cursor) {
                        appendTtmlSpan(
                            line.text.substring(cursor, wordStart),
                            cursorTimeMs,
                            word.startMs
                        )
                    }
                    if (wordEnd > wordStart) {
                        appendTtmlSpan(
                            line.text.substring(wordStart, wordEnd),
                            word.startMs,
                            word.endMs
                        )
                    }
                    cursor = maxOf(cursor, wordEnd)
                    cursorTimeMs = word.endMs
                }
                if (cursor < line.text.length) {
                    appendTtmlSpan(
                        line.text.substring(cursor),
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
                            modifier = Modifier.weight(1f),
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
                        overflow = TextOverflow.Ellipsis
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
                        Icon(
                            imageVector = if (favorited) {
                                Icons.Filled.Star
                            } else {
                                Icons.Outlined.StarBorder
                            },
                            contentDescription = when (isFavorited) {
                                true -> "已收藏，点击取消收藏"
                                false -> "未收藏，点击收藏"
                                null -> "收藏状态尚未确认，点击重试查询"
                            },
                            tint = if (favorited) {
                                controlColor
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(30.dp)
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
    cacheCount: Int,
    cacheBytes: Long,
    statusBarLyricsEnabled: Boolean,
    lyricsFullscreenEnabled: Boolean,
    themeMode: AppThemeMode,
    useDynamicColors: Boolean,
    manualThemeSeedHex: String,
    modifier: Modifier = Modifier,
    onLogin: () -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onDynamicColorsChange: (Boolean) -> Unit,
    onManualThemeSeedChange: (String) -> Unit,
    onStatusBarLyricsEnabledChange: (Boolean) -> Unit,
    onLyricsFullscreenEnabledChange: (Boolean) -> Unit,
    onOpenCache: () -> Unit,
    onOpenFavoriteSongsProbe: () -> Unit
) {
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val manualThemeActive = !useDynamicColors
    val context = LocalContext.current
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
            .padding(horizontal = 20.dp, vertical = 18.dp),
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
                .clickable {
                    context.startActivity(Intent(context, RadioProbeActivity::class.java))
                },
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
                        Icons.Filled.MusicNote,
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
                    Text("个性电台接口测试", fontWeight = FontWeight.SemiBold)
                    Text(
                        "查看电台首批歌曲与续播接口响应",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Text("打开", color = MaterialTheme.colorScheme.primary)
            }
        }
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenFavoriteSongsProbe),
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
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp)
                ) {
                    Text("Apple Music 喜爱歌曲列表测试", fontWeight = FontWeight.SemiBold)
                    Text(
                        "列出账号歌单，并逐个测试曲目接口",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Text("打开", color = MaterialTheme.colorScheme.primary)
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
    val minGigabytes = 0.5f
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
                    val selectedGigabytes = (sliderValue * 2).roundToInt() / 2f
                    sliderValue = selectedGigabytes
                    onLimitChange((selectedGigabytes * BYTES_PER_GIGABYTE).toLong())
                },
                modifier = Modifier.height(34.dp),
                valueRange = minGigabytes..maxGigabytes,
                steps = 38
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
    onTogglePlayback: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val accentColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp)
            .drawWithContent {
                drawContent()
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
                if (pathLength > 0f) {
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        style = android.graphics.Paint.Style.STROKE
                        this.strokeWidth = strokeWidth
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        color = trackColor.toArgb()
                    }
                    drawIntoCanvas { it.nativeCanvas.drawPath(path, paint) }
                    if (progress > 0f) {
                        val progressPath = android.graphics.Path()
                        pathMeasure.getSegment(0f, pathLength * progress, progressPath, true)
                        paint.color = accentColor.toArgb()
                        drawIntoCanvas { it.nativeCanvas.drawPath(progressPath, paint) }
                    }
                }
            }
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(start = 10.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song.artist,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onTogglePlayback,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "暂停播放" else "继续播放",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(27.dp)
                    )
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "下一首",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingNavigationBar(
    selectedPage: ToolPage,
    onSelect: (ToolPage) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 2.dp,
        shadowElevation = 12.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .width(240.dp)
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            val indicatorOffset by animateDpAsState(
                targetValue = if (selectedPage == ToolPage.Search) 0.dp else maxWidth / 2,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = 520f
                ),
                label = "navigationIndicator"
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = indicatorOffset)
                    .width(maxWidth / 2)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {}
            Row(Modifier.fillMaxWidth()) {
                NavigationPillItem(
                    label = "主页",
                    icon = Icons.Filled.Search,
                    selected = selectedPage == ToolPage.Search,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(ToolPage.Search) }
                )
                NavigationPillItem(
                    label = "设置",
                    icon = Icons.Filled.Settings,
                    selected = selectedPage == ToolPage.Settings,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(ToolPage.Settings) }
                )
            }
        }
    }
}

@Composable
private fun NavigationPillItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "navigationItemColor"
    )
    Box(
        modifier = Modifier
            .then(modifier)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor
            )
            Text(
                label,
                color = contentColor,
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
