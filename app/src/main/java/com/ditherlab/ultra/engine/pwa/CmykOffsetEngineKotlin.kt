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
import kotlin.math.min
import kotlin.math.roundToInt

class CmykOffsetEngineKotlin : VisualEngine {
    override val engineName: String = "CmykOffset"

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        val w = input.width
        val h = input.height

        val offsetPx = config.cmykOffsetPx
        val dotSize = config.cmykDotSize

        val dSize = max(3, dotSize.roundToInt())
        val offPx = offsetPx.roundToInt().toFloat()
        
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        // Saf Fildişi Matbaa Kağıdı Zemin
        canvas.drawColor(Color.parseColor("#fcfaf7"))
        
        val pixels = IntArray(w * h)
        input.getPixels(pixels, 0, w, 0, 0, w, h)
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        }
        
        val maxRadius = (dSize / 2f) * 1.15f
        
        val cyanPaint = Paint(paint).apply { color = Color.parseColor("#00e5ff") }
        val magentaPaint = Paint(paint).apply { color = Color.parseColor("#ff007f") }
        val yellowPaint = Paint(paint).apply { color = Color.parseColor("#ffeb3b") }
        val blackPaint = Paint(paint).apply { color = Color.parseColor("#1a1a1a") }
        
        // 1. CYAN KATMANI
        for (y in 0 until h step dSize) {
            for (x in 0 until w step dSize) {
                val srcX = min(w - 1, x + dSize / 2)
                val srcY = min(h - 1, y + dSize / 2)
                val pixel = pixels[srcY * w + srcX]
                
                val a = Color.alpha(pixel) / 255f
                if (a < 0.1f) continue
                
                val r = Color.red(pixel) / 255f
                val g = Color.green(pixel) / 255f
                val b = Color.blue(pixel) / 255f
                
                val k = 1f - max(r, max(g, b))
                val c = if (k == 1f) 0f else (1f - r - k) / (1f - k)
                
                if (c > 0.04f) {
                    canvas.drawCircle(x - offPx, y - offPx * 0.5f, c * maxRadius, cyanPaint)
                }
            }
        }

        // 2. MAGENTA KATMANI
        for (y in 0 until h step dSize) {
            for (x in 0 until w step dSize) {
                val srcX = min(w - 1, x + dSize / 2)
                val srcY = min(h - 1, y + dSize / 2)
                val pixel = pixels[srcY * w + srcX]
                
                val a = Color.alpha(pixel) / 255f
                if (a < 0.1f) continue
                
                val r = Color.red(pixel) / 255f
                val g = Color.green(pixel) / 255f
                val b = Color.blue(pixel) / 255f
                
                val k = 1f - max(r, max(g, b))
                val m = if (k == 1f) 0f else (1f - g - k) / (1f - k)
                
                if (m > 0.04f) {
                    canvas.drawCircle(x + offPx, y + offPx * 0.5f, m * maxRadius, magentaPaint)
                }
            }
        }

        // 3. YELLOW KATMANI
        for (y in 0 until h step dSize) {
            for (x in 0 until w step dSize) {
                val srcX = min(w - 1, x + dSize / 2)
                val srcY = min(h - 1, y + dSize / 2)
                val pixel = pixels[srcY * w + srcX]
                
                val a = Color.alpha(pixel) / 255f
                if (a < 0.1f) continue
                
                val r = Color.red(pixel) / 255f
                val g = Color.green(pixel) / 255f
                val b = Color.blue(pixel) / 255f
                
                val k = 1f - max(r, max(g, b))
                val yVal = if (k == 1f) 0f else (1f - b - k) / (1f - k)
                
                if (yVal > 0.04f) {
                    canvas.drawCircle(x.toFloat(), y + offPx * 0.3f, yVal * maxRadius, yellowPaint)
                }
            }
        }

        // 4. KEY / BLACK KATMANI
        for (y in 0 until h step dSize) {
            for (x in 0 until w step dSize) {
                val srcX = min(w - 1, x + dSize / 2)
                val srcY = min(h - 1, y + dSize / 2)
                val pixel = pixels[srcY * w + srcX]
                
                val a = Color.alpha(pixel) / 255f
                if (a < 0.1f) continue
                
                val r = Color.red(pixel) / 255f
                val g = Color.green(pixel) / 255f
                val b = Color.blue(pixel) / 255f
                
                val k = 1f - max(r, max(g, b))
                
                if (k > 0.05f) {
                    canvas.drawCircle(x.toFloat(), y.toFloat(), k * maxRadius, blackPaint)
                }
            }
        }
        
        output
    }
}
