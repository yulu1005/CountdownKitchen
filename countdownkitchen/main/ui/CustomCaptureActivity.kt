package com.example.smartfridgeassistant

// 🔹 套件匯入
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class CustomCaptureActivity : AppCompatActivity() {

    // 🔹 宣告條碼掃描工具與掃描畫面
    private lateinit var capture: CaptureManager
    private lateinit var barcodeView: DecoratedBarcodeView

    // ✅ 1. 畫面初始化與 CaptureManager 設定
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.custom_capture)

        // 顯示 ActionBar 返回鍵與標題
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "掃描"

        // 初始化掃描畫面與管理器
        barcodeView = findViewById(R.id.zxing_barcode_scanner)
        capture = CaptureManager(this, barcodeView)
        capture.initializeFromIntent(intent, savedInstanceState)
        capture.decode()  // 啟動掃描
    }

    // ✅ 2. 生命週期事件處理（resume / pause / destroy / save）
    override fun onResume() {
        super.onResume()
        capture.onResume()
    }

    override fun onPause() {
        super.onPause()
        capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture.onSaveInstanceState(outState)
    }

    // ✅ 3. 返回箭頭事件處理（點左上角返回）
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
