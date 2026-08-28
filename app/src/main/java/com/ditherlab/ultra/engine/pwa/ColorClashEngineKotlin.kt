package com.ditherlab.ultra.engine.pwa

import android.graphics.Bitmap
import android.graphics.Color
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

class ColorClashEngineKotlin : VisualEngine {
    override val engineName: String = "ColorClash"
    
    private val spectrumPalette = arrayOf(
        intArrayOf(0, 0, 0),
        intArrayOf(0, 0, 192),
        intArrayOf(192, 0, 0),
        intArrayOf(192, 0, 192),
        intArrayOf(0, 192, 0),
        intArrayOf(0, 192, 192),
        intArrayOf(192, 192, 0),
        intArrayOf(192, 192, 192),
        intArrayOf(0, 0, 255),
        intArrayOf(255, 0, 0),
        intArrayOf(255, 0, 255),
        intArrayOf(0, 255, 0),
        intArrayOf(0, 255, 255),
        intArrayOf(255, 255, 0),
        intArrayOf(255, 255, 255)
    )

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        val w = input.width
        val h = input.height
        val blockSize = config.colorClashBlockSize
        
        val maxDim = max(w, h).toFloat()
        val scale = maxDim / 1200f
        val scaledBlockSize = max(2, Math.round(blockSize * scale))
        
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        input.getPixels(pixels, 0, w, 0, 0, w, h)
        
        for (by in 0 until h step scaledBlockSize) {
            for (bx in 0 until w step scaledBlockSize) {
                var minLum = 255f
                var maxLum = 0f
                var totalR = 0f
                var totalG = 0f
                var totalB = 0f
                var count = 0
                
                for (py in 0 until scaledBlockSize) {
                    if (by + py >= h) continue
                    for (px in 0 until scaledBlockSize) {
                        if (bx + px >= w) continue
                        val i = (by + py) * w + (bx + px)
                        val p = pixels[i]
                        val r = Color.red(p).toFloat()
                        val g = Color.green(p).toFloat()
                        val b = Color.blue(p).toFloat()
                        val lum = 0.299f * r + 0.587f * g + 0.114f * b
                        
                        if (lum < minLum) minLum = lum
                        if (lum > maxLum) maxLum = lum
                        
                        totalR += r
                        totalG += g
                        totalB += b
                        count++
                    }
                }
                
                if (count == 0) continue
                
                val lumDiff = maxLum - minLum
                val avgR = totalR / count
                val avgG = totalG / count
                val avgB = totalB / count
                
                if (lumDiff < 22f) {
                    val solidColor = getClosestPaletteColor(avgR, avgG, avgB)
                    fillBlockWithColor(pixels, w, h, bx, by, scaledBlockSize, solidColor)
                    continue
                }
                
                val avgLum = (minLum + maxLum) / 2f
                var darkSumR = 0f; var darkSumG = 0f; var darkSumB = 0f
                var darkCount = 0
                var lightSumR = 0f; var lightSumG = 0f; var lightSumB = 0f
                var lightCount = 0
                
                for (py in 0 until scaledBlockSize) {
                    if (by + py >= h) continue
                    for (px in 0 until scaledBlockSize) {
                        if (bx + px >= w) continue
                        val i = (by + py) * w + (bx + px)
                        val p = pixels[i]
                        val r = Color.red(p).toFloat()
                        val g = Color.green(p).toFloat()
                        val b = Color.blue(p).toFloat()
                        val lum = 0.299f * r + 0.587f * g + 0.114f * b
                        
                        if (lum < avgLum) {
                            darkSumR += r; darkSumG += g; darkSumB += b
                            darkCount++
                        } else {
                            lightSumR += r; lightSumG += g; lightSumB += b
                            lightCount++
                        }
                    }
                }
                
                val darkAvgR = if (darkCount > 0) darkSumR / darkCount else avgR
                val darkAvgG = if (darkCount > 0) darkSumG / darkCount else avgG
                val darkAvgB = if (darkCount > 0) darkSumB / darkCount else avgB
                
                val lightAvgR = if (lightCount > 0) lightSumR / lightCount else avgR
                val lightAvgG = if (lightCount > 0) lightSumG / lightCount else avgG
                val lightAvgB = if (lightCount > 0) lightSumB / lightCount else avgB
                
                val inkColor = getClosestPaletteColor(darkAvgR, darkAvgG, darkAvgB)
                val paperColor = getClosestPaletteColor(lightAvgR, lightAvgG, lightAvgB)
                
                for (py in 0 until scaledBlockSize) {
                    if (by + py >= h) continue
                    for (px in 0 until scaledBlockSize) {
                        if (bx + px >= w) continue
                        val i = (by + py) * w + (bx + px)
                        val p = pixels[i]
                        val r = Color.red(p).toFloat()
                        val g = Color.green(p).toFloat()
                        val b = Color.blue(p).toFloat()
                        val lum = 0.299f * r + 0.587f * g + 0.114f * b
                        
                        val chosenColor = if (lum < avgLum) inkColor else paperColor
                        val a = Color.alpha(p)
                        pixels[i] = Color.argb(a, chosenColor[0].toInt(), chosenColor[1].toInt(), chosenColor[2].toInt())
                    }
                }
            }
        }
        
        output.setPixels(pixels, 0, w, 0, 0, w, h)
        output
    }
    
    private fun getClosestPaletteColor(r: Float, g: Float, b: Float): IntArray {
        var minDist = Float.MAX_VALUE
        var closest = spectrumPalette[0]
        
        for (p in spectrumPalette) {
            val dist = (r - p[0]).pow(2) + (g - p[1]).pow(2) + (b - p[2]).pow(2)
            if (dist < minDist) {
                minDist = dist
                closest = p
            }
        }
        return closest
    }
    
    private fun fillBlockWithColor(d: IntArray, w: Int, h: Int, bx: Int, by: Int, bSize: Int, color: IntArray) {
        for (py in 0 until bSize) {
            if (by + py >= h) continue
            for (px in 0 until bSize) {
                if (bx + px >= w) continue
                val i = (by + py) * w + (bx + px)
                val a = Color.alpha(d[i])
                d[i] = Color.argb(a, color[0].toInt(), color[1].toInt(), color[2].toInt())
            }
        }
    }
}
