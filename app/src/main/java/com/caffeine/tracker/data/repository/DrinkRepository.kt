package com.caffeine.tracker.data.repository

import com.caffeine.tracker.data.local.DrinkDao
import com.caffeine.tracker.data.local.DrinkRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrinkRepository @Inject constructor(
    private val drinkDao: DrinkDao
) {
    fun getAllRecords(): Flow<List<DrinkRecord>> = drinkDao.getAllRecords()

    fun getRecentRecords(limit: Int = 20): Flow<List<DrinkRecord>> =
        drinkDao.getRecentRecords(limit)

    fun getRecentDrinks(limit: Int = 5): Flow<List<DrinkRecord>> =
        drinkDao.getRecentDrinks(limit)

    fun getRecordsForDay(startOfDay: Long, endOfDay: Long): Flow<List<DrinkRecord>> =
        drinkDao.getRecordsForDay(startOfDay, endOfDay)

    suspend fun getRecordsForDayOnce(startOfDay: Long, endOfDay: Long): List<DrinkRecord> =
        drinkDao.getRecordsForDayOnce(startOfDay, endOfDay)

    suspend fun getRecordsSince(since: Long): List<DrinkRecord> =
        drinkDao.getRecordsSince(since)

    fun getRecordsSinceFlow(since: Long): Flow<List<DrinkRecord>> =
        drinkDao.getRecordsSinceFlow(since)

    suspend fun insert(record: DrinkRecord) = drinkDao.insert(record)

    suspend fun delete(record: DrinkRecord) = drinkDao.delete(record)

    suspend fun getTotalCaffeineForDay(startOfDay: Long, endOfDay: Long): Double =
        drinkDao.getTotalCaffeineForDay(startOfDay, endOfDay) ?: 0.0
}
