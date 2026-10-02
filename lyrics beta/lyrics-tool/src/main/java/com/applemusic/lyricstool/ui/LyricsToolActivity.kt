package com.applemusic.lyricstool.ui

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.applemusic.android.apple.MusicRepository
import com.applemusic.android.player.LiveLyricsNotification
import com.applemusic.android.player.MusicService
import com.applemusic.android.player.PlaybackEngine
import com.applemusic.android.util.Logx
import com.applemusic.lyricstool.data.AppleLyricsClient
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class LyricsToolActivity : ComponentActivity() {
    private val lyricsClient by lazy { AppleLyricsClient(applicationContext) }
    private var nowPlayingRequest by mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            LyricsToolScreen(lyricsClient, nowPlayingRequest)
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
        PlaybackEngine.onStateChanged = null
        super.onDestroy()
    }
}
