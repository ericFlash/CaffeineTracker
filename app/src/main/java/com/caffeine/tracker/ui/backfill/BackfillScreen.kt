package com.caffeine.tracker.ui.backfill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.caffeine.tracker.data.model.DrinkTemplate
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackfillScreen(
    viewModel: BackfillViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("补录饮品", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BackfillDatePicker(
            selectedDateMillis = state.selectedDateUtcMillis,
            onDateSelected = viewModel::setDate
        )

        BackfillTimePicker(
            hour = state.selectedHour,
            minute = state.selectedMinute,
            onTimeSelected = viewModel::setTime
        )

        BackfillDrinkDropdown(
            drinks = state.drinks,
            selectedDrink = state.selectedDrink,
            onDrinkSelected = viewModel::selectDrink
        )

        BackfillSizeChips(
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

        if (state.selectedDrink != null) {
            if (state.showCustomVolume) {
                OutlinedTextField(
                    value = state.customVolumeMl,
                    onValueChange = viewModel::setCustomVolume,
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
                Text(if (state.saving) "保存中…" else "保存补录", modifier = Modifier.padding(8.dp))
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackfillDatePicker(
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val df = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val display = df.format(java.util.Date(selectedDateMillis))

    BackfillPickerField("① 日期", display, { showDialog = true })

    if (showDialog) {
        val today = BackfillUiState.Companion.todayUtcMillis()
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis,
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= today
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { onDateSelected(it) }
                    showDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackfillTimePicker(
    hour: Int,
    minute: Int,
    onTimeSelected: (Int, Int) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val display = "%02d:%02d".format(hour, minute)

    BackfillPickerField("② 时间", display, { showDialog = true })

    if (showDialog) {
        val timeState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeSelected(timeState.hour, timeState.minute)
                    showDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("取消") }
            },
            text = {
                TimePicker(state = timeState)
            }
        )
    }
}

@Composable
private fun BackfillDrinkDropdown(
    drinks: List<DrinkTemplate>,
    selectedDrink: DrinkTemplate?,
    onDrinkSelected: (DrinkTemplate) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val displayText = selectedDrink?.let { "${it.emoji} ${it.name}" } ?: "点击选择饮品"

    BackfillPickerField("③ 选择饮品", displayText, { showDialog = true })

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackfillSizeChips(
    enabled: Boolean,
    sizes: List<String>,
    selectedSizeLabel: String,
    showCustomVolume: Boolean,
    onSizeSelected: (String) -> Unit,
    onCustomSelected: () -> Unit,
) {
    Column {
        Text("④ 杯量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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

@Composable
private fun BackfillPickerField(
    label: String,
    displayText: String,
    onClick: () -> Unit,
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
