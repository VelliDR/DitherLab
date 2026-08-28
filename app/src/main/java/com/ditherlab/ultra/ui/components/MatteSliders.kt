package com.ditherlab.ultra.ui.components

import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ditherlab.ultra.ui.theme.PrimaryMatteGreen
import com.ditherlab.ultra.ui.theme.SecondaryMutedGreen

@Composable
fun MatteSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = PrimaryMatteGreen,
            activeTrackColor = PrimaryMatteGreen,
            inactiveTrackColor = SecondaryMutedGreen
        ),
        modifier = modifier
    )
}
