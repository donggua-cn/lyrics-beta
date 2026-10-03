package com.applemusic.lyricstool.ui

import android.text.Html
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.Matrix
import android.view.TextureView
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.applemusic.android.apple.AppleAlbum
import com.applemusic.android.apple.AppleArtist
import com.applemusic.android.apple.AppleArtistPage
import com.applemusic.android.apple.ApplePlaylist
import com.applemusic.android.apple.AppleSong
import com.applemusic.lyricstool.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.material3.darkColorScheme

@Composable
internal fun CatalogArtistPage(
    page: AppleArtistPage?,
    recyclerState: ComposeRecyclerListState,
    sectionStateHolder: androidx.compose.runtime.saveable.SaveableStateHolder,
    isLoading: Boolean,
    error: String?,
    statusBarHeight: Dp,
    bottomPadding: Dp,
    onBack: () -> Unit,
    onScrollChanged: (Boolean, Boolean) -> Unit,
    onShare: () -> Unit,
    onRetry: () -> Unit,
    onOpenTopSongs: () -> Unit,
    isArtistFavorite: Boolean?,
    artistSavePending: Boolean,
    onToggleArtistFavorite: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onPlaySong: (AppleSong) -> Unit,
    onOpenSongAlbum: (AppleSong) -> Unit,
    onOpenArtist: (AppleArtist) -> Unit,
    onOpenAlbum: (AppleAlbum) -> Unit,
    onAddAlbum: (AppleAlbum) -> Unit,
    onOpenPlaylist: (ApplePlaylist) -> Unit,
    modifier: Modifier = Modifier
) {
    val songListState = rememberLazyListState()
    val songPages = remember(page?.topSongs) {
        page?.topSongs.orEmpty().chunked(5)
    }
    val currentSongPage by remember(songListState) {
        derivedStateOf { songListState.firstVisibleItemIndex }
    }
    val infoDialogOpen = remember { mutableStateOf(false) }
    val moreMenuOpen = remember { mutableStateOf(false) }
    val isScrolled = recyclerState.isContentScrolled
    val density = LocalDensity.current
    var previousScrollDirection by remember(recyclerState) { mutableStateOf<Boolean?>(null) }
    var accumulatedScrollDistance by remember(recyclerState) { mutableIntStateOf(0) }
    var scrollStateInitialized by remember(recyclerState) { mutableStateOf(false) }
    val scrollDirectionThresholdPx = with(density) { 10.dp.roundToPx() }
    val onRecyclerScroll: (RecyclerView, Int, Int) -> Unit = { recyclerView, _, dy ->
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
        val firstPosition = layoutManager?.findFirstVisibleItemPosition() ?: 0
        val firstView = layoutManager?.findViewByPosition(firstPosition)
        val atTop = firstPosition == 0 && (firstView?.top ?: 0) >= 0
        if (!scrollStateInitialized) {
            onScrollChanged(!atTop, true)
            scrollStateInitialized = true
        } else if (atTop) {
            onScrollChanged(false, false)
            accumulatedScrollDistance = 0
            previousScrollDirection = null
        } else if (dy != 0) {
            val scrollingDown = dy > 0
            if (previousScrollDirection != scrollingDown) {
                previousScrollDirection = scrollingDown
                accumulatedScrollDistance = 0
            }
            accumulatedScrollDistance += kotlin.math.abs(dy)
            if (accumulatedScrollDistance >= scrollDirectionThresholdPx) {
                onScrollChanged(scrollingDown, false)
                accumulatedScrollDistance = 0
            }
        }
    }
    val screen = LocalConfiguration.current
    val heroHeight = (screen.screenHeightDp.dp * 0.51f).coerceIn(440.dp, 560.dp)
    val songPageWidth = (screen.screenWidthDp.dp - 40.dp).coerceAtLeast(280.dp)
    val newestRelease = page?.latestRelease ?: page?.albums?.firstOrNull()
    val heroVisible by remember(recyclerState) {
        derivedStateOf {
            recyclerState.firstVisibleItemPosition == 0 &&
                recyclerState.firstVisibleItemKey == "artist-hero"
        }
    }
    val latestHeroFocused = rememberUpdatedState(heroVisible)
    val context = LocalContext.current
    val imageLoader = remember(context) { context.imageLoader }
    var pageBackground by remember(page?.artist?.id) { mutableStateOf(Color(0xFF373739)) }
    var pageAccent by remember(page?.artist?.id) { mutableStateOf(Color(0xFFE3D18A)) }
    val animatedPageBackground by animateColorAsState(
        pageBackground,
        animationSpec = tween(500),
        label = "artistArtworkBackground"
    )
    val animatedPageAccent by animateColorAsState(
        pageAccent,
        animationSpec = tween(500),
        label = "artistArtworkAccent"
    )
    val artworkUrl = page?.artist?.motionPreviewUrl
        ?.takeIf(String::isNotBlank)
        ?: page?.artist?.artworkUrl.orEmpty()
    val artistVideoPositionMs = remember(page?.artist?.id, artworkUrl) {
        mutableLongStateOf(0L)
    }
    val artistColors = darkColorScheme(
        primary = animatedPageAccent,
        background = animatedPageBackground,
        surface = animatedPageBackground,
        surfaceVariant = lerp(animatedPageBackground, Color.White, 0.12f),
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color.White.copy(alpha = 0.78f)
    )
    val latestOnOpenTopSongs = rememberUpdatedState(onOpenTopSongs)
    val latestOnToggleFavorite = rememberUpdatedState(onToggleArtistFavorite)
    val latestOnPlay = rememberUpdatedState(onPlay)
    val latestOnPlaySong = rememberUpdatedState(onPlaySong)
    val latestOnOpenSongAlbum = rememberUpdatedState(onOpenSongAlbum)
    val latestOnOpenArtist = rememberUpdatedState(onOpenArtist)
    val latestOnOpenAlbum = rememberUpdatedState(onOpenAlbum)
    val latestOnAddAlbum = rememberUpdatedState(onAddAlbum)
    val latestOnOpenPlaylist = rememberUpdatedState(onOpenPlaylist)
    val sections = buildList {
        if (page != null) {
            add(
                ComposeRecyclerSection(
                    key = "artist-hero",
                    viewType = 10,
                    revision = listOf(
                        page.artist,
                        heroHeight,
                        isArtistFavorite,
                        artistSavePending
                    )
                ) {
                    ArtistHeroSection(
                        artist = page.artist,
                        artworkUrl = artworkUrl,
                        videoPositionMs = artistVideoPositionMs,
                        heroHeight = heroHeight,
                        isFocused = latestHeroFocused.value,
                        isFavorite = isArtistFavorite,
                        savePending = artistSavePending,
                        infoDialogOpen = infoDialogOpen,
                        onToggleFavorite = { latestOnToggleFavorite.value() },
                        onPlay = { latestOnPlay.value() }
                    )
                }
            )
            newestRelease?.let { album ->
                add(
                    ComposeRecyclerSection(
                        key = "artist-latest-release",
                        viewType = 11,
                        revision = album
                    ) {
                        LatestArtistRelease(
                            album = album,
                            onOpen = { latestOnOpenAlbum.value(album) },
                            onAdd = { latestOnAddAlbum.value(album) }
                        )
                    }
                )
            }
            if (page.topSongs.isNotEmpty()) {
                add(
                    ComposeRecyclerSection(
                        key = "artist-top-songs",
                        viewType = 12,
                        revision = listOf(page.artist, page.topSongs)
                    ) {
                        ArtistTopSongsSection(
                            artist = page.artist,
                            songListState = songListState,
                            songPages = songPages,
                            currentSongPage = currentSongPage,
                            songPageWidth = songPageWidth,
                            onOpenTopSongs = { latestOnOpenTopSongs.value() },
                            onPlaySong = { latestOnPlaySong.value(it) },
                            onOpenSongAlbum = { latestOnOpenSongAlbum.value(it) },
                            onOpenArtist = { latestOnOpenArtist.value(it) }
                        )
                    }
                )
            }
            if (page.albums.isNotEmpty()) {
                add(
                    ComposeRecyclerSection(
                        key = "artist-section:Albums",
                        viewType = 13,
                        revision = listOf(page.artist, page.albums)
                    ) {
                        ArtistAlbumShelfContent(
                            title = "Albums",
                            albums = page.albums,
                            onOpenAlbum = { latestOnOpenAlbum.value(it) },
                            onOpenArtist = { latestOnOpenArtist.value(page.artist) }
                        )
                    }
                )
            }
            if (page.singles.isNotEmpty()) {
                add(
                    ComposeRecyclerSection(
                        key = "artist-section:Singles & EPs",
                        viewType = 13,
                        revision = listOf(page.artist, page.singles)
                    ) {
                        ArtistAlbumShelfContent(
                            title = "Singles & EPs",
                            albums = page.singles,
                            onOpenAlbum = { latestOnOpenAlbum.value(it) },
                            onOpenArtist = { latestOnOpenArtist.value(page.artist) }
                        )
                    }
                )
            }
            if (page.musicVideos.isNotEmpty()) {
                add(
                    ComposeRecyclerSection(
                        key = "artist-section:Music Videos",
                        viewType = 14,
                        revision = listOf(page.artist, page.musicVideos)
                    ) {
                        ArtistAlbumShelfContent(
                            title = "Music Videos",
                            albums = page.musicVideos,
                            onOpenAlbum = { latestOnOpenAlbum.value(it) },
                            onOpenArtist = { latestOnOpenArtist.value(page.artist) },
                            openable = false,
                            videoShelf = true
                        )
                    }
                )
            }
            if (page.playlists.isNotEmpty()) {
                add(
                    ComposeRecyclerSection(
                        key = "artist-playlists",
                        viewType = 15,
                        revision = page.playlists
                    ) {
                        ArtistPlaylistsSection(
                            playlists = page.playlists,
                            onOpenPlaylist = { latestOnOpenPlaylist.value(it) }
                        )
                    }
                )
            }
            if (page.similarArtists.isNotEmpty()) {
                add(
                    ComposeRecyclerSection(
                        key = "artist-similar-artists",
                        viewType = 16,
                        revision = page.similarArtists
                    ) {
                        ArtistSimilarArtistsSection(
                            artists = page.similarArtists,
                            onOpenArtist = { latestOnOpenArtist.value(it) }
                        )
                    }
                )
            }
        } else if (isLoading) {
            add(
                ComposeRecyclerSection("artist-loading", 17, isLoading) {
                    Box(
                        Modifier.fillMaxWidth().height(360.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            )
        } else if (error != null) {
            add(
                ComposeRecyclerSection("artist-error", 18, error) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(error, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                            Text("重试")
                        }
                    }
                }
            )
        }
    }

    LaunchedEffect(page?.artist?.id, artworkUrl) {
        if (artworkUrl.isBlank()) return@LaunchedEffect
        try {
            val result = imageLoader.execute(
                ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .size(256)
                    .allowHardware(false)
                    .build()
            )
            if (result is SuccessResult) {
                val palette = withContext(Dispatchers.Default) {
                    Palette.from(result.drawable.toBitmap()).generate()
                }
                val base = palette.darkMutedSwatch
                    ?: palette.darkVibrantSwatch
                    ?: palette.mutedSwatch
                    ?: palette.dominantSwatch
                val accent = palette.vibrantSwatch
                    ?: palette.lightVibrantSwatch
                    ?: palette.darkVibrantSwatch
                    ?: palette.dominantSwatch
                base?.let {
                    val extracted = Color(it.rgb)
                    pageBackground = lerp(
                        extracted,
                        Color.Black,
                        if (extracted.luminance() > 0.3f) 0.58f else 0.26f
                    )
                }
                accent?.let {
                    val extracted = Color(it.rgb)
                    pageAccent = if (extracted.luminance() < 0.28f) {
                        lerp(extracted, Color.White, 0.55f)
                    } else {
                        extracted
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w("CatalogArtistPage", "艺人封面取色失败：${error.message}")
        }
    }

    MaterialTheme(colorScheme = artistColors) {
      Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ComposeRecyclerSections(
            sections = sections,
            state = recyclerState,
            saveableStateHolder = sectionStateHolder,
            contentPadding = PaddingValues(bottom = bottomPadding + 96.dp),
            onScroll = onRecyclerScroll,
            trackScrollOffset = false,
            contentScrolledThresholdPx = with(density) { 80.dp.roundToPx() },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .padding(top = statusBarHeight + 6.dp, start = 20.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color(0x66000000)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "返回", tint = Color.White)
                }
            }
            Spacer(Modifier.weight(1f))
            if (isScrolled) {
                Text(
                    page?.artist?.name.orEmpty(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(2f).padding(horizontal = 12.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(Modifier.weight(2f))
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0x66000000)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Filled.Share, "分享艺人", tint = Color.White)
                    }
                    Box {
                        IconButton(onClick = { moreMenuOpen.value = true }) {
                            Icon(Icons.Filled.MoreHoriz, "更多操作", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = moreMenuOpen.value,
                            onDismissRequest = { moreMenuOpen.value = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("播放") },
                                onClick = {
                                    moreMenuOpen.value = false
                                    onPlay()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("随机播放") },
                                onClick = {
                                    moreMenuOpen.value = false
                                    onShuffle()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("分享艺人") },
                                onClick = {
                                    moreMenuOpen.value = false
                                    onShare()
                                }
                            )
                        }
                    }
                }
            }
        }

        if (infoDialogOpen.value && page != null) {
            AlertDialog(
                onDismissRequest = { infoDialogOpen.value = false },
                title = { Text(page.artist.name) },
                text = {
                    val description = Html.fromHtml(
                        page.artist.editorialDescription,
                        Html.FROM_HTML_MODE_LEGACY
                    ).toString().trim()
                    val bornOrFormed = page.artist.bornOrFormed
                        .takeIf(String::isNotBlank)
                        ?.let { date ->
                            Regex("""^(\d{4})-(\d{2})-(\d{2})$""")
                                .matchEntire(date)
                                ?.destructured
                                ?.let { (year, month, day) -> "${year}年${month.toInt()}月${day.toInt()}日" }
                                ?: date
                        }
                    Column(
                        Modifier.heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        bornOrFormed?.let { date ->
                            Text(
                                if (page.artist.isGroup) "出道日期" else "出生日期",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(date, modifier = Modifier.padding(bottom = 12.dp))
                        }
                        if (page.artist.genreNames.isNotEmpty()) {
                            Text(
                                "类型",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                page.artist.genreNames.joinToString("、"),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                        Text(description.ifBlank { "Apple Music 暂未提供艺人介绍" })
                    }
                },
                confirmButton = {
                    TextButton(onClick = { infoDialogOpen.value = false }) {
                        Text("完成")
                    }
                }
            )
        }
    }
}
}

@Composable
private fun ArtistHeroSection(
artist: AppleArtist,
artworkUrl: String,
videoPositionMs: androidx.compose.runtime.MutableLongState,
heroHeight: Dp,
isFocused: Boolean,
isFavorite: Boolean?,
savePending: Boolean,
infoDialogOpen: androidx.compose.runtime.MutableState<Boolean>,
onToggleFavorite: () -> Unit,
onPlay: () -> Unit
) {
Box(Modifier.fillMaxWidth().height(heroHeight)) {
    AmbientArtistArtwork(
        previewUrl = artworkUrl,
        videoUrl = artist.motionVideoUrl,
        playbackPositionMs = videoPositionMs,
        isFocused = isFocused,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to Color(0x55000000),
                0.38f to Color.Transparent,
                0.7f to Color(0x66000000),
                1f to MaterialTheme.colorScheme.background
            )
        )
    )
    Column(
        Modifier.align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 26.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            artist.name,
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                color = Color(0x55000000),
                onClick = { infoDialogOpen.value = true }
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_apple_artist_info),
                    contentDescription = "艺人简介",
                    colorFilter = ColorFilter.tint(Color.White),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.requiredSize(12.5.dp, 24.dp)
                )
            }
            Surface(
                modifier = Modifier.size(68.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                onClick = onPlay
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_apple_music_play),
                    contentDescription = "播放艺人热门歌曲",
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.padding(16.dp)
                )
            }
            Surface(
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                color = Color(0x55000000),
                enabled = !savePending,
                onClick = onToggleFavorite
            ) {
                ArtistFavoriteStar(
                    filled = isFavorite == true,
                    modifier = Modifier.padding(15.dp).size(24.dp)
                )
            }
        }
    }
}
}

