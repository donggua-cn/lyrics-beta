package com.applemusic.lyricstool.ui

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.view.FrameMetrics
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.applemusic.android.apple.MusicRepository
import com.applemusic.android.player.LiveLyricsNotification
import com.applemusic.android.player.MusicService
import com.applemusic.android.player.PlaybackEngine
import com.applemusic.android.util.Logx
import com.applemusic.lyricstool.BuildConfig
import com.applemusic.lyricstool.data.AppleLyricsClient
import com.applemusic.lyricstool.data.QQMusicLyricsClient
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.io.File
import java.io.IOException

class LyricsToolActivity : ComponentActivity() {
    private val lyricsClient by lazy { AppleLyricsClient(applicationContext) }
    private val qqMusicLyricsClient by lazy { QQMusicLyricsClient() }
    private var nowPlayingRequest by mutableLongStateOf(0L)
    private var frameJankMonitor: FrameJankMonitor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            frameJankMonitor = FrameJankMonitor(
                window,
                File(cacheDir, "frame-jank.log")
            ).also { it.start() }
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        Logx.init(filesDir)
        MusicRepository.init(applicationContext)
        PlaybackEngine.init(applicationContext)
        startService(android.content.Intent(this, MusicService::class.java))
        LiveLyricsNotification.start(applicationContext)
        recordNowPlayingRequest(intent)
        setContent {
            LyricsToolScreen(lyricsClient, qqMusicLyricsClient, nowPlayingRequest)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recordNowPlayingRequest(intent)
    }

    private fun recordNowPlayingRequest(intent: android.content.Intent?) {
        if (intent?.action == PlaybackEngine.ACTION_OPEN_NOW_PLAYING) {
            nowPlayingRequest = SystemClock.elapsedRealtime()
        }
    }

    override fun onDestroy() {
        frameJankMonitor?.stop()
        frameJankMonitor = null
        PlaybackEngine.onStateChanged = null
        super.onDestroy()
    }
}

internal object ArtistScrollDiagnostics {
    @Volatile
    var visibleItems: String = "unknown"

    @Volatile
    var isScrolling: Boolean = false
}

private class FrameJankMonitor(
    private val window: Window,
    private val logFile: File
) {
    private val thread = HandlerThread("FrameJankMonitor")
    private lateinit var handler: Handler
    private var windowStartNs = 0L
    private var jankyFrames = 0
    private var droppedReports = 0
    private var maxTotalNs = 0L
    private var maxLayoutNs = 0L
    private var maxDrawNs = 0L
    private var maxSyncNs = 0L
    private var maxGpuNs = 0L
    private val severeFrames = ArrayList<String>()

    private val listener = Window.OnFrameMetricsAvailableListener { _, metrics, dropped ->
        val totalNs = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
        val refreshRate = window.decorView.display?.refreshRate ?: 60f
        val frameBudgetNs = (1_000_000_000L / refreshRate.coerceAtLeast(1f)).toLong()
        if (totalNs > frameBudgetNs) {
            jankyFrames++
            maxTotalNs = maxOf(maxTotalNs, totalNs)
            maxLayoutNs = maxOf(maxLayoutNs, metrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION))
            maxDrawNs = maxOf(maxDrawNs, metrics.getMetric(FrameMetrics.DRAW_DURATION))
            maxSyncNs = maxOf(maxSyncNs, metrics.getMetric(FrameMetrics.SYNC_DURATION))
            maxGpuNs = maxOf(maxGpuNs, metrics.getMetric(FrameMetrics.GPU_DURATION))
            if (totalNs >= frameBudgetNs * 3 && severeFrames.size < MAX_SEVERE_FRAMES_PER_WINDOW) {
                severeFrames +=
                    "total=${totalNs / 1_000_000}ms " +
                        "input=${metrics.getMetric(FrameMetrics.INPUT_HANDLING_DURATION) / 1_000_000}ms " +
                        "animation=${metrics.getMetric(FrameMetrics.ANIMATION_DURATION) / 1_000_000}ms " +
                        "layout=${metrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION) / 1_000_000}ms " +
                        "draw=${metrics.getMetric(FrameMetrics.DRAW_DURATION) / 1_000_000}ms " +
                        "command=${metrics.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION) / 1_000_000}ms " +
                        "swap=${metrics.getMetric(FrameMetrics.SWAP_BUFFERS_DURATION) / 1_000_000}ms " +
                        "unknown=${metrics.getMetric(FrameMetrics.UNKNOWN_DELAY_DURATION) / 1_000_000}ms " +
                        "gpu=${metrics.getMetric(FrameMetrics.GPU_DURATION) / 1_000_000}ms " +
                        "scroll=${ArtistScrollDiagnostics.isScrolling} " +
                        "artistItems=${ArtistScrollDiagnostics.visibleItems}"
            }
        }
        droppedReports += dropped

        val now = SystemClock.elapsedRealtimeNanos()
        if (windowStartNs == 0L) windowStartNs = now
        if (now - windowStartNs >= REPORT_INTERVAL_NS) {
            if (jankyFrames > 0 || droppedReports > 0) {
                val report =
                    "window=${(now - windowStartNs) / 1_000_000}ms " +
                        "jank=$jankyFrames droppedReports=$droppedReports " +
                        "maxTotal=${maxTotalNs / 1_000_000}ms " +
                        "maxLayout=${maxLayoutNs / 1_000_000}ms " +
                        "maxDraw=${maxDrawNs / 1_000_000}ms " +
                        "maxSync=${maxSyncNs / 1_000_000}ms " +
                        "maxGpu=${maxGpuNs / 1_000_000}ms " +
                        "artistItems=${ArtistScrollDiagnostics.visibleItems} " +
                        "severeFrames=${severeFrames.joinToString(separator = " | ")}"
                Log.w(TAG, report)
                try {
                    logFile.appendText("${SystemClock.elapsedRealtime()} $report\n")
                } catch (error: IOException) {
                    Log.e(TAG, "Unable to write frame diagnostics", error)
                }
            }
            windowStartNs = now
            jankyFrames = 0
            droppedReports = 0
            maxTotalNs = 0L
            maxLayoutNs = 0L
            maxDrawNs = 0L
            maxSyncNs = 0L
            maxGpuNs = 0L
            severeFrames.clear()
        }
    }

    fun start() {
        thread.start()
        handler = Handler(thread.looper)
        window.addOnFrameMetricsAvailableListener(listener, handler)
        val refreshRate = window.decorView.display?.refreshRate
        val message = "Frame metrics monitor enabled; refresh=${refreshRate}Hz"
        Log.i(TAG, message)
        try {
            logFile.writeText("${SystemClock.elapsedRealtime()} $message\n")
        } catch (error: IOException) {
            Log.e(TAG, "Unable to initialize frame diagnostics", error)
        }
    }

    fun stop() {
        window.removeOnFrameMetricsAvailableListener(listener)
        thread.quitSafely()
    }

    private companion object {
        const val TAG = "FrameJank"
        const val REPORT_INTERVAL_NS = 2_000_000_000L
        const val MAX_SEVERE_FRAMES_PER_WINDOW = 10
    }
}
