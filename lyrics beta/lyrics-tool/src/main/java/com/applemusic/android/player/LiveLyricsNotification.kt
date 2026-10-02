package com.applemusic.android.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.applemusic.android.lyrics.Lyrics
import com.applemusic.lyricstool.R
import java.lang.reflect.InvocationTargetException

object LiveLyricsNotification {
    private const val TAG = "LiveLyricsNotification"
    private const val CHANNEL_ID = "live_lyrics_spotlight"
    private const val NOTIFICATION_ID = 4102
    private const val UPDATE_INTERVAL_MS = 750L

    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var lyricsSongId: String? = null
    @Volatile private var lyrics: Lyrics? = null
    @Volatile private var enabled = true
    private var appContext: Context? = null
    private var lastSignature: String? = null
    private var lastPromotionStatus: String? = null

    private val updateTask = object : Runnable {
        override fun run() {
            updateNotification()
            handler.postDelayed(this, UPDATE_INTERVAL_MS)
        }
    }

    fun start(context: Context) {
        appContext = context.applicationContext
        enabled = context.getSharedPreferences("lyrics_appearance", Context.MODE_PRIVATE)
            .getBoolean("status_bar_lyrics_enabled", true)
        createChannel(context.applicationContext)
        handler.removeCallbacks(updateTask)
        handler.post(updateTask)
    }

    fun setEnabled(context: Context, value: Boolean) {
        enabled = value
        context.applicationContext
            .getSharedPreferences("lyrics_appearance", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("status_bar_lyrics_enabled", value)
            .apply()
        handler.post { updateNotification() }
    }

    fun setLyrics(songId: String, value: Lyrics?) {
        lyricsSongId = songId
        lyrics = value
        lastSignature = null
        handler.post { updateNotification() }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "正在播放的歌词",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "显示当前播放歌曲的歌词"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                }
            )
        }
    }

    private fun updateNotification() {
        val context = appContext ?: return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!enabled) {
            manager.cancel(NOTIFICATION_ID)
            lastSignature = null
            return
        }
        if (!manager.areNotificationsEnabled()) {
            Log.w(TAG, "Notifications are disabled for this app")
            return
        }
        val song = PlaybackEngine.currentSong()
        val player = PlaybackEngine.player

        if (song == null || player == null || !PlaybackEngine.isPlaying()) {
            manager.cancel(NOTIFICATION_ID)
            lastSignature = null
            return
        }

        val songId = song.appleId.ifBlank {
            song.catalogId.ifBlank { song.uri.removePrefix("apple:") }
        }
        val currentLyrics = lyrics?.takeIf { lyricsSongId == songId }
        val position = PlaybackEngine.positionMs()
        val line = currentLyrics?.let { value ->
            value.lines.getOrNull(value.indexAt(position))
                ?.text
                ?.takeIf { it.isNotBlank() }
        }
        val lyricText = line ?: "正在获取歌词…"
        val signature = "$songId|$lyricText"
        if (signature == lastSignature) return
        lastSignature = signature

        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                action = PlaybackEngine.ACTION_OPEN_NOW_PLAYING
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            ?: Intent(context, MusicService::class.java)
                .setAction(PlaybackEngine.ACTION_OPEN_NOW_PLAYING)
        val contentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_transparent)
            .setWhen(System.currentTimeMillis())
            .setCategory(Notification.CATEGORY_STATUS)
            .setPriority(Notification.PRIORITY_HIGH)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setContentTitle(lyricText)
            .setSubText(null)
            .setContentText(null)
            .setShortCriticalText(lyricText.take(12))

        requestPromotion(builder)
        val notification = builder.build()
        val promotable = notification.hasPromotableCharacteristics()
        val promotionAllowed = manager.canPostPromotedNotifications()
        val promotionStatus =
            "promotable=$promotable, promotionAllowed=$promotionAllowed, flags=${notification.flags}"
        if (promotionStatus != lastPromotionStatus) {
            Log.i(TAG, "Live Update eligibility: $promotionStatus")
            lastPromotionStatus = promotionStatus
        }
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun requestPromotion(builder: Notification.Builder) {
        try {
            builder.javaClass
                .getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                .invoke(builder, true)
        } catch (_: NoSuchMethodException) {
            Log.w(TAG, "This Android 16 build does not expose setRequestPromotedOngoing")
        } catch (error: InvocationTargetException) {
            Log.e(TAG, "Could not request Live Update promotion", error.cause ?: error)
        } catch (error: IllegalAccessException) {
            Log.e(TAG, "Could not request Live Update promotion", error)
        }
    }
}