@Composable
private fun ArtistTopSongsSection(
artist: AppleArtist,
songListState: LazyListState,
songPages: List<List<AppleSong>>,
currentSongPage: Int,
songPageWidth: Dp,
onOpenTopSongs: () -> Unit,
onPlaySong: (AppleSong) -> Unit,
onOpenSongAlbum: (AppleSong) -> Unit,
onOpenArtist: (AppleArtist) -> Unit
) {
Column(Modifier.padding(top = 28.dp)) {
    SectionHeading("Top Songs", chevron = true, onClick = onOpenTopSongs)
    LazyRow(
        state = songListState,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        itemsIndexed(
            songPages,
            key = { index, _ -> "top-songs:$index" },
            contentType = { _, _ -> "artist-top-songs-page" }
        ) { _, songsOnPage ->
            Column(
                Modifier.width(songPageWidth),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                songsOnPage.forEachIndexed { index, song ->
                    ArtistTopSongRow(
                        song = song,
                        artist = artist,
                        onPlay = { onPlaySong(song) },
                        onOpenAlbum = { onOpenSongAlbum(song) },
                        onOpenArtist = { onOpenArtist(artist) }
                    )
                    if (index < songsOnPage.lastIndex) {
                        Box(
                            Modifier.fillMaxWidth().padding(start = 70.dp)
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.12f))
                        )
                    }
                }
            }
        }
    }
    if (songPages.size > 1) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(songPages.size) { index ->
                Box(
                    Modifier.padding(horizontal = 3.dp)
                        .size(if (currentSongPage == index) 7.dp else 5.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentSongPage == index) {
                                MaterialTheme.colorScheme.onBackground
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            }
                        )
                )
            }
        }
    }
}
}

