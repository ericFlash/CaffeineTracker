package com.caffeine.tracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DrinkRecord::class], version = 1, exportSchema = false)
abstract class CaffeineDatabase : RoomDatabase() {
    abstract fun drinkDao(): DrinkDao

    companion object {
        @Volatile
        private var INSTANCE: CaffeineDatabase? = null

        fun getInstance(context: Context): CaffeineDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CaffeineDatabase::class.java,
                    "caffeine_tracker.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
