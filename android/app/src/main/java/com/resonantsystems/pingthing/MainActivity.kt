package com.resonantsystems.pingthing

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.JavascriptInterface
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
 * The Ping Thing — native shell, Phase 2.
 *
 * Crash-visibility scaffolding (permanent): any Throwable during init, and any
 * uncaught exception later, lands in files/crash.txt and is rendered full-screen
 * with a COPY button instead of "keeps stopping".
 *
 * Phase 2 (PLAN §5): AndroidHost JS bridge (feature-detected by the HTML; inert
 * in browsers), audio focus (suspend on loss — an instrument ducking sounds
 * wrong), BACKGROUND AUDIO via mediaPlayback foreground service, haptic taps.
 */
class MainActivity : Activity() {

    private var webView: WebView? = null
    private var backPressedAt = 0L

    private lateinit var audioManager: AudioManager
    private var focusRequest: AudioFocusRequest? = null

    /** True while the in-app BACKGROUND AUDIO toggle is on. */
    @Volatile
    var bgAudio = false
        private set

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN ->
                evalJs("if(window.ctx)ctx.resume();")
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                evalJs("if(window.ctx)ctx.suspend();")
            AudioManager.AUDIOFOCUS_LOSS -> {
                evalJs("if(window.ctx)ctx.suspend();if(window._setBgAudioUI)_setBgAudioUI(false);")
                if (bgAudio) setBackgroundAudioInternal(false)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        active = this
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        volumeControlStream = AudioManager.STREAM_MUSIC

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
        wv.addJavascriptInterface(AndroidHostBridge(), "AndroidHost")

        setContentView(wv)
        hideSystemBars()
        wv.loadUrl("https://appassets.androidplatform.net/assets/ping-thing.html")
    }

    // ── AndroidHost JS bridge ─────────────────────────────────────────

    inner class AndroidHostBridge {

        @JavascriptInterface
        fun onAudioStarted() {
            runOnUiThread { requestAudioFocus() }
        }

        @JavascriptInterface
        fun keepAwake(on: Boolean) {
            runOnUiThread {
                if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        @JavascriptInterface
        fun hapticTap(ms: Int) {
            val dur = ms.coerceIn(1, 80).toLong()
            val vib = if (Build.VERSION.SDK_INT >= 31) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                    .defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vib.vibrate(VibrationEffect.createOneShot(dur, VibrationEffect.DEFAULT_AMPLITUDE))
        }

        @Suppress("DEPRECATION")
        @JavascriptInterface
        fun getAppVersion(): String =
            try { packageManager.getPackageInfo(packageName, 0).versionName ?: "?" }
            catch (e: Exception) { "?" }

        @JavascriptInterface
        fun setBackgroundAudio(on: Boolean) {
            runOnUiThread { setBackgroundAudioInternal(on) }
        }
    }

    private fun requestAudioFocus() {
        if (focusRequest != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener(focusListener)
            .build()
        focusRequest = req
        audioManager.requestAudioFocus(req)
    }

    private fun setBackgroundAudioInternal(on: Boolean) {
        bgAudio = on
        if (on) {
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
            }
            startForegroundService(Intent(this, PlaybackService::class.java))
        } else {
            stopService(Intent(this, PlaybackService::class.java))
        }
    }

    /** Called by PlaybackService when the notification STOP action fires. */
    fun stopBackgroundAudioFromNotification() {
        runOnUiThread {
            bgAudio = false
            evalJs("if(window.ctx)ctx.suspend();if(window._setBgAudioUI)_setBgAudioUI(false);")
        }
    }

    private fun evalJs(js: String) {
        runOnUiThread { webView?.evaluateJavascript(js, null) }
    }

    // ── Crash visibility (permanent scaffolding) ─────────────────────

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

    // ── System chrome & lifecycle ─────────────────────────────────────

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
        // With BACKGROUND AUDIO on we deliberately keep the WebView fully alive:
        // pausing it would throttle the JS scheduler the service exists to protect.
        if (!bgAudio) {
            webView?.let {
                it.evaluateJavascript("if(window.ctx)ctx.suspend();", null)
                it.onPause()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        webView?.let {
            it.onResume()
            if (!bgAudio) it.evaluateJavascript("if(window.ctx)ctx.resume();", null)
        }
    }

    override fun onDestroy() {
        active = null
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        stopService(Intent(this, PlaybackService::class.java))
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

    companion object {
        /** Handle for PlaybackService's notification STOP action. */
        @JvmStatic
        var active: MainActivity? = null
    }
}
