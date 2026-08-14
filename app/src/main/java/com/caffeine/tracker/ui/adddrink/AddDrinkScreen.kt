package com.caffeine.tracker.ui.adddrink

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.caffeine.tracker.data.model.DrinkTemplate
import com.caffeine.tracker.ui.theme.AppAlpha
import com.caffeine.tracker.ui.theme.AppDimens
import com.caffeine.tracker.ui.theme.AppType
import java.util.Calendar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDrinkScreen(
    viewModel: AddDrinkViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val sizeIndex = if (state.recentDrinks.isNotEmpty()) 2 else 1

    // 选中饮品后自动滚动到「② 杯量」，避免杯量藏在列表深处被直接跳过
    LaunchedEffect(state.selectedDrink) {
        if (state.selectedDrink != null) {
            listState.animateScrollToItem(sizeIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加饮品", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            if (state.selectedDrink != null) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Button(
                        onClick = {
                            scope.launch {
                                if (viewModel.saveRecord()) onSaved()
                            }
                        },
                        // 未显式选择杯量（caffeine 未计算）时禁用，防止误点直接记录
                        enabled = !state.saving && state.calculatedCaffeine > 0,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (state.saving) "保存中…" else "记录", modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.recentDrinks.isNotEmpty()) {
                item(key = "recent") {
                    Column {
                        Text("最近常用", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.recentDrinks, key = { "${it.name}-${it.emoji}" }) { drink ->
                                FilterChip(
                                    selected = state.selectedDrink?.name == drink.name,
                                    onClick = { viewModel.selectRecent(drink) },
                                    label = { Text("${drink.emoji} ${drink.name}") }
                                )
                            }
                        }
                    }
                }
            }

            item(key = "drink") {
                Column {
                    Text("① 选择饮品", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    DrinkGrid(
                        drinks = state.drinks,
                        selectedDrink = state.selectedDrink,
                        onDrinkSelected = { viewModel.selectDrink(it) }
                    )
                }
            }

            item(key = "size") {
                SizeChips(
                    enabled = state.selectedDrink != null,
                    sizes = state.selectedDrink?.sizes?.map { it.label } ?: emptyList(),
                    selectedSizeLabel = state.selectedSize?.label ?: "",
                    showCustomVolume = state.showCustomVolume,
                    onSizeSelected = { label ->
                        state.selectedDrink?.let { d ->
                            val match = d.sizes.find { it.label == label }
                            if (match != null) viewModel.selectSize(match)
                        }
                    },
                    onCustomSelected = {
                        state.selectedDrink?.let { d ->
                            viewModel.setCustomVolume(d.standardVolumeMl.toString())
                        }
                    }
                )
            }

            item(key = "time") {
                TimeField(
                    enabled = state.selectedDrink != null,
                    timestamp = state.timestamp,
                    onTimeSelected = { h, m -> viewModel.setCustomTime(h, m) },
                    onNow = { viewModel.setTimestampNow() }
                )
            }

            if (state.showCustomVolume && state.selectedDrink != null) {
                item(key = "custom") {
                    OutlinedTextField(
                        value = state.customVolumeMl,
                        onValueChange = { viewModel.setCustomVolume(it) },
                        label = { Text("自定义毫升数") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (state.selectedDrink != null && state.calculatedCaffeine > 0) {
                item(key = "caffeine") {
                    Column {
                        Text("③ 咖啡因含量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                Text("咖啡因含量", style = MaterialTheme.typography.labelMedium)
                                Text("%.0f mg".format(state.calculatedCaffeine),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeField(
    enabled: Boolean,
    timestamp: Long,
    onTimeSelected: (Int, Int) -> Unit,
    onNow: () -> Unit,
) {
    val now = remember { Calendar.getInstance() }
    val cal = remember(timestamp) {
        Calendar.getInstance().apply { if (timestamp > 0) timeInMillis = timestamp }
    }
    val isNow = timestamp <= 0
    val hour = if (isNow) now.get(Calendar.HOUR_OF_DAY) else cal.get(Calendar.HOUR_OF_DAY)
    val minute = if (isNow) now.get(Calendar.MINUTE) else cal.get(Calendar.MINUTE)
    var localHour by remember { mutableStateOf(hour) }
    var localMinute by remember { mutableStateOf(minute) }

    Column {
        Text("时间", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Surface(
            onClick = { /* 卡片整体不可点；时/分数字框单独操作 */ },
            enabled = enabled,
            shape = RoundedCornerShape(AppDimens.RadiusListItem),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NumberColumn(
                        value = localHour,
                        range = 0..23,
                        onValueChange = {
                            localHour = it
                            onTimeSelected(localHour, localMinute)
                        },
                        label = "时"
                    )
                    Text(
                        ":",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    NumberColumn(
                        value = localMinute,
                        range = 0..59,
                        onValueChange = {
                            localMinute = it
                            onTimeSelected(localHour, localMinute)
                        },
                        label = "分"
                    )
                }
                Spacer(Modifier.height(10.dp))
                FilterChip(
                    selected = isNow,
                    onClick = onNow,
                    label = { Text("现在") }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isNow) "按当前时刻计入"
                           else "已选过去时间，将按该时刻计入",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isNow) MaterialTheme.colorScheme.onSurface.copy(alpha = AppAlpha.Hint)
                            else MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun NumberColumn(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = {
                val next = if (value + 1 > range.last) range.first else value + 1
                onValueChange(next)
            },
            modifier = Modifier.size(AppDimens.IconTouchInner)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "增加")
        }
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(48.dp)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "%02d".format(value),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(
            onClick = {
                val prev = if (value - 1 < range.first) range.last else value - 1
                onValueChange(prev)
            },
            modifier = Modifier.size(AppDimens.IconTouchInner)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "减少")
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = AppAlpha.Hint)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DrinkGrid(
    drinks: List<DrinkTemplate>,
    selectedDrink: DrinkTemplate?,
    onDrinkSelected: (DrinkTemplate) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        drinks.forEach { drink ->
            DrinkGridItem(
                drink = drink,
                selected = selectedDrink?.name == drink.name,
                onClick = { onDrinkSelected(drink) }
            )
        }
    }
}

@Composable
private fun DrinkGridItem(
    drink: DrinkTemplate,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = container,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor
        ),
        modifier = Modifier
            .fillMaxWidth(0.31f)
            .defaultMinSize(minHeight = 84.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = drink.emoji,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = drink.name,
                fontSize = AppType.GridName,
                textAlign = TextAlign.Center,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SizeChips(
    enabled: Boolean,
    sizes: List<String>,
    selectedSizeLabel: String,
    showCustomVolume: Boolean,
    onSizeSelected: (String) -> Unit,
    onCustomSelected: () -> Unit,
) {
    Column {
        Text("② 杯量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        if (!enabled) {
            Text("请先选择饮品，再选择杯量",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sizes.forEach { label ->
                    FilterChip(
                        selected = !showCustomVolume && selectedSizeLabel == label,
                        onClick = { onSizeSelected(label) },
                        label = { Text(label) }
                    )
                }
                FilterChip(
                    selected = showCustomVolume,
                    onClick = onCustomSelected,
                    label = { Text("自定义") }
                )
            }
        }
    }
}
