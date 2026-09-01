package com.ditherlab.ultra.engine.pwa

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

class NoirComicEngineKotlin : VisualEngine {
    override val engineName: String = "NoirComic"

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        val w = input.width
        val h = input.height

        val dotSize = config.noirDotSize
        val contrast = config.noirContrast
        val textureDensity = config.noirTextureDensity
        val colorMode = config.noirColorMode // 0: B&W Noir, 1: Spider-Red Pop, 2: Full Color
        val dotColorHex = when (config.noirDotColor) {
            "red" -> "#C8102E"
            "navy" -> "#1B263B"
            else -> "#121212"
        }

        val dSize = max(3, dotSize.roundToInt())
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        // Solid opaque paper background
        canvas.drawColor(Color.parseColor("#F4F3EE"))

        val pixels = IntArray(w * h)
        input.getPixels(pixels, 0, w, 0, 0, w, h)

        val processedPixels = IntArray(w * h)
        val contrastFactor = contrast.coerceIn(0.5f, 5.0f)
        
        for (i in 0 until w * h) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            when (colorMode) {
                1 -> { // Spider-Red Pop: keep reds vibrant, convert everything else to high-contrast Noir B&W
                    val isRed = (r > 1.2f * g) && (r > 1.2f * b) && (r > 60)
                    if (isRed) {
                        val nr = ((r - 128) * contrastFactor + 128).toInt().coerceIn(0, 255)
                        val ng = (g * 0.35f).toInt().coerceIn(0, 255)
                        val nb = (b * 0.35f).toInt().coerceIn(0, 255)
                        processedPixels[i] = Color.rgb(nr, ng, nb)
                    } else {
                        val lum = (0.299f * r + 0.587f * g + 0.114f * b)
                        val adjLum = ((lum - 128f) * contrastFactor + 128f).toInt().coerceIn(0, 255)
                        processedPixels[i] = Color.rgb(adjLum, adjLum, adjLum)
                    }
                }
                2 -> { // Full Color Comic: preserve original colors with enhanced contrast
                    val nr = ((r - 128) * contrastFactor + 128).toInt().coerceIn(0, 255)
                    val ng = ((g - 128) * contrastFactor + 128).toInt().coerceIn(0, 255)
                    val nb = ((b - 128) * contrastFactor + 128).toInt().coerceIn(0, 255)
                    processedPixels[i] = Color.rgb(nr, ng, nb)
                }
                else -> { // 0: Classic Noir B&W
                    val lum = (0.299f * r + 0.587f * g + 0.114f * b)
                    val adjLum = ((lum - 128f) * contrastFactor + 128f).toInt().coerceIn(0, 255)
                    processedPixels[i] = Color.rgb(adjLum, adjLum, adjLum)
                }
            }
        }

        val baseBitmap = Bitmap.createBitmap(processedPixels, w, h, Bitmap.Config.ARGB_8888)
        canvas.drawBitmap(baseBitmap, 0f, 0f, null)
        baseBitmap.recycle()

        // 2. Add Halftone Dot Pattern over base image opaquely
        val dotPattern = Bitmap.createBitmap(dSize, dSize, Bitmap.Config.ARGB_8888)
        val dotCanvas = Canvas(dotPattern)
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(dotColorHex)
        }
        dotCanvas.drawCircle(dSize / 2f, dSize / 2f, dSize * 0.32f, dotPaint)

        val patternPaint = Paint().apply {
            shader = android.graphics.BitmapShader(dotPattern, android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        }

        val layerId = canvas.saveLayer(0f, 0f, w.toFloat(), h.toFloat(), null)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), patternPaint)
        canvas.restoreToCount(layerId)
        dotPattern.recycle()

        // 3. Rain / Scratch & Newspaper Texture
        if (textureDensity > 0f) {
            val scratchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                strokeWidth = 2f
                style = Paint.Style.STROKE
            }
            
            val noisePaint = Paint().apply {
                color = Color.parseColor("#101010")
            }
            
            val random = Random(42)
            
            val numScratches = (w * h * textureDensity * 0.00008f).toInt()
            for (i in 0 until numScratches) {
                val startX = random.nextFloat() * w
                val startY = random.nextFloat() * h
                val length = random.nextFloat() * h * 0.12f
                val endX = startX + (random.nextFloat() * 16f - 8f)
                val endY = startY + length
                
                if (random.nextBoolean()) {
                    scratchPaint.color = Color.parseColor("#F4F3EE")
                } else {
                    scratchPaint.color = Color.parseColor("#121212")
                }
                canvas.drawLine(startX, startY, endX, endY, scratchPaint)
            }
            
            val numNoise = (w * h * textureDensity * 0.005f).toInt()
            for (i in 0 until numNoise) {
                val nx = random.nextFloat() * w
                val ny = random.nextFloat() * h
                canvas.drawRect(nx, ny, nx + 2f, ny + 2f, noisePaint)
            }
        }

        output
    }
}
