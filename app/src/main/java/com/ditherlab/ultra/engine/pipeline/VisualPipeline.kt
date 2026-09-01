package com.ditherlab.ultra.engine.pipeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.data.model.TargetLayer
import com.ditherlab.ultra.engine.base.VisualEngine
import com.ditherlab.ultra.engine.pwa.GlitchEngineKotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VisualPipeline {

    suspend fun processImage(
        original: Bitmap,
        config: EffectConfig,
        backgroundEngine: VisualEngine,
        foregroundEngine: VisualEngine,
        subjectMaskBitmap: Bitmap?,
        customMaskBitmap: Bitmap? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        kotlinx.coroutines.yield()
        
        // 1. Process Background
        val bgResult = backgroundEngine.process(original, config)
        
        // 2. Process Foreground (only if different engine, or if targetLayer is used, but now we use separate engines)
        val fgResult = if (backgroundEngine != foregroundEngine) {
            foregroundEngine.process(original, config)
        } else {
            bgResult
        }
        
        // 3. Apply Shape Mask according to shapeTargetLayer
        var maskedBg = bgResult
        var maskedFg = fgResult
        
        if (customMaskBitmap != null) {
            when (config.shapeTargetLayer) {
                TargetLayer.BACKGROUND -> {
                    maskedBg = MaskManager.applyMask(bgResult, customMaskBitmap, config.maskIsTransparent)
                }
                TargetLayer.SUBJECT -> {
                    maskedFg = MaskManager.applyMask(fgResult, customMaskBitmap, config.maskIsTransparent)
                }
                TargetLayer.ALL -> {
                    // Handled after composition
                }
            }
        }
        
        // 4. Composite FG and BG using ML Kit subject mask
        val composited = if (subjectMaskBitmap != null) {
            val result = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            
            // Draw background
            canvas.drawBitmap(maskedBg, 0f, 0f, null)
            
            // Draw foreground masked by subject mask
            val scaledSubjectMask = if (original.width != subjectMaskBitmap.width || original.height != subjectMaskBitmap.height) {
                Bitmap.createScaledBitmap(subjectMaskBitmap, original.width, original.height, true)
            } else subjectMaskBitmap
            
            val fgLayerId = canvas.saveLayer(0f, 0f, original.width.toFloat(), original.height.toFloat(), null)
            canvas.drawBitmap(maskedFg, 0f, 0f, null)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            canvas.drawBitmap(scaledSubjectMask, 0f, 0f, paint)
            paint.xfermode = null
            canvas.restoreToCount(fgLayerId)
            
            if (scaledSubjectMask != subjectMaskBitmap) scaledSubjectMask.recycle()
            
            result
        } else if (customMaskBitmap != null && config.shapeTargetLayer == TargetLayer.SUBJECT) {
            val result = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            canvas.drawBitmap(maskedBg, 0f, 0f, null)
            canvas.drawBitmap(maskedFg, 0f, 0f, null)
            result
        } else {
            maskedBg // Default to BG if no mask
        }
        
        // 5. Apply Custom Shape Mask to ALL if selected
        val finalResult = if (customMaskBitmap != null && config.shapeTargetLayer == TargetLayer.ALL) {
            MaskManager.applyMask(composited, customMaskBitmap, config.maskIsTransparent)
        } else {
            composited
        }
        
        // Note: Memory cleanup for intermediate bitmaps
        if (maskedBg != bgResult && maskedBg != original && maskedBg != finalResult) maskedBg.recycle()
        if (maskedFg != fgResult && maskedFg != original && maskedFg != finalResult && maskedFg != maskedBg) maskedFg.recycle()
        
        // Note: Memory cleanup for intermediate bitmaps
        if (bgResult != original && bgResult != finalResult) bgResult.recycle()
        if (fgResult != original && fgResult != finalResult && fgResult != bgResult) fgResult.recycle()
        if (composited != finalResult && composited != original && composited != bgResult && composited != fgResult) composited.recycle()
        
        return@withContext finalResult
    }
}
