package com.ditherlab.ultra.engine.pwa

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Typeface
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

class TextGlitchEngineKotlin : VisualEngine {
    override val engineName: String = "TextGlitch"

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        val w = input.width
        val h = input.height
        val maxDim = max(w, h)
        val scale = maxDim / 1200f

        val outBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val ctx = Canvas(outBitmap)
        ctx.drawBitmap(input, 0f, 0f, null)

        val text = config.textGlitchText
        val glitchStyle = config.textGlitchStyle // 'vhs', 'rgb_shift', 'stamp'
        val fontSize = config.textGlitchFontSize

        ctx.save()

        if (glitchStyle == "vhs") {
            val vhsFontSize = max(12f, fontSize * 0.75f)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = vhsFontSize
                typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.LEFT
            }

            val posX = (w * 0.05f).roundToInt().toFloat()
            val posY = (h - (h * 0.08f)).roundToInt().toFloat()
            val displayText = "PLAY \u25BA ${text.uppercase()}"

            // Shadow
            paint.color = Color.BLACK
            ctx.drawText(displayText, posX + 2f, posY + 2f, paint)

            // Neon Cyan
            paint.color = Color.parseColor("#00ffcc")
            ctx.drawText(displayText, posX, posY, paint)

            // Tracking Line
            val lineY = posY - (vhsFontSize * 0.4f).roundToInt()
            val linePaint = Paint().apply {
                color = Color.argb((255 * 0.35f).toInt(), 255, 255, 255)
                this.style = Paint.Style.FILL
            }
            ctx.drawRect(0f, lineY, w.toFloat(), lineY + max(1, (2 * scale).roundToInt()), linePaint)

        } else if (glitchStyle == "rgb_shift") {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = fontSize
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }

            val centerX = w / 2f
            val centerY = h / 2f
            val textOffset = (paint.descent() + paint.ascent()) / 2f
            val drawY = centerY - textOffset
            
            val shift = max(2f, fontSize * 0.12f)

            // Shadow
            paint.color = Color.argb((255 * 0.70f).toInt(), 0, 0, 0)
            ctx.drawText(text, centerX + 3f, drawY + 3f, paint)

            // Red Shift
            paint.color = Color.parseColor("#ff0055")
            ctx.drawText(text, centerX + shift, drawY, paint)

            // Cyan Shift
            paint.color = Color.parseColor("#00f0ff")
            ctx.drawText(text, centerX - shift, drawY, paint)

            // White Center
            paint.color = Color.WHITE
            ctx.drawText(text, centerX, drawY, paint)

            // Micro Glitch Slices
            val sliceY = (centerY - fontSize * 0.2f).toInt()
            val sliceH = max(2, (fontSize * 0.15f).roundToInt())
            val sliceShift = (fontSize * 0.15f).roundToInt()

            val srcRect = Rect((centerX - (w * 0.4f)).toInt(), sliceY, (centerX - (w * 0.4f) + (w * 0.8f)).toInt(), sliceY + sliceH)
            val dstRect = Rect(srcRect.left + sliceShift, srcRect.top, srcRect.right + sliceShift, srcRect.bottom)
            
            // In Android Canvas, copying from itself while drawing requires a secondary bitmap or saveLayer, 
            // but we can just use createBitmap to slice and draw.
            if (srcRect.left >= 0 && srcRect.top >= 0 && srcRect.right <= w && srcRect.bottom <= h && srcRect.width() > 0 && srcRect.height() > 0) {
                val sliceBitmap = Bitmap.createBitmap(outBitmap, srcRect.left, srcRect.top, srcRect.width(), srcRect.height())
                ctx.drawBitmap(sliceBitmap, null, dstRect, null)
                sliceBitmap.recycle()
            }

        } else if (glitchStyle == "stamp") {
            val centerX = w / 2f
            val centerY = h / 2f

            ctx.translate(centerX, centerY)
            ctx.rotate(-12f)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = fontSize
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
                color = Color.parseColor("#dc2626")
            }

            val textWidth = paint.measureText(text)
            val padX = (fontSize * 0.4f).roundToInt()
            val padY = (fontSize * 0.2f).roundToInt()
            val boxW = textWidth + padX * 2
            val boxH = fontSize + padY * 2
            val borderW = max(3f, fontSize * 0.1f)

            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.STROKE
                color = Color.parseColor("#dc2626")
                strokeWidth = borderW
            }

            ctx.drawRect(-boxW / 2f, -boxH / 2f, boxW / 2f, boxH / 2f, strokePaint)

            val textOffset = (paint.descent() + paint.ascent()) / 2f
            ctx.drawText(text, 0f, -textOffset, paint)

            val clearPaint = Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                this.style = Paint.Style.FILL
            }

            val dotCount = floor((boxW * boxH) * 0.012).toInt()
            for (d in 0 until dotCount) {
                val rx = (Math.random() - 0.5) * boxW
                val ry = (Math.random() - 0.5) * boxH
                val rRadius = (Math.random() * (2.5 * scale) + 0.5).toFloat()
                ctx.drawCircle(rx.toFloat(), ry.toFloat(), rRadius, clearPaint)
            }
        }

        ctx.restore()
        outBitmap
    }
}
