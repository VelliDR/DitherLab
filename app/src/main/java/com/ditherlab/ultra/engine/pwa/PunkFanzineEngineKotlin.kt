package com.ditherlab.ultra.engine.pwa

import android.graphics.*
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*
import kotlin.random.Random

class PunkFanzineEngineKotlin : VisualEngine {
    override val engineName: String = "PunkFanzine"

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        val w = input.width
        val h = input.height
        val maxDim = max(w, h).toFloat()
        val scale = maxDim / 1200f
        
        val contrastBoost = config.punkContrastBoost
        val tonerNoise = config.punkTonerNoise
        val normNoise = tonerNoise / 100f

        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        input.getPixels(pixels, 0, w, 0, 0, w, h)
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = Color.alpha(pixel)
            if (a < 10) continue
            
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            
            var lum = 0.299f * r + 0.587f * g + 0.114f * b
            lum = (lum - 128f) * contrastBoost + 128f
            
            val microNoise = (Random.nextFloat() - 0.5f) * normNoise * 75f
            val clumpNoise = if (Random.nextFloat() > 0.94f) (Random.nextFloat() - 0.5f) * normNoise * 140f else 0f
            
            lum += microNoise + clumpNoise
            
            if (lum < 125f) {
                pixels[i] = Color.argb(a, 20, 20, 20)
            } else {
                val paperGrain = (Random.nextFloat() - 0.5f) * 12f
                val pr = min(255f, max(0f, 228f + paperGrain)).toInt()
                val pg = min(255f, max(0f, 224f + paperGrain)).toInt()
                val pb = min(255f, max(0f, 215f + paperGrain)).toInt()
                pixels[i] = Color.argb(a, pr, pg, pb)
            }
        }
        
        output.setPixels(pixels, 0, w, 0, 0, w, h)
        val canvas = Canvas(output)
        
        // Toner Streaks
        if (normNoise > 0.15f) {
            val streakCount = floor((2f + normNoise * 6f) * scale).toInt()
            val streakPaint = Paint().apply {
                color = Color.argb((0.12f * 255).toInt(), 20, 20, 20)
            }
            for (s in 0 until streakCount) {
                val streakX = floor(Random.nextFloat() * w)
                val streakW = max(1f, Math.round((1f + Random.nextFloat() * 3f) * scale).toFloat())
                canvas.drawRect(streakX, 0f, streakX + streakW, h.toFloat(), streakPaint)
            }
        }
        
        // Open Lid Edge Burn
        val outerRadius = max(1f, max(w, h) * 0.68f)
        val innerRadius = min(w, h) * 0.35f
        
        val stop0 = min(1f, innerRadius / outerRadius)
        val stop1 = min(1f, (innerRadius + 0.7f * (outerRadius - innerRadius)) / outerRadius)
        val stop2 = 1f
        
        val safeStop0 = max(0f, min(stop0, 0.98f))
        val safeStop1 = max(safeStop0 + 0.01f, min(stop1, 0.99f))
        
        val fixedRadGrad = RadialGradient(
            w / 2f, h / 2f,
            outerRadius,
            intArrayOf(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                Color.argb((0.20f * 255).toInt(), 0, 0, 0),
                Color.argb((0.75f * 255).toInt(), 0, 0, 0)
            ),
            floatArrayOf(0f, safeStop0, safeStop1, stop2),
            Shader.TileMode.CLAMP
        )
        
        val burnPaint = Paint().apply {
            shader = fixedRadGrad
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), burnPaint)
        
        output
    }
}
