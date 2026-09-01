package com.ditherlab.ultra.engine.pipeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MaskManager {
    /**
     * Converts a given image to a pure black and white mask using a threshold.
     * White areas will be opaque (alpha 255), black areas will be transparent (alpha 0).
     */
    suspend fun createThresholdMask(input: Bitmap, invert: Boolean = false, threshold: Float = 0.5f): Bitmap = withContext(Dispatchers.Default) {
        val output = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()

        // 1. Grayscale matrix
        val grayMatrix = ColorMatrix().apply { setSaturation(0f) }

        // 2. Threshold & Alpha mapping matrix
        // We want to turn luminance into alpha.
        // If invert is true, black -> white/opaque, white -> black/transparent
        // A simple approach using a ColorMatrix:
        val thr = threshold * 255f
        val mult = if (invert) -255f else 255f
        val add = if (invert) 255f else -255f

        val thresholdMatrix = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        
        // This requires per-pixel manipulation for a hard threshold since ColorMatrix isn't perfect for hard step functions.
        // Let's do pixel manipulation for perfect thresholding.
        val pixels = IntArray(input.width * input.height)
        input.getPixels(pixels, 0, input.width, 0, 0, input.width, input.height)

        val thresholdInt = (threshold * 255).toInt()

        for (i in pixels.indices) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            
            val isOpaque = if (invert) luminance < thresholdInt else luminance >= thresholdInt
            
            if (isOpaque) {
                pixels[i] = 0xFFFFFFFF.toInt() // White and fully opaque
            } else {
                pixels[i] = 0x00000000 // Fully transparent
            }
        }
        
        output.setPixels(pixels, 0, input.width, 0, 0, input.width, input.height)
        return@withContext output
    }

    /**
     * Applies the mask to a bitmap. 
     * If isTransparent is true, outside mask becomes transparent.
     * If false, it keeps the background color (or original content if compositing).
     */
    suspend fun applyMask(original: Bitmap, mask: Bitmap, isTransparent: Boolean): Bitmap = withContext(Dispatchers.Default) {
        val result = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (!isTransparent) {
            // Fill with black or white background if not transparent? 
            // Usually the UI composites this, so maybe just return the masked bitmap anyway.
            canvas.drawColor(android.graphics.Color.BLACK)
        }

        canvas.drawBitmap(original, 0f, 0f, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
        
        // Scale mask if dimensions differ
        val scaledMask = if (original.width != mask.width || original.height != mask.height) {
            Bitmap.createScaledBitmap(mask, original.width, original.height, true)
        } else mask
        
        canvas.drawBitmap(scaledMask, 0f, 0f, paint)
        
        if (scaledMask != mask) {
            scaledMask.recycle()
        }

        return@withContext result
    }

    suspend fun createShapeMask(shape: String, width: Int, height: Int): Bitmap = withContext(Dispatchers.Default) {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }

        when (shape.lowercase()) {
            "circle" -> {
                val cx = width / 2f
                val cy = height / 2f
                val radius = Math.min(cx, cy)
                canvas.drawCircle(cx, cy, radius, paint)
            }
            "heart" -> {
                // Heart shape using path
                val path = android.graphics.Path()
                val w = width.toFloat()
                val h = height.toFloat()
                path.moveTo(w / 2, h / 4)
                path.cubicTo(w * 5/8, 0f, w, h / 8, w / 2, h * 3/4)
                path.moveTo(w / 2, h / 4)
                path.cubicTo(w * 3/8, 0f, 0f, h / 8, w / 2, h * 3/4)
                canvas.drawPath(path, paint)
            }
            "star" -> {
                val path = android.graphics.Path()
                val cx = width / 2f
                val cy = height / 2f
                val outerRadius = Math.min(cx, cy)
                val innerRadius = outerRadius * 0.4f
                val points = 5
                var angle = -Math.PI / 2.0
                val angleIncrement = Math.PI / points
                
                path.moveTo(cx + (outerRadius * Math.cos(angle)).toFloat(), cy + (outerRadius * Math.sin(angle)).toFloat())
                for (i in 0 until points * 2) {
                    angle += angleIncrement
                    val r = if (i % 2 == 0) innerRadius else outerRadius
                    path.lineTo(cx + (r * Math.cos(angle)).toFloat(), cy + (r * Math.sin(angle)).toFloat())
                }
                path.close()
                canvas.drawPath(path, paint)
            }
            else -> {
                // "none" or unknown: solid white mask (no masking)
                canvas.drawColor(android.graphics.Color.WHITE)
            }
        }
        return@withContext output
    }
}
