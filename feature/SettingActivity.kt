package com.example.smartfridgeassistant

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
    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 123
    }

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var dao: FoodDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setting)

        // 检查通知权限
        checkNotificationPermission()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE)
        dao = AppDatabase.getDatabase(this).foodDao()

        // 初始化提醒時間設置
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

        // 設置提醒時間變更監聽器
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val reminderTime = when (checkedId) {
                R.id.radio_week_before -> 0
                R.id.radio_day_before -> 1
                R.id.radio_same_day -> 2
                else -> 0
            }
            sharedPreferences.edit().putInt("reminder_time", reminderTime).apply()
            checkAndSendNotifications(reminderTime)
        }

        setupBottomNav(this, R.id.nav_setting)
    }

    private fun checkAndSendNotifications(reminderTime: Int) {
        lifecycleScope.launch {
            val allFoods = dao.getAll()
            val currentDate = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // 计算目标日期
            val targetDate = Calendar.getInstance()
            targetDate.add(
                Calendar.DAY_OF_MONTH, when (reminderTime) {
                    0 -> 7  // 一周后
                    1 -> 1  // 一天后
                    else -> 0  // 当天
                }
            )
            val targetDateStr = dateFormat.format(targetDate.time)

            // 添加调试信息
            Log.d("SettingActivity", "目标日期: $targetDateStr")
            Log.d("SettingActivity", "所有食品: ${allFoods.map { "${it.name}(${it.type})" }}")

            // 检查所有食品并发送通知
            var notificationSent = false
            allFoods.forEach { food ->
                // 添加调试信息
                Log.d(
                    "SettingActivity",
                    "检查食品: ${food.name}, 类型: '${food.type}', 到期日: ${food.expiryDate}"
                )

                // 确保日期格式一致
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
                        Log.d("SettingActivity", "发送通知: ${food.name}")
                    }
                }
            }

            // 如果没有找到符合条件的食品，显示提示
            if (!notificationSent) {
                runOnUiThread {
                    val message = "沒有找到${
                        when (reminderTime) {
                            0 -> "一周后"
                            1 -> "一天后"
                            else -> "当天"
                        }
                    }過期的食品"
                    Toast.makeText(this@SettingActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
