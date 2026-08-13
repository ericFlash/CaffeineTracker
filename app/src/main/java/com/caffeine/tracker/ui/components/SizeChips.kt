package com.caffeine.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SizeChips(
    sizes: List<String>,
    selectedLabel: String,
    showCustom: Boolean,
    onSizeSelected: (String) -> Unit,
    onCustomSelected: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sizes.forEach { label ->
            FilterChip(
                selected = !showCustom && selectedLabel == label,
                onClick = { onSizeSelected(label) },
                label = { Text(label) }
            )
        }
        FilterChip(
            selected = showCustom,
            onClick = onCustomSelected,
            label = { Text("自定义") }
        )
    }
}
