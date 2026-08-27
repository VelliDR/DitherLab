package com.ditherlab.ultra.engine.pwa

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class FlirThermalEngineKotlin : VisualEngine {
    override val engineName: String = "FlirThermal"

    private val lutCache = mutableMapOf<String, IntArray>()

    private fun getLUT(mode: String): IntArray {
        val activeMode = if (mode.isNotBlank()) mode else "ironbow"
        if (lutCache.containsKey(activeMode)) {
            return lutCache[activeMode]!!
        }

        val lut = IntArray(256)
        for (i in 0 until 256) {
            val norm = i / 255.0
            var r = 0
            var g = 0
            var b = 0

            if (activeMode == "emerald") {
                if (norm < 0.5) {
                    val t = norm / 0.5
                    r = floor(t * 15).toInt()
                    g = floor(t * 180).toInt()
                    b = floor(t * 30).toInt()
                } else {
                    val t = (norm - 0.5) / 0.5
                    r = floor(15 + t * 240).toInt()
                    g = floor(180 + t * 75).toInt()
                    b = floor(30 + t * 225).toInt()
                }
            } else if (activeMode == "rainbow") {
                val h = (1.0 - norm) * 240.0
                val rgb = hslToRgb(h / 360.0, 1.0, 0.5)
                r = rgb[0]
                g = rgb[1]
                b = rgb[2]
            } else {
                if (norm < 0.25) {
                    val t = norm / 0.25
                    r = floor(t * 45).toInt()
                    g = 0
                    b = floor(80 + t * 100).toInt()
                } else if (norm < 0.5) {
                    val t = (norm - 0.25) / 0.25
                    r = floor(45 + t * 180).toInt()
                    g = floor(t * 30).toInt()
                    b = floor(180 - t * 180).toInt()
                } else if (norm < 0.75) {
                    val t = (norm - 0.5) / 0.25
                    r = floor(225 + t * 30).toInt()
                    g = floor(30 + t * 195).toInt()
                    b = 0
                } else {
                    val t = (norm - 0.75) / 0.25
                    r = 255
                    g = floor(225 + t * 30).toInt()
                    b = floor(t * 255).toInt()
                }
            }
            lut[i] = Color.rgb(r, g, b)
        }
        lutCache[activeMode] = lut
        return lut
    }

    private fun hslToRgb(h: Double, s: Double, l: Double): IntArray {
        var r: Double
        var g: Double
        var b: Double
        if (s == 0.0) {
            r = l
            g = l
            b = l
        } else {
            val q = if (l < 0.5) l * (1 + s) else l + s - l * s
            val p = 2 * l - q
            r = hueToRgb(p, q, h + 1.0 / 3.0)
            g = hueToRgb(p, q, h)
            b = hueToRgb(p, q, h - 1.0 / 3.0)
        }
        return intArrayOf((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
    }

    private fun hueToRgb(p: Double, q: Double, tIn: Double): Double {
        var t = tIn
        if (t < 0) t += 1.0
        if (t > 1) t -= 1.0
        if (t < 1.0 / 6.0) return p + (q - p) * 6.0 * t
        if (t < 1.0 / 2.0) return q
        if (t < 2.0 / 3.0) return p + (q - p) * (2.0 / 3.0 - t) * 6.0
        return p
    }

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        val w = input.width
        val h = input.height

        val outBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

        val srcPixels = IntArray(w * h)
        input.getPixels(srcPixels, 0, w, 0, 0, w, h)

        val mode = config.flirMode
        val lut = getLUT(mode)

        for (i in srcPixels.indices) {
            val color = srcPixels[i]
            val a = (color shr 24) and 0xFF
            if (a < 10) continue

            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF

            val lum = Math.round(0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
            srcPixels[i] = lut[lum]
        }

        outBitmap.setPixels(srcPixels, 0, w, 0, 0, w, h)
        
        val ctx = Canvas(outBitmap)

        // Glow (Downscale blur)
        val blurW = max(16, w / 4)
        val blurH = max(16, h / 4)
        val smallBitmap = Bitmap.createScaledBitmap(outBitmap, blurW, blurH, true)
        val glowPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            alpha = (255 * 0.20f).toInt()
            isFilterBitmap = true
        }
        ctx.drawBitmap(smallBitmap, null, android.graphics.RectF(0f, 0f, w.toFloat(), h.toFloat()), glowPaint)
        smallBitmap.recycle()

        // Vignette
        val radGrad = RadialGradient(
            w / 2f, h / 2f,
            max(w, h) * 0.70f,
            intArrayOf(Color.TRANSPARENT, Color.argb((255 * 0.65f).toInt(), 0, 0, 0)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        val vignettePaint = Paint().apply {
            shader = radGrad
            style = Paint.Style.FILL
        }
        ctx.drawRect(0f, 0f, w.toFloat(), h.toFloat(), vignettePaint)

        outBitmap
    }
}
