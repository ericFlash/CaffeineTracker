package com.caffeine.tracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_drinks")
data class CustomDrink(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emoji: String = "☕",
    val caffeineMg: Double,
    val standardVolumeMl: Int,
    val createdAt: Long = System.currentTimeMillis()
)