@Composable
private fun ArtistPlaylistsSection(
playlists: List<ApplePlaylist>,
onOpenPlaylist: (ApplePlaylist) -> Unit
) {
Column(Modifier.padding(top = 28.dp)) {
    SectionHeading("Playlists")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(
            playlists,
            key = { "playlist:${it.id}" },
            contentType = { "artist-playlist-card" }
        ) { playlist ->
            val currentPlaylist = rememberUpdatedState(playlist)
            val currentOpenPlaylist = rememberUpdatedState(onOpenPlaylist)
            val openPlaylist = remember {
                { currentOpenPlaylist.value(currentPlaylist.value) }
            }
            ArtistPlaylistCard(
                name = playlist.name,
                artworkUrl = playlist.artworkUrl,
                onOpen = openPlaylist
            )
        }
    }
}
}

@Composable
private fun ArtistSimilarArtistsSection(
artists: List<AppleArtist>,
onOpenArtist: (AppleArtist) -> Unit
) {
Column(Modifier.padding(top = 28.dp)) {
    SectionHeading("Similar Artists", chevron = true)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(
            artists,
            key = { "similar:${it.id}" },
            contentType = { "artist-similar-artist-card" }
        ) { artist ->
            val currentArtist = rememberUpdatedState(artist)
            val currentOpenArtist = rememberUpdatedState(onOpenArtist)
            val openArtist = remember {
                { currentOpenArtist.value(currentArtist.value) }
            }
            ArtistSimilarArtistCard(
                name = artist.name,
                artworkUrl = artist.artworkUrl,
                onOpen = openArtist
            )
        }
    }
}
}

