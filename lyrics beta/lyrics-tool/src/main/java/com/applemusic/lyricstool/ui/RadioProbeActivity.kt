package com.applemusic.lyricstool.ui

import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.applemusic.android.apple.AppleApi
import com.applemusic.android.apple.AppleLibrarySync
import com.applemusic.android.apple.MusicRepository
import com.applemusic.android.apple.ShelfItem
import com.applemusic.android.player.PlaybackEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class RadioProbeActivity : ComponentActivity() {
    companion object {
        const val EXTRA_STATION_ID = "com.applemusic.lyricstool.extra.STATION_ID"
        const val EXTRA_AUTO_PLAY = "com.applemusic.lyricstool.extra.AUTO_PLAY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requestedStationId = intent.getStringExtra(EXTRA_STATION_ID)
        val autoPlay = intent.getBooleanExtra(EXTRA_AUTO_PLAY, false)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RadioProbeScreen(
                        onBack = ::finish,
                        requestedStationId = requestedStationId,
                        autoPlay = autoPlay
                    )
                }
            }
        }
    }
}

private data class RadioProbeTrack(
    val id: String,
    val title: String,
    val artist: String,
    val artwork: String
)

private data class RadioProbeResult(
    val label: String,
    val stationId: String? = null,
    val path: String? = null,
    val statusCode: Int? = null,
    val tracks: List<RadioProbeTrack> = emptyList(),
    val parseWarning: String? = null,
    val responsePreview: String = "",
    val error: String? = null
)

internal data class RadioStartBatch(
    val firstResponse: AppleApi.StationProbeResponse,
    val rows: List<AppleLibrarySync.SongRow>,
    val warning: String? = null
)

internal fun restoreNativeRadioPrefetch(context: Context, api: AppleApi) {
    NativeRadioPlayback.restore(context.applicationContext, api)
}

