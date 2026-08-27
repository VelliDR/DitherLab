package com.ditherlab.ultra.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ditherlab.ultra.ui.theme.AccentOlive
import com.ditherlab.ultra.ui.theme.PrimaryLightSage
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LightGizmoWidget(
    angleRads: Float,
    onAngleChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(80.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val pos = change.position
                    val dy = pos.y - center.y
                    val dx = pos.x - center.x
                    val angle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
                    onAngleChanged(angle)
                }
            }
    ) {
        val radius = size.minDimension / 2f - 8.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        
        // Çember Rayı
        drawCircle(
            color = AccentOlive.copy(alpha = 0.5f),
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Işık Yönü Çizgisi ve Noktası
        val handleX = center.x + cos(angleRads.toDouble()).toFloat() * radius
        val handleY = center.y + sin(angleRads.toDouble()).toFloat() * radius
        
        drawLine(
            color = PrimaryLightSage,
            start = center,
            end = Offset(handleX, handleY),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
        
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = Offset(handleX, handleY)
        )
    }
}
