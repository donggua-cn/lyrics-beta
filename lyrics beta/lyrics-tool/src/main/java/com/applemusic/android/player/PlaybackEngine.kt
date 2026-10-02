package com.applemusic.android.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.applemusic.android.apple.AppleCache
import com.applemusic.android.apple.AppleDownloader
import com.applemusic.android.apple.AppleLibrarySync
import com.applemusic.android.apple.MusicRepository
import com.applemusic.android.util.Logx
import java.io.File

/**
 * 播放引擎。
 *
 * 对应鸿蒙版 `MediaService` 的「点 PlaybackHelper → MediaService.loadAsset」这一层：
 * 队列里存的是 `apple:<id>` 这样的 uri，播放前确保本地已有解密好的文件
 * （没有就先走 AppleDownloader 的六步流水线），然后交给 ExoPlayer 播本地 m4a。
 *
 * 之所以不用 ExoPlayer 的原生 DRM 直接播 HLS：Apple 的 license 端点需要
 * `adamId`/`uri`/`key-system` 这一套自定义 JSON 体，和 ExoPlayer 的
 * DrmSessionManager 默认请求格式对不上，反而更绕。
 */
object PlaybackEngine {

    private const val TAG = "PlaybackEngine"
    const val ACTION_OPEN_NOW_PLAYING = "com.applemusic.android.action.OPEN_NOW_PLAYING"

    var player: ExoPlayer? = null
        private set

    private val main = Handler(Looper.getMainLooper())

    /** 队列（uri 形式）。 */
    private val queue = ArrayList<String>()

    /** 用户选中 / 正在准备的那一首。 */
    @Volatile
    var queueIndex = -1
        private set

    @Volatile
    private var skipPendingForQueueContinuation = false

    /**
     * **真正在出声**的那一首。
     *
     * 界面（标题、封面、歌词）一律以它为准，而不是 [queueIndex]。
     * 这样「点了新歌但下载失败」时，界面会停留在上一首 + 一条错误提示，
     * 不会出现「歌名是新歌、声音是旧歌」这种对不上的情况。
     */
    @Volatile
    private var playingIndex = -1

    @Volatile
    var preparing = false
        private set

    @Volatile
    var preparingNote = ""

    /** 状态变化回调（主线程）。 */
    var onStateChanged: (() -> Unit)? = null

    /**
     * 队列快见底或用户手动切歌时的回调（智能电台用来「续杯」）。
     * 普通列表播放时把它置为 null 即可。
     */
    var onQueueLow: ((remaining: Int) -> Unit)? = null

    /** 水位：剩这么几首就提前去要下一批，避免播到断流。 */
    private val QUEUE_LOW_WATER = 3

    /**
     * 往队列**末尾**追加曲目（已有曲目不会重复添加）。
     *
     * 追加不会移动已有下标，所以 [playingIndex] / [queueIndex] 依然有效 ——
     * 这正是「无限电台」能边放边续的关键。
     */
    fun appendToQueue(uris: List<String>): Int {
        if (uris.isEmpty()) return 0
        var added = 0
        synchronized(queue) {
            val existing = HashSet(queue)
            for (u in uris) {
                if (u.isEmpty() || !existing.add(u)) continue
                queue.add(u)
                added++
            }
        }
        if (added > 0) {
            Logx.i(TAG, "队列续杯 +$added 首（现共 ${synchronized(queue) { queue.size }} 首）")
            onStateChanged?.invoke()
            val pendingIndex = synchronized(queue) {
                if (skipPendingForQueueContinuation) {
                    skipPendingForQueueContinuation = false
                    maxOf(queueIndex, playingIndex) + 1
                } else {
                    -1
                }
            }
            if (pendingIndex >= 0 && pendingIndex < synchronized(queue) { queue.size }) {
                playAt(pendingIndex)
            }
        }
        return added
    }

    /** 剩余待播曲目数。 */
    fun queueRemaining(): Int = synchronized(queue) { queue.size - queueIndex - 1 }

