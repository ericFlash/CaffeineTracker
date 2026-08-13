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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.caffeine.tracker.data.model.DrinkTemplate
import com.caffeine.tracker.ui.components.SizeChips
import kotlinx.coroutines.launch

@Composable
fun AddDrinkScreen(
    viewModel: AddDrinkViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text("添加饮品", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (state.recentDrinks.isNotEmpty()) {
            Text("最近常用", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.recentDrinks, key = { "${it.name}-${it.emoji}" }) { drink ->
                    FilterChip(
                        selected = state.selectedDrink?.name == drink.name,
                        onClick = {
                            viewModel.selectRecent(drink)
                        },
                        label = { Text("${drink.emoji} ${drink.name}") }
                    )
                }
            }
        }

        Text("选择饮品", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        DrinkGrid(
            drinks = state.drinks,
            selectedDrink = state.selectedDrink,
            onDrinkSelected = { viewModel.selectDrink(it) }
        )

        Text("② 杯量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        if (state.selectedDrink == null) {
            Text("请先选择饮品后设置杯量", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        } else {
            SizeChips(
                sizes = state.selectedDrink!!.sizes.map { it.label },
                selectedLabel = state.selectedSize?.label ?: "",
                showCustom = state.showCustomVolume,
                onSizeSelected = { label ->
                    val match = state.selectedDrink!!.sizes.find { it.label == label }
                    if (match != null) viewModel.selectSize(match)
                },
                onCustomSelected = {
                    viewModel.setCustomVolume(state.selectedDrink!!.standardVolumeMl.toString())
                }
            )
        }

        if (state.selectedDrink != null) {
            if (state.showCustomVolume) {
                OutlinedTextField(
                    value = state.customVolumeMl,
                    onValueChange = { viewModel.setCustomVolume(it) },
                    label = { Text("自定义毫升数") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

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

            Button(
                onClick = {
                    scope.launch {
                        if (viewModel.saveRecord()) onSaved()
                    }
                },
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (state.saving) "保存中…" else "记录摄入", modifier = Modifier.padding(8.dp))
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
