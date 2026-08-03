package com.caffeine.tracker.domain

import com.caffeine.tracker.data.local.DrinkRecord
import kotlin.math.exp
import kotlin.math.ln

object CaffeinePharmacokinetics {

    const val SLEEP_SAFE_MG = 50.0

    fun calculateCurrentLevel(
        records: List<DrinkRecord>,
        halfLifeHours: Double = 5.0,
        now: Long = System.currentTimeMillis()
    ): Double = calculateCurrentLevel(records.map { it.caffeineMg }, records.map { it.timestamp }, halfLifeHours, now)

    fun calculateCurrentLevel(
        caffeineMgs: List<Double>,
        timestamps: List<Long>,
        halfLifeHours: Double = 5.0,
        now: Long = System.currentTimeMillis()
    ): Double {
        if (caffeineMgs.isEmpty()) return 0.0
        val lambda = ln(2.0) / (halfLifeHours * 3_600_000.0)
        return caffeineMgs.indices.sumOf { i ->
            val elapsedMs = (now - timestamps[i]).toDouble().coerceAtLeast(0.0)
            caffeineMgs[i] * exp(-lambda * elapsedMs)
        }
    }

    data class CurvePoint(
        val timestamp: Long,
        val level: Double
    )

    fun generateCurve(
        records: List<DrinkRecord>,
        halfLifeHours: Double,
        startTime: Long,
        endTime: Long,
        intervalMinutes: Int = 5
    ): List<CurvePoint> {
        val lambda = ln(2.0) / (halfLifeHours * 3_600_000.0)
        val intervalMs = intervalMinutes * 60_000L
        val points = mutableListOf<CurvePoint>()
        var t = startTime
        while (t <= endTime) {
            val level = records.sumOf { record ->
                val elapsedMs = (t - record.timestamp).toDouble().coerceAtLeast(0.0)
                record.caffeineMg * exp(-lambda * elapsedMs)
            }
            points.add(CurvePoint(t, level))
            t += intervalMs
        }
        return points
    }

    private fun timeToTargetMs(
        records: List<DrinkRecord>,
        halfLifeHours: Double,
        targetMg: Double,
        now: Long
    ): Long {
        val current = calculateCurrentLevel(records, halfLifeHours, now)
        if (current <= targetMg) return 0L
        val lambda = ln(2.0) / (halfLifeHours * 3_600_000.0)
        val tMs = ln(current / targetMg) / lambda
        return tMs.toLong().coerceAtLeast(0L)
    }

    fun estimatedTimeToZero(
        records: List<DrinkRecord>,
        halfLifeHours: Double,
        now: Long = System.currentTimeMillis()
    ): Long = timeToTargetMs(records, halfLifeHours, 1.0, now)

    fun estimatedTimeToSleepSafe(
        records: List<DrinkRecord>,
        halfLifeHours: Double,
        now: Long = System.currentTimeMillis()
    ): Long = timeToTargetMs(records, halfLifeHours, SLEEP_SAFE_MG, now)
}
