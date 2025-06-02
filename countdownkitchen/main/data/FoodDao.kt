package com.example.smartfridgeassistant

import androidx.room.*

@Dao
interface FoodDao {
    // ➤ 新增食材
    @Insert
    suspend fun insert(item: FoodItem)

    // ➤ 取得所有食材（兩個方法都可以，視命名習慣）
    @Query("SELECT * FROM food_table")
    suspend fun getAll(): List<FoodItem>

    @Query("SELECT * FROM food_table")
    suspend fun getAllFoods(): List<FoodItem>

    // ➤ 依分類查詢食材（例如 冷藏、冷凍）
    @Query("SELECT * FROM food_table WHERE category = :category")
    suspend fun getByCategory(category: String): List<FoodItem>

    // ➤ 刪除某筆食材
    @Delete
    suspend fun delete(item: FoodItem)

    // ➤ 更新某筆食材（例如修改到期日或備註）
    @Update
    suspend fun update(item: FoodItem)
}

@Dao
interface WasteDao {
    // ➤ 新增一筆廚餘資料
    @Insert
    suspend fun insert(item: WasteItem)

    // ➤ 查詢所有廚餘記錄
    @Query("SELECT * FROM waste_table")
    suspend fun getAll(): List<WasteItem>

    // ➤ 刪除某筆廚餘記錄（例如從分析頁返回食材時）
    @Delete
    suspend fun delete(item: WasteItem)
}

@Dao
interface EatenDao {
    // ➤ 新增一筆完食記錄
    @Insert
    suspend fun insert(item: EatenItem)

    // ➤ 查詢所有完食記錄
    @Query("SELECT * FROM eaten_table")
    suspend fun getAll(): List<EatenItem>

    // ➤ 刪除某筆完食記錄
    @Delete
    suspend fun delete(item: EatenItem)
}
@Dao
interface DeletedDao {
    // ➤ 新增一筆刪除記錄
    @Insert
    suspend fun insert(item: DeletedItem)

    // ➤ 查詢所有刪除記錄
    @Query("SELECT * FROM deleted_table")
    suspend fun getAll(): List<DeletedItem>

    // ➤ 刪除某筆刪除記錄
    @Delete
    suspend fun delete(item: DeletedItem)
}
