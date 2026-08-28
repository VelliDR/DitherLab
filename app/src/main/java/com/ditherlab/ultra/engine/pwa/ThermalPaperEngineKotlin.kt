package com.ditherlab.ultra.engine.pwa

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ThermalPaperEngineKotlin : VisualEngine {
    override val engineName: String = "ThermalPaper"

    private val bayerMatrix4x4 = arrayOf(
        intArrayOf(0, 8, 2, 10),
        intArrayOf(12, 4, 14, 6),
        intArrayOf(3, 11, 1, 9),
        intArrayOf(15, 7, 13, 5)
    )

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        val w = input.width
        val h = input.height

        val maxDim = maxOf(w, h)
        val scale = maxDim / 1200f

        val outBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val ctx = Canvas(outBitmap)

        val paperType = config.thermalPaperType // 'aged' or 'fresh'
        val wear = config.thermalWear // 0 - 100
        val tornEdge = config.thermalTornEdge

        val bgColor = if (paperType == "aged") Color.parseColor("#f3ebd7") else Color.parseColor("#f7f7f4")
        val dotColor = if (paperType == "aged") Color.parseColor("#221e1a") else Color.parseColor("#111111")

        ctx.drawColor(bgColor)

        val srcPixels = IntArray(w * h)
        input.getPixels(srcPixels, 0, w, 0, 0, w, h)

        val dSize = maxOf(2, Math.round(3 * scale))
        val paint = Paint().apply {
            color = dotColor
            style = Paint.Style.FILL
        }

        val matrixCols = w / dSize
        val matrixRows = h / dSize

        for (gy in 0 until matrixRows) {
            val y = gy * dSize
            for (gx in 0 until matrixCols) {
                val x = gx * dSize

                val srcX = minOf(w - 1, (x + dSize / 2))
                val srcY = minOf(h - 1, (y + dSize / 2))
                val idx = srcY * w + srcX
                val color = srcPixels[idx]
                
                val a = (color shr 24) and 0xFF
                if (a < 25) continue // Alpha check

                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF

                val lum = 0.299f * r + 0.587f * g + 0.114f * b

                val bayerVal = (bayerMatrix4x4[gy % 4][gx % 4] / 16.0f) - 0.5f
                val ditheredLum = (lum + bayerVal * 60f).coerceIn(0f, 255f)

                if (ditheredLum < 128f) {
                    ctx.drawRect(x.toFloat(), y.toFloat(), (x + dSize).toFloat(), (y + dSize).toFloat(), paint)
                }
            }
        }

        val normWear = wear / 100f
        if (normWear > 0) {
            val streakCount = ((3 + normWear * 14) * scale).toInt()
            paint.color = bgColor
            for (s in 0 until streakCount) {
                val streakX = (Math.random() * w).toFloat()
                val streakW = maxOf(1f, Math.round((1 + Math.random() * 2.5) * scale).toFloat())
                ctx.drawRect(streakX, 0f, streakX + streakW, h.toFloat(), paint)
            }
        }

        if (tornEdge) {
            val clearPaint = Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                style = Paint.Style.FILL
            }

            val teethSize = maxOf(10f, Math.round(18 * scale).toFloat())
            val toothH = maxOf(4f, Math.round(8 * scale).toFloat())

            val topPath = Path()
            topPath.moveTo(0f, 0f)
            var tx = 0f
            while (tx < w) {
                topPath.lineTo(tx + teethSize / 2, (Math.random() * toothH).toFloat() + (toothH * 0.5f))
                topPath.lineTo(tx + teethSize, 0f)
                tx += teethSize
            }
            topPath.lineTo(w.toFloat(), 0f)
            topPath.close()
            ctx.drawPath(topPath, clearPaint)

            val bottomPath = Path()
            bottomPath.moveTo(0f, h.toFloat())
            tx = 0f
            while (tx < w) {
                bottomPath.lineTo(tx + teethSize / 2, h - ((Math.random() * toothH).toFloat() + (toothH * 0.5f)))
                bottomPath.lineTo(tx + teethSize, h.toFloat())
                tx += teethSize
            }
            bottomPath.lineTo(w.toFloat(), h.toFloat())
            bottomPath.close()
            ctx.drawPath(bottomPath, clearPaint)
        }

        outBitmap
    }
}
