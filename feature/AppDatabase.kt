package com.example.smartfridgeassistant

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FoodItem::class,
        WasteItem::class,
        EatenItem::class,
        DeletedItem::class
    ],
    version = 3,  // ← 確保你有改成大於原本的版本（1 → 2）
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun wasteDao(): WasteDao
    abstract fun eatenDao(): EatenDao
    abstract fun deletedDao(): DeletedDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "food_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
