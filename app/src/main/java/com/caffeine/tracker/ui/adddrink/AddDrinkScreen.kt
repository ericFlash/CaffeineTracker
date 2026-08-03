package com.caffeine.tracker.ui.adddrink

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
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

@Composable
fun AddDrinkScreen(
    viewModel: AddDrinkViewModel,
    onSaved: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("选择饮品", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        items(state.drinks) { drink ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectDrink(drink) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.selectedDrink == drink)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(if (state.selectedDrink == drink) 2.dp else 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(drink.emoji, fontSize = MaterialTheme.typography.headlineSmall.fontSize)
                    Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(drink.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("%.0f mg / %dml".format(drink.defaultCaffeineMg, drink.standardVolumeMl),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
        }

        if (state.selectedDrink != null) {
            item {
                Spacer(Modifier.height(8.dp))
                SizeDropdown(
                    sizes = state.selectedDrink!!.sizes.map { it.label },
                    selectedLabel = if (state.showCustomVolume) "自定义 (${
                        state.customVolumeMl.takeIf { it.isNotEmpty() } ?: "0"
                    }ml)"
                    else state.selectedSize?.label ?: "",
                    onSizeSelected = { label ->
                        val drink = state.selectedDrink!!
                        val match = drink.sizes.find { it.label == label }
                        if (match != null) viewModel.selectSize(match)
                    },
                    onCustomSelected = {
                        val defaultVol = state.selectedDrink!!.standardVolumeMl
                        viewModel.setCustomVolume(defaultVol.toString())
                    }
                )
            }

            if (state.showCustomVolume) {
                item {
                    OutlinedTextField(
                        value = state.customVolumeMl,
                        onValueChange = { viewModel.setCustomVolume(it) },
                        label = { Text("自定义毫升数") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
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

            item {
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
}

@Composable
private fun SizeDropdown(
    sizes: List<String>,
    selectedLabel: String,
    onSizeSelected: (String) -> Unit,
    onCustomSelected: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text("杯量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null,
                    modifier = Modifier.clickable { expanded = true })
            },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            shape = RoundedCornerShape(12.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            sizes.forEach { label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSizeSelected(label)
                        expanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("自定义", fontWeight = FontWeight.Medium) },
                onClick = {
                    onCustomSelected()
                    expanded = false
                }
            )
        }
    }
}
