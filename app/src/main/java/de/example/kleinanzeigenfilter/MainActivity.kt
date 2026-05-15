package de.example.kleinanzeigenfilter

import android.content.Intent
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var blacklistManager: BlacklistManager
    private lateinit var delayRemovalManager: DelayRemovalManager

    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshWordsAndInject()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        blacklistManager = BlacklistManager(this)
        delayRemovalManager = DelayRemovalManager(this)
        blacklistManager.loadInitialFromAssetsIfNeeded()

        webView = findViewById(R.id.webView)
        findViewById<FloatingActionButton>(R.id.fabSettings).setOnClickListener {
            settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
        }

        configureWebView()
        applyExpiredRemovals()
        webView.loadUrl("https://www.kleinanzeigen.de")
    }

    override fun onResume() {
        super.onResume()
        applyExpiredRemovals()
        refreshWordsAndInject()
    }

    private fun applyExpiredRemovals() {
        delayRemovalManager.popExpiredWords().forEach { blacklistManager.removeWordFinal(it) }
    }

    private fun configureWebView() {
        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        s.loadsImagesAutomatically = true
        s.allowContentAccess = true

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                refreshWordsAndInject()
            }
        }
    }

    private fun refreshWordsAndInject() {
        applyExpiredRemovals()
        val script = WebFilterInjector.buildInjectionScript(blacklistManager.getActiveWords())
        webView.evaluateJavascript(script, null)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
