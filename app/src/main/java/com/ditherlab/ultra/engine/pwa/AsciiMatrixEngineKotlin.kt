package com.ditherlab.ultra.engine.pwa

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.floor

class AsciiMatrixEngineKotlin : VisualEngine {
    override val engineName: String = "AsciiMatrix"

    private val charSets = mapOf(
        "density" to " .:-=+*#%@",
        "binary" to "01",
        "hex" to "0123456789ABCDEF"
    )

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        val w = input.width
        val h = input.height

        val outBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val ctx = Canvas(outBitmap)

        ctx.drawColor(Color.parseColor("#040804"))

        val srcPixels = IntArray(w * h)
        input.getPixels(srcPixels, 0, w, 0, 0, w, h)

        val charSetKey = if (config.asciiEnabled) config.asciiCharSetKey else config.asciiCharSetKey
        val colorMode = config.asciiColorMode
        val fontSize = maxOf(6f, config.asciiFontSize)

        val cellH = maxOf(6, Math.round(fontSize))
        val cellW = maxOf(3, Math.round(cellH * 0.6f))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = cellH.toFloat()
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
        
        // Font metrics to center vertically
        val textOffset = (paint.descent() + paint.ascent()) / 2

        val chars = charSets[charSetKey] ?: charSetKey
        var customCharIdx = 0

        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val srcX = minOf(w - 1, x + cellW / 2)
                val srcY = minOf(h - 1, y + cellH / 2)
                val idx = srcY * w + srcX
                val color = srcPixels[idx]

                val a = (color shr 24) and 0xFF
                if (a < 25) {
                    x += cellW
                    continue
                }

                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF

                val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f

                val charToDraw = if (charSets.containsKey(charSetKey)) {
                    val charIdx = floor(lum * (chars.length - 1)).toInt()
                    chars[charIdx].toString()
                } else {
                    val c = chars[customCharIdx % chars.length].toString()
                    customCharIdx++
                    c
                }

                if (colorMode == "matrix") {
                    val green = (50 + lum * 205).toInt().coerceIn(0, 255)
                    val redBlue = if (lum > 0.75f) ((lum - 0.75f) * 4 * 180).toInt().coerceIn(0, 255) else 0
                    paint.color = Color.rgb(redBlue, green, maxOf(20, redBlue))
                } else if (colorMode == "amber") {
                    val red = (80 + lum * 175).toInt().coerceIn(0, 255)
                    val green = (lum * 180).toInt().coerceIn(0, 255)
                    val blue = if (lum > 0.85f) ((lum - 0.85f) * 6 * 150).toInt().coerceIn(0, 255) else 0
                    paint.color = Color.rgb(red, green, blue)
                } else {
                    paint.color = Color.rgb(r, g, b)
                }

                ctx.drawText(charToDraw, x + cellW / 2f, y + cellH / 2f - textOffset, paint)
                x += cellW
            }
            y += cellH
        }

        outBitmap
    }
}
