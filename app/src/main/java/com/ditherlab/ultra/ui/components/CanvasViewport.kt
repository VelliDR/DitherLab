package com.ditherlab.ultra.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import com.ditherlab.ultra.data.model.PointF
import com.ditherlab.ultra.data.model.StudioUiState
import com.ditherlab.ultra.ui.theme.*

@Composable
fun CanvasViewport(
    uiState: StudioUiState.Active,
    modifier: Modifier = Modifier,
    onBrushPointAdded: (PointF, Boolean) -> Unit = { _, _ -> },
    onBrushPathFinished: () -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    
    val isBrush = uiState.currentConfig.isBrushModeActive

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isBrush) {
                if (isBrush) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val drawImage = uiState.processedImage ?: uiState.originalImage ?: return@detectDragGestures
                            val imageWidth = drawImage.width * scale
                            val imageHeight = drawImage.height * scale
                            val centerX = (size.width - imageWidth) / 2f + panOffset.x
                            val centerY = (size.height - imageHeight) / 2f + panOffset.y
                            
                            val normX = ((startOffset.x - centerX) / imageWidth).coerceIn(0f, 1f)
                            val normY = ((startOffset.y - centerY) / imageHeight).coerceIn(0f, 1f)
                            onBrushPointAdded(PointF(normX, normY), true)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val drawImage = uiState.processedImage ?: uiState.originalImage ?: return@detectDragGestures
                            val imageWidth = drawImage.width * scale
                            val imageHeight = drawImage.height * scale
                            val centerX = (size.width - imageWidth) / 2f + panOffset.x
                            val centerY = (size.height - imageHeight) / 2f + panOffset.y
                            
                            val normX = ((change.position.x - centerX) / imageWidth).coerceIn(0f, 1f)
                            val normY = ((change.position.y - centerY) / imageHeight).coerceIn(0f, 1f)
                            onBrushPointAdded(PointF(normX, normY), false)
                        },
                        onDragEnd = {
                            onBrushPathFinished()
                        }
                    )
                } else {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 10f)
                        panOffset += pan
                    }
                }
            }
    ) {
        val original = uiState.originalImage?.takeIf { !it.isRecycled }?.asImageBitmap()
        val processed = uiState.processedImage?.takeIf { !it.isRecycled }?.asImageBitmap()
        
        if (processed == null && original == null) return@Canvas
        
        val drawImage = processed ?: original!!
        
        val imageWidth = drawImage.width * scale
        val imageHeight = drawImage.height * scale
        
        val centerX = (size.width - imageWidth) / 2f + panOffset.x
        val centerY = (size.height - imageHeight) / 2f + panOffset.y

        if (!uiState.isA_BSplitActive || original == null || processed == null) {
            drawImage(
                image = drawImage,
                dstOffset = androidx.compose.ui.unit.IntOffset(centerX.toInt(), centerY.toInt()),
                dstSize = androidx.compose.ui.unit.IntSize(imageWidth.toInt(), imageHeight.toInt())
            )
        } else {
            val splitX = centerX + (imageWidth * uiState.splitPosition)
            clipRect(right = splitX) {
                drawImage(
                    image = original,
                    dstOffset = androidx.compose.ui.unit.IntOffset(centerX.toInt(), centerY.toInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(imageWidth.toInt(), imageHeight.toInt())
                )
            }
            clipRect(left = splitX) {
                drawImage(
                    image = processed,
                    dstOffset = androidx.compose.ui.unit.IntOffset(centerX.toInt(), centerY.toInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(imageWidth.toInt(), imageHeight.toInt())
                )
            }
            drawLine(
                color = AccentOlive,
                start = Offset(splitX, 0f),
                end = Offset(splitX, size.height),
                strokeWidth = 6f
            )
        }
        
        // Fırça İzi Overlay (Kırmızı transparan maske)
        if (isBrush && uiState.currentConfig.brushPaths.isNotEmpty()) {
            val brushPaths = uiState.currentConfig.brushPaths
            clipRect(
                left = centerX,
                top = centerY,
                right = centerX + imageWidth,
                bottom = centerY + imageHeight
            ) {
                val overlayColor = Color(0x66FF0000) // Yarı saydam kırmızı
                for (path in brushPaths) {
                    if (path.points.isEmpty()) continue
                    
                    val composePath = Path()
                    val first = path.points.first()
                    composePath.moveTo(centerX + (first.x * imageWidth), centerY + (first.y * imageHeight))
                    
                    for (i in 1 until path.points.size) {
                        val pt = path.points[i]
                        composePath.lineTo(centerX + (pt.x * imageWidth), centerY + (pt.y * imageHeight))
                    }
                    
                    // Stroke width'i ekran genişliğine orantıla (örn 0.05f = ekranın %5'i)
                    val strokeW = path.strokeWidth * imageWidth
                    drawPath(
                        path = composePath,
                        color = overlayColor,
                        style = Stroke(
                            width = strokeW,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}
