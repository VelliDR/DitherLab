package com.ditherlab.ultra.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ditherlab.ultra.ui.theme.PrimaryLightSage
import com.ditherlab.ultra.ui.theme.SurfaceVariantDark

@Composable
fun LayerSelectorTab(
    modifier: Modifier = Modifier,
    selectedLayer: Int,
    onLayerSelected: (Int) -> Unit
) {
    val layers = listOf("[ Tüm Ekran ]", "[ Sadece Özne ]", "[ Sadece Zemin ]")
    
    Row(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        layers.forEachIndexed { index, label ->
            FilterChip(
                selected = selectedLayer == index,
                onClick = { onLayerSelected(index) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryLightSage,
                    containerColor = SurfaceVariantDark
                )
            )
        }
    }
}
