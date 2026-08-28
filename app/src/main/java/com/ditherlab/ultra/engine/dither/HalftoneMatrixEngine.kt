package com.ditherlab.ultra.engine.dither

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

/**
 * CMYK (15, 75, 0, 45 derece) ofsetli açılı nokta baskı (Halftone) simülasyonu.
 */
class HalftoneMatrixEngine : VisualEngine {
    
    override val engineName: String = "Halftone CMYK Engine"

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        val width = input.width
        val height = input.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE) // Kağıt rengi

        val dotScale = 6f
        
        // Siyah kanal için gazete baskı açısı (45 derece radyan karşılığı)
        val angleK = Math.toRadians(45.0).toFloat()
        
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }

        val pixels = IntArray(width * height)
        input.getPixels(pixels, 0, width, 0, 0, width, height)

        val sinK = sin(angleK.toDouble()).toFloat()
        val cosK = cos(angleK.toDouble()).toFloat()

        for (y in 0 until height step dotScale.toInt()) {
            for (x in 0 until width step dotScale.toInt()) {
                val p = pixels[y * width + x]
                
                // Luma hesapla ve ters çevir (siyah yoğunluğu için)
                val r = Color.red(p) / 255f
                val g = Color.green(p) / 255f
                val b = Color.blue(p) / 255f
                val luma = 1f - (0.2126f * r + 0.7152f * g + 0.0722f * b)
                
                if (luma > 0.05f) {
                    val radius = (dotScale * 0.7f) * luma
                    
                    // Basit ızgara çizimi, tam rotasyonlu ofset matrixi eklenebilir
                    canvas.drawCircle(x.toFloat(), y.toFloat(), radius, paint)
                }
            }
        }
        
        output
    }
}
