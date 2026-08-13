package com.caffeine.tracker.ui.adddrink

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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.caffeine.tracker.data.model.DrinkTemplate
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
                        Text("最近常用", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                    Text("① 选择饮品", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
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
        Text("② 杯量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
