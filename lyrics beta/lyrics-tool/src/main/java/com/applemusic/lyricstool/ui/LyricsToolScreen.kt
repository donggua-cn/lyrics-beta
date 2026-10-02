package com.applemusic.lyricstool.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import java.util.Locale

@Composable
private fun LyricsToolTheme(
    context: android.content.Context,
    useSystemColors: Boolean,
    manualSeed: Color,
    content: @Composable () -> Unit
) {
    val colorScheme = remember(context, useSystemColors, manualSeed) {
        if (useSystemColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicLightColorScheme(context)
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
    val useCoverAccent: Boolean = false,
    val glowRadius: Float = 5f,
    val fontSize: Float = 28f,
    val lineSpacing: Float = 18f,
    val autoScroll: Boolean = true,
    val interpolate: Boolean = true,
    val blur: Boolean = false,
    val reducedMotion: Boolean = false,
    val hidePlayedLines: Boolean = false
)

private data class LyricsFetchResult(
    val lyrics: FetchedLyrics?,
    val fallbackLyrics: FetchedLyrics? = null,
    val fallbackSourceName: String? = null,
    val searchOtherSources: Boolean = false,
    val notice: String? = null
)

private suspend fun fetchQqLyricsForAppleSong(
    appleClient: AppleLyricsClient,
    qqClient: QQMusicLyricsClient,
    song: SongHit
): FetchedLyrics? {
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
        result
    } catch (error: Exception) {
        metadataError?.takeIf { it !== error }?.let(error::addSuppressed)
        throw error
    }
}

private suspend fun fetchAppleLyricsWithQqFallback(
    appleClient: AppleLyricsClient,
    qqClient: QQMusicLyricsClient,
    song: SongHit
): LyricsFetchResult {
    var appleError: Exception? = null
    val appleLyrics = try {
        appleClient.fetchLyrics(song.id)
    } catch (error: Exception) {
        appleError = error
        null
    }
    if ((appleLyrics?.lyrics?.wordTimedLines() ?: 0) > 0) {
        return LyricsFetchResult(appleLyrics)
    }

    var qqError: Exception? = null
    val qqLyrics = try {
        fetchQqLyricsForAppleSong(appleClient, qqClient, song)
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
    val themePreferences = remember(context) {
        context.getSharedPreferences("app_theme", android.content.Context.MODE_PRIVATE)
    }
    var useSystemColors by remember(context) {
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
    LyricsToolTheme(context, useSystemColors, manualThemeSeed) {
        val imageLoader = remember(context) { ImageLoader(context) }
        val scope = rememberCoroutineScope()
        val clipboard = LocalClipboardManager.current
        val snackbar = remember { SnackbarHostState() }
        var lyricsNotice by remember { mutableStateOf<String?>(null) }
        var query by remember { mutableStateOf("") }
        var songs by remember { mutableStateOf<List<SongHit>>(emptyList()) }
        var selectedSong by remember { mutableStateOf<SongHit?>(null) }
        var selectedPage by remember { mutableStateOf(ToolPage.Search) }
        var fullscreenLyrics by remember { mutableStateOf(false) }
        var fetchedLyrics by remember { mutableStateOf<FetchedLyrics?>(null) }
        var lyricsError by remember { mutableStateOf<String?>(null) }
        var searching by remember { mutableStateOf(false) }
        var loadingLyrics by remember { mutableStateOf(false) }
        var playbackPositionMs by remember { mutableStateOf(0L) }
        var playbackDurationMs by remember { mutableStateOf(0L) }
        var playbackIsPlaying by remember { mutableStateOf(false) }
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
        var lyricsAppearance by remember(context) {
            mutableStateOf(
                LyricsAppearance(
                    highlightColor = appearancePreferences.getString("highlight_color", "#6750A4")
                        ?: "#6750A4",
                    useCoverAccent = appearancePreferences.getBoolean("use_cover_accent", false),
                    glowRadius = appearancePreferences.getFloat("glow_radius", 5f),
                    fontSize = appearancePreferences.getFloat("font_size", 28f),
                    lineSpacing = appearancePreferences.getFloat("line_spacing", 18f),
                    autoScroll = appearancePreferences.getBoolean("auto_scroll", true),
                    interpolate = appearancePreferences.getBoolean("interpolate", true),
                    blur = appearancePreferences.getBoolean("blur", false),
                    reducedMotion = appearancePreferences.getBoolean("reduced_motion", false),
                    hidePlayedLines = appearancePreferences.getBoolean("hide_played_lines", false)
                )
            )
        }
        var coverAccentColor by remember { mutableStateOf<String?>(null) }
        var pendingExport by remember { mutableStateOf<Pair<String, ExportFormat>?>(null) }
        var cacheManagerOpen by remember { mutableStateOf(false) }
        var cachedItems by remember { mutableStateOf<List<AppleCache.CacheItem>>(emptyList()) }
        var pendingCacheDelete by remember { mutableStateOf<AppleCache.CacheItem?>(null) }
        var statusBarLyricsEnabled by remember(context) {
            mutableStateOf(
                context.getSharedPreferences("lyrics_appearance", android.content.Context.MODE_PRIVATE)
                    .getBoolean("status_bar_lyrics_enabled", true)
            )
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
                .putBoolean("auto_scroll", appearance.autoScroll)
                .putBoolean("interpolate", appearance.interpolate)
                .putBoolean("blur", appearance.blur)
                .putBoolean("reduced_motion", appearance.reducedMotion)
                .putBoolean("hide_played_lines", appearance.hidePlayedLines)
                .apply()
        }

        LaunchedEffect(selectedSong?.artworkUrl, lyricsAppearance.useCoverAccent) {
            val artworkUrl = selectedSong?.artworkUrl
            if (!lyricsAppearance.useCoverAccent || artworkUrl.isNullOrBlank()) {
                coverAccentColor = null
                return@LaunchedEffect
            }
            coverAccentColor = null
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
                    coverAccentColor = accent?.rgb?.let {
                        "#%06X".format(Locale.US, 0xFFFFFF and it)
                    }
                    if (accent == null) {
                        lyricsNotice = "无法从封面提取强调色，继续使用默认颜色"
                    }
                } else if (result is ErrorResult) {
                    lyricsNotice = "封面加载失败，无法提取强调色"
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                lyricsNotice = "封面加载失败：${error.message ?: "无法提取强调色"}"
            }
        }

        val effectiveLyricsAppearance = remember(lyricsAppearance, coverAccentColor) {
            coverAccentColor?.takeIf { lyricsAppearance.useCoverAccent }
                ?.let { lyricsAppearance.copy(highlightColor = it) }
                ?: lyricsAppearance
        }

        val playbackAccentColor =
            (coverAccentColor.takeIf { lyricsAppearance.useCoverAccent })
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
            selectedSong = null
            fullscreenLyrics = false
            fetchedLyrics = null
            lyricsError = null
            scope.launch {
                try {
                    songs = client.searchSongs(term)
                    if (songs.isEmpty()) snackbar.showSnackbar("没有找到歌曲，试试歌名或艺人名")
                    client.lastSearchLocalizationWarning?.let { snackbar.showSnackbar(it) }
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
            val songId = row?.appleId?.ifBlank { row.catalogId }
                ?.ifBlank { row.uri.removePrefix("apple:") }
            if (row == null || songId.isNullOrBlank()) {
                snackbar.showSnackbar("无法恢复当前播放歌曲")
                return@LaunchedEffect
            }

            val song = SongHit(
                id = songId,
                title = row.title,
                artist = row.artist,
                album = row.album,
                artworkUrl = row.coverUrl,
                durationMs = row.durationMs
            )
            selectedSong = song
            sourceLookupRequest = 0
            fullscreenLyrics = false
            fetchedLyrics = null
            lyricsError = null
            loadingLyrics = true
            playbackStatus = PlaybackEngine.preparingNote

            try {
                val lyricsResult = fetchAppleLyricsWithQqFallback(
                    client,
                    qqMusicClient,
                    song
                )
                applyLyricsResult(song, lyricsResult)
            } catch (error: Exception) {
                if (selectedSong == song) {
                    val message = error.message ?: "获取歌词失败，请稍后重试"
                    lyricsError = message
                }
            } finally {
                if (selectedSong == song) loadingLyrics = false
            }
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
            val song = selectedSong ?: return@LaunchedEffect
            while (true) {
                playbackPositionMs = PlaybackEngine.positionMs()
                playbackDurationMs = maxOf(PlaybackEngine.durationMs(), song.durationMs)
                playbackIsPlaying = PlaybackEngine.isPlaying()
                playbackStatus = PlaybackEngine.preparingNote
                delay(100L)
            }
        }

        BackHandler(enabled = selectedSong != null || selectedPage != ToolPage.Search) {
            if (fullscreenLyrics) {
                fullscreenLyrics = false
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
                        .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
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
                                        placeholder = { Text("歌名或艺人") },
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
                                items = cachedItems,
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
                                onTogglePinned = { item ->
                                    MusicRepository.cache.setPinned(item.songId, !item.pinned)
                                    cachedItems = MusicRepository.cache.cachedItems()
                                }
                            )
                        }

                        if (lyricsAppearanceOpen) {
                            LyricsAppearanceDialog(
                                appearance = lyricsAppearance,
                                coverAccentColor = coverAccentColor,
                                onAppearanceChange = updateLyricsAppearance,
                                onDismiss = { lyricsAppearanceOpen = false }
                            )
                        }

                        if (page == ToolPage.Settings && !fullscreenLyrics) {
                            SettingsPage(
                                signedIn = signedIn,
                                cacheCount = cachedItems.size,
                                cacheBytes = cachedItems.sumOf { it.size },
                                statusBarLyricsEnabled = statusBarLyricsEnabled,
                                useSystemColors = useSystemColors,
                                manualThemeSeedHex = manualThemeSeedHex,
                                modifier = Modifier.weight(1f),
                                onLogin = openLogin,
                                onUseSystemColorsChange = { enabled ->
                                    useSystemColors = enabled
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
                                onOpenCache = {
                                    cachedItems = MusicRepository.cache.cachedItems()
                                    cacheManagerOpen = true
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
                                                accentColor = playbackAccentColor,
                                                preparing = PlaybackEngine.preparing,
                                                playbackStatus = playbackStatus,
                                                fullscreen = fullscreenLyrics,
                                                onBack = {
                                                    selectedSong = null
                                                    fetchedLyrics = null
                                                    lyricsError = null
                                                    fullscreenLyrics = false
                                                },
                                                onToggleFullscreen = {
                                                    fullscreenLyrics = !fullscreenLyrics
                                                },
                                                onLogin = openLogin,
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
                                                            val result = client.fetchLyrics(song.id)
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
                                        onSongClick = { song ->
                                            sourceLookupRequest = 0
                                            fullscreenLyrics = false
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

                if (!fullscreenLyrics) {
                        FloatingNavigationBar(
                            selectedPage = selectedPage,
                            onSelect = { selectedPage = it },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(
                                    bottom = WindowInsets.navigationBars.asPaddingValues()
                                        .calculateBottomPadding() + 12.dp
                                )
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
                    else "换个关键词再试试，也可以输入艺人名。",
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
                    "搜索结果",
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
    accentColor: Color,
    error: String?,
    loading: Boolean,
    signedIn: Boolean,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    externalSourceMode: Boolean,
    externalSourceName: String?,
    externalFallbackSourceName: String?,
    sourceLookupInProgress: Boolean,
    sourceLookupRequest: Int,
    lyricsAppearance: LyricsAppearance,
    preparing: Boolean,
    playbackStatus: String,
    fullscreen: Boolean,
    onBack: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onLogin: () -> Unit,
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
    onTranslationUnavailable: () -> Unit
) {
    var exportMenuExpanded by remember { mutableStateOf(false) }
    var actionsMenuExpanded by remember { mutableStateOf(false) }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        AnimatedVisibility(
            visible = !fullscreen,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("‹  搜索结果")
                }
                Spacer(Modifier.weight(1f))
                if (fetchedLyrics != null) {
                    IconButton(onClick = onCopy) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "复制 $lyricSourceName 歌词",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Box {
                        IconButton(onClick = { exportMenuExpanded = true }) {
                            Icon(Icons.Filled.Download, contentDescription = "下载歌词")
                        }
                        DropdownMenu(
                            expanded = exportMenuExpanded,
                            onDismissRequest = { exportMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "下载 $lyricSourceName TXT"
                                    )
                                },
                                onClick = {
                                    exportMenuExpanded = false
                                    onExport(ExportFormat.Text)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (fetchedLyrics.lyrics.synced) {
                                            "下载 $lyricSourceName LRC 时间轴"
                                        }
                                        else "LRC 时间轴不可用（无同步时间）"
                                    )
                                },
                                enabled = fetchedLyrics.lyrics.synced,
                                onClick = {
                                    exportMenuExpanded = false
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
                                    exportMenuExpanded = false
                                    onExport(ExportFormat.Ttml)
                                }
                            )
                            if (fetchedLyrics.qqQrcDebugText != null) {
                                DropdownMenuItem(
                                    text = { Text("下载 QQ QRC 解密结构（供排查）") },
                                    onClick = {
                                        exportMenuExpanded = false
                                        onExport(ExportFormat.QqQrc)
                                    }
                                )
                            }
                        }
                    }
                }
                Box {
                    IconButton(onClick = { actionsMenuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "歌词选项")
                    }
                    DropdownMenu(
                        expanded = actionsMenuExpanded,
                        onDismissRequest = { actionsMenuExpanded = false }
                    ) {
                        if (
                            externalFallbackSourceName != null &&
                            externalFallbackSourceName != lyricSourceName
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text("返回 $externalFallbackSourceName 歌词")
                                },
                                onClick = {
                                    actionsMenuExpanded = false
                                    onRestoreAppleLyrics()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (!externalSourceMode) {
                                        "Apple Music 歌词（当前）"
                                    } else {
                                        "使用 Apple Music 歌词"
                                    }
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
                                    if (externalSourceMode &&
                                        externalSourceName != "QQ 音乐"
                                    ) {
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
        }

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
            fullscreen = fullscreen,
            onToggleFullscreen = onToggleFullscreen,
            showTranslations = showTranslations,
            onToggleTranslations = {
                if (!showTranslations && !hasTranslations) {
                    onTranslationUnavailable()
                } else {
                    showTranslations = !showTranslations
                }
            },
            onSeekStart = { dragPositionMs = it },
            onSeekChange = { dragPositionMs = it },
            onSeekFinished = {
                dragPositionMs?.let(onSeekTo)
                dragPositionMs = null
            }
        )
        Spacer(Modifier.height(8.dp))
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
                        onTranslationUnavailable = {
                            if (showTranslations) showTranslations = false
                            onTranslationUnavailable()
                        },
                        onSeekTo = onSeekTo
                    )
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
}

@Composable
private fun LyricsAppearanceDialog(
    appearance: LyricsAppearance,
    coverAccentColor: String?,
    onAppearanceChange: (LyricsAppearance) -> Unit,
    onDismiss: () -> Unit
) {
    val highlightColors = listOf(
        "#6750A4" to "薰衣草紫",
        "#007AFF" to "亮蓝",
        "#E35D6A" to "珊瑚红",
        "#2E9D73" to "薄荷绿"
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
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
                Text("歌词效果", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "逐字发光需要歌词文件本身带有字级时间轴。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

@Composable
private fun AppearanceSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
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
    state.seekTo = onSeekTo
    val assetLoader = remember(context) {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
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
                loadUrl("https://appassets.androidplatform.net/assets/am-lyrics/index.html")
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

    if (state.sentAppearance != state.appearance) {
        val appearance = state.appearance
        val appearanceJson = JSONObject()
            .put("highlightColor", appearance.highlightColor)
            .put("glowRadius", appearance.glowRadius)
            .put("fontSize", appearance.fontSize)
            .put("lineSpacing", appearance.lineSpacing)
            .put("autoScroll", appearance.autoScroll)
            .put("interpolate", appearance.interpolate)
            .put("blur", appearance.blur)
            .put("reducedMotion", appearance.reducedMotion)
            .put("hidePlayedLines", appearance.hidePlayedLines)
        evaluateJavascript(
            "window.amLyricsSetAppearance(${JSONObject.quote(appearanceJson.toString())})",
            null
        )
        state.sentAppearance = appearance
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
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    showTranslations: Boolean,
    onToggleTranslations: () -> Unit,
    onSeekStart: (Long) -> Unit,
    onSeekChange: (Long) -> Unit,
    onSeekFinished: () -> Unit
) {
    val progress = (positionMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat())
        .coerceIn(0f, 1f)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val frameHitSlop = with(density) { 24.dp.toPx() }
    val frameCornerRadius = with(density) { 28.dp.toPx() }
    val frameStrokeWidth = with(density) { 2.dp.toPx() }
    val cardShape = RoundedCornerShape(28.dp)
    val outlineColor = accentColor.toArgb()
    val containerColor = lerp(MaterialTheme.colorScheme.surface, accentColor, 0.14f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(durationMs) {
                if (durationMs <= 0L) return@pointerInput
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

                    val initialPosition = seekAt(down.position)
                        ?: return@awaitEachGesture
                    onSeekStart(initialPosition)
                    down.consume()
                    var pointerPressed = true
                    while (pointerPressed) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.first()
                        seekAt(pointer.position)?.let(onSeekChange)
                        pointer.consume()
                        pointerPressed = pointer.pressed
                    }
                    onSeekFinished()
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
                        color = accentColor.copy(alpha = 0.18f).toArgb()
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
        colors = CardDefaults.cardColors(containerColor = containerColor)
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
                AsyncImage(
                    model = song.artworkUrl,
                    contentDescription = "${song.title} 专辑封面",
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(19.dp))
                )
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        song.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song.artist,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 3.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = onTogglePlayback,
                    enabled = !preparing || playbackStatus.startsWith("无法播放"),
                    modifier = Modifier.size(48.dp)
                ) {
                    if (preparing) {
                        CircularProgressIndicator(modifier = Modifier.size(23.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "暂停播放" else "继续播放",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = if (fullscreen) "退出全屏歌词" else "全屏歌词",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(
                    onClick = onToggleTranslations,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Translate,
                        contentDescription = if (showTranslations) "隐藏歌词翻译" else "显示歌词翻译",
                        tint = if (showTranslations) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
            if (playbackStatus.contains("session has ended", ignoreCase = true)) {
                TextButton(
                    onClick = onLogin,
                    modifier = Modifier.align(Alignment.End)
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
                        preparing -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${formatPlaybackTime(positionMs)} / ${formatPlaybackTime(durationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    useSystemColors: Boolean,
    manualThemeSeedHex: String,
    modifier: Modifier = Modifier,
    onLogin: () -> Unit,
    onUseSystemColorsChange: (Boolean) -> Unit,
    onManualThemeSeedChange: (String) -> Unit,
    onStatusBarLyricsEnabledChange: (Boolean) -> Unit,
    onOpenCache: () -> Unit
) {
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
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
                        Text("应用主题色", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (supportsDynamicColor) {
                                "跟随系统壁纸动态取色，或手动选择颜色"
                            } else {
                                "手动选择应用主题色；动态取色需要 Android 12 及以上"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    Switch(
                        checked = useSystemColors && supportsDynamicColor,
                        onCheckedChange = onUseSystemColorsChange,
                        enabled = supportsDynamicColor
                    )
                }
                Text(
                    if (useSystemColors && supportsDynamicColor) "Material You · 系统动态色"
                    else "手动主题色",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    themeColorPresets.forEach { preset ->
                        val color = Color(android.graphics.Color.parseColor(preset.hex))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                onClick = {
                                    if (useSystemColors) onUseSystemColorsChange(false)
                                    onManualThemeSeedChange(preset.hex)
                                },
                                modifier = Modifier.size(42.dp),
                                shape = CircleShape,
                                color = color,
                                border = if (
                                    !useSystemColors && manualThemeSeedHex.equals(
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
                                    !useSystemColors && manualThemeSeedHex.equals(
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
    items: List<AppleCache.CacheItem>,
    currentSongId: String?,
    pendingDelete: AppleCache.CacheItem?,
    onDismiss: () -> Unit,
    onRequestDelete: (AppleCache.CacheItem) -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: (AppleCache.CacheItem) -> Unit,
    onTogglePinned: (AppleCache.CacheItem) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
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
                } else {
                    Text(
                        "播放缓存",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${items.size} 首 · ${formatCacheSize(items.sumOf { it.size })}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )
                    if (items.isEmpty()) {
                        Text(
                            "还没有缓存歌曲。播放歌曲后会自动缓存到本机。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 440.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items, key = { it.songId }) { item ->
                                val song = MusicRepository.findSong(item.songId)
                                val isCurrentSong = item.songId == currentSongId
                                val title = item.title.ifBlank { song?.title ?: item.songId }
                                val artist = item.artist.ifBlank { song?.artist.orEmpty() }
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
                                            Text(
                                                buildString {
                                                    artist.takeIf { it.isNotBlank() }?.let { append(it); append(" · ") }
                                                    append(formatCacheSize(item.size))
                                                    when {
                                                        item.downloading -> append(" · 下载中")
                                                        isCurrentSong -> append(" · 当前歌曲")
                                                        item.pinned -> append(" · 已固定")
                                                    }
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(onClick = { onTogglePinned(item) }) {
                                            Icon(
                                                Icons.Filled.PushPin,
                                                contentDescription = if (item.pinned) "取消固定缓存" else "固定缓存",
                                                tint = if (item.pinned) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = { onRequestDelete(item) },
                                            enabled = !item.downloading && !isCurrentSong
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "删除缓存",
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
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 8.dp)
                    ) {
                        Text("完成")
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
