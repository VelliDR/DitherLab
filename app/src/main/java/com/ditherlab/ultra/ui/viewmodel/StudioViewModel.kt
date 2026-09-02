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
import com.ditherlab.ultra.engine.pwa.NoirComicEngineKotlin
import com.ditherlab.ultra.engine.karanlik.ChromaticAberrationEngineKotlin
import com.ditherlab.ultra.engine.karanlik.SwirlyBokehEngineKotlin
import com.ditherlab.ultra.engine.karanlik.DarkroomEngineKotlin
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
import java.io.File

class StudioViewModel : ViewModel() {

    private val paletteRepository = PaletteRepository()
    
    val availableEngines: List<VisualEngine> = listOf(
        com.ditherlab.ultra.engine.pwa.OrijinalEngineKotlin(),
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
        SensorCorruptEngineKotlin(),
        ChromaticAberrationEngineKotlin(),
        SwirlyBokehEngineKotlin(),
        NoirComicEngineKotlin(),
        DarkroomEngineKotlin(),
        com.ditherlab.ultra.engine.gpu.GlitchShaderEngine(),
        com.ditherlab.ultra.engine.gpu.VanGoghShaderEngine(),
        com.ditherlab.ultra.engine.gpu.CrtTvShaderEngine()
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

    fun setOriginalVideo(context: android.content.Context, uri: android.net.Uri) {
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            appContext = context.applicationContext
            var previewBitmap: Bitmap? = null
            var durationMs = 0L
            val thumbnails = mutableListOf<Bitmap>()
            
            try {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                
                val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMs = durationStr?.toLongOrNull() ?: 0L
                
                previewBitmap = retriever.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                
                // Extract 7 evenly spaced thumbnails
                if (durationMs > 0) {
                    val thumbCount = 7
                    for (i in 0 until thumbCount) {
                        val timeUs = (i * durationMs * 1000) / Math.max(1, thumbCount - 1)
                        val bmp = retriever.getFrameAtTime(timeUs, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        if (bmp != null) thumbnails.add(bmp)
                    }
                }
                
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    (state as? StudioUiState.Active)?.copy(
                        originalVideoUri = uri, 
                        originalImage = previewBitmap, 
                        subjectMaskBitmap = null,
                        videoDurationMs = durationMs,
                        videoThumbnails = thumbnails
                    ) ?: state
                }
                
                if (previewBitmap != null) {
                    applyEffects() // İlk hızlı render
                    // Arka planda ML Kit ile Özneyi Maskele
                    extractSubjectMask(previewBitmap)
                }
            }
        }
    }

    private var appContext: android.content.Context? = null
    private var videoPreviewJob: kotlinx.coroutines.Job? = null

    fun seekVideoPreviewToMs(timeMs: Long) {
        updateVideoPreviewFrame(timeMs)
    }

