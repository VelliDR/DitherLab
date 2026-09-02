package com.ditherlab.ultra.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ditherlab.ultra.data.model.StudioUiState
import com.ditherlab.ultra.ui.theme.DeepCanvasBlack
import com.ditherlab.ultra.ui.theme.MatteOrange
import com.ditherlab.ultra.ui.theme.SurfaceDark
import com.ditherlab.ultra.ui.viewmodel.StudioViewModel
import kotlin.math.abs

private enum class DragHandle { START, END }

@Composable
fun VideoTimelineSidebar(state: StudioUiState.Active, viewModel: StudioViewModel) {
    val durationMs = state.videoDurationMs
    if (durationMs <= 0L) return
    
    val startMs = state.currentConfig.effectStartTimeMs
    val endMs = if (state.currentConfig.effectEndTimeMs == -1L) durationMs else state.currentConfig.effectEndTimeMs
    
    val startFraction = (startMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    val endFraction = (endMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

    // Ensure gesture callbacks always read the latest state values without stale closure captures
    val currentStartFraction by rememberUpdatedState(startFraction)
    val currentEndFraction by rememberUpdatedState(endFraction)
    val currentDurationMs by rememberUpdatedState(durationMs)
    val currentViewModel by rememberUpdatedState(viewModel)
    
    fun formatMs(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        val tenths = (ms % 1000) / 100
        return String.format("%02d:%02d.%d", min, sec, tenths)
    }

    Column(
        modifier = Modifier
            .width(110.dp)
            .fillMaxHeight()
            .background(SurfaceDark)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ZAMAN ÇİZELGESİ", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(4.dp))
        
        Text(
            text = "${formatMs(startMs)} - ${formatMs(endMs)}",
            color = MatteOrange,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(Modifier.height(6.dp))
        
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
                    if (!bmp.isRecycled) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            
            // Custom Vertical Range Slider Overlay
            var height by remember { mutableIntStateOf(0) }
            var activeDragHandle by remember { mutableStateOf<DragHandle?>(null) }
            
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { height = it.height }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { down ->
                                if (height == 0) return@detectVerticalDragGestures
                                currentViewModel.setSliderDragging(true)
                                val fraction = (down.y / height).coerceIn(0f, 1f)
                                val distStart = abs(fraction - currentStartFraction)
                                val distEnd = abs(fraction - currentEndFraction)
                                activeDragHandle = if (distStart <= distEnd) DragHandle.START else DragHandle.END
                            },
                            onDragEnd = {
                                currentViewModel.setSliderDragging(false)
                                activeDragHandle = null
                            },
                            onDragCancel = {
                                currentViewModel.setSliderDragging(false)
                                activeDragHandle = null
                            },
                            onVerticalDrag = { change, dragAmount ->
                                if (height == 0 || activeDragHandle == null) return@detectVerticalDragGestures
                                change.consume()
                                val deltaFraction = dragAmount / height.toFloat()
                                val minGapFraction = 500f / currentDurationMs.toFloat() // at least 500ms gap
                                
                                when (activeDragHandle) {
                                    DragHandle.START -> {
                                        val newStart = (currentStartFraction + deltaFraction).coerceIn(0f, currentEndFraction - minGapFraction)
                                        currentViewModel.updateConfig { it.copy(effectStartTimeMs = (newStart * currentDurationMs).toLong()) }
                                    }
                                    DragHandle.END -> {
                                        val newEnd = (currentEndFraction + deltaFraction).coerceIn(currentStartFraction + minGapFraction, 1f)
                                        currentViewModel.updateConfig { it.copy(effectEndTimeMs = (newEnd * currentDurationMs).toLong()) }
                                    }
                                    null -> {}
                                }
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height
                
                val startY = startFraction * h
                val endY = endFraction * h
                
                // Dim unselected areas (top and bottom)
                drawRect(
                    color = Color.Black.copy(alpha = 0.65f),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, startY)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.65f),
                    topLeft = Offset(0f, endY),
                    size = Size(w, h - endY)
                )
                
                // Selected region outline
                drawRect(
                    color = MatteOrange.copy(alpha = 0.15f),
                    topLeft = Offset(0f, startY),
                    size = Size(w, maxOf(1f, endY - startY))
                )
                
                // Highlight top and bottom handle bars
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
                
                // Top Handle Knob
                val isStartActive = activeDragHandle == DragHandle.START
                val startRadius = if (isStartActive) 22f else 18f
                drawCircle(
                    color = if (isStartActive) Color.White else MatteOrange,
                    radius = startRadius,
                    center = Offset(w / 2f, startY)
                )
                drawCircle(
                    color = if (isStartActive) MatteOrange else Color.White,
                    radius = startRadius * 0.6f,
                    center = Offset(w / 2f, startY)
                )
                
                // Bottom Handle Knob
                val isEndActive = activeDragHandle == DragHandle.END
                val endRadius = if (isEndActive) 22f else 18f
                drawCircle(
                    color = if (isEndActive) Color.White else MatteOrange,
                    radius = endRadius,
                    center = Offset(w / 2f, endY)
                )
                drawCircle(
                    color = if (isEndActive) MatteOrange else Color.White,
                    radius = endRadius * 0.6f,
                    center = Offset(w / 2f, endY)
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Trim Toggle Button
        val isTrimmed = state.currentConfig.trimVideoToEffect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(if (isTrimmed) MatteOrange else DeepCanvasBlack)
                .clickable { viewModel.updateConfig { it.copy(trimVideoToEffect = !isTrimmed) } }
                .padding(vertical = 6.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isTrimmed) "KIRPILACAK" else "TAM VİDEO",
                color = if (isTrimmed) DeepCanvasBlack else Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(6.dp))
    }
}