@Composable
private fun RadioProbeScreen(
    onBack: () -> Unit,
    requestedStationId: String? = null,
    autoPlay: Boolean = false
) {
    val context = LocalContext.current
    val api = MusicRepository.api
    val scope = rememberCoroutineScope()
    var stations by remember { mutableStateOf<List<ShelfItem>>(emptyList()) }
    var selectedStation by remember { mutableStateOf<ShelfItem?>(null) }
    var stationMenuExpanded by remember { mutableStateOf(false) }
    var loadingStations by remember { mutableStateOf(false) }
    var probing by remember { mutableStateOf(false) }
    var prefetchStopRequested by remember { mutableStateOf(false) }
    val prefetchStopSignal = remember { AtomicBoolean(false) }
    var preparingTrackId by remember { mutableStateOf<String?>(null) }
    var startingRadio by remember { mutableStateOf(false) }
    var radioActive by remember { mutableStateOf(false) }
    var favoritedTrackIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var favoritePendingTrackId by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf("") }
    var playbackMessage by remember { mutableStateOf("") }
    var favoriteMessage by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<RadioProbeResult>>(emptyList()) }
    var queueingResultLabel by remember { mutableStateOf<String?>(null) }
    var queuedResultLabels by remember { mutableStateOf<Set<String>>(emptySet()) }

    val loadStations: () -> Unit = {
        if (!loadingStations) {
            scope.launch {
                loadingStations = true
                statusMessage = ""
                try {
                    val loaded = withContext(Dispatchers.IO) {
                        api.getRecommendations()
                            .flatMap { it.items }
                            .filter(ShelfItem::isStation)
                            .distinctBy(ShelfItem::id)
                    }
                    stations = loaded
                    selectedStation = if (requestedStationId != null) {
                        loaded.firstOrNull { it.id == requestedStationId }
                    } else {
                        loaded.firstOrNull()
                    }
                    statusMessage = when {
                        loaded.isEmpty() ->
                            "推荐页没有返回电台卡片；请确认账号登录状态和 Apple Music storefront。"
                        requestedStationId != null && selectedStation == null ->
                            "没有在 Apple Music 推荐中找到对应电台（ID：$requestedStationId），未切换到其他电台。"
                        requestedStationId != null ->
                            "已绑定电台：${selectedStation?.title}（ID 与主页卡片一致）"
                        else -> "找到 ${loaded.size} 个电台。"
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    statusMessage = "读取个性推荐失败：${error.message ?: error.javaClass.simpleName}"
                } finally {
                    loadingStations = false
                }
            }
        }
    }

    val loginLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        api.restore()
        if (api.isAuthorized()) loadStations()
        else statusMessage = "未完成 Apple Music 登录，无法读取个性电台。"
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (api.isAuthorized()) {
            loadStations()
        } else {
            statusMessage = "请先连接 Apple Music，然后读取账号的个性电台。"
        }
    }

    val runProbe: () -> Unit = {
        val station = selectedStation
        if (!probing && station != null) {
            scope.launch {
                prefetchStopSignal.set(false)
                prefetchStopRequested = false
                probing = true
                try {
                    val result = try {
                        val prefetched = withContext(Dispatchers.IO) {
                            val firstResponse = api.probeStationNextTracks(station.id)
                            if (firstResponse.statusCode !in 200..299) {
                                throw IllegalStateException(
                                    "HTTP ${firstResponse.statusCode}：${firstResponse.body.take(300)}"
                                )
                            }
                            val tracks = LinkedHashMap<String, RadioProbeTrack>()
                            val firstParsed = parseRadioTracks(firstResponse.body)
                            firstParsed.first.forEach { tracks.putIfAbsent(it.id, it) }
                            var duplicateResponses = 0
                            var requests = 0
                            var warning = firstParsed.second
                            while (
                                tracks.size < MIN_RADIO_QUEUE_TRACKS &&
                                requests < MAX_RADIO_PREFETCH_REQUESTS &&
                                !prefetchStopSignal.get()
                            ) {
                                requests++
                                val response = api.probeStationNextTracks(station.id, limit = 1)
                                if (response.statusCode !in 200..299) {
                                    warning = "后续请求失败，HTTP ${response.statusCode}"
                                    break
                                }
                                val nextParsed = parseRadioTracks(response.body)
                                val before = tracks.size
                                nextParsed.first.forEach { tracks.putIfAbsent(it.id, it) }
                                if (tracks.size == before) {
                                    duplicateResponses++
                                    if (duplicateResponses >= MAX_RADIO_DUPLICATE_RESPONSES) {
                                        warning = "Apple 连续返回重复歌曲，停止预取"
                                        break
                                    }
                                } else {
                                    duplicateResponses = 0
                                }
                                if (nextParsed.first.isEmpty()) {
                                    warning = nextParsed.second ?: "Apple 没有返回后续歌曲"
                                    break
                                }
                            }
                            if (prefetchStopSignal.get()) warning = "已手动停止预取"
                            else if (
                                tracks.size < MIN_RADIO_QUEUE_TRACKS &&
                                warning == null
                            ) {
                                warning = "已达到预取次数上限，当前仅获得 ${tracks.size} 首"
                            }
                            Triple(firstResponse, tracks.values.toList(), warning)
                        }
                        val localized = withContext(Dispatchers.IO) {
                            localizeRadioTracks(api, prefetched.second)
                        }
                        val queuedRows = localized.first.map { track ->
                            AppleLibrarySync.SongRow().apply {
                                appleId = track.id
                                catalogId = track.id
                                uri = "apple:${track.id}"
                                title = track.title.ifBlank { "未知歌曲" }
                                artist = track.artist.ifBlank { "未知艺人" }
                                coverUrl = track.artwork
                                isLibrary = false
                            }
                        }
                        val addedToQueue = if (queuedRows.isNotEmpty()) {
                            NativeRadioPlayback.enqueue(
                                context.applicationContext,
                                api,
                                station.id,
                                queuedRows,
                                enablePrefetch = !prefetchStopSignal.get()
                            )
                        } else {
                            0
                        }
                        if (queuedRows.isNotEmpty()) {
                            radioActive = !prefetchStopSignal.get()
                            queuedResultLabels = queuedResultLabels + "网页电台续播响应"
                            playbackMessage =
                                "已自动将 ${queuedRows.size} 首歌曲加入播放队列（新增 $addedToQueue 首）；" +
                                    "未下载的歌曲会在播放时再准备。" +
                                    (prefetched.third?.let { "\n$it" } ?: "")
                        }
                        RadioProbeResult(
                            label = "网页电台续播响应",
                            stationId = station.id,
                            path = "POST ${prefetched.first.path}",
                            statusCode = prefetched.first.statusCode,
                            tracks = localized.first,
                            parseWarning = listOfNotNull(prefetched.third, localized.second)
                                .joinToString("\n")
                                .ifEmpty { null },
                            responsePreview = prefetched.first.body.take(MAX_RESPONSE_PREVIEW_CHARS)
                        )
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        RadioProbeResult(
                            label = "网页电台续播响应",
                            stationId = station.id,
                            error = error.message ?: error.javaClass.simpleName
                        )
                    }
                    results = results + result
                } finally {
                    probing = false
                    prefetchStopRequested = false
                }
            }
        }
    }

    val playTrack: (RadioProbeTrack) -> Unit = { track ->
        if (preparingTrackId == null) {
            scope.launch {
                NativeRadioPlayback.stop(context.applicationContext)
                radioActive = false
                preparingTrackId = track.id
                playbackMessage = "正在读取歌曲详情…"
                try {
                    val row = withContext(Dispatchers.IO) {
                        val song = api.getCatalogSongLocalized(track.id)
                            ?: throw IllegalStateException("Apple Music 没有返回这首歌的目录详情")
                        AppleLibrarySync.toRow(song).also { it.isLibrary = false }
                    }
                    MusicRepository.registerTransient(listOf(row))
                    PlaybackEngine.playSingle(row.uri)
                    playbackMessage =
                        "已请求播放：${row.title} — ${row.artist}。正在准备音频；若失败，主界面会显示原因。"
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    playbackMessage =
                        "无法播放 ${track.title}：${error.message ?: error.javaClass.simpleName}"
                } finally {
                    preparingTrackId = null
                }
            }
        }
    }

    val favoriteTrack: (RadioProbeTrack) -> Unit = { track ->
        if (favoritePendingTrackId == null) {
            scope.launch {
                favoritePendingTrackId = track.id
                favoriteMessage = ""
                val favorite = track.id !in favoritedTrackIds
                try {
                    withContext(Dispatchers.IO) {
                        api.setCatalogSongFavorite(track.id, favorite)
                    }
                    favoritedTrackIds = if (favorite) {
                        favoritedTrackIds + track.id
                    } else {
                        favoritedTrackIds - track.id
                    }
                    favoriteMessage = if (favorite) {
                        "Apple Music 已接受收藏：${track.title}"
                    } else {
                        "已从 Apple Music 收藏中移除：${track.title}"
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    favoriteMessage =
                        "${if (favorite) "收藏" else "取消收藏"}失败：" +
                            (error.message ?: error.javaClass.simpleName)
                } finally {
                    favoritePendingTrackId = null
                }
            }
        }
    }

    val startRadio: () -> Unit = {
        val station = selectedStation
        if (!startingRadio && station != null) {
            scope.launch {
                prefetchStopSignal.set(false)
                prefetchStopRequested = false
                startingRadio = true
                playbackMessage = "正在向 Apple Music 请求电台首批曲目…"
                radioActive = false
                try {
                    val started = startRecommendedRadio(
                        context.applicationContext,
                        api,
                        station.id,
                        prefetchStopSignal
                    )
                    results = listOf(
                        RadioProbeResult(
                            label = "原生电台首批响应",
                            path = "POST ${started.firstResponse.path}",
                            statusCode = started.firstResponse.statusCode,
                            tracks = started.rows.map { row ->
                                RadioProbeTrack(row.appleId, row.title, row.artist, row.coverUrl)
                            },
                            parseWarning = started.warning,
                            responsePreview = started.firstResponse.body.take(MAX_RESPONSE_PREVIEW_CHARS)
                        )
                    )
                    radioActive = true
                    playbackMessage =
                        "Apple 原生电台已开始：共 ${started.rows.size} 首已加入队列，" +
                            "其中 ${maxOf(0, started.rows.size - 1)} 首为后续曲目。" +
                            (started.warning?.let { "\n$it" } ?: "")
                    if (autoPlay) onBack()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    playbackMessage =
                        "无法启动原生电台：${error.message ?: error.javaClass.simpleName}"
                } finally {
                    startingRadio = false
                    prefetchStopRequested = false
                }
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(autoPlay, selectedStation?.id, loadingStations) {
        if (autoPlay && selectedStation != null && !loadingStations) {
            startRadio()
        }
    }

    val stopPrefetch: () -> Unit = {
        prefetchStopRequested = true
        prefetchStopSignal.set(true)
        NativeRadioPlayback.stopPrefetch()
        if (radioActive) {
            playbackMessage = "已停止自动预取；当前队列仍可播放完。"
        }
    }

    if (autoPlay) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (
                    !loadingStations &&
                    selectedStation == null &&
                    statusMessage.isNotBlank()
                ) {
                    Text(statusMessage, color = MaterialTheme.colorScheme.error)
                } else if (startingRadio || loadingStations || selectedStation == null) {
                    CircularProgressIndicator()
                    Text(
                        if (startingRadio) "正在启动电台…" else "正在连接 Apple Music 电台…",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                } else {
                    Text(
                        playbackMessage.ifBlank { statusMessage },
                        color = if (playbackMessage.startsWith("无法启动")) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    } else Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) { Text("返回") }
        Text("个性电台接口测试", style = MaterialTheme.typography.headlineSmall)
        Text(
            "直接使用 Apple 个性电台的 next-tracks 获取曲目。开始后播放 Apple 返回的歌曲，" +
                "队列快用完时继续请求下一首；歌曲名称优先简体中文，没有时使用商店地区语言。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!api.isAuthorized()) {
            Button(
                onClick = {
                    loginLauncher.launch(Intent(context, AppleMusicLoginActivity::class.java))
                }
            ) {
                Text("连接 Apple Music")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { stationMenuExpanded = true },
                enabled = stations.isNotEmpty()
            ) {
                Text(selectedStation?.title ?: "选择电台")
            }
            DropdownMenu(
                expanded = stationMenuExpanded,
                onDismissRequest = { stationMenuExpanded = false }
            ) {
                stations.forEach { station ->
                    DropdownMenuItem(
                        text = { Text("${station.title} · ${station.subtitle}") },
                        onClick = {
                            selectedStation = station
                            stationMenuExpanded = false
                        }
                    )
                }
            }
            OutlinedButton(
                onClick = loadStations,
                enabled = !loadingStations && api.isAuthorized()
            ) {
                Text(if (loadingStations) "读取中…" else "刷新电台")
            }
        }

        Text(
            selectedStation?.let { "电台 ID：${it.id}" } ?: "尚未选择电台",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    results = emptyList()
                    runProbe()
                },
                enabled = !probing && selectedStation != null && api.isAuthorized()
            ) {
                Text(if (probing) "正在预取…" else "获取电台曲目")
            }
            OutlinedButton(
                onClick = stopPrefetch,
                enabled = probing || startingRadio || radioActive
            ) {
                Text(if (prefetchStopRequested) "正在停止…" else "停止预取")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = startRadio,
                enabled = !startingRadio && selectedStation != null && api.isAuthorized()
            ) {
                Text(if (startingRadio) "正在启动…" else "开始 Apple 原生电台")
            }
        }

        if (probing) {
            Text("正在请求 Apple Music…", color = MaterialTheme.colorScheme.primary)
        }
        if (statusMessage.isNotEmpty()) {
            Text(statusMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        results.forEach { result ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(result.label, style = MaterialTheme.typography.titleMedium)
                    when {
                        result.error != null -> {
                            Text(
                                "请求失败：${result.error}",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {
                            Text("HTTP ${result.statusCode} · 识别到 ${result.tracks.size} 首歌曲")
                            result.path?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                            if (result.tracks.isNotEmpty() && result.stationId != null) {
                                OutlinedButton(
                                    onClick = {
                                        if (queueingResultLabel == null) {
                                            scope.launch {
                                                queueingResultLabel = result.label
                                                try {
                                                    val rows = withContext(Dispatchers.IO) {
                                                        result.tracks.map { track ->
                                                            val song = api.getCatalogSongLocalized(track.id)
                                                                ?: throw IllegalStateException(
                                                                    "无法读取歌曲目录详情：${track.title}"
                                                                )
                                                            AppleLibrarySync.toRow(song).also {
                                                                it.isLibrary = false
                                                            }
                                                        }
                                                    }
                                                    val added = NativeRadioPlayback.enqueue(
                                                        context.applicationContext,
                                                        api,
                                                        result.stationId,
                                                        rows
                                                    )
                                                    radioActive = true
                                                    queuedResultLabels = queuedResultLabels + result.label
                                                    playbackMessage = if (added > 0) {
                                                        "已将 ${rows.size} 首歌曲加入播放队列（新增 $added 首）。未下载的歌曲会在播放时再准备。"
                                                    } else {
                                                        "这些歌曲已在当前播放队列中。"
                                                    }
                                                } catch (error: CancellationException) {
                                                    throw error
                                                } catch (error: Exception) {
                                                    playbackMessage =
                                                        "加入播放队列失败：${error.message ?: error.javaClass.simpleName}"
                                                } finally {
                                                    queueingResultLabel = null
                                                }
                                            }
                                        }
                                    },
                                    enabled = queueingResultLabel == null
                                ) {
                                    Text(
                                        when {
                                            queueingResultLabel == result.label -> "加入中…"
                                            result.label in queuedResultLabels -> "再次加入播放队列"
                                            else -> "加入播放队列"
                                        }
                                    )
                                }
                            }
                            result.tracks.forEachIndexed { index, track ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${index + 1}. ${track.title} — ${track.artist}",
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = { playTrack(track) },
                                        enabled = preparingTrackId == null
                                    ) {
                                        Text(
                                            if (preparingTrackId == track.id) "准备中…" else "播放"
                                        )
                                    }
                                    IconButton(
                                        onClick = { favoriteTrack(track) },
                                        enabled = favoritePendingTrackId == null
                                    ) {
                                        Icon(
                                            imageVector = if (track.id in favoritedTrackIds) {
                                                Icons.Filled.Star
                                            } else {
                                                Icons.Outlined.StarBorder
                                            },
                                            contentDescription = if (
                                                track.id in favoritedTrackIds
                                            ) {
                                                "取消收藏"
                                            } else {
                                                "收藏到资料库"
                                            },
                                            tint = if (track.id in favoritedTrackIds) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }
                            result.parseWarning?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                            if (result.responsePreview.isNotEmpty()) {
                                Text(
                                    "响应片段：\n${result.responsePreview}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (playbackMessage.isNotEmpty()) {
                        Text(
                            playbackMessage,
                            color = if (playbackMessage.startsWith("无法播放")) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                    if (favoriteMessage.isNotEmpty()) {
                        Text(
                            favoriteMessage,
                            color = if (
                                favoriteMessage.startsWith("收藏失败") ||
                                favoriteMessage.startsWith("取消收藏失败")
                            ) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun parseRadioTracks(body: String): Pair<List<RadioProbeTrack>, String?> {
    val root = try {
        JSONObject(body)
    } catch (error: Exception) {
        return emptyList<RadioProbeTrack>() to
            "响应不是有效 JSON：${error.message ?: error.javaClass.simpleName}"
    }
    val found = LinkedHashMap<String, RadioProbeTrack>()
    fun visit(value: Any?) {
        when (value) {
            is JSONObject -> {
                val type = value.optString("type")
                val attributes = value.optJSONObject("attributes")
                if (attributes != null && (
                        type.contains("song", ignoreCase = true) ||
                            (attributes.has("artistName") &&
                                (attributes.has("durationInMillis") || attributes.has("playParams")))
                        )
                ) {
                    val title = attributes.optString("name")
                    val playParams = attributes.optJSONObject("playParams")
                    val id = playParams?.optString("catalogId").orEmpty()
                        .ifBlank { value.optString("id") }
                    if (title.isNotBlank() && id.isNotBlank()) {
                        val artwork = attributes.optJSONObject("artwork")
                            ?.optString("url", "")
                            .orEmpty()
                            .replace("{w}", "600")
                            .replace("{h}", "600")
                            .replace("{f}", "jpg")
                        found.putIfAbsent(
                            id,
                            RadioProbeTrack(
                                id,
                                title,
                                attributes.optString("artistName", "未知艺人"),
                                artwork
                            )
                        )
                    }
                }
                val keys = value.keys()
                while (keys.hasNext()) visit(value.opt(keys.next()))
            }
            is JSONArray -> {
                for (index in 0 until value.length()) visit(value.opt(index))
            }
        }
    }
    visit(root)
    val warning = if (found.isEmpty()) {
        "响应中没有识别到标准歌曲对象；请结合上方原始响应判断接口结构。"
    } else {
        null
    }
    return found.values.toList() to warning
}

private fun localizeRadioTracks(
    api: AppleApi,
    tracks: List<RadioProbeTrack>
): Pair<List<RadioProbeTrack>, String?> {
    val warnings = ArrayList<String>()
    val localized = tracks.map { track ->
        try {
            val song = api.getCatalogSongLocalized(track.id)
                ?: throw IllegalStateException("Apple Music 没有返回目录歌曲")
            track.copy(
                title = song.name.ifBlank { track.title },
                artist = song.artistName.ifBlank { track.artist },
                artwork = song.artworkUrl.ifBlank { track.artwork }
            )
        } catch (error: Exception) {
            warnings.add(
                "${track.title} 元数据本地化失败：${error.message ?: error.javaClass.simpleName}"
            )
            track
        }
    }
    return localized to warnings.take(3).joinToString("\n").ifEmpty { null }
}

internal suspend fun startRecommendedRadio(
    context: Context,
    api: AppleApi,
    stationId: String,
    stopSignal: AtomicBoolean = AtomicBoolean(false)
): RadioStartBatch {
    NativeRadioPlayback.stop(context.applicationContext)
    val started = withContext(Dispatchers.IO) {
        val firstResponse = api.probeStationNextTracks(stationId)
        if (firstResponse.statusCode !in 200..299) {
            throw IllegalStateException(
                "电台首批请求失败，HTTP ${firstResponse.statusCode}：" +
                    firstResponse.body.take(300)
            )
        }
        val firstTracks = parseRadioTracks(firstResponse.body).first
        if (firstTracks.isEmpty()) {
            throw IllegalStateException("Apple Music 没有在首批响应中返回可播放歌曲")
        }
        val rows = ArrayList<AppleLibrarySync.SongRow>()
        val seen = firstTracks.mapTo(HashSet<String>()) { it.id }
        firstTracks.forEach { track ->
            val song = api.getCatalogSongLocalized(track.id)
                ?: throw IllegalStateException("无法读取歌曲目录详情：${track.id}")
            rows.add(AppleLibrarySync.toRow(song).also { it.isLibrary = false })
        }
        var duplicateResponses = 0
        var requests = 0
        var warning: String? = null
        while (
            rows.size < MIN_RADIO_QUEUE_TRACKS &&
            requests < MAX_RADIO_PREFETCH_REQUESTS &&
            !stopSignal.get()
        ) {
            requests++
            val requested = (MIN_RADIO_QUEUE_TRACKS - rows.size).coerceAtLeast(1)
            val nextResponse = api.probeStationNextTracks(stationId, limit = requested)
            if (nextResponse.statusCode !in 200..299) {
                warning = "后续预取失败，HTTP ${nextResponse.statusCode}"
                break
            }
            val nextTracks = parseRadioTracks(nextResponse.body).first
            if (nextTracks.isEmpty()) {
                warning = "Apple 没有再返回后续歌曲"
                break
            }
            var addedFromResponse = 0
            for (nextTrack in nextTracks) {
                if (!seen.add(nextTrack.id)) continue
                val song = api.getCatalogSongLocalized(nextTrack.id)
                    ?: throw IllegalStateException("无法读取后续歌曲详情：${nextTrack.id}")
                rows.add(AppleLibrarySync.toRow(song).also { it.isLibrary = false })
                addedFromResponse++
                if (rows.size >= MIN_RADIO_QUEUE_TRACKS) break
            }
            if (addedFromResponse == 0) {
                duplicateResponses++
                if (duplicateResponses >= MAX_RADIO_DUPLICATE_RESPONSES) {
                    warning = "Apple 连续返回重复歌曲，预取已停止"
                    break
                }
            } else {
                duplicateResponses = 0
            }
        }
        if (stopSignal.get()) {
            warning = "已手动停止预取"
        } else if (rows.size < MIN_RADIO_QUEUE_TRACKS && warning == null) {
            warning = "最多预取 $MAX_RADIO_PREFETCH_REQUESTS 次后仍不足 $MIN_RADIO_QUEUE_TRACKS 首"
        }
        RadioStartBatch(firstResponse, rows, warning)
    }
    NativeRadioPlayback.start(
        context.applicationContext,
        api,
        stationId,
        started.rows,
        enablePrefetch = !stopSignal.get()
    )
    return started
}

private object NativeRadioPlayback {
    private const val RADIO_PREFERENCES = "native_radio_playback"
    private const val RADIO_STATION_KEY = "station_id"
    private const val RADIO_PREFETCH_KEY = "prefetch_enabled"

    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "apple-radio-next").apply { isDaemon = true }
    }
    private val fetching = AtomicBoolean(false)
    private val generation = java.util.concurrent.atomic.AtomicLong(0L)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val seenSongIds = HashSet<String>()

    @Volatile
    private var activeStationId = ""

    @Volatile
    private var api: AppleApi? = null

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var prefetchEnabled = false

    fun enqueue(
        context: Context,
        stationApi: AppleApi,
        stationId: String,
        rows: List<AppleLibrarySync.SongRow>,
        enablePrefetch: Boolean = true
    ): Int {
        require(rows.isNotEmpty()) { "没有可加入队列的歌曲" }
        appContext = context.applicationContext
        api = stationApi
        activeStationId = stationId
        prefetchEnabled = enablePrefetch
        persistRadioState()
        generation.incrementAndGet()
        synchronized(seenSongIds) {
            seenSongIds.clear()
            PlaybackEngine.queueSnapshot().forEach { uri ->
                MusicRepository.findSong(uri)?.let { row ->
                    seenSongIds.add(row.appleId.ifBlank { row.catalogId })
                }
            }
            rows.forEach { row -> seenSongIds.add(row.appleId.ifBlank { row.catalogId }) }
        }
        MusicRepository.registerTransient(rows)
        PlaybackEngine.onQueueLow = if (enablePrefetch) {
            { requestNextBatch(MIN_RADIO_UPCOMING_TRACKS) }
        } else {
            null
        }
        val currentQueue = PlaybackEngine.queueSnapshot()
        return if (currentQueue.isEmpty()) {
            PlaybackEngine.playQueue(rows.map(AppleLibrarySync.SongRow::uri), 0)
            rows.size
        } else {
            PlaybackEngine.appendToQueue(rows.map(AppleLibrarySync.SongRow::uri))
        }
    }

    fun start(
        context: Context,
        stationApi: AppleApi,
        stationId: String,
        initialRows: List<AppleLibrarySync.SongRow>,
        enablePrefetch: Boolean = true
    ) {
        require(initialRows.isNotEmpty()) { "Apple 电台没有返回可播放歌曲" }
        stop(context)
        synchronized(seenSongIds) {
            seenSongIds.clear()
            initialRows.forEach { seenSongIds.add(it.appleId) }
        }
        appContext = context.applicationContext
        api = stationApi
        activeStationId = stationId
        prefetchEnabled = enablePrefetch
        persistRadioState()
        generation.incrementAndGet()
        MusicRepository.registerTransient(initialRows)
        PlaybackEngine.onQueueLow = if (enablePrefetch) {
            { requestNextBatch(MIN_RADIO_UPCOMING_TRACKS) }
        } else {
            null
        }
        PlaybackEngine.playQueue(initialRows.map(AppleLibrarySync.SongRow::uri), 0)
    }

    fun stopPrefetch() {
        prefetchEnabled = false
        persistRadioState()
        PlaybackEngine.onQueueLow = null
    }

    fun stop(context: Context? = null) {
        activeStationId = ""
        prefetchEnabled = false
        (context?.applicationContext ?: appContext)
            ?.getSharedPreferences(RADIO_PREFERENCES, Context.MODE_PRIVATE)
            ?.edit()
            ?.clear()
            ?.apply()
        generation.incrementAndGet()
        if (PlaybackEngine.onQueueLow != null) {
            PlaybackEngine.onQueueLow = null
        }
    }

    fun restore(context: Context, stationApi: AppleApi) {
        val preferences = context.getSharedPreferences(RADIO_PREFERENCES, Context.MODE_PRIVATE)
        val stationId = preferences.getString(RADIO_STATION_KEY, "").orEmpty()
        if (stationId.isBlank() || !preferences.getBoolean(RADIO_PREFETCH_KEY, false)) return
        if (PlaybackEngine.queueSnapshot().isEmpty()) return

        appContext = context.applicationContext
        api = stationApi
        activeStationId = stationId
        prefetchEnabled = true
        synchronized(seenSongIds) {
            seenSongIds.clear()
            PlaybackEngine.queueSnapshot().forEach { uri ->
                MusicRepository.findSong(uri)?.let { row ->
                    seenSongIds.add(row.appleId.ifBlank { row.catalogId })
                }
            }
        }
        PlaybackEngine.onQueueLow = { requestNextBatch(MIN_RADIO_UPCOMING_TRACKS) }
        if (PlaybackEngine.queueRemaining() <= MIN_RADIO_UPCOMING_TRACKS) {
            requestNextBatch(MIN_RADIO_UPCOMING_TRACKS)
        }
        Log.i("NativeRadioPlayback", "已恢复电台预取：$stationId")
    }

    private fun persistRadioState() {
        val context = appContext ?: return
        context.getSharedPreferences(RADIO_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(RADIO_STATION_KEY, activeStationId)
            .putBoolean(RADIO_PREFETCH_KEY, prefetchEnabled)
            .apply()
    }

    private fun requestNextBatch(minimumUpcoming: Int = MIN_RADIO_UPCOMING_TRACKS) {
        val stationId = activeStationId
        val stationApi = api
        if (
            !prefetchEnabled ||
            stationId.isBlank() ||
            stationApi == null ||
            !fetching.compareAndSet(false, true)
        ) {
            return
        }
        val requestGeneration = generation.get()
        executor.execute {
            try {
                val freshRows = ArrayList<AppleLibrarySync.SongRow>()
                val knownRemaining = PlaybackEngine.queueRemaining()
                var duplicateResponses = 0
                var requests = 0
                var lastError: String? = null
                while (
                    prefetchEnabled &&
                    knownRemaining + freshRows.size < minimumUpcoming &&
                    requests < MAX_RADIO_PREFETCH_REQUESTS
                ) {
                    requests++
                    val requested = (
                        minimumUpcoming - knownRemaining - freshRows.size
                    ).coerceAtLeast(1)
                    val response = stationApi.probeStationNextTracks(
                        stationId,
                        limit = requested
                    )
                    if (response.statusCode !in 200..299) {
                        lastError = "HTTP ${response.statusCode}：${response.body.take(250)}"
                        break
                    }
                    val tracks = parseRadioTracks(response.body).first
                    if (tracks.isEmpty()) {
                        lastError = "Apple 没有返回下一首歌曲"
                        break
                    }
                    var addedFromResponse = 0
                    for (track in tracks) {
                        val isNew = synchronized(seenSongIds) { seenSongIds.add(track.id) }
                        if (!isNew) continue
                        val song = stationApi.getCatalogSongLocalized(track.id)
                            ?: throw IllegalStateException("无法读取下一首歌曲详情：${track.id}")
                        freshRows.add(AppleLibrarySync.toRow(song).also { it.isLibrary = false })
                        addedFromResponse++
                        if (knownRemaining + freshRows.size >= minimumUpcoming) break
                    }
                    if (addedFromResponse == 0) {
                        duplicateResponses++
                        if (duplicateResponses >= MAX_RADIO_DUPLICATE_RESPONSES) {
                            lastError = "Apple 连续返回重复歌曲，无法补足后续队列"
                            break
                        }
                    } else {
                        duplicateResponses = 0
                    }
                }
                if (
                    generation.get() == requestGeneration &&
                    activeStationId == stationId
                ) {
                    if (freshRows.isNotEmpty()) {
                        mainHandler.post {
                            MusicRepository.registerTransient(freshRows)
                            val appended = PlaybackEngine.appendToQueue(
                                freshRows.map(AppleLibrarySync.SongRow::uri)
                            )
                            Log.i(TAG, "Apple 原生电台续入 $appended 首")
                            if (lastError != null) showError("电台队列未能补足：$lastError")
                        }
                    } else if (lastError != null) {
                        showError("电台续歌失败：$lastError")
                    }
                    if (
                        knownRemaining + freshRows.size < minimumUpcoming &&
                        requests >= MAX_RADIO_PREFETCH_REQUESTS
                    ) {
                        showError(
                            "Apple 最多预取 $MAX_RADIO_PREFETCH_REQUESTS 次，" +
                                "队列仍少于 $minimumUpcoming 首后续歌曲"
                        )
                    }
                }
            } catch (error: Exception) {
                if (generation.get() == requestGeneration) {
                    Log.e(TAG, "Apple 原生电台续播失败", error)
                    showError("电台续歌失败：${error.message ?: error.javaClass.simpleName}")
                }
            } finally {
                fetching.set(false)
            }
        }
    }

    private fun showError(message: String) {
        val context = appContext ?: return
        mainHandler.post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    private const val TAG = "NativeRadioPlayback"
}

private const val MAX_RESPONSE_PREVIEW_CHARS = 6000
private const val MIN_RADIO_UPCOMING_TRACKS = 5
private const val MIN_RADIO_QUEUE_TRACKS = MIN_RADIO_UPCOMING_TRACKS + 1
private const val MAX_RADIO_PREFETCH_REQUESTS = 20
private const val MAX_RADIO_DUPLICATE_RESPONSES = 3
