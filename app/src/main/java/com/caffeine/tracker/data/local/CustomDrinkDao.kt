package com.caffeine.tracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomDrinkDao {
    @Query("SELECT * FROM custom_drinks ORDER BY createdAt ASC")
    fun getAll(): Flow<List<CustomDrink>>

    @Query("SELECT * FROM custom_drinks ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<CustomDrink>

    @Insert
    suspend fun insert(drink: CustomDrink)

    @Delete
    suspend fun delete(drink: CustomDrink)
}
