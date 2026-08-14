package com.caffeine.tracker.ui.theme

import androidx.compose.ui.unit.dp

/** 间距/圆角/触控令牌，全 App 唯一来源。 */
object AppDimens {
    val ScreenPadding = 16.dp       // 屏幕水平内边距
    val CardInnerPadding = 16.dp    // 卡片内边距
    val RadiusListItem = 12.dp      // 列表项圆角
    val RadiusCard = 16.dp          // 常规卡片圆角
    val RadiusFeatureCard = 20.dp   // 特色卡片（首页头部/曲线/统计卡）
    val MinTouchTarget = 48.dp      // 最小触控目标
    val IconTouchInner = 20.dp      // 图标本体（外壳 48dp 内）
}
