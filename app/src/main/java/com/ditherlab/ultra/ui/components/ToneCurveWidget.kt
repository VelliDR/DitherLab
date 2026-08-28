package com.ditherlab.ultra.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.ditherlab.ultra.data.model.PointF
import com.ditherlab.ultra.data.model.ToneCurveState
import com.ditherlab.ultra.ui.theme.AccentOlive
import com.ditherlab.ultra.ui.theme.PrimaryLightSage
import com.ditherlab.ultra.ui.theme.SurfaceVariantDark

@Composable
fun ToneCurveWidget(
    state: ToneCurveState,
    onPointChanged: (Int, PointF) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val pos = change.position
                    val normX = (pos.x / size.width).coerceIn(0f, 1f)
                    val normY = 1f - (pos.y / size.height).coerceIn(0f, 1f)
                    
                    // En yakın noktayı bul
                    var closestIdx = -1
                    var minDistance = Float.MAX_VALUE
                    state.points.forEachIndexed { index, p ->
                        val dx = p.x - normX
                        val dy = p.y - normY
                        val dist = dx * dx + dy * dy
                        if (dist < minDistance) {
                            minDistance = dist
                            closestIdx = index
                        }
                    }
                    
                    // Sadece iç noktaları X ekseninde hareket ettir. Uçlar yatayda sabit kalmalı.
                    if (closestIdx != -1) {
                        val newX = if (closestIdx == 0) 0f else if (closestIdx == 4) 1f else normX
                        onPointChanged(closestIdx, PointF(newX, normY))
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        
        // Arka plan Grid
        drawRect(color = SurfaceVariantDark)
        for (i in 1..3) {
            drawLine(Color.DarkGray, Offset(0f, h * i / 4f), Offset(w, h * i / 4f), 1f)
            drawLine(Color.DarkGray, Offset(w * i / 4f, 0f), Offset(w * i / 4f, h), 1f)
        }
        
        // Eğri Çizimi
        val path = Path()
        state.points.forEachIndexed { i, p ->
            val px = p.x * w
            val py = h - p.y * h
            if (i == 0) path.moveTo(px, py)
            else path.lineTo(px, py)
        }
        
        drawPath(
            path = path,
            color = AccentOlive,
            style = Stroke(width = 4f)
        )
        
        // Kontrol Noktaları
        state.points.forEach { p ->
            drawCircle(
                color = PrimaryLightSage,
                radius = 12f,
                center = Offset(p.x * w, h - p.y * h)
            )
        }
    }
}
