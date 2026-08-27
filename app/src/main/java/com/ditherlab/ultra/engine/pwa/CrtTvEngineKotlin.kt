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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class CrtTvEngineKotlin : VisualEngine {
    override val engineName: String = "CrtTv"

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        val w = input.width
        val h = input.height
        val maxDim = max(w, h)
        val scale = maxDim / 1200f

        val outBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val ctx = Canvas(outBitmap)

        val scanlineGap = config.crtScanlineGap.toInt()
        val phosphorGlow = config.crtPhosphorGlow

        ctx.drawBitmap(input, 0f, 0f, null)

        val shiftPx = max(1, (2 * scale).roundToInt())
        val screenPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            alpha = (255 * 0.35f).toInt()
        }
        ctx.drawBitmap(input, -shiftPx.toFloat(), 0f, screenPaint)

        if (phosphorGlow) {
            val glowPaint = Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                alpha = (255 * 0.30f).toInt()
                isFilterBitmap = true
            }
            // Simple blur simulation via downscale-upscale
            val blurW = max(16, w / 4)
            val blurH = max(16, h / 4)
            val smallBitmap = Bitmap.createScaledBitmap(input, blurW, blurH, true)
            
            ctx.drawBitmap(smallBitmap, null, android.graphics.RectF(0f, 0f, w.toFloat(), h.toFloat()), glowPaint)
            smallBitmap.recycle()
        }

        val lineH = max(1, (scanlineGap * 0.45f).roundToInt())
        val scanlinePaint = Paint().apply {
            color = Color.argb((255 * 0.40f).toInt(), 0, 0, 0)
            style = Paint.Style.FILL
        }

        var y = 0
        while (y < h) {
            ctx.drawRect(0f, y.toFloat(), w.toFloat(), (y + lineH).toFloat(), scanlinePaint)
            y += scanlineGap
        }

        val radGrad = RadialGradient(
            w / 2f, h / 2f,
            max(w, h) * 0.72f,
            intArrayOf(Color.TRANSPARENT, Color.argb((255 * 0.25f).toInt(), 0, 0, 0), Color.argb((255 * 0.85f).toInt(), 0, 0, 0)),
            floatArrayOf(0f, 0.7f, 1f),
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
