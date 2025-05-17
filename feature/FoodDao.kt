package com.example.smartfridgeassistant

import androidx.room.*

@Dao
interface FoodDao {
    @Insert
    suspend fun insert(item: FoodItem)

    @Query("SELECT * FROM food_table")
    suspend fun getAll(): List<FoodItem>

    @Query("SELECT * FROM food_table WHERE category = :category")
    suspend fun getByCategory(category: String): List<FoodItem>

    @Query("SELECT * FROM food_table")
    suspend fun getAllFoods(): List<FoodItem>

    @Delete
    suspend fun delete(item: FoodItem)

    @Update
    suspend fun update(item: FoodItem)
}

@Dao
interface WasteDao {
    @Insert
    suspend fun insert(item: WasteItem)

    @Query("SELECT * FROM waste_table")
    suspend fun getAll(): List<WasteItem>
    
    @Delete
    suspend fun delete(item: WasteItem)
}

@Dao
interface EatenDao {
    @Insert
    suspend fun insert(item: EatenItem)

    @Query("SELECT * FROM eaten_table")
    suspend fun getAll(): List<EatenItem>
    
    @Delete
    suspend fun delete(item: EatenItem)
}
