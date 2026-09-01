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
        
        // 1. Process Background
        val bgResult = backgroundEngine.process(original, config)
        
        // 2. Process Foreground (only if different engine, or if targetLayer is used, but now we use separate engines)
        val fgResult = if (backgroundEngine != foregroundEngine) {
            foregroundEngine.process(original, config)
        } else {
            bgResult
        }
        
        // 3. Composite FG and BG using ML Kit subject mask
        val composited = if (subjectMaskBitmap != null) {
            val result = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            
            // Draw background
            canvas.drawBitmap(bgResult, 0f, 0f, null)
            
            // Draw foreground masked by subject mask
            val scaledMask = if (original.width != subjectMaskBitmap.width || original.height != subjectMaskBitmap.height) {
                Bitmap.createScaledBitmap(subjectMaskBitmap, original.width, original.height, true)
            } else subjectMaskBitmap
            
            val fgLayerId = canvas.saveLayer(0f, 0f, original.width.toFloat(), original.height.toFloat(), null)
            canvas.drawBitmap(fgResult, 0f, 0f, null)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            canvas.drawBitmap(scaledMask, 0f, 0f, paint)
            paint.xfermode = null
            canvas.restoreToCount(fgLayerId)
            
            if (scaledMask != subjectMaskBitmap) scaledMask.recycle()
            
            result
        } else {
            bgResult // Default to BG if no mask
        }
        
        // 4. Apply Custom Shape Mask (if any)
        val finalResult = if (customMaskBitmap != null) {
            MaskManager.applyMask(composited, customMaskBitmap, config.maskIsTransparent)
        } else {
            composited
        }
        
        // Note: Memory cleanup for intermediate bitmaps
        if (bgResult != original && bgResult != fgResult && bgResult != finalResult) bgResult.recycle()
        if (fgResult != original && fgResult != finalResult && fgResult != bgResult) fgResult.recycle()
        if (composited != finalResult && composited != original && composited != bgResult && composited != fgResult) composited.recycle()
        
        return@withContext finalResult
    }
}
