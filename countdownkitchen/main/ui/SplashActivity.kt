package com.example.smartfridgeassistant

// 🔹 1. 匯入所需套件（Activity、WebView、Intent 等）
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 🔸 2. 設定狀態列透明＋全螢幕（沉浸式畫面）
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        // 🔸 3. 初始化 WebView 顯示動畫頁面
        val webView: WebView = findViewById(R.id.webView)
        webView.webViewClient = WebViewClient() // 點擊仍在 WebView 中處理
        webView.settings.javaScriptEnabled = true // 啟用 JavaScript
        webView.loadUrl("https://yulu1005.github.io/Countdown-Kitchen-Page/splash.html")

        // 🔸 4. 延遲 3 秒後跳轉至主畫面 Main.kt
        webView.postDelayed({
            startActivity(Intent(this, Main::class.java))
            finish() // 結束 SplashActivity
        }, 3000)
    }
}
