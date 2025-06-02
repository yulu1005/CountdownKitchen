package com.example.smartfridgeassistant

// 🔹 1. 匯入所需的套件
import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.RadioGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SettingActivity : AppCompatActivity() {

    // 🔹 2. 常數：通知權限請求碼
    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 123
    }

    // 🔹 3. 宣告變數
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var dao: FoodDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setting)

        // 🔹 4. 檢查是否有通知權限
        checkNotificationPermission()

        // 🔹 5. 調整系統狀態列的邊距（避免被遮擋）
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 🔹 6. 取得 SharedPreferences 和 DAO
        sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE)
        dao = AppDatabase.getDatabase(this).foodDao()

        // 🔹 7. 初始化提醒時間選項（RadioButton 預設勾選）
        val radioGroup = findViewById<RadioGroup>(R.id.radio_group_reminder)
        val savedReminderTime = sharedPreferences.getInt("reminder_time", 0)
        radioGroup.check(
            when (savedReminderTime) {
                0 -> R.id.radio_week_before
                1 -> R.id.radio_day_before
                2 -> R.id.radio_same_day
                else -> R.id.radio_week_before
            }
        )

        // 🔹 8. 設定提醒時間選項變更監聽器
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val reminderTime = when (checkedId) {
                R.id.radio_week_before -> 0
                R.id.radio_day_before -> 1
                R.id.radio_same_day -> 2
                else -> 0
            }

            // 存入設定值
            sharedPreferences.edit().putInt("reminder_time", reminderTime).apply()

            // 檢查並發送通知
            checkAndSendNotifications(reminderTime)
        }

        // 🔹 9. 設定底部導覽列（高亮設定頁）
        setupBottomNav(this, R.id.nav_setting)
    }

    // 🔹 10. 執行通知檢查與發送邏輯
    private fun checkAndSendNotifications(reminderTime: Int) {
        lifecycleScope.launch {
            val allFoods = dao.getAll()  // 取得所有食材
            val currentDate = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // 計算目標日期
            val targetDate = Calendar.getInstance()
            targetDate.add(
                Calendar.DAY_OF_MONTH, when (reminderTime) {
                    0 -> 7   // 提前一週
                    1 -> 1   // 提前一天
                    else -> 0 // 當天
                }
            )
            val targetDateStr = dateFormat.format(targetDate.time)

            // 除錯日誌：顯示目標日期與食材列表
            Log.d("SettingActivity", "目標日期: $targetDateStr")
            Log.d("SettingActivity", "所有食品: ${allFoods.map { "${it.name}(${it.type})" }}")

            var notificationSent = false

            // 逐一比對是否有符合條件的食材
            allFoods.forEach { food ->
                Log.d("SettingActivity", "檢查食品: ${food.name}, 類型: '${food.type}', 到期日: ${food.expiryDate}")

                val foodExpiryDate = dateFormat.parse(food.expiryDate)
                val targetDateParsed = dateFormat.parse(targetDateStr)

                if (foodExpiryDate != null && targetDateParsed != null) {
                    val datesMatch = foodExpiryDate.time == targetDateParsed.time
                    Log.d("SettingActivity", "日期是否匹配: $datesMatch")

                    if (datesMatch) {
                        notificationSent = true
                        NotificationHelper(this@SettingActivity).showExpiryNotification(
                            food.name,
                            food.expiryDate
                        )
                        Log.d("SettingActivity", "發送通知: ${food.name}")
                    }
                }
            }

            // 若無符合條件者，顯示提示訊息
            if (!notificationSent) {
                runOnUiThread {
                    val message = "沒有找到${
                        when (reminderTime) {
                            0 -> "一週後"
                            1 -> "一天後"
                            else -> "當天"
                        }
                    }過期的食品"
                    Toast.makeText(this@SettingActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 🔹 11. 檢查 Android 13+ 是否已授權通知
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    // 🔹 12. 接收權限回應的處理
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "通知權限已授予", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "需要通知權限才能接收提醒", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
