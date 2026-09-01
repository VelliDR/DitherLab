package com.ditherlab.ultra.engine.karanlik

import android.graphics.*
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

class SwirlyBokehEngineKotlin : VisualEngine {
    override val engineName: String = "SwirlyBokeh"

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        val intensity = config.swirlyBokehIntensity
        if (intensity <= 0f) return@withContext input

        val w = input.width
        val h = input.height
        val cx = w / 2f
        val cy = h / 2f
        val maxRadius = min(w, h) * 0.35f

        val swirlLayer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val sCanvas = Canvas(swirlLayer)
        sCanvas.drawBitmap(input, 0f, 0f, null)

        val steps = if (w > 2000) 2 else 4
        val angleStep = (intensity / 100f) * (if (w > 2000) 3.6f else 1.8f)

        val alphaPaint = Paint().apply { alpha = (0.25f * 255).toInt() }
        for (i in -steps..steps) {
            if (i == 0) continue
            sCanvas.save()
            sCanvas.translate(cx, cy)
            sCanvas.rotate(i * angleStep)
            sCanvas.drawBitmap(input, -cx, -cy, alphaPaint)
            sCanvas.restore()
        }

        val maskLayer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val mCanvas = Canvas(maskLayer)
        val gradPaint = Paint().apply {
            shader = RadialGradient(
                cx, cy, maxRadius * 1.3f,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.BLACK),
                floatArrayOf(0f, 0.4f / 1.3f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        mCanvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), gradPaint)

        val destInPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        sCanvas.drawBitmap(maskLayer, 0f, 0f, destInPaint)

        val resultLayer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val resultCanvas = Canvas(resultLayer)
        resultCanvas.drawBitmap(input, 0f, 0f, null)
        resultCanvas.drawBitmap(swirlLayer, 0f, 0f, null)

        resultLayer
    }
}
