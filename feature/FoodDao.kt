package com.example.smartfridgeassistant

import androidx.room.*

@Dao
interface FoodDao {
    @Insert
    suspend fun insert(item: FoodItem)

    @Query("SELECT * FROM food_table")
    suspend fun getAll(): List<FoodItem>

    @Delete
    suspend fun delete(item: FoodItem)

    @Update
    suspend fun update(item: FoodItem)
}
