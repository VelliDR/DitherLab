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
        val maxRadius = min(w, h) * 0.45f

        // 1. Create accumulation layer for smooth swirly blur
        val swirlLayer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val sCanvas = Canvas(swirlLayer)

        // 2. Multi-sample rotation steps for continuous Petzval swirl (24 smooth steps)
        val steps = 12
        val maxAngle = (intensity / 100f) * 6.0f // max swirl rotation arc in degrees
        val stepAlpha = 1f / (steps * 2 + 1)
        val paint = Paint().apply {
            alpha = (stepAlpha * 255).toInt()
            isAntiAlias = true
            isFilterBitmap = true
        }

        // Draw center base
        sCanvas.drawBitmap(input, 0f, 0f, paint)

        // Draw multi-step rotational samples
        for (i in 1..steps) {
            val stepFraction = i.toFloat() / steps
            val angle = stepFraction * maxAngle

            // Clockwise rotation step
            sCanvas.save()
            sCanvas.translate(cx, cy)
            sCanvas.rotate(angle)
            sCanvas.drawBitmap(input, -cx, -cy, paint)
            sCanvas.restore()

            // Counter-clockwise rotation step
            sCanvas.save()
            sCanvas.translate(cx, cy)
            sCanvas.rotate(-angle)
            sCanvas.drawBitmap(input, -cx, -cy, paint)
            sCanvas.restore()
        }

        // 3. Highlight Boost Layer to create Petzval Cat's-Eye Bokeh highlights
        val highlightLayer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val hCanvas = Canvas(highlightLayer)
        val hlPaint = Paint().apply {
            // Brightness boost filter for highlights
            colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                1.5f, 0f, 0f, 0f, -50f,
                0f, 1.5f, 0f, 0f, -50f,
                0f, 0f, 1.5f, 0f, -50f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        hCanvas.drawBitmap(swirlLayer, 0f, 0f, hlPaint)

        // Add highlight boost back onto swirlLayer using ADD / SCREEN mode
        val addPaint = Paint().apply {
            alpha = ((intensity / 100f) * 80).toInt().coerceIn(0, 255)
            blendMode = BlendMode.SCREEN
        }
        sCanvas.drawBitmap(highlightLayer, 0f, 0f, addPaint)
        highlightLayer.recycle()

        // 4. Create Radial Falloff Mask so center remains sharp (Petzval optic characteristic)
        val maskLayer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val mCanvas = Canvas(maskLayer)
        val gradPaint = Paint().apply {
            shader = RadialGradient(
                cx, cy, maxRadius,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.BLACK),
                floatArrayOf(0f, 0.25f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        mCanvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), gradPaint)

        // Mask the swirl layer so only outer area swirls
        val destInPaint = Paint().apply {
            blendMode = BlendMode.DST_IN
        }
        sCanvas.drawBitmap(maskLayer, 0f, 0f, destInPaint)
        maskLayer.recycle()

        // 5. Final Composite (Sharp Input + Swirled Edges)
        val resultLayer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val resultCanvas = Canvas(resultLayer)
        resultCanvas.drawBitmap(input, 0f, 0f, null)
        resultCanvas.drawBitmap(swirlLayer, 0f, 0f, null)
        swirlLayer.recycle()

        resultLayer
    }
}
