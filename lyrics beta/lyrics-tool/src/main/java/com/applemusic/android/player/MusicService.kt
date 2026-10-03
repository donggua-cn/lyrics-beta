package com.applemusic.android.player

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.applemusic.android.util.Logx

/**
 * 媒体后台服务。
 *
 * ## 为什么必须有它
 *
 * 只注册 [androidx.media3.session.MediaSession] 是不够的：
 *
 *  1. **系统不会为本应用发媒体通知**（通知栏 / 锁屏 / 音量面板的媒体卡片都没有），
 *     因为通知是由 `MediaSessionService` 负责投递的；
 *  2. 真机上 Honor/MagicOS 的 `AudioHardening` 会反复警告
 *     `background playback would be muted`，**切到后台就可能被静音/杀掉**；
 *  3. 没有前台服务，系统在内存紧张时会直接回收本进程。
 *
 * 这些都是「系统不认为本应用在放歌」的不同侧面，根子都在这里。
 *
 * 服务本身很薄：播放器与会话仍由 [PlaybackEngine] 持有（单例），
 * 本服务只负责把它交给系统（`onGetSession`），从而让系统来投递通知、
 * 并把本进程提升为媒体播放前台服务。
 */
class MusicService : MediaSessionService() {

    private companion object {
        const val TAG = "MusicService"
    }

    override fun onCreate() {
        super.onCreate()
        // 幂等：Activity 里可能已经初始化过
        PlaybackEngine.init(this)
        LiveLyricsNotification.start(this)
        Logx.i(TAG, "媒体服务已创建")
    }

    /**
     * 系统（SystemUI / 耳机 / 车机 / 锁屏）来取会话时把它交出去。
     *
     * 返回同一个 [PlaybackEngine] 持有的会话，所以通知栏的控制和 App 内
     * 的操作是**同一份状态**，不会出现两边不同步。
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        val s = PlaybackEngine.session()
        Logx.i(
            TAG,
            "onGetSession 被调用：package=${controllerInfo.packageName} " +
                    "uid=${controllerInfo.uid} 会话=${if (s != null) "有" else "无"}"
        )
        return s
    }

    /**
     * media3 决定「要不要投递媒体通知」的钩子。
     *
     * 这里把入参写进日志 —— 通知出不来时，必须能区分是
     * **media3 压根没调用**（控制器/会话没挂上），还是调用了但不满足前台条件。
     */
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        Logx.i(
            TAG,
            "onUpdateNotification：startInForegroundRequired=$startInForegroundRequired " +
                    "isPlaying=${session.player.isPlaying} " +
                    "state=${session.player.playbackState}"
        )
        try {
            super.onUpdateNotification(session, startInForegroundRequired)
        } catch (t: Throwable) {
            Logx.e(TAG, "投递媒体通知失败：${t.message}", t)
        }
    }

    /** 清除最近任务时保存播放位置并暂停；下次打开只恢复状态，不自动出声。 */
    override fun onTaskRemoved(rootIntent: Intent?) {
        PlaybackEngine.pauseForTaskRemoval()
        Logx.i(TAG, "任务被移除，播放已暂停并保存队列")
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Logx.i(TAG, "媒体服务已销毁")
        // 服务真正结束时才释放播放器，避免后台播放被误杀
        PlaybackEngine.release()
        super.onDestroy()
    }
}
