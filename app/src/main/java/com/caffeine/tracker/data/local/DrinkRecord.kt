package com.caffeine.tracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drink_records")
data class DrinkRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val drinkName: String,
    val emoji: String = "☕",
    val caffeineMg: Double,
    val volumeMl: Int,
    val timestamp: Long,
    val note: String? = null
)
