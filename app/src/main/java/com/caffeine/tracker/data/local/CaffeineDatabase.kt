package com.caffeine.tracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [DrinkRecord::class], version = 2, exportSchema = false)
abstract class CaffeineDatabase : RoomDatabase() {
    abstract fun drinkDao(): DrinkDao

    companion object {
        @Volatile
        private var INSTANCE: CaffeineDatabase? = null

        // v1 -> v2：为 drink_records 添加 emoji 列（非空，默认 "☕"），保留历史数据
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE drink_records ADD COLUMN emoji TEXT NOT NULL DEFAULT '\u2615'"
                )
            }
        }

        fun getInstance(context: Context): CaffeineDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CaffeineDatabase::class.java,
                    "caffeine_tracker.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