    /** 水位检查：剩得不多就通知外部补货。 */
    private fun notifyIfQueueLow() {
        val cb = onQueueLow ?: return
        val remaining = queueRemaining()
        if (remaining <= QUEUE_LOW_WATER) {
            Logx.i(TAG, "队列只剩 $remaining 首，请求续杯")
            cb(remaining)
        }
    }

    /**
     * 系统媒体会话。
     *
     * 【为什么必须有】只调 ExoPlayer 的话，**安卓系统媒体控制完全不知道本应用在放歌**：
     * 通知栏 / 锁屏 / 蓝牙耳机都不显示、媒体按键也送不到这里。
     * 真机上表现为「系统播放器管理里根本没有我们在放歌」。
     *
     * 挂上 [androidx.media3.session.MediaSession] 之后，系统才知道，
     * 并且 `KEYCODE_MEDIA_NEXT` / `KEYCODE_MEDIA_PREVIOUS` 等按键才会路由到本应用。
     */
    private var mediaSession: androidx.media3.session.MediaSession? = null

    /** 本应用自己连到自身会话的控制器（用来促使系统投递媒体通知）。 */
    private var selfController: androidx.media3.session.MediaController? = null

    /**
     * 把会话交给 [MusicService]（系统通过它来投递媒体通知、提升前台服务）。
     *
     * 只注册 `MediaSession` 而没有 `MediaSessionService` 时，系统**不会主动**
     * 为本应用发媒体通知，通知栏/锁屏/音量面板都不会出现媒体卡片。
     */
    fun session(): androidx.media3.session.MediaSession? = mediaSession

