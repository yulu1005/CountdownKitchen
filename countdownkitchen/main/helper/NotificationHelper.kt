package com.example.smartfridgeassistant

// 🔹 匯入必要的套件，包含通知、Intent、版本控制等
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

// 🔸 建立一個輔助類別，用來顯示「食品到期提醒通知」
class NotificationHelper(private val context: Context) {

    companion object {
        // 通知頻道的識別資訊（Android 8.0+ 必需設定）
        const val CHANNEL_ID = "food_expiry_channel"           // 頻道 ID
        const val CHANNEL_NAME = "食品到期提醒"                // 頻道名稱（顯示給使用者看）
        const val CHANNEL_DESCRIPTION = "提醒您食品即將到期"  // 頻道描述
    }

    // 🔸 初始化時自動建立通知頻道
    init {
        createNotificationChannel()
    }

    // 🔸 建立通知頻道（只在 Android 8.0 以上執行）
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // 🔸 顯示即將到期的食材通知（外部會呼叫這個 function）
    fun showExpiryNotification(foodName: String, expiryDate: String) {
        // 🔹 點擊通知後要跳轉到 Main 畫面
        val intent = Intent(context, Main::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // 🔹 包裝成 PendingIntent，讓通知知道要跳去哪
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // 🔹 建立通知內容與樣式
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm) // 通知圖示（你要有這張圖）
            .setContentTitle("食品即將到期提醒") // 通知標題
            .setContentText("$foodName 將在 $expiryDate 到期") // 通知內文
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // 通知優先級
            .setContentIntent(pendingIntent) // 點擊後動作
            .setAutoCancel(true) // 點擊後自動關閉通知
            .build()

        // 🔹 檢查是否有通知權限（Android 13+ 特別檢查），再發送通知
        with(NotificationManagerCompat.from(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    notify(System.currentTimeMillis().toInt(), notification)
                }
            } else {
                notify(System.currentTimeMillis().toInt(), notification)
            }
        }
    }
}
