package com.ditherlab.ultra.engine.karanlik

import android.graphics.*
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChromaticAberrationEngineKotlin : VisualEngine {
    override val engineName: String = "ChromaticAberration"

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        val intensity = config.chromaticAberrationAmount
        if (intensity <= 0f) return@withContext input

        val w = input.width
        val h = input.height
        val shift = (intensity / 100f) * (w.toFloat() * 0.012f)
        val scale = 1f + (shift / w.toFloat()) * 2f

        val resultLayer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultLayer)

        val redLayer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val redCanvas = Canvas(redLayer)
        redCanvas.save()
        redCanvas.translate(w.toFloat() / 2f, h.toFloat() / 2f)
        redCanvas.scale(scale, scale)
        redCanvas.drawBitmap(input, -w.toFloat() / 2f - shift, -h.toFloat() / 2f, null)
        redCanvas.restore()

        val multiplyPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        }
        multiplyPaint.color = Color.RED
        redCanvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), multiplyPaint)

        val cyanLayer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cyanCanvas = Canvas(cyanLayer)
        cyanCanvas.save()
        cyanCanvas.translate(w.toFloat() / 2f, h.toFloat() / 2f)
        cyanCanvas.scale(scale, scale)
        cyanCanvas.drawBitmap(input, -w.toFloat() / 2f + shift, -h.toFloat() / 2f, null)
        cyanCanvas.restore()

        multiplyPaint.color = Color.CYAN
        cyanCanvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), multiplyPaint)

        canvas.drawBitmap(redLayer, 0f, 0f, null)
        val screenPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        }
        canvas.drawBitmap(cyanLayer, 0f, 0f, screenPaint)

        resultLayer
    }
}
