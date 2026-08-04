package com.caffeine.tracker.ui.adddrink

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun AddDrinkScreen(
    viewModel: AddDrinkViewModel,
    onSaved: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("添加饮品", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        DrinkDropdown(
            drinks = state.drinks,
            selectedDrink = state.selectedDrink,
            onDrinkSelected = { viewModel.selectDrink(it) }
        )

        if (state.selectedDrink != null) {
            SizeDropdown(
                sizes = state.selectedDrink!!.sizes.map { it.label },
                selectedLabel = if (state.showCustomVolume) "自定义 (${
                    state.customVolumeMl.takeIf { it.isNotEmpty() } ?: "0"
                }ml)"
                else state.selectedSize?.label ?: "",
                onSizeSelected = { label ->
                    val match = state.selectedDrink!!.sizes.find { it.label == label }
                    if (match != null) viewModel.selectSize(match)
                },
                onCustomSelected = {
                    val defaultVol = state.selectedDrink!!.standardVolumeMl
                    viewModel.setCustomVolume(defaultVol.toString())
                }
            )

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
                    viewModel.saveRecord()
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("记录摄入", modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
private fun DrinkDropdown(
    drinks: List<com.caffeine.tracker.data.model.DrinkTemplate>,
    selectedDrink: com.caffeine.tracker.data.model.DrinkTemplate?,
    onDrinkSelected: (com.caffeine.tracker.data.model.DrinkTemplate) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    val displayText = selectedDrink?.let { "${it.emoji} ${it.name}" } ?: "点击选择饮品"

    PickerField(
        label = "选择饮品",
        displayText = displayText,
        onClick = { showDialog = true }
    )

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().height(400.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("选择饮品", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(drinks, key = { it.name }) { drink ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(drink.emoji, fontSize = MaterialTheme.typography.titleMedium.fontSize)
                                        Column(modifier = Modifier.padding(start = 12.dp)) {
                                            Text(drink.name, fontWeight = FontWeight.Medium)
                                            Text("%.0f mg / %dml".format(drink.defaultCaffeineMg, drink.standardVolumeMl),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        }
                                    }
                                },
                                onClick = {
                                    onDrinkSelected(drink)
                                    showDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SizeDropdown(
    sizes: List<String>,
    selectedLabel: String,
    onSizeSelected: (String) -> Unit,
    onCustomSelected: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    PickerField(
        label = "杯量",
        displayText = selectedLabel,
        onClick = { showDialog = true }
    )

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().height(360.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("选择杯量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(sizes) { label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onSizeSelected(label)
                                    showDialog = false
                                }
                            )
                        }
                        item {
                            DropdownMenuItem(
                                text = { Text("自定义", fontWeight = FontWeight.Medium) },
                                onClick = {
                                    onCustomSelected()
                                    showDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerField(
    label: String,
    displayText: String,
    onClick: () -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        androidx.compose.material3.Surface(
            onClick = { onClick() },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (displayText.isBlank()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "选择",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
    }
}
