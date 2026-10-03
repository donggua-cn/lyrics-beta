package com.applemusic.lyricstool.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.applemusic.android.apple.AppleApi
import com.applemusic.android.apple.AppleLibrarySync
import com.applemusic.android.apple.MusicRepository
import com.applemusic.android.player.PlaybackEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

internal data class RadioStartBatch(
    val firstResponse: AppleApi.StationProbeResponse,
    val rows: List<AppleLibrarySync.SongRow>,
    val warning: String? = null
)

internal fun restoreNativeRadioPrefetch(context: Context, api: AppleApi) {
    NativeRadioPlayback.restore(context.applicationContext, api)
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

private data class RadioStationTrack(val id: String)

private fun parseRadioTracks(body: String): Pair<List<RadioStationTrack>, String?> {
    val root = try {
        JSONObject(body)
    } catch (error: Exception) {
        return emptyList<RadioStationTrack>() to
            "响应不是有效 JSON：${error.message ?: error.javaClass.simpleName}"
    }
    val found = LinkedHashMap<String, RadioStationTrack>()
    fun visit(value: Any?) {
        when (value) {
            is JSONObject -> {
                val type = value.optString("type")
                val attributes = value.optJSONObject("attributes")
                if (attributes != null && (
                        type.contains("song", ignoreCase = true) ||
                            (attributes.has("artistName") &&
                                (attributes.has("durationInMillis") ||
                                    attributes.has("playParams")))
                        )
                ) {
                    val title = attributes.optString("name")
                    val playParams = attributes.optJSONObject("playParams")
                    val id = playParams?.optString("catalogId").orEmpty()
                        .ifBlank { value.optString("id") }
                    if (title.isNotBlank() && id.isNotBlank()) {
                        found.putIfAbsent(id, RadioStationTrack(id))
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
    val warning = if (found.isEmpty()) "响应中没有可播放歌曲" else null
    return found.values.toList() to warning
}

private object NativeRadioPlayback {
    private const val RADIO_PREFERENCES = "native_radio_playback"
    private const val RADIO_STATION_KEY = "station_id"
    private const val RADIO_PREFETCH_KEY = "prefetch_enabled"

    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "apple-radio-next").apply { isDaemon = true }
    }
    private val fetching = AtomicBoolean(false)
    private val generation = AtomicLong(0L)
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
        Log.i(TAG, "已恢复电台预取：$stationId")
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
                        freshRows.add(
                            AppleLibrarySync.toRow(song).also { it.isLibrary = false }
                        )
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

private const val MIN_RADIO_UPCOMING_TRACKS = 5
private const val MIN_RADIO_QUEUE_TRACKS = MIN_RADIO_UPCOMING_TRACKS + 1
private const val MAX_RADIO_PREFETCH_REQUESTS = 20
private const val MAX_RADIO_DUPLICATE_RESPONSES = 3
