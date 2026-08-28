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
        engine: VisualEngine,
        subjectMaskBitmap: Bitmap?
    ): Bitmap = withContext(Dispatchers.Default) {
        val targetLayer = config.targetLayer
        val brushPaths = config.brushPaths

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
                val scaledSubjectMask = subjectMaskBitmap?.let { 
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
                    val scaledSubjectMask = subjectMaskBitmap?.let { 
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

        val result = engine.process(engineInput, config)
        
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
                val blended = Bitmap.createBitmap(result.width, result.height, Bitmap.Config.ARGB_8888)
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
        
        return@withContext finalResult
    }
}