@Composable
internal fun CatalogArtistTopSongsPage(
    artist: AppleArtist?,
    listState: LazyListState,
    songs: List<AppleSong>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onPlaySong: (AppleSong) -> Unit,
    onOpenSongAlbum: (AppleSong) -> Unit,
    onOpenArtist: (AppleArtist) -> Unit,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LaunchedEffect(listState, songs.size, isLoading, hasMore) {
        if (!hasMore || isLoading || songs.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }.collect { lastVisibleIndex ->
            if (lastVisibleIndex >= songs.size - 4) onLoadMore()
        }
    }
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = statusBarHeight + 84.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding() + 96.dp
            )
        ) {
            item {
                SectionHeading("Top Songs")
            }
            itemsIndexed(songs, key = { _, song -> "full-top-song:${song.id}" }) { index, song ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${index + 1}",
                        modifier = Modifier.width(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium
                    )
                    ArtistTopSongRow(
                        song = song,
                        artist = artist ?: AppleArtist(),
                        onPlay = { onPlaySong(song) },
                        onOpenAlbum = { onOpenSongAlbum(song) },
                        onOpenArtist = {
                            artist?.let(onOpenArtist)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (isLoading) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (songs.isEmpty()) {
                item {
                    Text(
                        "暂无热门歌曲",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Row(
            Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .padding(top = statusBarHeight + 18.dp, start = 20.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "返回")
                }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    chevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ).padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (chevron) {
            Text(
                "›",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
private fun LatestArtistRelease(
    album: AppleAlbum,
    onOpen: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onOpen)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = album.artworkUrl,
            contentDescription = album.name,
            modifier = Modifier.size(108.dp).clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Crop
        )
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                if (album.year > 0) album.year.toString() else "最新发行",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                album.name,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 5.dp)
            )
            Text(
                "${album.trackCount.coerceAtLeast(1)} Song${if (album.trackCount == 1) "" else "s"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.12f),
            onClick = onAdd
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "添加到资料库",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(18.dp)
            )
        }
    }
}

@Composable
private fun ArtistTopSongRow(
    song: AppleSong,
    artist: AppleArtist,
    onPlay: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenArtist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val menuOpen = remember(song.id) { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth().clickable(onClick = onPlay).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.artworkUrl,
            contentDescription = null,
            modifier = Modifier.size(58.dp).clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenAlbum),
            contentScale = ContentScale.Crop
        )
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            Text(
                song.name,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onOpenAlbum)
            )
            val subtitle = buildString {
                append(song.albumName.ifBlank { song.artistName.ifBlank { artist.name } })
                if (song.year > 0) append(" · ${song.year}")
            }
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp).clickable(onClick = onOpenAlbum)
            )
        }
        Text(
            song.durationMs.toTrackDuration(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 4.dp)
        )
        Box {
            IconButton(onClick = { menuOpen.value = true }) {
                Icon(
                    Icons.Filled.MoreHoriz,
                    contentDescription = "更多：${song.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = menuOpen.value,
                onDismissRequest = { menuOpen.value = false }
            ) {
                DropdownMenuItem(
                    text = { Text("播放") },
                    onClick = {
                        menuOpen.value = false
                        onPlay()
                    }
                )
                DropdownMenuItem(
                    text = { Text("查看专辑") },
                    onClick = {
                        menuOpen.value = false
                        onOpenAlbum()
                    }
                )
                DropdownMenuItem(
                    text = { Text("查看艺人") },
                    onClick = {
                        menuOpen.value = false
                        onOpenArtist()
                    }
                )
            }
        }
    }
}

