package com.caffeine.tracker.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * 状态色/阈值单一来源。首页圆点、曲线、柱状图、热力图、小组件统一调用。
 * 断点统一为 25/50/75（原 StatsScreen/Glance 用 34/67，须改到此处）。
 */
object CaffeineLevels {
    const val T1 = 0.25f // 绿→黄
    const val T2 = 0.50f // 黄→橙
    const val T3 = 0.75f // 橙→红

    private val lightGreen  = Color(0xFF8CAF8A)
    private val lightYellow = Color(0xFFC7A34A)
    private val lightOrange = Color(0xFFD98E4A)
    private val lightRed    = Color(0xFFC2563C)

    private val darkGreen  = Color(0xFFA8C8A4)
    private val darkYellow = Color(0xFFE6C26A)
    private val darkOrange = Color(0xFFEFA968)
    private val darkRed    = Color(0xFFE0735A)

    fun ramp(dark: Boolean): List<Color> =
        if (dark) listOf(darkGreen, darkYellow, darkOrange, darkRed)
        else listOf(lightGreen, lightYellow, lightOrange, lightRed)

    fun colorForRatio(ratio: Float, dark: Boolean): Color = when {
        ratio >= T3 -> if (dark) darkRed else lightRed
        ratio >= T2 -> if (dark) darkOrange else lightOrange
        ratio >= T1 -> if (dark) darkYellow else lightYellow
        else -> if (dark) darkGreen else lightGreen
    }

    /** 连续渐变（曲线填充、热力图数值、小组件时点色）。value/maxVal 需同单位。 */
    fun gradient(value: Double, maxVal: Double, dark: Boolean): Color {
        val ramp = ramp(dark)
        val t = (value / maxVal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
        return when {
            t < T1 -> lerp(ramp[0], ramp[1], t / T1)
            t < T2 -> lerp(ramp[1], ramp[2], (t - T1) / (T2 - T1))
            t < T3 -> lerp(ramp[2], ramp[3], (t - T2) / (T3 - T2))
            else -> ramp[3]
        }
    }
}
