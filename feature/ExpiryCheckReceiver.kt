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
                // 1. 获取数据库和通知助手
                val database = AppDatabase.getDatabase(context)
                val foodDao = database.foodDao()
                val notificationHelper = NotificationHelper(context)
                
                // 2. 获取设置
                val sharedPrefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
                val selectedTypes = sharedPrefs.getStringSet("food_types", setOf("all")) ?: setOf("all")
                val reminderTime = sharedPrefs.getInt("reminder_time", 0)
                
                // 3. 获取所有食品
                val allFoods = foodDao.getAll()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                
                // 4. 计算目标日期（例如：今天是9号，选择一周后就是16号）
                val targetDate = Calendar.getInstance()
                targetDate.add(Calendar.DAY_OF_MONTH, when (reminderTime) {
                    0 -> 7  // 一周后
                    1 -> 1  // 一天后
                    else -> 0  // 当天
                })
                val targetDateStr = dateFormat.format(targetDate.time)
                
                // 5. 记录初始状态
                Log.d("ExpiryCheckReceiver", "=== 开始处理通知 ===")
                Log.d("ExpiryCheckReceiver", "选中的类型: $selectedTypes")
                Log.d("ExpiryCheckReceiver", "目标日期: $targetDateStr")
                Log.d("ExpiryCheckReceiver", "数据库中的食品总数: ${allFoods.size}")
                
                // 6. 根据日期和类型筛选食品
                val filteredFoods = allFoods.filter { food ->
                    // 首先检查日期是否匹配
                    val foodExpiryDate = dateFormat.parse(food.expiryDate)
                    val targetDateParsed = dateFormat.parse(targetDateStr)
                    val datesMatch = foodExpiryDate?.time == targetDateParsed?.time
                    
                    // 然后检查类型是否匹配
                    val typeMatches = when {
                        // 如果选中了"全部"，则接受所有类型
                        selectedTypes.contains("all") -> {
                            Log.d("ExpiryCheckReceiver", "选中了全部类型")
                            true
                        }
                        // 如果食品类型在选中列表中，则匹配
                        selectedTypes.contains(food.type) -> {
                            Log.d("ExpiryCheckReceiver", "食品类型 ${food.type} 在选中列表中")
                            true
                        }
                        // 否则不匹配
                        else -> {
                            Log.d("ExpiryCheckReceiver", "食品类型 ${food.type} 不在选中列表中")
                            false
                        }
                    }
                    
                    // 记录每个食品的筛选过程
                    Log.d("ExpiryCheckReceiver", """
                        检查食品: ${food.name}
                        - 类型: ${food.type}
                        - 到期日: ${food.expiryDate}
                        - 目标日期: $targetDateStr
                        - 日期是否匹配: $datesMatch
                        - 类型是否匹配: $typeMatches
                        - 是否选中: ${datesMatch && typeMatches}
                    """.trimIndent())
                    
                    // 同时满足日期和类型条件
                    datesMatch && typeMatches
                }
                
                // 7. 记录筛选结果
                Log.d("ExpiryCheckReceiver", "筛选后的食品数量: ${filteredFoods.size}")
                Log.d("ExpiryCheckReceiver", "筛选后的食品列表:")
                filteredFoods.forEach { food ->
                    Log.d("ExpiryCheckReceiver", "- ${food.name} (${food.type}, 到期日: ${food.expiryDate})")
                }
                
                // 8. 发送通知
                if (filteredFoods.isNotEmpty()) {
                    Log.d("ExpiryCheckReceiver", "开始发送通知...")
                    filteredFoods.forEach { food ->
                        try {
                            notificationHelper.showExpiryNotification(food.name, food.expiryDate)
                            Log.d("ExpiryCheckReceiver", "已发送通知: ${food.name}")
                        } catch (e: Exception) {
                            Log.e("ExpiryCheckReceiver", "发送通知失败: ${food.name}", e)
                        }
                    }
                    Log.d("ExpiryCheckReceiver", "通知发送完成")
                } else {
                    Log.d("ExpiryCheckReceiver", "没有找到符合条件的食品，不发送通知")
                }
                
                Log.d("ExpiryCheckReceiver", "=== 处理完成 ===")
                
            } catch (e: Exception) {
                Log.e("ExpiryCheckReceiver", "处理过程中发生错误", e)
            }
        }
    }
}