private fun Long.toTrackDuration(): String =
    if (this <= 0L) "" else "%d:%02d".format(this / 60_000, this / 1_000 % 60)

@Composable
private fun ArtistAlbumShelfContent(
    title: String,
    albums: List<AppleAlbum>,
    onOpenAlbum: (AppleAlbum) -> Unit,
    onOpenArtist: () -> Unit,
    openable: Boolean = true,
    videoShelf: Boolean = false
) {
    if (albums.isEmpty()) return
    Column(Modifier.padding(top = 28.dp)) {
        SectionHeading(title)
        val currentOpenArtist = rememberUpdatedState(onOpenArtist)
        val openArtist = remember { { currentOpenArtist.value() } }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(
                albums,
                key = { "$title:${it.id}" },
                contentType = { "artist-album-card" }
            ) { album ->
                val currentAlbum = rememberUpdatedState(album)
                val currentOpenAlbum = rememberUpdatedState(onOpenAlbum)
                val openAlbum = remember {
                    { currentOpenAlbum.value(currentAlbum.value) }
                }
                ArtistAlbumCard(
                    name = album.name,
                    artistName = album.artistName,
                    year = album.year,
                    artworkUrl = album.artworkUrl,
                    videoShelf = videoShelf,
                    openable = openable,
                    onOpenAlbum = openAlbum,
                    onOpenArtist = openArtist
                )
            }
        }
    }
}

