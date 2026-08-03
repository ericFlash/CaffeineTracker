package com.caffeine.tracker.domain

import com.caffeine.tracker.data.local.DrinkRecord
import kotlin.math.exp
import kotlin.math.ln

object CaffeinePharmacokinetics {

    fun calculateCurrentLevel(
        records: List<DrinkRecord>,
        halfLifeHours: Double = 5.0,
        now: Long = System.currentTimeMillis()
    ): Double {
        if (records.isEmpty()) return 0.0
        val lambda = ln(2.0) / (halfLifeHours * 3_600_000.0)
        return records.sumOf { record ->
            val elapsedMs = (now - record.timestamp).toDouble().coerceAtLeast(0.0)
            record.caffeineMg * exp(-lambda * elapsedMs)
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

    fun estimatedTimeToZero(
        records: List<DrinkRecord>,
        halfLifeHours: Double,
        now: Long = System.currentTimeMillis()
    ): Long {
        val current = calculateCurrentLevel(records, halfLifeHours, now)
        if (current <= 1.0) return 0L
        val lambda = ln(2.0) / (halfLifeHours * 3_600_000.0)
        val hoursToZero = ln(current / 1.0) / (lambda * 3_600_000.0)
        return (hoursToZero * 3_600_000.0).toLong().coerceAtLeast(0L)
    }
}
