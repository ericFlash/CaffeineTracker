package com.caffeine.tracker.data.repository

import com.caffeine.tracker.data.local.CustomDrink
import com.caffeine.tracker.data.local.CustomDrinkDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomDrinkRepository @Inject constructor(
    private val customDrinkDao: CustomDrinkDao
) {
    fun getAll(): Flow<List<CustomDrink>> = customDrinkDao.getAll()

    suspend fun getAllOnce(): List<CustomDrink> = customDrinkDao.getAllOnce()

    suspend fun insert(drink: CustomDrink) = customDrinkDao.insert(drink)

    suspend fun delete(drink: CustomDrink) = customDrinkDao.delete(drink)
}
