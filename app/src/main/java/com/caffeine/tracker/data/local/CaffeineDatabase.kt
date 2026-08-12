package com.caffeine.tracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [DrinkRecord::class, CustomDrink::class], version = 3, exportSchema = false)
abstract class CaffeineDatabase : RoomDatabase() {
    abstract fun drinkDao(): DrinkDao
    abstract fun customDrinkDao(): CustomDrinkDao

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

        // v2 -> v3：新增 custom_drinks 表（自定义饮品库），不动 drink_records
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `custom_drinks` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`emoji` TEXT NOT NULL DEFAULT '\u2615', " +
                        "`caffeineMg` REAL NOT NULL, " +
                        "`standardVolumeMl` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