@Composable
private fun ArtistAlbumCard(
    name: String,
    artistName: String,
    year: Int,
    artworkUrl: String,
    videoShelf: Boolean,
    openable: Boolean,
    onOpenAlbum: () -> Unit,
    onOpenArtist: () -> Unit
) {
    val card = Modifier.width(if (videoShelf) 260.dp else 176.dp)
    Column(if (openable) card.clickable(onClick = onOpenAlbum) else card) {
        ArtistArtwork(
            artworkUrl,
            name,
            aspectRatio = if (videoShelf) 16f / 9f else 1f,
            modifier = if (openable) Modifier.clickable(onClick = onOpenAlbum) else Modifier
        )
        Text(
            name,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp).then(
                if (openable) Modifier.clickable(onClick = onOpenAlbum) else Modifier
            )
        )
        Text(
            if (year > 0) "$year · $artistName" else artistName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(onClick = onOpenArtist)
        )
    }
}

@Composable
private fun ArtistPlaylistCard(
    name: String,
    artworkUrl: String,
    onOpen: () -> Unit
) {
    Column(Modifier.width(190.dp).clickable(onClick = onOpen)) {
        ArtistArtwork(artworkUrl, name)
        Text(
            name,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 9.dp)
        )
        Text(
            "Apple Music",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ArtistSimilarArtistCard(
    name: String,
    artworkUrl: String,
    onOpen: () -> Unit
) {
    Column(
        Modifier.width(112.dp).clickable(onClick = onOpen),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = artistShelfArtworkUrl(artworkUrl, 360),
            contentDescription = name,
            modifier = Modifier.size(104.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Text(
            name,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 9.dp),
            textAlign = TextAlign.Center
        )
    }
}

private val artistArtworkSizePattern = Regex("/\\d+x\\d+bb(?=\\.)")

@Composable
private fun ArtistArtwork(
    url: String,
    description: String,
    aspectRatio: Float = 1f,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = artistShelfArtworkUrl(url, 480),
        contentDescription = description,
        modifier = modifier.fillMaxWidth().aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentScale = ContentScale.Crop
    )
}

private fun artistShelfArtworkUrl(url: String, size: Int): String {
    if (url.isBlank()) return url
    return url.replace("{w}", size.toString())
        .replace("{h}", size.toString())
        .replace(artistArtworkSizePattern, "/${size}x${size}bb")
}

@Composable
internal fun ArtistFavoriteStar(
    filled: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.minDimension * 0.46f
        val innerRadius = outerRadius * 0.43f
        val vertices = List(10) { index ->
            val angle = -Math.PI / 2.0 + index * Math.PI / 5.0
            val radius = if (index % 2 == 0) outerRadius else innerRadius
            Offset(
                center.x + kotlin.math.cos(angle).toFloat() * radius,
                center.y + kotlin.math.sin(angle).toFloat() * radius
            )
        }
        val incoming = ArrayList<Offset>(vertices.size)
        val outgoing = ArrayList<Offset>(vertices.size)
        vertices.forEachIndexed { index, vertex ->
            val previous = vertices[(index + vertices.size - 1) % vertices.size]
            val next = vertices[(index + 1) % vertices.size]
            val rounding = size.minDimension * if (index % 2 == 0) 0.075f else 0.14f
            val incomingDistance = (vertex - previous).getDistance()
            val outgoingDistance = (next - vertex).getDistance()
            incoming += vertex + (previous - vertex) *
                (minOf(rounding, incomingDistance * 0.32f) / incomingDistance)
            outgoing += vertex + (next - vertex) *
                (minOf(rounding, outgoingDistance * 0.32f) / outgoingDistance)
        }
        val path = Path().apply {
            moveTo(incoming.first().x, incoming.first().y)
            vertices.indices.forEach { index ->
                val vertex = vertices[index]
                cubicTo(
                    vertex.x,
                    vertex.y,
                    vertex.x,
                    vertex.y,
                    outgoing[index].x,
                    outgoing[index].y
                )
                val nextIndex = (index + 1) % vertices.size
                lineTo(incoming[nextIndex].x, incoming[nextIndex].y)
            }
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = if (filled) {
                Fill
            } else {
                Stroke(
                    width = size.minDimension * 0.085f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            }
        )
    }
}

@Composable
private fun AmbientArtistArtwork(
    previewUrl: String,
    videoUrl: String,
    playbackPositionMs: androidx.compose.runtime.MutableLongState,
    isFocused: Boolean,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val context = LocalContext.current
    var player by remember(context, videoUrl) { mutableStateOf<ExoPlayer?>(null) }
    var textureView by remember(videoUrl) { mutableStateOf<TextureView?>(null) }
    var layoutChangeListener by remember(videoUrl) {
        mutableStateOf<android.view.View.OnLayoutChangeListener?>(null)
    }
    var renderedFirstFrame by remember(videoUrl) { mutableStateOf(false) }
    var playbackEnabled by remember(videoUrl) { mutableStateOf(false) }
    LaunchedEffect(videoUrl, isFocused) {
        playbackEnabled = false
        if (!isFocused || !videoUrl.startsWith("https://", ignoreCase = true)) {
            return@LaunchedEffect
        }
        delay(300)
        if (player == null) {
            player = ExoPlayer.Builder(context.applicationContext).build().apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_ONE
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                setMediaItem(MediaItem.fromUri(videoUrl))
                prepare()
                seekTo(playbackPositionMs.longValue.coerceAtLeast(0L))
            }
        }
        playbackEnabled = true
    }
    SideEffect {
        player?.playWhenReady = isFocused && playbackEnabled
    }
    Box(modifier = modifier.clip(androidx.compose.ui.graphics.RectangleShape)) {
        AsyncImage(
            model = previewUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale
        )
        val activePlayer = player
        if (activePlayer != null) {
            DisposableEffect(activePlayer, videoUrl) {
                val listener = object : Player.Listener {
                    override fun onRenderedFirstFrame() {
                        renderedFirstFrame = true
                    }

                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        textureView?.let { updateArtistVideoTransform(it, videoSize) }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.w("CatalogArtistPage", "艺人动态封面播放失败", error)
                    }
                }
                activePlayer.addListener(listener)
                onDispose {
                    playbackPositionMs.longValue =
                        activePlayer.currentPosition.coerceAtLeast(0L)
                    activePlayer.playWhenReady = false
                    activePlayer.removeListener(listener)
                    textureView?.let { view ->
                        layoutChangeListener?.let(view::removeOnLayoutChangeListener)
                        activePlayer.clearVideoTextureView(view)
                    }
                    activePlayer.release()
                    textureView = null
                    layoutChangeListener = null
                }
            }
            AndroidView(
                factory = { viewContext ->
                    TextureView(viewContext).also { view ->
                        textureView = view
                        val listener = android.view.View.OnLayoutChangeListener {
                                changedView, _, _, _, _, _, _, _, _ ->
                            updateArtistVideoTransform(
                                changedView as TextureView,
                                activePlayer.videoSize
                            )
                        }
                        layoutChangeListener = listener
                        view.addOnLayoutChangeListener(listener)
                        activePlayer.setVideoTextureView(view)
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

private fun updateArtistVideoTransform(view: TextureView, videoSize: VideoSize) {
    if (view.width == 0 || view.height == 0 || videoSize.width == 0 || videoSize.height == 0) {
        return
    }
    var videoWidth = videoSize.width * videoSize.pixelWidthHeightRatio
    var videoHeight = videoSize.height.toFloat()
    if (videoSize.unappliedRotationDegrees % 180 != 0) {
        val rotatedWidth = videoHeight
        videoHeight = videoWidth
        videoWidth = rotatedWidth
    }
    val viewAspectRatio = view.width.toFloat() / view.height
    val videoAspectRatio = videoWidth / videoHeight
    val scaleX = if (videoAspectRatio > viewAspectRatio) {
        videoAspectRatio / viewAspectRatio
    } else {
        1f
    }
    val scaleY = if (videoAspectRatio < viewAspectRatio) {
        viewAspectRatio / videoAspectRatio
    } else {
        1f
    }
    view.setTransform(
        Matrix().apply {
            setScale(scaleX, scaleY, view.width / 2f, view.height / 2f)
        }
    )
}