    fun updateVideoPreviewFrame(timeMs: Long) {
        val ctx = appContext ?: return
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        val videoUri = currentState.originalVideoUri ?: return
        
        videoPreviewJob?.cancel()
        videoPreviewJob = viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(80) // Smooth debounce for slider gestures
            try {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(ctx, videoUri)
                val timeUs = timeMs * 1000L
                val frame = retriever.getFrameAtTime(timeUs, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                retriever.release()
                
                if (frame != null) {
                    withContext(Dispatchers.Main) {
                        val activeState = _uiState.value as? StudioUiState.Active ?: return@withContext
                        _uiState.update { activeState.copy(originalImage = frame, subjectMaskBitmap = null) }
                        applyEffects()
                        extractSubjectMask(frame)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateConfig(update: (EffectConfig) -> EffectConfig) {
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        val oldStartMs = currentState.currentConfig.effectStartTimeMs
        val oldEndMs = currentState.currentConfig.effectEndTimeMs
        val newConfig = update(currentState.currentConfig)
        _uiState.update { currentState.copy(currentConfig = newConfig) }
        applyEffects()
        
        // Dynamically update main canvas preview frame when start or end slider handle moves!
        if (currentState.originalVideoUri != null) {
            if (newConfig.effectStartTimeMs != oldStartMs) {
                updateVideoPreviewFrame(newConfig.effectStartTimeMs)
            } else if (newConfig.effectEndTimeMs != oldEndMs && newConfig.effectEndTimeMs > 0) {
                updateVideoPreviewFrame(newConfig.effectEndTimeMs)
            }
        }
    }

    fun setOriginalImage(bitmap: Bitmap) {
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        // Görüntüyü set et ve ML Kit analizini başlat
        _uiState.update { 
            currentState.copy(
                originalImage = bitmap, 
                originalVideoUri = null,
                subjectMaskBitmap = null
            ) 
        }
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
            .addOnCompleteListener {
                segmenter.close()
            }
    }
    
    suspend fun extractSubjectMaskSync(bitmap: Bitmap): Bitmap? = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()
        val segmenter = SubjectSegmentation.getClient(options)
        val image = InputImage.fromBitmap(bitmap, 0)
        
        segmenter.process(image)
            .addOnSuccessListener { result ->
                continuation.resume(result.foregroundBitmap) { }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                continuation.resume(null) { }
            }
            .addOnCompleteListener {
                segmenter.close()
            }
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
        setBackgroundEngine(index)
    }
    
    fun setBackgroundEngine(index: Int) {
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        _uiState.update { currentState.copy(backgroundEngineIndex = index) }
        applyEffects()
    }
    
    fun setForegroundEngine(index: Int) {
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        _uiState.update { currentState.copy(foregroundEngineIndex = index) }
        applyEffects()
    }

    fun setMaskShape(shape: String) {
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        if (currentState.maskShape == shape) return
        
        viewModelScope.launch {
            val original = currentState.originalImage ?: return@launch
            val customMask = if (shape != "none") {
                com.ditherlab.ultra.engine.pipeline.MaskManager.createShapeMask(shape, original.width, original.height)
            } else null
            
            _uiState.update { (it as? StudioUiState.Active)?.copy(
                maskShape = shape,
                customMaskBitmap = customMask
            ) ?: it }
            applyEffects()
        }
    }
    
    fun setSliderDragging(isDragging: Boolean) {
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        if (currentState.sliderIsDragging != isDragging) {
            _uiState.update { currentState.copy(sliderIsDragging = isDragging) }
        }
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
            val bgEngine = availableEngines.getOrNull(currentState.backgroundEngineIndex) ?: availableEngines.first()
            val fgEngine = availableEngines.getOrNull(currentState.foregroundEngineIndex) ?: bgEngine
            
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
            
            val finalResult = com.ditherlab.ultra.engine.pipeline.VisualPipeline.processImage(
                original = previewBitmap,
                config = configWithPalette,
                backgroundEngine = bgEngine,
                foregroundEngine = fgEngine,
                subjectMaskBitmap = currentState.subjectMaskBitmap,
                customMaskBitmap = currentState.customMaskBitmap
            )
            
            if (previewBitmap != original && !previewBitmap.isRecycled) {
                // Let GC clean preview downscaled bitmap safely when unreferenced
            }
            
            _uiState.update { state ->
                val activeState = state as? StudioUiState.Active ?: return@update state
                activeState.copy(processedImage = finalResult) 
            }
        }
    }

    fun exportImage(context: android.content.Context) {
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        val original = currentState.originalImage ?: return
        
        viewModelScope.launch {
            _uiState.update { currentState.copy(isExporting = true) }
            try {
                val bgEngine = availableEngines.getOrNull(currentState.backgroundEngineIndex) ?: availableEngines.first()
                val fgEngine = availableEngines.getOrNull(currentState.foregroundEngineIndex) ?: bgEngine
                
                val selectedPalette = currentState.availablePalettes.find { it.id == currentState.currentConfig.selectedPaletteId } ?: currentState.availablePalettes.first()
                val configWithPalette = currentState.currentConfig.copy(resolvedPalette = selectedPalette)
                
                val finalResult = com.ditherlab.ultra.engine.pipeline.VisualPipeline.processImage(
                    original = original,
                    config = configWithPalette,
                    backgroundEngine = bgEngine,
                    foregroundEngine = fgEngine,
                    subjectMaskBitmap = currentState.subjectMaskBitmap,
                    customMaskBitmap = currentState.customMaskBitmap
                )
                
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
    
    suspend fun extractSubjectMaskWithSegmenterSync(
        segmenter: com.google.mlkit.vision.segmentation.subject.SubjectSegmenter,
        bitmap: Bitmap,
        maxDim: Float = 360f
    ): Bitmap? = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        val scale = if (bitmap.width > bitmap.height) maxDim / bitmap.width else maxDim / bitmap.height
        val scaledBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }
        
        val image = InputImage.fromBitmap(scaledBitmap, 0)
        segmenter.process(image)
            .addOnSuccessListener { result ->
                val mask = result.foregroundBitmap
                if (scaledBitmap != bitmap) {
                    scaledBitmap.recycle()
                }
                continuation.resume(mask) { }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                if (scaledBitmap != bitmap) {
                    scaledBitmap.recycle()
                }
                continuation.resume(null) { }
            }
    }

    fun exportVideo(context: android.content.Context) {
        val currentState = _uiState.value as? StudioUiState.Active ?: return
        val videoUri = currentState.originalVideoUri ?: return
        
        viewModelScope.launch {
            _uiState.update { currentState.copy(isExporting = true, isProcessingVideo = true, videoProgress = 0f) }
            
            val options = SubjectSegmenterOptions.Builder()
                .enableForegroundBitmap()
                .build()
            val segmenter = SubjectSegmentation.getClient(options)
            
            try {
                val bgEngine = availableEngines.getOrNull(currentState.backgroundEngineIndex) ?: availableEngines.first()
                val fgEngine = availableEngines.getOrNull(currentState.foregroundEngineIndex) ?: bgEngine
                
                val selectedPalette = currentState.availablePalettes.find { it.id == currentState.currentConfig.selectedPaletteId } ?: currentState.availablePalettes.first()
                val configWithPalette = currentState.currentConfig.copy(resolvedPalette = selectedPalette)
                val targetLayer = currentState.currentConfig.targetLayer
                
                val inputPath = getRealPathFromURI(context, videoUri) ?: throw Exception("Invalid video URI")
                
                val outputDir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES), "DitherLab")
                if (!outputDir.exists()) outputDir.mkdirs()
                val outputPath = File(outputDir, "DitherLab_Video_${System.currentTimeMillis()}.mp4").absolutePath
                
                val videoProcessor = com.ditherlab.ultra.engine.video.VideoProcessor(context)
                
                val success = videoProcessor.processVideo(
                    inputVideoPath = inputPath,
                    outputVideoPath = outputPath,
                    startTimeMs = currentState.currentConfig.effectStartTimeMs,
                    endTimeMs = currentState.currentConfig.effectEndTimeMs,
                    trimVideo = currentState.currentConfig.trimVideoToEffect,
                    maxDurationSeconds = 30,
                    maxConcurrency = if (targetLayer != TargetLayer.ALL || currentState.currentConfig.shapeTargetLayer != TargetLayer.ALL || bgEngine.engineName != fgEngine.engineName) 1 else 4,
                    onProgress = { progress ->
                        _uiState.update { (it as? StudioUiState.Active)?.copy(videoProgress = progress) ?: it }
                    },
                    frameProcessor = { bitmap ->
                        val mask = if (targetLayer != TargetLayer.ALL || currentState.currentConfig.shapeTargetLayer != TargetLayer.ALL || bgEngine.engineName != fgEngine.engineName) {
                            extractSubjectMaskWithSegmenterSync(segmenter, bitmap)
                        } else null
                        
                        val composited = com.ditherlab.ultra.engine.pipeline.VisualPipeline.processImage(
                            original = bitmap,
                            config = configWithPalette,
                            backgroundEngine = bgEngine,
                            foregroundEngine = fgEngine,
                            subjectMaskBitmap = mask,
                            customMaskBitmap = currentState.customMaskBitmap
                        )
                        mask?.recycle()
                        composited
                    }
                )
                
                if (success) {
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, File(outputPath).name)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_MOVIES + "/DitherLab")
                        }
                    }
                    val insertedUri = context.contentResolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                    insertedUri?.let { uri ->
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            File(outputPath).inputStream().use { input ->
                                input.copyTo(out)
                            }
                        }
                    }
                    File(outputPath).delete()
                }
                
                _uiState.update { (it as StudioUiState.Active).copy(isExporting = false, isProcessingVideo = false) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { (it as StudioUiState.Active).copy(isExporting = false, isProcessingVideo = false) }
            } finally {
                segmenter.close()
            }
        }
    }
    
    private fun getRealPathFromURI(context: android.content.Context, contentUri: android.net.Uri): String? {
        var cursor: android.database.Cursor? = null
        try {
            val proj = arrayOf(android.provider.MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(contentUri, proj, null, null, null)
            val column_index = cursor?.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA)
            cursor?.moveToFirst()
            return column_index?.let { cursor?.getString(it) }
        } catch (e: Exception) {
            // Eğer ContentProvider DATA desteklemiyorsa, cache'e kopyalayıp path dön.
            try {
                val tempFile = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
                context.contentResolver.openInputStream(contentUri)?.use { input ->
                    java.io.FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                return tempFile.absolutePath
            } catch (e2: Exception) {
                e2.printStackTrace()
                return null
            }
        } finally {
            cursor?.close()
        }
    }
}
