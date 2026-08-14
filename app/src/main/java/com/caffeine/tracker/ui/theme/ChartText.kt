package com.caffeine.tracker.ui.theme

import android.graphics.Paint

/**
 * Canvas 内文字统一从这里创建 Paint。
 * 关键：textSize 必须乘 density，否则高 DPI 设备文字会过小。
 * 用法示例：
 *   val p = ChartText.paint(ChartText.AXIS_SP, textArgb, density, fontScale,
 *                            Paint.Align.CENTER, alpha = 100)
 *   drawContext.canvas.nativeCanvas.drawText(label, x, y, p)
 */
object ChartText {
    const val AXIS_SP = 11f            // Y 轴刻度 / X 轴时间 / 柱状图星期
    const val ANNOTATION_SP = 11f      // "每日限额""睡眠安全"注释
    const val HEATMAP_DAY_SP = 10f     // 热力图日期
    const val HEATMAP_VALUE_SP = 10f   // 热力图数值
    const val HEATMAP_HEADER_SP = 11f  // 热力图星期表头
    const val LEGEND_SP = 10f          // 图例

    fun paint(
        sp: Float,
        colorArgb: Int,
        density: Float,
        fontScale: Float,
        align: Paint.Align = Paint.Align.CENTER,
        alpha: Int = 255,
    ): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp * density * fontScale
        this.color = colorArgb
        textAlign = align
        this.alpha = alpha
    }

    /** 图表内字体缩放 clamp，防系统大字号下标签重叠。 */
    fun clampFontScale(raw: Float): Float = raw.coerceIn(1.0f, 1.3f)
}
