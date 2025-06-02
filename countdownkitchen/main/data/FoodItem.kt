package com.example.smartfridgeassistant

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_table")
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // ➤ 自動產生主鍵
    val name: String,      // ➤ 食材名稱
    val category: String,  // ➤ 分類（冷藏 / 冷凍 / 常溫）
    val expiryDate: String,// ➤ 到期日（格式：yyyy-MM-dd）
    val note: String,      // ➤ 備註欄位
    val type: String       // ➤ 食材類型（例如 蔬菜 / 肉類 / 飲料）
)

@Entity(tableName = "waste_table")
data class WasteItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // ➤ 主鍵
    val name: String,      // ➤ 食材名稱
    val category: String,  // ➤ 分類
    val note: String,      // ➤ 備註
    val type: String,      // ➤ 類型
    val date: String       // ➤ 加入廚餘的日期
)

@Entity(tableName = "eaten_table")
data class EatenItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // ➤ 主鍵
    val name: String,      // ➤ 食材名稱
    val category: String,  // ➤ 分類
    val note: String,      // ➤ 備註
    val type: String,      // ➤ 類型
    val date: String       // ➤ 完食日期
)

@Entity(tableName = "deleted_table")
data class DeletedItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val type: String,
    val expiryDate: String,
    val note: String
)
