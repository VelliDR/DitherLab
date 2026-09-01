package com.ditherlab.ultra.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ditherlab.ultra.data.model.StudioUiState
import com.ditherlab.ultra.ui.theme.DeepCanvasBlack
import com.ditherlab.ultra.ui.theme.MatteOrange
import com.ditherlab.ultra.ui.theme.SurfaceDark
import com.ditherlab.ultra.ui.viewmodel.StudioViewModel
import kotlin.math.abs

@Composable
fun VideoTimelineSidebar(state: StudioUiState.Active, viewModel: StudioViewModel) {
    val durationMs = state.videoDurationMs
    if (durationMs <= 0L) return
    
    val startMs = state.currentConfig.effectStartTimeMs
    val endMs = if (state.currentConfig.effectEndTimeMs == -1L) durationMs else state.currentConfig.effectEndTimeMs
    
    val startFraction = (startMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    val endFraction = (endMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    
    Column(
        modifier = Modifier
            .width(100.dp)
            .fillMaxHeight()
            .background(SurfaceDark)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ZAMAN", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Scrollable Thumbnails
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                state.videoThumbnails.forEach { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f/9f)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            // Custom Vertical Range Slider Overlay
            var height by remember { mutableIntStateOf(0) }
            
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { height = it.height }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, _ ->
                            if (height == 0) return@detectVerticalDragGestures
                            val y = change.position.y
                            val fraction = (y / height).coerceIn(0f, 1f)
                            
                            val distToStart = abs(fraction - startFraction)
                            val distToEnd = abs(fraction - endFraction)
                            
                            if (distToStart < distToEnd) {
                                val newStart = fraction.coerceAtMost(endFraction - 0.05f)
                                viewModel.updateConfig { it.copy(effectStartTimeMs = (newStart * durationMs).toLong()) }
                            } else {
                                val newEnd = fraction.coerceAtLeast(startFraction + 0.05f)
                                viewModel.updateConfig { it.copy(effectEndTimeMs = (newEnd * durationMs).toLong()) }
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                
                val startY = startFraction * h
                val endY = endFraction * h
                
                // Dim unselected areas
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, startY)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    topLeft = Offset(0f, endY),
                    size = Size(w, h - endY)
                )
                
                // Highlight borders
                drawLine(
                    color = MatteOrange,
                    start = Offset(0f, startY),
                    end = Offset(w, startY),
                    strokeWidth = 6f
                )
                drawLine(
                    color = MatteOrange,
                    start = Offset(0f, endY),
                    end = Offset(w, endY),
                    strokeWidth = 6f
                )
                
                // Draw Thumbs
                drawCircle(
                    color = Color.White,
                    radius = 16f,
                    center = Offset(w / 2, startY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 16f,
                    center = Offset(w / 2, endY)
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        // Trim Toggle
        val isTrimmed = state.currentConfig.trimVideoToEffect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(if (isTrimmed) MatteOrange else DeepCanvasBlack)
                .clickable { viewModel.updateConfig { it.copy(trimVideoToEffect = !isTrimmed) } }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SADECE KES",
                color = if (isTrimmed) DeepCanvasBlack else Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}
