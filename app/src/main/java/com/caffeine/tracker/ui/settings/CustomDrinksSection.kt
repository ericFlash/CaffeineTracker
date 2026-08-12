package com.caffeine.tracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.caffeine.tracker.data.model.CustomDrinkEmojis

@Composable
fun CustomDrinksSection(viewModel: SettingsViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("自定义饮品", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "新增饮品",
                    tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (state.customDrinks.isEmpty()) {
            Text("暂无自定义饮品，点击右上角 ＋ 添加", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }

        state.customDrinks.forEach { drink ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(drink.emoji, fontSize = MaterialTheme.typography.titleMedium.fontSize,
                    modifier = Modifier.padding(end = 10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(drink.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text("%.0f mg / %d ml".format(drink.caffeineMg, drink.standardVolumeMl),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                IconButton(onClick = { viewModel.deleteCustomDrink(drink) }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    if (showAdd) {
        CustomDrinkAddDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, emoji, mg, ml ->
                viewModel.addCustomDrink(name, emoji, mg, ml)
                showAdd = false
            }
        )
    }
}

@Composable
private fun CustomDrinkAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("☕") }
    var mg by remember { mutableStateOf("") }
    var ml by remember { mutableStateOf("240") }

    val mgValue = mg.toDoubleOrNull() ?: 0.0
    val mlValue = ml.toIntOrNull() ?: 0
    val valid = name.isNotBlank() && mgValue > 0 && mlValue > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增自定义饮品") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("名称") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Text("图标", style = MaterialTheme.typography.labelMedium)
                EmojiPicker(selected = emoji, onSelect = { emoji = it })
                OutlinedTextField(value = mg, onValueChange = { mg = it },
                    label = { Text("咖啡因 (mg)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ml, onValueChange = { ml = it },
                    label = { Text("默认杯量 (ml)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, emoji, mgValue, mlValue) }, enabled = valid) {
                Text("保存")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun EmojiPicker(selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(CustomDrinkEmojis.all) { emoji ->
            val isSelected = emoji == selected
            Column(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(emoji) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(emoji, fontSize = MaterialTheme.typography.titleMedium.fontSize)
            }
        }
    }
}
