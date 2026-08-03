package com.caffeine.tracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DrinkDao {
    @Insert
    suspend fun insert(record: DrinkRecord)

    @Delete
    suspend fun delete(record: DrinkRecord)

    @Query("SELECT * FROM drink_records WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp ASC")
    fun getRecordsForDay(startOfDay: Long, endOfDay: Long): Flow<List<DrinkRecord>>

    @Query("SELECT * FROM drink_records WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp ASC")
    suspend fun getRecordsForDayOnce(startOfDay: Long, endOfDay: Long): List<DrinkRecord>

    @Query("SELECT * FROM drink_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<DrinkRecord>>

    @Query("SELECT * FROM drink_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<DrinkRecord>>

    @Query("SELECT SUM(caffeineMg) FROM drink_records WHERE timestamp >= :startOfDay AND timestamp < :endOfDay")
    suspend fun getTotalCaffeineForDay(startOfDay: Long, endOfDay: Long): Double?

    @Query("SELECT * FROM drink_records WHERE id = :id")
    suspend fun getById(id: Long): DrinkRecord?
}
