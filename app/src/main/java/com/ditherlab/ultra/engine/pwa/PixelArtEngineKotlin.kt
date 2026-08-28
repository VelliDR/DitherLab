package com.ditherlab.ultra.engine.pwa

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PixelArtEngineKotlin : VisualEngine {
    override val engineName: String = "PixelArt"

    private val palettes = mapOf(
        "gameboy" to intArrayOf(
            0xFF0F380F.toInt(), // Koyu Yeşil
            0xFF306230.toInt(), // Orta Koyu Yeşil
            0xFF8BAC0F.toInt(), // Açık Yeşil
            0xFF9BBC0F.toInt()  // Parlak Sarı-Yeşil
        ),
        "gb-pocket" to intArrayOf(
            0xFF2B2B26.toInt(),
            0xFF707360.toInt(),
            0xFFA3A68D.toInt(),
            0xFFD4D7C1.toInt()
        ),
        "cga" to intArrayOf(
            0xFF000000.toInt(),
            0xFF55FFFF.toInt(),
            0xFFFF55FF.toInt(),
            0xFFFFFFFF.toInt()
        ),
        "cyberpunk" to intArrayOf(
            0xFF0A0A12.toInt(),
            0xFFFF007F.toInt(),
            0xFF00F0FF.toInt(),
            0xFF8A2BE2.toInt()
        ),
        "c64" to intArrayOf(
            0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF880000.toInt(), 0xFFAAFFEE.toInt(),
            0xFFCC44CC.toInt(), 0xFF00CC55.toInt(), 0xFF0000AA.toInt(), 0xFFEEEE4D.toInt(),
            0xFFDD8855.toInt(), 0xFF664400.toInt(), 0xFFFF7777.toInt(), 0xFF333333.toInt(),
            0xFF777777.toInt(), 0xFFAAFF66.toInt(), 0xFF0088FF.toInt(), 0xFFBBBBBB.toInt()
        ),
        "vaporwave" to intArrayOf(
            0xFF200E3A.toInt(),
            0xFFFF71CE.toInt(),
            0xFF01CDFE.toInt(),
            0xFFFF9966.toInt()
        )
    )

    private val bayerMatrix4x4 = arrayOf(
        intArrayOf(0, 8, 2, 10),
        intArrayOf(12, 4, 14, 6),
        intArrayOf(3, 11, 1, 9),
        intArrayOf(15, 7, 13, 5)
    )

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        val w = input.width
        val h = input.height

        val pixelSize = config.pixelSize.coerceIn(16f, 512f)
        val scale = pixelSize / maxOf(w, h).toFloat()
        val smallW = maxOf(8, (w * scale).toInt())
        val smallH = maxOf(8, (h * scale).toInt())

        val smallBitmap = Bitmap.createScaledBitmap(input, smallW, smallH, true)
        val pixels = IntArray(smallW * smallH)
        smallBitmap.getPixels(pixels, 0, smallW, 0, 0, smallW, smallH)

        val palette = palettes[config.paletteKey] ?: palettes["gameboy"]!!
        val isLowColorPalette = palette.size <= 6

        for (y in 0 until smallH) {
            for (x in 0 until smallW) {
                val idx = y * smallW + x
                val color = pixels[idx]

                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF

                var newR = r.toFloat()
                var newG = g.toFloat()
                var newB = b.toFloat()

                if (isLowColorPalette) {
                    val bayerValue = bayerMatrix4x4[y % 4][x % 4]
                    val bayerFactor = (bayerValue / 15.0f - 0.5f) * 64.0f // Yayılım
                    newR = (newR + bayerFactor).coerceIn(0f, 255f)
                    newG = (newG + bayerFactor).coerceIn(0f, 255f)
                    newB = (newB + bayerFactor).coerceIn(0f, 255f)
                }

                val mappedColor = findNearestColorPerceptual(newR.toInt(), newG.toInt(), newB.toInt(), palette)
                pixels[idx] = mappedColor
            }
        }

        smallBitmap.setPixels(pixels, 0, smallW, 0, 0, smallW, smallH)

        // Scale back up without filtering to preserve hard pixel edges
        val outBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)
        val paint = Paint().apply {
            isFilterBitmap = false
            isDither = false
        }
        canvas.drawBitmap(smallBitmap, Rect(0, 0, smallW, smallH), Rect(0, 0, w, h), paint)
        
        smallBitmap.recycle()

        outBitmap
    }

    private fun findNearestColorPerceptual(r: Int, g: Int, b: Int, palette: IntArray): Int {
        var minDistance = Float.MAX_VALUE
        var nearest = palette[0]

        for (c in palette) {
            val pr = (c shr 16) and 0xFF
            val pg = (c shr 8) and 0xFF
            val pb = c and 0xFF

            val meanR = (r + pr) / 2.0f
            val dR = (r - pr).toFloat()
            val dG = (g - pg).toFloat()
            val dB = (b - pb).toFloat()

            val dist = (2f + meanR / 256f) * dR * dR +
                       4f * dG * dG +
                       (2f + (255f - meanR) / 256f) * dB * dB

            if (dist < minDistance) {
                minDistance = dist
                nearest = c
            }
        }
        return nearest
    }
}
