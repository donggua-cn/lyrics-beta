package com.applemusic.android.util

import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 极简日志 + 落盘日志。
 *
 * 对应鸿蒙版的 `AppleDiag`：真机排查时 logcat 会被刷掉，所以同时写一份到
 * `filesDir/diag.log`（超过 2MB 截断重来）。
 */
object Logx {

    private const val TAG = "AppleMusic"
    private const val MAX_LOG_BYTES = 2L * 1024 * 1024

    @Volatile
    var logFile: File? = null

    private val fmt = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    fun init(filesDir: File) {
        logFile = File(filesDir, "diag.log")
    }

    fun d(tag: String, msg: String) {
        Log.d("$TAG/$tag", msg)
        write("D", tag, msg)
    }

    fun i(tag: String, msg: String) {
        Log.i("$TAG/$tag", msg)
        write("I", tag, msg)
    }

    fun w(tag: String, msg: String) {
        Log.w("$TAG/$tag", msg)
        write("W", tag, msg)
    }

    fun e(tag: String, msg: String) {
        Log.e("$TAG/$tag", msg)
        write("E", tag, msg)
    }

    fun e(tag: String, msg: String, t: Throwable) {
        Log.e("$TAG/$tag", msg, t)
        write("E", tag, "$msg\n${Log.getStackTraceString(t)}")
    }

    private fun write(level: String, tag: String, msg: String) {
        val f = logFile ?: return
        try {
            if (f.exists() && f.length() > MAX_LOG_BYTES) {
                f.delete()
            }
            f.appendText("${fmt.format(Date())} $level/$tag $msg\n")
        } catch (_: Throwable) {
            // 日志本身不抛异常
        }
    }
}
