package com.applemusic.lyricstool.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.applemusic.lyricstool.data.AppleLyricsClient
import java.util.Locale

class AppleMusicLoginActivity : ComponentActivity() {
    private val client by lazy { AppleLyricsClient(applicationContext) }
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private var loginComplete = false

    private val loadTimeout = Runnable {
        if (!loginComplete && webView.progress < 100) {
            progressBar.visibility = View.GONE
            statusText.text = "页面加载超时。请检查网络后点“重新加载”。"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(255, 248, 247)
        window.navigationBarColor = Color.rgb(255, 248, 247)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(10))
            setBackgroundColor(Color.rgb(255, 248, 247))
        }
        root.addView(
            TextView(this).apply {
                text = "连接 Apple Music"
                textSize = 22f
                setTextColor(Color.rgb(37, 29, 50))
                setTypeface(typeface, Typeface.BOLD)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            TextView(this).apply {
                text = "请在下方页面登录。登录凭据仅保存在本机。"
                textSize = 14f
                setTextColor(Color.rgb(81, 75, 91))
                setPadding(0, dp(5), 0, dp(5))
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        statusText = TextView(this).apply {
            text = "正在打开 Apple Music…"
            textSize = 13f
            setTextColor(Color.rgb(103, 80, 164))
            setPadding(0, dp(4), 0, dp(4))
        }
        root.addView(
            statusText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
        }
        root.addView(
            progressBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(3)
            )
        )

        webView = WebView(this).apply {
            setBackgroundColor(Color.WHITE)
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString =
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progressBar.progress = newProgress
                    progressBar.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    statusText.text = "正在连接 Apple Music…"
                    progressBar.visibility = View.VISIBLE
                    mainHandler.removeCallbacks(loadTimeout)
                    mainHandler.postDelayed(loadTimeout, LOAD_TIMEOUT_MS)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    mainHandler.removeCallbacks(loadTimeout)
                    progressBar.visibility = View.GONE
                    statusText.text = "请登录 Apple Music；登录成功后会自动返回。"
                    readLoginCookie()
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        mainHandler.removeCallbacks(loadTimeout)
                        progressBar.visibility = View.GONE
                        statusText.text =
                            "页面加载失败：${error?.description ?: "网络错误"}。请检查网络后重新加载。"
                    }
                }
            }
        }
        root.addView(
            webView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = dp(6)
                bottomMargin = dp(8)
            }
        )

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }
        actions.addView(
            Button(this).apply {
                text = "重新加载"
                setOnClickListener {
                    statusText.text = "正在连接 Apple Music…"
                    progressBar.visibility = View.VISIBLE
                    webView.loadUrl(LOGIN_URL)
                }
            }
        )
        actions.addView(
            Button(this).apply {
                text = "关闭"
                setOnClickListener { finish() }
            }
        )
        root.addView(
            actions,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(root)
        webView.loadUrl(LOGIN_URL)
    }

    private fun readLoginCookie() {
        val cookieManager = CookieManager.getInstance()
        val token = COOKIE_URLS.asSequence()
            .mapNotNull(cookieManager::getCookie)
            .map(client::extractUserToken)
            .firstOrNull { it.isNotEmpty() }
            ?: return

        loginComplete = true
        mainHandler.removeCallbacks(loadTimeout)
        client.saveUserToken(token)
        cookieManager.flush()
        statusText.text = "登录成功，正在返回…"
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) webView.onResume()
    }

    override fun onPause() {
        if (::webView.isInitialized) webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(loadTimeout)
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val LOGIN_URL = "https://music.apple.com/cn/browse"
        const val LOAD_TIMEOUT_MS = 30_000L
        val COOKIE_URLS = listOf(
            "https://music.apple.com/us/browse",
            "https://music.apple.com/cn/browse",
            "https://amp-api.music.apple.com/"
        )
    }
}
