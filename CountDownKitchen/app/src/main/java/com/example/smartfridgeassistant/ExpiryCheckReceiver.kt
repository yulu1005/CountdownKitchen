package com.example.smartfridgeassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ExpiryCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val foodDao = database.foodDao()
                val notificationHelper = NotificationHelper(context)

                val sharedPrefs = context.getSharedPreferences("Reminders", Context.MODE_PRIVATE)

                val allFoods = foodDao.getAll()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = Calendar.getInstance()




                for (food in allFoods) {
                    val expiryDate = dateFormat.parse(food.expiryDate)
                    if (expiryDate != null) {
                        val diffInMillis = expiryDate.time - today.timeInMillis
                        val diffDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

                        // 讀取對應這個食材名稱的提醒設定（幾天前）
                        val key = "reminder_${food.name}"
                        if (sharedPrefs.contains(key)) {
                            val reminderDaysBefore = sharedPrefs.getInt(key, -1)

                            if (diffDays == reminderDaysBefore) {
                                // 時間到了！發送通知
                                notificationHelper.showExpiryNotification(food.name, food.expiryDate)
                                Log.d("ExpiryCheckReceiver", "發送通知: ${food.name}, 剩 $diffDays 天到期")
                            }
                        }
                    }
                }

                Log.d("ExpiryCheckReceiver", "提醒檢查完成")

            } catch (e: Exception) {
                Log.e("ExpiryCheckReceiver", "提醒處理發生錯誤", e)
            }
        }
    }
}
