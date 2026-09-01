package com.ditherlab.ultra.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ditherlab.ultra.ui.theme.PrimaryMatteGreen
import com.ditherlab.ultra.ui.theme.SecondaryMutedGreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MatteSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier,
    onDraggingChanged: ((Boolean) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    LaunchedEffect(interactionSource) {
        var dragCount = 0
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start, is PressInteraction.Press -> {
                    dragCount++
                    if (dragCount == 1) onDraggingChanged?.invoke(true)
                }
                is DragInteraction.Stop, is DragInteraction.Cancel, 
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    dragCount--
                    if (dragCount <= 0) {
                        dragCount = 0
                        onDraggingChanged?.invoke(false)
                    }
                }
            }
        }
    }

    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = PrimaryMatteGreen,
            activeTrackColor = PrimaryMatteGreen,
            inactiveTrackColor = SecondaryMutedGreen
        ),
        interactionSource = interactionSource,
        modifier = modifier
    )
}
