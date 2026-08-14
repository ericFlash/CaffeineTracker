package com.caffeine.tracker.ui.theme

import androidx.compose.ui.unit.sp

/**
 * 语义化排版令牌。全 App 按"角色"引用字号，禁止在业务 Composable 里散写字号魔数。
 * 角色 → 字号映射见 README 表；此处是唯一来源。
 */
object AppType {
    val PageTitle = 22.sp       // 页面标题（历史/统计/设置）
    val SectionTitle = 16.sp    // 区块标题 L1（今日记录/近7天趋势/代谢参数/步骤①②等）
    val CardLabel = 12.sp       // 卡片小标题 L2（咖啡因含量/近7日均值 等标签）
    val HeroValue = 36.sp       // Hero 主数值（首页"当前体内咖啡因"）
    val StatValue = 28.sp       // 统计卡关键值（均值/最爱饮品）
    val ListTitle = 16.sp       // 列表主文（饮品名）
    val ListSecondary = 12.sp   // 列表副文（mg/ml、时间戳、说明、弱提示）
    val SliderLabel = 14.sp     // 设置页滑杆标签
    val GridName = 13.sp        // 添加页饮品网格名字
}

/** 弱化文字透明度令牌，统一全 App 对比度。 */
object AppAlpha {
    const val Primary = 1.0f    // 主文
    const val Secondary = 0.65f // 副文/时间戳
    const val Hint = 0.55f      // 弱提示/占位
}
