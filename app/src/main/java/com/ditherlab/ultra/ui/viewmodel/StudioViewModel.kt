package com.ditherlab.ultra.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.data.model.PointF
import com.ditherlab.ultra.data.model.StudioUiState
import com.ditherlab.ultra.data.model.TargetLayer
import com.ditherlab.ultra.data.repository.PaletteRepository
import com.ditherlab.ultra.engine.pwa.PixelArtEngineKotlin
import com.ditherlab.ultra.engine.pwa.GlitchEngineKotlin
import com.ditherlab.ultra.engine.pwa.VanGoghEngineKotlin
import com.ditherlab.ultra.engine.pwa.MinecraftEngineKotlin
import com.ditherlab.ultra.engine.pwa.PostcardEngineKotlin
import com.ditherlab.ultra.engine.pwa.ThermalPaperEngineKotlin
import com.ditherlab.ultra.engine.pwa.AsciiMatrixEngineKotlin
import com.ditherlab.ultra.engine.pwa.CrtTvEngineKotlin
import com.ditherlab.ultra.engine.pwa.FlirThermalEngineKotlin
import com.ditherlab.ultra.engine.pwa.CmykOffsetEngineKotlin
import com.ditherlab.ultra.engine.pwa.PunkFanzineEngineKotlin
import com.ditherlab.ultra.engine.pwa.ColorClashEngineKotlin
import com.ditherlab.ultra.engine.pwa.TextGlitchEngineKotlin
import com.ditherlab.ultra.engine.pwa.SensorCorruptEngineKotlin
import com.ditherlab.ultra.engine.pwa.VanGoghBetaEngineKotlin
import com.ditherlab.ultra.engine.base.VisualEngine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StudioViewModel : ViewModel() {

    private val paletteRepository = PaletteRepository()
    
    val availableEngines: List<VisualEngine> = listOf(
        PixelArtEngineKotlin(),
        GlitchEngineKotlin(),
        VanGoghEngineKotlin(),
        MinecraftEngineKotlin(),
        PostcardEngineKotlin(),
        ThermalPaperEngineKotlin(),
        AsciiMatrixEngineKotlin(),
        CrtTvEngineKotlin(),
        FlirThermalEngineKotlin(),
        VanGoghBetaEngineKotlin(),
        CmykOffsetEngineKotlin(),
        PunkFanzineEngineKotlin(),
        ColorClashEngineKotlin(),
        TextGlitchEngineKotlin(),
        SensorCorruptEngineKotlin()
    )
    
    private val _uiState = MutableStateFlow<StudioUiState>(StudioUiState.Loading)
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val palettes = paletteRepository.getDefaultPalettes()
            _uiState.value = StudioUiState.Active(
                availablePalettes = palettes,
                currentConfig = EffectConfig(selectedPaletteId = palettes.first().id)
            )
        }
    }

    fun setOriginalImage(bitmap: Bitmap) {
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        // Görüntüyü set et ve ML Kit analizini başlat
        _uiState.update { currentState.copy(originalImage = bitmap, subjectMaskBitmap = null) }
        applyEffects() // İlk hızlı render
        
        // Arka planda ML Kit ile Özneyi (Subject) Maskele
        extractSubjectMask(bitmap)
    }
    
    private fun extractSubjectMask(bitmap: Bitmap) {
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()
        val segmenter = SubjectSegmentation.getClient(options)
        val image = InputImage.fromBitmap(bitmap, 0)
        
        segmenter.process(image)
            .addOnSuccessListener { result ->
                val fgBitmap = result.foregroundBitmap
                if (fgBitmap != null) {
                    // Sadece fgBitmap'i kullan (ARGB_8888 olduğu için xfermode sorunsuz çalışacak)
                    val mask = fgBitmap
                    val currentState = _uiState.value as? StudioUiState.Active ?: return@addOnSuccessListener
                    _uiState.update { currentState.copy(subjectMaskBitmap = mask) }
                    applyEffects() // Maske geldikten sonra tekrar işle
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                // Hata durumunda maske boş kalır, TargetLayer.ALL olarak devam eder
            }
    }

    fun updateConfig(update: (EffectConfig) -> EffectConfig) {
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        val newConfig = update(currentState.currentConfig)
        _uiState.update { currentState.copy(currentConfig = newConfig) }
        applyEffects()
    }
    
    fun updateTilt(tiltX: Float, tiltY: Float) {
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        if (currentState.currentConfig.gyroEffect) {
            val newConfig = currentState.currentConfig.copy(tiltX = tiltX, tiltY = tiltY)
            _uiState.update { currentState.copy(currentConfig = newConfig) }
            applyEffects()
        }
    }

    fun setEngine(index: Int) {
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        _uiState.update { currentState.copy(selectedEngineIndex = index) }
        applyEffects()
    }
    
    fun setSplitViewPosition(position: Float) {
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        _uiState.update { currentState.copy(splitPosition = position) }
    }
    
    fun toggleSplitView() {
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        _uiState.update { currentState.copy(isA_BSplitActive = !currentState.isA_BSplitActive) }
    }

    fun updateToneCurvePoint(index: Int, point: PointF) {
        updateConfig { config ->
            val newPoints = config.toneCurve.points.toMutableList()
            newPoints[index] = point
            config.copy(toneCurve = config.toneCurve.copy(points = newPoints))
        }
    }

    fun toggleBrushMode() {
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        val isActive = !currentState.currentConfig.isBrushModeActive
        _uiState.update { 
            currentState.copy(
                currentConfig = currentState.currentConfig.copy(isBrushModeActive = isActive)
            ) 
        }
        applyEffects()
    }
    
    fun clearBrushPaths() {
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        _uiState.update { 
            currentState.copy(
                currentConfig = currentState.currentConfig.copy(brushPaths = emptyList())
            ) 
        }
        applyEffects()
    }
    
    fun addBrushPoint(point: PointF, isNewPath: Boolean) {
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        val currentPaths = currentState.currentConfig.brushPaths.toMutableList()
        val brushSize = currentState.currentConfig.brushSize
        
        if (isNewPath || currentPaths.isEmpty()) {
            currentPaths.add(com.ditherlab.ultra.data.model.BrushPath(listOf(point), brushSize))
        } else {
            val lastPath = currentPaths.last()
            val newPoints = lastPath.points.toMutableList()
            newPoints.add(point)
            currentPaths[currentPaths.lastIndex] = lastPath.copy(points = newPoints)
        }
        
        _uiState.update { 
            currentState.copy(
                currentConfig = currentState.currentConfig.copy(brushPaths = currentPaths)
            ) 
        }
    }
    
    fun finishBrushPath() {
        applyEffects()
    }

    private var renderJob: kotlinx.coroutines.Job? = null

    private fun applyEffects() {
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        val original = currentState.originalImage ?: return
        val targetLayer = currentState.currentConfig.targetLayer
        val brushPaths = currentState.currentConfig.brushPaths
        
        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            val engine = availableEngines.getOrNull(currentState.selectedEngineIndex) ?: availableEngines.first()
            
            // Path B: Canlı Önizleme (Live Preview) için Downscale
            val previewBitmap = withContext(Dispatchers.Default) {
                val maxDim = 512
                if (original.width > maxDim || original.height > maxDim) {
                    val ratio = Math.min(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height)
                    val newW = (original.width * ratio).toInt()
                    val newH = (original.height * ratio).toInt()
                    Bitmap.createScaledBitmap(original, newW, newH, true)
                } else {
                    original
                }
            }
                
            val selectedPalette = currentState.availablePalettes.find { it.id == currentState.currentConfig.selectedPaletteId } ?: currentState.availablePalettes.first()
            val configWithPalette = currentState.currentConfig.copy(resolvedPalette = selectedPalette)
            
            val finalMask = withContext(Dispatchers.Default) {
                var userMaskBitmap: Bitmap? = null
                if (brushPaths.isNotEmpty()) {
                    userMaskBitmap = Bitmap.createBitmap(previewBitmap.width, previewBitmap.height, Bitmap.Config.ARGB_8888)
                    val maskCanvas = Canvas(userMaskBitmap)
                    
                    val brushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.WHITE
                        style = Paint.Style.STROKE
                        strokeJoin = Paint.Join.ROUND
                        strokeCap = Paint.Cap.ROUND
                    }
                    
                    brushPaths.forEach { path ->
                        brushPaint.strokeWidth = path.strokeWidth * previewBitmap.width
                        val androidPath = android.graphics.Path()
                        if (path.points.isNotEmpty()) {
                            val first = path.points.first()
                            androidPath.moveTo(first.x * previewBitmap.width, first.y * previewBitmap.height)
                            for (i in 1 until path.points.size) {
                                val pt = path.points[i]
                                androidPath.lineTo(pt.x * previewBitmap.width, pt.y * previewBitmap.height)
                            }
                            maskCanvas.drawPath(androidPath, brushPaint)
                        }
                    }
                }

                if (userMaskBitmap != null) {
                    val combinedMask = Bitmap.createBitmap(previewBitmap.width, previewBitmap.height, Bitmap.Config.ARGB_8888)
                    val maskCanvas = Canvas(combinedMask)
                    maskCanvas.drawBitmap(userMaskBitmap, 0f, 0f, null)
                    
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                    val scaledSubjectMask = currentState.subjectMaskBitmap?.let { 
                        Bitmap.createScaledBitmap(it, previewBitmap.width, previewBitmap.height, true) 
                    }
                    
                    if (targetLayer == TargetLayer.SUBJECT && scaledSubjectMask != null) {
                        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                        maskCanvas.drawBitmap(scaledSubjectMask, 0f, 0f, paint)
                    } else if (targetLayer == TargetLayer.BACKGROUND && scaledSubjectMask != null) {
                        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                        maskCanvas.drawBitmap(scaledSubjectMask, 0f, 0f, paint)
                    }
                    combinedMask
                } else {
                    if (targetLayer == TargetLayer.ALL) {
                        null
                    } else {
                        val scaledSubjectMask = currentState.subjectMaskBitmap?.let { 
                            Bitmap.createScaledBitmap(it, previewBitmap.width, previewBitmap.height, true) 
                        }
                        
                        if (targetLayer == TargetLayer.SUBJECT) {
                            scaledSubjectMask
                        } else { 
                            if (scaledSubjectMask != null) {
                                val invertedMask = Bitmap.createBitmap(previewBitmap.width, previewBitmap.height, Bitmap.Config.ARGB_8888)
                                val invCanvas = Canvas(invertedMask)
                                invCanvas.drawColor(android.graphics.Color.WHITE)
                                val invPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                                }
                                invCanvas.drawBitmap(scaledSubjectMask, 0f, 0f, invPaint)
                                invertedMask
                            } else {
                                null
                            }
                        }
                    }
                }
            }
            
            val engineInput = if (finalMask != null && engine is GlitchEngineKotlin) {
                val masked = Bitmap.createBitmap(previewBitmap.width, previewBitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(masked)
                canvas.drawBitmap(previewBitmap, 0f, 0f, null)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
                canvas.drawBitmap(finalMask, 0f, 0f, paint)
                masked
            } else {
                previewBitmap
            }

            val result = engine.process(engineInput, configWithPalette)
            
            val finalResult = withContext(Dispatchers.Default) {
                if (finalMask == null) {
                    result
                } else if (engine is GlitchEngineKotlin) {
                    val blended = Bitmap.createBitmap(previewBitmap.width, previewBitmap.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(blended)
                    canvas.drawBitmap(previewBitmap, 0f, 0f, null)
                    canvas.drawBitmap(result, 0f, 0f, null)
                    blended
                } else {
                    val blended = Bitmap.createBitmap(result.width, result.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(blended)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                    
                    canvas.drawBitmap(previewBitmap, 0f, 0f, null)
                    
                    val layerId = canvas.saveLayer(0f, 0f, result.width.toFloat(), result.height.toFloat(), null)
                    canvas.drawBitmap(result, 0f, 0f, null)
                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                    canvas.drawBitmap(finalMask, 0f, 0f, paint)
                    paint.xfermode = null
                    canvas.restoreToCount(layerId)
                    
                    blended
                }
            }
            
            _uiState.update { 
                (it as StudioUiState.Active).copy(processedImage = finalResult) 
            }
        }
    }

    fun exportImage(context: android.content.Context) {
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        val original = currentState.originalImage ?: return
        
        viewModelScope.launch {
            _uiState.update { currentState.copy(isExporting = true) }
            try {
                val engine = availableEngines.getOrNull(currentState.selectedEngineIndex) ?: availableEngines.first()
                
                val selectedPalette = currentState.availablePalettes.find { it.id == currentState.currentConfig.selectedPaletteId } ?: currentState.availablePalettes.first()
                val configWithPalette = currentState.currentConfig.copy(resolvedPalette = selectedPalette)
                
                val brushPaths = currentState.currentConfig.brushPaths
                val targetLayer = currentState.currentConfig.targetLayer
                
                val finalMask = withContext(Dispatchers.Default) {
                    var userMaskBitmap: Bitmap? = null
                    if (brushPaths.isNotEmpty()) {
                        userMaskBitmap = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
                        val maskCanvas = Canvas(userMaskBitmap)
                        
                        val brushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.WHITE
                            style = Paint.Style.STROKE
                            strokeJoin = Paint.Join.ROUND
                            strokeCap = Paint.Cap.ROUND
                        }
                        
                        brushPaths.forEach { path ->
                            brushPaint.strokeWidth = path.strokeWidth * original.width
                            val androidPath = android.graphics.Path()
                            if (path.points.isNotEmpty()) {
                                val first = path.points.first()
                                androidPath.moveTo(first.x * original.width, first.y * original.height)
                                for (i in 1 until path.points.size) {
                                    val pt = path.points[i]
                                    androidPath.lineTo(pt.x * original.width, pt.y * original.height)
                                }
                                maskCanvas.drawPath(androidPath, brushPaint)
                            }
                        }
                    }

                    if (userMaskBitmap != null) {
                        val combinedMask = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
                        val maskCanvas = Canvas(combinedMask)
                        maskCanvas.drawBitmap(userMaskBitmap, 0f, 0f, null)
                        
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                        val scaledSubjectMask = currentState.subjectMaskBitmap?.let { 
                            Bitmap.createScaledBitmap(it, original.width, original.height, true) 
                        }
                        
                        if (targetLayer == TargetLayer.SUBJECT && scaledSubjectMask != null) {
                            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                            maskCanvas.drawBitmap(scaledSubjectMask, 0f, 0f, paint)
                        } else if (targetLayer == TargetLayer.BACKGROUND && scaledSubjectMask != null) {
                            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                            maskCanvas.drawBitmap(scaledSubjectMask, 0f, 0f, paint)
                        }
                        combinedMask
                    } else {
                        if (targetLayer == TargetLayer.ALL) {
                            null
                        } else {
                            val scaledSubjectMask = currentState.subjectMaskBitmap?.let { 
                                Bitmap.createScaledBitmap(it, original.width, original.height, true) 
                            }
                            
                            if (targetLayer == TargetLayer.SUBJECT) {
                                scaledSubjectMask
                            } else {
                                if (scaledSubjectMask != null) {
                                    val invertedMask = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
                                    val invCanvas = Canvas(invertedMask)
                                    invCanvas.drawColor(android.graphics.Color.WHITE)
                                    val invPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                                    }
                                    invCanvas.drawBitmap(scaledSubjectMask, 0f, 0f, invPaint)
                                    invertedMask
                                } else {
                                    null
                                }
                            }
                        }
                    }
                }

                val engineInput = if (finalMask != null && engine is GlitchEngineKotlin) {
                    val masked = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(masked)
                    canvas.drawBitmap(original, 0f, 0f, null)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
                    canvas.drawBitmap(finalMask, 0f, 0f, paint)
                    masked
                } else {
                    original
                }

                val result = withContext(Dispatchers.Default) {
                    engine.process(engineInput, configWithPalette)
                }
                
                val finalResult = withContext(Dispatchers.Default) {
                    if (finalMask == null) {
                        result
                    } else if (engine is GlitchEngineKotlin) {
                        val blended = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(blended)
                        canvas.drawBitmap(original, 0f, 0f, null)
                        canvas.drawBitmap(result, 0f, 0f, null)
                        blended
                    } else {
                        val blended = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(blended)
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                        
                        canvas.drawBitmap(original, 0f, 0f, null)
                        
                        val layerId = canvas.saveLayer(0f, 0f, result.width.toFloat(), result.height.toFloat(), null)
                        canvas.drawBitmap(result, 0f, 0f, null)
                        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                        canvas.drawBitmap(finalMask, 0f, 0f, paint)
                        paint.xfermode = null
                        canvas.restoreToCount(layerId)
                        
                        blended
                    }
                }
                
                withContext(Dispatchers.IO) {
                    val filename = "DitherLab_${System.currentTimeMillis()}.png"
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/DitherLab")
                        }
                    }
                    
                    val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        context.contentResolver.openOutputStream(it)?.use { out ->
                            finalResult.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    }
                }
                
                _uiState.update { (it as StudioUiState.Active).copy(isExporting = false) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { (it as StudioUiState.Active).copy(isExporting = false) }
            }
        }
    }
}
