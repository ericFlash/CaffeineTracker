package com.caffeine.tracker.domain

import com.caffeine.tracker.data.local.DrinkRecord
import kotlin.math.exp
import kotlin.math.ln

object CaffeinePharmacokinetics {

    const val SLEEP_SAFE_MG = 50.0

    // 计算体内残留咖啡因时回溯的历史窗口（小时）。
    // 半衰期约 5h，48h 后残留可忽略；用于避免跨午夜时把昨日记录漏算导致清零。
    const val RESIDUAL_WINDOW_HOURS = 48L

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

    // 计算某时刻之前摄入、到该时刻仍未代谢完的残留量。
    // 用于"今日可用限额 = 日限额 − 今日零点结转残留"，避免把今日已喝的重复计入。
    fun calculateCarryoverLevel(
        records: List<DrinkRecord>,
        halfLifeHours: Double = 5.0,
        atTime: Long
    ): Double = calculateCarryoverLevel(
        records.map { it.caffeineMg }, records.map { it.timestamp }, halfLifeHours, atTime
    )

    fun calculateCarryoverLevel(
        caffeineMgs: List<Double>,
        timestamps: List<Long>,
        halfLifeHours: Double,
        atTime: Long
    ): Double {
        val idx = caffeineMgs.indices.filter { timestamps[it] <= atTime }
        if (idx.isEmpty()) return 0.0
        return calculateCurrentLevel(
            idx.map { caffeineMgs[it] }, idx.map { timestamps[it] }, halfLifeHours, atTime
        )
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
        intervalMinutes: Int = 1
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
