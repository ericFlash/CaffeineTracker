package com.caffeine.tracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.caffeine.tracker.data.repository.SettingsRepository
import com.caffeine.tracker.ui.theme.AppAlpha
import com.caffeine.tracker.ui.theme.AppType

private const val DEFAULT_WEIGHT = 70f
private const val DEFAULT_HALF_LIFE = 5.0f
private const val DEFAULT_LIMIT = 400f

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    viewModel: SettingsViewModel,
    onSettingsChanged: () -> Unit = {},
) {
    var halfLife by remember { mutableFloatStateOf(settingsRepository.halfLifeHours.toFloat()) }
    var dailyLimit by remember { mutableFloatStateOf(settingsRepository.dailyLimitMg.toFloat()) }
    var weight by remember { mutableFloatStateOf(settingsRepository.bodyWeightKg) }
    var limitCustomized by remember { mutableStateOf(settingsRepository.limitCustomized) }
    var showResetDialog by remember { mutableStateOf(false) }

    val autoApplied = !limitCustomized
    val recommendedLimit = (weight * 5.7f).coerceIn(200f, 600f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("代谢参数", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))

                LabeledSlider(
                    label = "咖啡因半衰期: %.1f 小时".format(halfLife),
                    value = halfLife,
                    valueRange = 2f..12f,
                    onValueChange = { halfLife = it },
                    onValueChangeFinished = {
                        settingsRepository.halfLifeHours = halfLife.toDouble()
                        onSettingsChanged()
                    }
                )
                Spacer(Modifier.height(12.dp))

                LabeledSlider(
                    label = "体重: %.0f kg".format(weight),
                    value = weight,
                    valueRange = 30f..150f,
                    onValueChange = { weight = it },
                    onValueChangeFinished = {
                        settingsRepository.bodyWeightKg = weight
                        // 仅当用户未自定义每日限额时，才自动按 5.7mg/kg 更新限额（200-600 区间）
                        if (!limitCustomized) {
                            val recommended = (weight * 5.7f).coerceIn(200f, 600f)
                            dailyLimit = recommended
                            settingsRepository.dailyLimitMg = recommended.toDouble()
                        }
                        onSettingsChanged()
                    }
                )

                Spacer(Modifier.height(12.dp))
                Text("每日安全限额: %.0f mg".format(dailyLimit),
                    style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (autoApplied)
                        "基于体重推荐: %.0f mg (%.0f kg × 5.7mg/kg)，已自动应用".format(recommendedLimit, weight)
                    else
                        "基于体重推荐: %.0f mg (%.0f kg × 5.7mg/kg)，已自定义限额，未自动覆盖".format(recommendedLimit, weight),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = dailyLimit,
                    onValueChange = { dailyLimit = it },
                    onValueChangeFinished = {
                        limitCustomized = true
                        settingsRepository.limitCustomized = true
                        settingsRepository.dailyLimitMg = dailyLimit.toDouble()
                        onSettingsChanged()
                    },
                    valueRange = 100f..1000f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("外观", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Column {
                    Text("深色/浅色模式跟随系统", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(Modifier.height(4.dp))
                    Text("如需切换请使用系统设置", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = AppAlpha.Hint))
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                CustomDrinksSection(viewModel = viewModel)
            }
        }

        Button(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("恢复默认设置")
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("恢复默认设置？") },
            text = {
                Text("将覆盖当前自定义参数（体重 / 半衰期 / 每日限额），恢复为默认值。此操作不可撤销。")
            },
            confirmButton = {
                TextButton(onClick = {
                    weight = DEFAULT_WEIGHT
                    halfLife = DEFAULT_HALF_LIFE
                    dailyLimit = DEFAULT_LIMIT
                    limitCustomized = false
                    settingsRepository.bodyWeightKg = DEFAULT_WEIGHT
                    settingsRepository.halfLifeHours = DEFAULT_HALF_LIFE.toDouble()
                    settingsRepository.dailyLimitMg = DEFAULT_LIMIT.toDouble()
                    settingsRepository.limitCustomized = false
                    onSettingsChanged()
                    showResetDialog = false
                }) { Text("恢复默认") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Text(label, fontSize = AppType.SliderLabel)
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished ?: {},
        valueRange = valueRange,
        modifier = Modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
    )
}
