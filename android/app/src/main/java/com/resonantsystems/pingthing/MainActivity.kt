package com.resonantsystems.pingthing

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.webkit.WebViewAssetLoader
import java.io.File

/**
 * The Ping Thing — thin native shell (Phase 1 + crash-visibility hotfix).
 *
 * The launch crash reported on the owner device is captured here instead of
 * dying silently: any Throwable during init — and any uncaught exception
 * later — is written to files/crash.txt and rendered full-screen with a COPY
 * button on this or the next launch. Once the real cause is fixed and
 * verified, this scaffolding stays (it is harmless and tiny).
 */
class MainActivity : Activity() {

    private var webView: WebView? = null
    private var backPressedAt = 0L

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) From this line on, no crash is ever silent.
        installCrashHandler()

        // 2) A previous launch crashed? Show the trace instead of the instrument.
        val pending = crashFile()
        if (pending.exists()) {
            showTraceScreen(pending.readText())
            return
        }

        // 3) Normal init, fully guarded — failure renders the trace immediately.
        try {
            initInstrument()
        } catch (t: Throwable) {
            val trace = Log.getStackTraceString(t)
            runCatching { crashFile().writeText(trace) }
            showTraceScreen(trace)
        }
    }

    private fun initInstrument() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        // WebView construction is the classic instant-crash site when the
        // System WebView provider is disabled or mid-update — now visible.
        val wv = WebView(this)
        webView = wv
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.mediaPlaybackRequiresUserGesture = false
        wv.settings.setSupportZoom(false)
        wv.setBackgroundColor(0xFF090807.toInt())
        wv.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)
        }

        setContentView(wv)
        // Immersive AFTER the decor has content — ordering hygiene.
        hideSystemBars()
        wv.loadUrl("https://appassets.androidplatform.net/assets/ping-thing.html")
    }

    // ── Crash visibility ─────────────────────────────────────────────

    private fun crashFile() = File(filesDir, "crash.txt")

    private fun installCrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            runCatching { crashFile().writeText(Log.getStackTraceString(e)) }
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    private fun showTraceScreen(trace: String) {
        val pad = (resources.displayMetrics.density * 16).toInt()

        val title = TextView(this).apply {
            text = "PING THING — CRASH REPORT\nLong-press to select, or use COPY, and send this to the build agent."
            setTextColor(Color.parseColor("#FF5050"))
            typeface = Typeface.MONOSPACE
            textSize = 13f
            setPadding(0, 0, 0, pad)
        }
        val body = TextView(this).apply {
            text = trace
            setTextColor(Color.parseColor("#00DD88"))
            typeface = Typeface.MONOSPACE
            textSize = 11f
            setTextIsSelectable(true)
        }
        val copyBtn = Button(this).apply {
            text = "COPY TRACE"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("ping-thing-crash", trace))
                Toast.makeText(this@MainActivity, "Copied", Toast.LENGTH_SHORT).show()
            }
        }
        val retryBtn = Button(this).apply {
            text = "DELETE REPORT AND RETRY"
            setOnClickListener {
                crashFile().delete()
                recreate()
            }
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#090807"))
            setPadding(pad, pad * 2, pad, pad * 2)
            addView(title)
            addView(copyBtn)
            addView(retryBtn)
            addView(body)
        }
        setContentView(ScrollView(this).apply { addView(column) })
    }

    // ── Phase 1 behaviour, unchanged below ───────────────────────────

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && webView != null) hideSystemBars()
    }

    override fun onPause() {
        super.onPause()
        webView?.let {
            it.evaluateJavascript("if(window.ctx)ctx.suspend();", null)
            it.onPause()
        }
    }

    override fun onResume() {
        super.onResume()
        webView?.let {
            it.onResume()
            it.evaluateJavascript("if(window.ctx)ctx.resume();", null)
        }
    }

    override fun onDestroy() {
        webView?.destroy()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val now = System.currentTimeMillis()
        if (now - backPressedAt < 2000) {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        } else {
            backPressedAt = now
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }
}