    fun init(context: Context) {
        if (player != null) return
        val p = ExoPlayer.Builder(context.applicationContext).build()

        // ═══════════════ 音频焦点（必须） ═══════════════
        //
        // ExoPlayer 只有在设置 AudioAttributes 并启用音频焦点处理后才会申请焦点。
        //   1. 别的 app 在放歌时我们照放，两路声音打架；
        //   2. 系统不认为我们是「当前媒体播放器」，
        //      音量面板 / 媒体卡片显示的是**别的 app**（真机上看到的是 com.luna.music）；
        //   3. 别的 app 抢焦点时我们不会自动暂停。
        p.setAudioAttributes(
            androidx.media3.common.AudioAttributes.Builder()
                .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true
        )
        // 拔耳机 / 断开蓝牙时自动暂停（否则会突然外放）
        p.setHandleAudioBecomingNoisy(true)

        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                Logx.i(TAG, "playbackState=$playbackState (1=IDLE 2=BUFFERING 3=READY 4=ENDED)")
                if (playbackState == Player.STATE_READY) {
                    preparingNote = ""
                }
                if (playbackState == Player.STATE_ENDED) {
                    next()
                }
                onStateChanged?.invoke()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Logx.i(TAG, "isPlaying=$isPlaying")
                onStateChanged?.invoke()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // 【重要】播放器解不开文件时，必须把**完整异常链**写出来，
                // 否则只知道最外层的错误码，定位不到「到底在读文件的哪个位置越界」。
                Logx.e(TAG, "播放器错误：${error.errorCodeName} ${error.message}", error)
                var cause: Throwable? = error.cause
                var depth = 0
                while (cause != null && depth < 8) {
                    Logx.e(TAG, "  因果[$depth] ${cause.javaClass.simpleName}: ${cause.message}")
                    cause = cause.cause
                    depth++
                }
                Logx.e(
                    TAG,
                    "  上下文：uri=${currentUri()} title=${currentSong()?.title ?: "?"} " +
                            "duration=${player?.duration} position=${player?.currentPosition}"
                )
                preparing = false
                preparingNote = "播放失败：${error.errorCodeName}"
                onStateChanged?.invoke()
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                onStateChanged?.invoke()
            }
        })
        player = p
        Logx.i(TAG, "ExoPlayer 已初始化")
        registerMediaSession(context.applicationContext, p)
    }

    /**
     * 把播放器注册进系统媒体会话。
     *
     * 这样通知栏 / 锁屏 / 蓝牙耳机才知道「本应用正在放歌」，媒体按键也才会送过来。
     */
    private fun registerMediaSession(ctx: Context, p: Player) {
        if (mediaSession != null) return
        try {
            val appIntent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                ?: android.content.Intent(ctx, MusicService::class.java)
            appIntent.action = ACTION_OPEN_NOW_PLAYING
            appIntent.addFlags(
                android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            val openApp = android.app.PendingIntent.getActivity(
                ctx,
                0,
                appIntent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            mediaSession = androidx.media3.session.MediaSession.Builder(ctx, RadioPlayer(p))
                .setSessionActivity(openApp)
                .build()
            Logx.i(TAG, "MediaSession 已注册：通知栏/锁屏/蓝牙媒体按键现在能识别本应用")
            connectSelfController(ctx)
        } catch (t: Throwable) {
            // 注册失败不该影响播放本身
            Logx.w(TAG, "MediaSession 注册失败：${t.message}")
        }
    }

    /**
     * 自己连一个 [androidx.media3.session.MediaController] 到自己的会话。
     *
     * ## 为什么必须这么做
     *
     * media3 的 `MediaSessionService` **只有在「有控制器连上」时才会去管理媒体通知**。
     * 没连上时它既不会创建通知渠道、也不会发通知 ——
     * 真机表现就是 Honor 控制中心/通知栏的媒体卡片一直显示「**未在播放**」，
     * 尽管音频确实在响、音频焦点也确实在本应用手上（`dumpsys notification` 里
     * 本应用**一个 NotificationChannel 都没有**，就是"从未尝试发通知"的铁证）。
     *
     * 而系统侧又是因为「没有通知」才不来连 —— 典型的鸡生蛋。
     * 自己连一个控制器就能打破这个死循环，之后 SystemUI 才会接手。
     */
    private fun connectSelfController(ctx: Context) {
        val s = mediaSession ?: return
        if (selfController != null) return
        try {
            // 【关键】token 必须指向 **MusicService**，不能用 mediaSession.token。
            //
            // 用 mediaSession.token 时，MediaController 会**直接连到会话本身**
            // （同进程直连），**绕过 MediaSessionService** —— 服务那边
            // `onGetSession` 根本不会被调用（真机日志验证过：服务已创建，
            // 但 onGetSession 一次都没打印），于是 media3 永远不建通知。
            //
            // 用服务的 ComponentName 构造 token，连接才会走服务，
            // 服务才会 onGetSession → 挂上会话 → 建立通知管理器 → 投递媒体通知。
            val token = androidx.media3.session.SessionToken(
                ctx,
                android.content.ComponentName(ctx, MusicService::class.java)
            )
            val future = androidx.media3.session.MediaController.Builder(ctx, token).buildAsync()
            future.addListener(
                {
                    try {
                        selfController = future.get()
                        Logx.i(TAG, "已连接自身 MediaController（经服务）—— 系统现在会投递媒体通知")
                    } catch (t: Throwable) {
                        Logx.w(TAG, "自身 MediaController 连接失败：${t.message}")
                    }
                },
                androidx.core.content.ContextCompat.getMainExecutor(ctx)
            )
        } catch (t: Throwable) {
            Logx.w(TAG, "构造自身 MediaController 失败：${t.message}")
        }
    }

    /**
     * 把播放器包一层，**对外声明「上一首 / 下一首可用」**。
     *
     * 【为什么需要】队列是我们自己管的，ExoPlayer 里永远只有「当前这一首」，
     * 于是 `COMMAND_SEEK_TO_NEXT_MEDIA_ITEM` 被判为**不可用** ——
     * 系统媒体按键（耳机 / 锁屏 / 通知栏）发过来会被 MediaSession **直接忽略**，
     * 表现为「按键完全没反应」。
     *
     * 这里把这两个命令显式标成可用，并把实际动作接到 [next] / [prev]。
     */
    private class RadioPlayer(player: Player) :
        androidx.media3.common.ForwardingPlayer(player) {

        private val extra = intArrayOf(
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
        )

        override fun getAvailableCommands(): Player.Commands {
            var b = super.getAvailableCommands().buildUpon()
            for (c in extra) b = b.add(c)
            return b.build()
        }

        override fun isCommandAvailable(command: Int): Boolean {
            if (extra.contains(command)) return true
            return super.isCommandAvailable(command)
        }

        override fun hasNextMediaItem(): Boolean = true
        override fun hasPreviousMediaItem(): Boolean = true

        override fun seekToNext() {
            Logx.i(TAG, "媒体按键：下一首")
            PlaybackEngine.next()
        }

        override fun seekToNextMediaItem() = seekToNext()

        override fun seekToPrevious() {
            Logx.i(TAG, "媒体按键：上一首")
            PlaybackEngine.prev()
        }

        override fun seekToPreviousMediaItem() = seekToPrevious()
    }

    fun currentUri(): String? = queue.getOrNull(playingIndex)

    fun currentSong(): AppleLibrarySync.SongRow? = currentUri()?.let { MusicRepository.findSong(it) }

    /** 队列只读快照，供播放页展示；复制后不会暴露播放器内部列表。 */
    fun queueSnapshot(): List<String> = synchronized(queue) { ArrayList(queue) }

    /** 当前实际播放位置；首次播放尚未提交时回退到用户刚选中的位置。 */
    fun currentQueueIndex(): Int {
        val size = synchronized(queue) { queue.size }
        return if (playingIndex in 0 until size) playingIndex else queueIndex
    }

    fun isPlaying(): Boolean {
        return player?.isPlaying == true
    }

    /** 用一列 uri 播放下标 index 的歌。 */
    fun playQueue(uris: List<String>, index: Int) {
        synchronized(queue) {
            queue.clear()
            queue.addAll(uris)
            skipPendingForQueueContinuation = false
        }
        // 换了队列，旧的「正在出声的那首」下标已经无意义；
        // 坏条目记录也一并清掉（换个上下文可能就好了，例如网络恢复）
        playingIndex = -1
        synchronized(badUris) { badUris.clear() }
        playAt(index)
    }

    fun playSingle(uri: String) = playQueue(listOf(uri), 0)

    /** 播放队列里的第 index 首（会先确保本地文件就绪）。 */
    fun playAt(index: Int) {
        val uri = synchronized(queue) { queue.getOrNull(index) } ?: return
        queueIndex = index
        onStateChanged?.invoke()
        // 准备阶段就去检查水位：一首歌要下载十几秒，提前续杯能让切换无缝
        notifyIfQueueLow()
        prepareAndPlay(uri)
    }

    /**
     * 播放失败过的 uri。
     *
     * 【为什么需要】失败时 [failPlayback] 会把 `queueIndex` 回退到「真正在播的那首」，
     * 于是再点「下一首」还是同一个坏条目 —— 表现就是**一直重复同一首、永远跳不过去**
     * （真机上踩到过：队列里混进一个专辑 id，next 就卡死在它上面）。
     * 记住这些 uri，[next] / [prev] 会跳过。
     */
    private val badUris = HashSet<String>()

    private fun markBad(uri: String?) {
        if (uri.isNullOrEmpty()) return
        synchronized(badUris) { badUris.add(uri) }
    }

    private fun isBad(uri: String?): Boolean {
        if (uri.isNullOrEmpty()) return true
        return synchronized(badUris) { badUris.contains(uri) }
    }

    /** 跳过坏条目往后找一首；找不到就返回起点（避免死循环）。 */
    private fun findPlayable(from: Int, step: Int, n: Int): Int {
        var idx = ((from + step) % n + n) % n
        var guard = 0
        while (guard < n) {
            val u = synchronized(queue) { queue.getOrNull(idx) }
            if (!isBad(u)) return idx
            idx = ((idx + step) % n + n) % n
            guard++
        }
        return from
    }

    fun next() {
        val n = synchronized(queue) { queue.size }
        if (n == 0) return
        val currentIndex = maxOf(queueIndex, playingIndex)
        if (currentIndex + 1 < n) {
            playAt(findPlayable(currentIndex, 1, n))
            val continuation = onQueueLow
            if (continuation != null) {
                val remaining = queueRemaining()
                if (remaining > QUEUE_LOW_WATER) {
                    Logx.i(TAG, "手动切歌后主动请求电台续歌（剩余 $remaining 首）")
                    continuation(remaining)
                }
            }
            return
        }
        val continuation = onQueueLow
        if (continuation != null) {
            synchronized(queue) { skipPendingForQueueContinuation = true }
            Logx.i(TAG, "电台队列暂无下一首，等待 Apple 续歌后跳转")
            continuation(0)
            onStateChanged?.invoke()
            return
        }
        playAt(findPlayable(currentIndex, 1, n))
    }

    fun prev() {
        val n = synchronized(queue) { queue.size }
        if (n == 0) return
        playAt(findPlayable(maxOf(queueIndex, playingIndex), -1, n))
    }

    fun toggle() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
        onStateChanged?.invoke()
    }

    fun seekTo(ms: Long) {
        val p = player
        val before = positionMs()
        val dur = p?.duration ?: -1L
        // 【排障】分片 MP4 若没有 seek 索引（sidx），ExoPlayer 会认为不可 seek，
        // 调用 seekTo 会被夹回默认位置（0）—— 表现就是「一拖进度条就跳回开头」。
        // 所以把 isCurrentMediaItemSeekable 打出来，一眼能看出是不是这个原因。
        Logx.i(
            TAG,
            "seekTo($ms) 之前 pos=$before duration=$dur " +
                    "seekable=${p?.isCurrentMediaItemSeekable}"
        )
        p?.seekTo(ms)
        Logx.i(TAG, "seekTo 之后 requested=$ms actual=${p?.currentPosition} state=${p?.playbackState}")
        onStateChanged?.invoke()
    }

    fun positionMs(): Long = player?.currentPosition ?: 0

    fun durationMs(): Long {
        val d = player?.duration ?: 0
        if (d > 0) return d
        return currentSong()?.durationMs ?: 0
    }

    /**
     * 确保本地文件就绪后播放。
     *
     * 注意：下载 + 解密必须在后台线程（一首歌几秒到几十秒），
     * 而且同一首歌并发调用由 AppleCache 的 single-flight 兜住。
     */
    private fun prepareAndPlay(uri: String) {
        Logx.i(TAG, "prepareAndPlay 收到 uri=$uri")
        val song = MusicRepository.findSong(uri)
        if (song == null) {
            Logx.w(TAG, "找不到这首歌：$uri（本地库 ${MusicRepository.songCount()} 首）")
            preparingNote = "找不到这首歌（$uri）"
            onStateChanged?.invoke()
            return
        }
        val rawId = if (song.appleId.isNotEmpty()) song.appleId else uri.removePrefix("apple:")
        Logx.i(TAG, "song=${song.title} appleId=$rawId isLibrary=${song.isLibrary}")
        // 记下「用户想播的是队列里第几首」。真正提交要等文件就绪、开始出声之后。
        val wantIndex = queueIndex
        preparing = true
        preparingNote = "准备中…"
        onStateChanged?.invoke()

        Thread {
            try {
                val path = MusicRepository.cache.ensure(
                    rawId,
                    song.isLibrary,
                    onProgress = { stage, percent, note ->
                        preparingNote = "$note $percent%"
                        main.post { onStateChanged?.invoke() }
                    },
                    metadata = AppleCache.SongMetadata(
                        title = song.title,
                        artist = song.artist,
                        album = song.album
                    )
                )
                Logx.i(TAG, "缓存就绪：$path")
                main.post {
                    preparing = false
                    // 交给播放器这一段以前是「裸奔」的：任何异常都会让界面毫无反应，
                    // 所以这里把所有可能抛异常的调用都包起来并写日志。
                    try {
                        val local = File(path)
                        if (!local.exists() || local.length() < AppleCacheMin) {
                            failPlayback(wantIndex, "缓存文件异常（${local.length()}B）")
                            return@post
                        }
                        val p = player
                        if (p == null) {
                            failPlayback(wantIndex, "播放器未初始化")
                            Logx.e(TAG, "player == null，PlaybackEngine.init 可能没执行")
                            return@post
                        }
                        val fileUri = android.net.Uri.fromFile(local)
                        Logx.i(TAG, "交给 ExoPlayer：$fileUri size=${local.length()}")
                        // 【必须带 metadata】否则系统媒体控制（通知栏/锁屏/车载）拿到的
                        // 只是一个文件路径，而不是歌名 / 艺人 / 封面。
                        val md = MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artist)
                            .setAlbumTitle(song.album)
                            .apply {
                                if (song.coverUrl.isNotEmpty()) {
                                    setArtworkUri(android.net.Uri.parse(song.coverUrl))
                                }
                            }
                            .build()
                        p.setMediaItem(
                            MediaItem.Builder().setUri(fileUri).setMediaMetadata(md).build()
                        )
                        p.prepare()
                        p.play()
                        // 【关键】到这里才算「这首歌真的开始播了」，此时才提交当前曲目。
                        // 之前是在准备**之前**就改了 queueIndex，于是中途失败时
                        // 界面显示新歌、声音还是上一首 —— 用户看到的就是「歌名和声音对不上」。
                        playingIndex = wantIndex
                        preparingNote = ""
                        Logx.i(
                            TAG,
                            "已调用 play()，playerState=${p.playbackState} isPlaying=${p.isPlaying} " +
                                    "playingIndex=$playingIndex"
                        )
                        onStateChanged?.invoke()
                    } catch (t: Throwable) {
                        Logx.e(TAG, "启动播放失败：${t.message}", t)
                        failPlayback(wantIndex, "启动播放失败：${t.message}")
                    }
                }
            } catch (e: Throwable) {
                Logx.e(TAG, "准备失败 $rawId: ${e.message}", e)
                main.post {
                    failPlayback(wantIndex, e.message ?: "准备失败")
                }
            }
        }.start()
    }

    /**
     * 播放失败时的收尾。
     *
     * **不能让界面继续显示一首其实没在播的歌**：把当前曲目回退到「真正在出声的那首」，
     * 并把失败原因留在 [preparingNote]（界面会以副标题形式显示）。
     * 这样「歌名」和「声音」永远是一致的 —— 宁可显示上一首 + 一条错误提示，
     * 也不能显示新歌名却放着上一首的音频。
     */
    private fun failPlayback(wantIndex: Int, reason: String) {
        preparing = false
        preparingNote = "无法播放：$reason"
        // 把这一条标记为坏，next/prev 会跳过它 —— 否则「下一首」会一直卡在同一首上
        markBad(synchronized(queue) { queue.getOrNull(wantIndex) })
        if (playingIndex >= 0) {
            queueIndex = playingIndex
        } else {
            queueIndex = wantIndex
        }
        Logx.w(
            TAG,
            "播放失败，当前曲目回退到 index=$queueIndex，已标记坏条目" +
                    "（原因：$reason）"
        )
        onStateChanged?.invoke()
    }

    private const val AppleCacheMin = 1L

    fun release() {
        // 先释放会话再释放播放器：顺序反了会在系统侧留下一个指向已释放播放器的会话
        try {
            selfController?.release()
        } catch (_: Throwable) {
        }
        selfController = null
        try {
            mediaSession?.release()
        } catch (_: Throwable) {
        }
        mediaSession = null
        player?.release()
        player = null
    }
}
