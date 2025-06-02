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
                // ✅ 1. 取得資料庫與通知工具
                val database = AppDatabase.getDatabase(context)
                val foodDao = database.foodDao()
                val notificationHelper = NotificationHelper(context)

                // ✅ 2. 取得使用者設定：選擇的食材類型與提醒時間
                val sharedPrefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
                val selectedTypes = sharedPrefs.getStringSet("food_types", setOf("all")) ?: setOf("all")
                val reminderTime = sharedPrefs.getInt("reminder_time", 0)

                // ✅ 3. 取得所有食材資料
                val allFoods = foodDao.getAll()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                // ✅ 4. 計算通知的目標日期（今天、明天、一週後）
                val targetDate = Calendar.getInstance()
                targetDate.add(Calendar.DAY_OF_MONTH, when (reminderTime) {
                    0 -> 7  // 一週後
                    1 -> 1  // 一天後
                    else -> 0  // 當天
                })
                val targetDateStr = dateFormat.format(targetDate.time)

                // ✅ 5. 輸出初始偵錯資訊
                Log.d("ExpiryCheckReceiver", "=== 開始處理通知 ===")
                Log.d("ExpiryCheckReceiver", "選中的類型: $selectedTypes")
                Log.d("ExpiryCheckReceiver", "目標日期: $targetDateStr")
                Log.d("ExpiryCheckReceiver", "資料庫中的食品總數: ${allFoods.size}")

                // ✅ 6. 篩選符合日期與類型的食材
                val filteredFoods = allFoods.filter { food ->
                    // ➤ 檢查到期日是否符合
                    val foodExpiryDate = dateFormat.parse(food.expiryDate)
                    val targetDateParsed = dateFormat.parse(targetDateStr)
                    val datesMatch = foodExpiryDate?.time == targetDateParsed?.time

                    // ➤ 檢查食材類型是否符合設定
                    val typeMatches = when {
                        selectedTypes.contains("all") -> {
                            Log.d("ExpiryCheckReceiver", "選中了全部類型")
                            true
                        }
                        selectedTypes.contains(food.type) -> {
                            Log.d("ExpiryCheckReceiver", "食品類型 ${food.type} 在選中列表中")
                            true
                        }
                        else -> {
                            Log.d("ExpiryCheckReceiver", "食品類型 ${food.type} 不在選中列表中")
                            false
                        }
                    }

                    // ➤ 顯示每筆食材檢查過程
                    Log.d("ExpiryCheckReceiver", """
                        檢查食品: ${food.name}
                        - 類型: ${food.type}
                        - 到期日: ${food.expiryDate}
                        - 目標日期: $targetDateStr
                        - 日期是否匹配: $datesMatch
                        - 類型是否匹配: $typeMatches
                        - 是否選中: ${datesMatch && typeMatches}
                    """.trimIndent())

                    datesMatch && typeMatches
                }

                // ✅ 7. 輸出篩選結果
                Log.d("ExpiryCheckReceiver", "篩選後的食品數量: ${filteredFoods.size}")
                Log.d("ExpiryCheckReceiver", "篩選後的食品列表:")
                filteredFoods.forEach { food ->
                    Log.d("ExpiryCheckReceiver", "- ${food.name} (${food.type}, 到期日: ${food.expiryDate})")
                }

                // ✅ 8. 發送通知給使用者（如果有符合條件的食材）
                if (filteredFoods.isNotEmpty()) {
                    Log.d("ExpiryCheckReceiver", "開始發送通知...")
                    filteredFoods.forEach { food ->
                        try {
                            notificationHelper.showExpiryNotification(food.name, food.expiryDate)
                            Log.d("ExpiryCheckReceiver", "已發送通知: ${food.name}")
                        } catch (e: Exception) {
                            Log.e("ExpiryCheckReceiver", "發送通知失敗: ${food.name}", e)
                        }
                    }
                    Log.d("ExpiryCheckReceiver", "通知發送完成")
                } else {
                    Log.d("ExpiryCheckReceiver", "沒有找到符合條件的食品，不發送通知")
                }

                Log.d("ExpiryCheckReceiver", "=== 處理完成 ===")

            } catch (e: Exception) {
                Log.e("ExpiryCheckReceiver", "處理過程中發生錯誤", e)
            }
        }
    }
}
