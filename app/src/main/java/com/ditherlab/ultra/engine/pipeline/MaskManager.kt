package com.ditherlab.ultra.engine.pipeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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

        val w = width.toFloat()
        val h = height.toFloat()
        val path = Path()

        when (shape.lowercase()) {
            "fluidriver" -> {
                path.moveTo(w * 0.38f, 0f)
                path.cubicTo(w * 0.41f, h * 0.31f, w * 0.8f, h * 0.47f, w, h * 0.6f)
                path.lineTo(w, h)
                path.lineTo(w * 0.66f, h)
                path.cubicTo(w * 0.63f, h * 0.72f, w * 0.25f, h * 0.55f, 0f, h * 0.35f)
                path.lineTo(0f, 0f)
                path.close()
            }
            "fluidcorners" -> {
                path.moveTo(w * 0.38f, 0f)
                path.cubicTo(w * 0.41f, h * 0.31f, w * 0.8f, h * 0.47f, w, h * 0.6f)
                path.lineTo(w, 0f)
                path.close()
                
                path.moveTo(0f, h * 0.35f)
                path.cubicTo(w * 0.25f, h * 0.55f, w * 0.63f, h * 0.72f, w * 0.66f, h)
                path.lineTo(0f, h)
                path.close()
            }
            "blob" -> {
                path.moveTo(w * 0.5f, h * 0.08f)
                path.cubicTo(w * 0.83f, h * 0.06f, w * 0.97f, h * 0.29f, w * 0.91f, h * 0.55f)
                path.cubicTo(w * 0.86f, h * 0.82f, w * 0.69f, h * 0.94f, w * 0.44f, h * 0.92f)
                path.cubicTo(w * 0.13f, h * 0.9f, w * 0.05f, h * 0.74f, w * 0.08f, h * 0.49f)
                path.cubicTo(w * 0.11f, h * 0.19f, w * 0.22f, h * 0.09f, w * 0.5f, h * 0.08f)
                path.close()
            }
            "wavetop" -> {
                path.moveTo(0f, h * 0.23f)
                path.cubicTo(w * 0.25f, h * 0.11f, w * 0.5f, h * 0.35f, w * 0.75f, h * 0.19f)
                path.cubicTo(w * 0.87f, h * 0.11f, w * 0.94f, h * 0.15f, w, h * 0.21f)
                path.lineTo(w, h)
                path.lineTo(0f, h)
                path.close()
            }
            "arch" -> {
                val pad = w * 0.08f
                path.moveTo(pad, h - pad)
                path.lineTo(pad, h * 0.35f)
                path.cubicTo(pad, pad * 0.5f, w - pad, pad * 0.5f, w - pad, h * 0.35f)
                path.lineTo(w - pad, h - pad)
                path.close()
            }
            "doublearch" -> {
                val padX = w * 0.05f
                val padY = h * 0.04f
                val archW = (w - (padX * 3)) / 2f
                
                path.moveTo(padX, h - padY)
                path.lineTo(padX, h * 0.3f)
                path.cubicTo(padX, h * 0.1f, padX + archW, h * 0.1f, padX + archW, h * 0.3f)
                path.lineTo(padX + archW, h - padY)
                path.close()
                
                val rightX = padX * 2 + archW
                path.moveTo(rightX, h - padY)
                path.lineTo(rightX, h * 0.3f)
                path.cubicTo(rightX, h * 0.1f, rightX + archW, h * 0.1f, rightX + archW, h * 0.3f)
                path.lineTo(rightX + archW, h - padY)
                path.close()
            }
            "cinemaframe" -> {
                val x = w * 0.07f
                val y = h * 0.06f
                val fw = w * 0.86f
                val fh = h * 0.88f
                val r = min(w, h) * 0.1f
                path.addRoundRect(RectF(x, y, x + fw, y + fh), r, r, Path.Direction.CW)
            }
            "ticket" -> {
                val x = w * 0.05f
                val y = h * 0.08f
                val fw = w * 0.9f
                val fh = h * 0.84f
                val r = min(w, h) * 0.08f
                path.moveTo(x, y)
                path.lineTo(x + fw, y)
                path.lineTo(x + fw, y + fh / 2 - r)
                path.arcTo(RectF(x + fw - r, y + fh / 2 - r, x + fw + r, y + fh / 2 + r), 270f, -180f)
                path.lineTo(x + fw, y + fh)
                path.lineTo(x, y + fh)
                path.lineTo(x, y + fh / 2 + r)
                path.arcTo(RectF(x - r, y + fh / 2 - r, x + r, y + fh / 2 + r), 90f, -180f)
                path.close()
            }
            "torn1" -> {
                path.moveTo(w * 0.07f, h * 0.04f)
                path.lineTo(w * 0.93f, h * 0.03f)
                path.quadTo(w * 0.88f, h * 0.5f, w * 0.95f, h * 0.96f)
                path.lineTo(w * 0.04f, h * 0.97f)
                path.quadTo(w * 0.11f, h * 0.5f, w * 0.07f, h * 0.04f)
                path.close()
            }
            "torn2" -> {
                val pad = w * 0.05f
                path.moveTo(pad, h * 0.06f)
                path.lineTo(w - pad, h * 0.06f)
                path.lineTo(w - pad, h * 0.82f)
                
                val teethCount = 15
                val step = (w - pad * 2) / teethCount
                var i = w - pad
                while (i > pad) {
                    val rnd = if (Math.round(i).toInt() % 3 == 0) h * 0.05f else -h * 0.02f
                    path.lineTo(i - step / 2, h * 0.82f + rnd)
                    i -= step
                }
                path.lineTo(pad, h * 0.82f)
                path.close()
            }
            "tornwindow" -> {
                val padX = w * 0.1f
                val padY = h * 0.12f
                path.moveTo(padX, padY)
                
                val stepX = (w - padX * 2) / 10
                var i = padX
                while (i < w - padX) {
                    val rnd = if (Math.round(i).toInt() % 3 == 0) h * 0.02f else -h * 0.015f
                    path.lineTo(i + stepX / 2, padY + rnd)
                    i += stepX
                }
                path.lineTo(w - padX, h - padY)
                
                i = w - padX
                while (i > padX) {
                    val rnd = if (Math.round(i).toInt() % 3 == 0) -h * 0.02f else h * 0.015f
                    path.lineTo(i - stepX / 2, h - padY + rnd)
                    i -= stepX
                }
                path.lineTo(padX, padY)
                path.close()
            }
            "stamp" -> {
                val px = w * 0.05f
                val py = h * 0.05f
                val pw = w * 0.9f
                val ph = h * 0.9f
                val r = min(w, h) * 0.015f
                val stepsX = (pw / (r * 4)).toInt()
                val stepW = pw / stepsX
                val stepsY = (ph / (r * 4)).toInt()
                val stepH = ph / stepsY

                path.moveTo(px, py)
                for (i in 0 until stepsX) {
                    path.lineTo(px + i * stepW + stepW / 2 - r, py)
                    path.arcTo(RectF(px + i * stepW + stepW / 2 - r, py - r, px + i * stepW + stepW / 2 + r, py + r), 180f, -180f)
                    path.lineTo(px + (i + 1) * stepW, py)
                }
                for (i in 0 until stepsY) {
                    path.lineTo(px + pw, py + i * stepH + stepH / 2 - r)
                    path.arcTo(RectF(px + pw - r, py + i * stepH + stepH / 2 - r, px + pw + r, py + i * stepH + stepH / 2 + r), 270f, -180f)
                    path.lineTo(px + pw, py + (i + 1) * stepH)
                }
                for (i in stepsX downTo 1) {
                    path.lineTo(px + i * stepW - stepW / 2 + r, py + ph)
                    path.arcTo(RectF(px + i * stepW - stepW / 2 - r, py + ph - r, px + i * stepW - stepW / 2 + r, py + ph + r), 0f, -180f)
                    path.lineTo(px + (i - 1) * stepW, py + ph)
                }
                for (i in stepsY downTo 1) {
                    path.lineTo(px, py + i * stepH - stepH / 2 + r)
                    path.arcTo(RectF(px - r, py + i * stepH - stepH / 2 - r, px + r, py + i * stepH - stepH / 2 + r), 90f, -180f)
                    path.lineTo(px, py + (i - 1) * stepH)
                }
                path.close()
            }
            "puzzle" -> {
                val x = w * 0.1f
                val y = h * 0.1f
                val pw = w * 0.8f
                val ph = h * 0.8f
                val knob = min(w, h) * 0.08f
                
                path.moveTo(x, y)
                path.lineTo(x + pw / 2 - knob, y)
                path.cubicTo(x + pw / 2 - knob, y - knob * 2, x + pw / 2 + knob, y - knob * 2, x + pw / 2 + knob, y)
                path.lineTo(x + pw, y)
                path.lineTo(x + pw, y + ph / 2 - knob)
                path.cubicTo(x + pw - knob * 2, y + ph / 2 - knob, x + pw - knob * 2, y + ph / 2 + knob, x + pw, y + ph / 2 + knob)
                path.lineTo(x + pw, y + ph)
                path.lineTo(x + pw / 2 + knob, y + ph)
                path.cubicTo(x + pw / 2 + knob, y + ph - knob * 2, x + pw / 2 - knob, y + ph - knob * 2, x + pw / 2 - knob, y + ph)
                path.lineTo(x, y + ph)
                path.lineTo(x, y + ph / 2 + knob)
                path.cubicTo(x + knob * 2, y + ph / 2 + knob, x + knob * 2, y + ph / 2 - knob, x, y + ph / 2 - knob)
                path.close()
            }
            "diagonal" -> {
                path.moveTo(w * 0.1f, h * 0.04f)
                path.lineTo(w * 0.94f, h * 0.15f)
                path.lineTo(w * 0.88f, h * 0.96f)
                path.lineTo(w * 0.05f, h * 0.84f)
                path.close()
            }
            "diagonalsplit" -> {
                path.moveTo(0f, h * 0.15f)
                path.lineTo(w, 0f)
                path.lineTo(w, h * 0.82f)
                path.lineTo(0f, h * 0.98f)
                path.close()
            }
            "hexagon" -> {
                path.moveTo(w * 0.5f, h * 0.05f)
                path.lineTo(w * 0.93f, h * 0.25f)
                path.lineTo(w * 0.93f, h * 0.75f)
                path.lineTo(w * 0.5f, h * 0.95f)
                path.lineTo(w * 0.07f, h * 0.75f)
                path.lineTo(w * 0.07f, h * 0.25f)
                path.close()
            }
            "diamond" -> {
                path.moveTo(w * 0.5f, h * 0.03f)
                path.lineTo(w * 0.95f, h * 0.5f)
                path.lineTo(w * 0.5f, h * 0.97f)
                path.lineTo(w * 0.05f, h * 0.5f)
                path.close()
            }
            "badge" -> {
                val cx = w / 2f
                val cy = h / 2f
                val minD = min(w, h)
                val rOuter = minD * 0.45f
                val rInner = minD * 0.38f
                val points = 16
                for (i in 0 until points * 2) {
                    val r = if (i % 2 == 0) rOuter else rInner
                    val a = (i * Math.PI) / points
                    if (i == 0) {
                        path.moveTo((cx + r * Math.sin(a)).toFloat(), (cy - r * Math.cos(a)).toFloat())
                    } else {
                        path.lineTo((cx + r * Math.sin(a)).toFloat(), (cy - r * Math.cos(a)).toFloat())
                    }
                }
                path.close()
            }
            "circle" -> {
                val cx = w / 2f
                val cy = h / 2f
                val r = min(w, h) * 0.45f
                path.addCircle(cx, cy, r, Path.Direction.CW)
            }
            "ellipse" -> {
                val cx = w / 2f
                val cy = h / 2f
                val rx = w * 0.4f
                val ry = h * 0.43f
                path.addOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), Path.Direction.CW)
            }
            "windowgrid" -> {
                val gap = w * 0.03f
                val padX = w * 0.07f
                val padY = h * 0.05f
                val rw = (w - padX * 2f - gap) / 2f
                val rh = (h - padY * 2f - gap) / 2f
                path.addRect(RectF(padX, padY, padX + rw, padY + rh), Path.Direction.CW)
                path.addRect(RectF(padX + rw + gap, padY, padX + rw + gap + rw, padY + rh), Path.Direction.CW)
                path.addRect(RectF(padX, padY + rh + gap, padX + rw, padY + rh + gap + rh), Path.Direction.CW)
                path.addRect(RectF(padX + rw + gap, padY + rh + gap, padX + rw + gap + rw, padY + rh + gap + rh), Path.Direction.CW)
            }
            else -> {
                // "none" or unknown: solid white mask (no masking)
                path.addRect(RectF(0f, 0f, w, h), Path.Direction.CW)
            }
        }
        
        canvas.drawPath(path, paint)
        return@withContext output
    }
}
