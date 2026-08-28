package com.ditherlab.ultra.engine.pwa

import android.graphics.*
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

class MinecraftEngineKotlin : VisualEngine {
    override val engineName: String = "Minecraft"

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        val w = input.width
        val h = input.height
        val scale = max(w, h) / 1200f
        
        val intensity = config.minecraftBlockSize
        val blockSize = max(4, (intensity * scale).toInt())

        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val paint = Paint()
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.argb(40, 0, 0, 0)
            strokeWidth = 1f
        }
        
        val pixels = IntArray(w * h)
        input.getPixels(pixels, 0, w, 0, 0, w, h)

        for (y in 0 until h step blockSize) {
            for (x in 0 until w step blockSize) {
                // Sample center of block
                val srcX = (x + blockSize / 2).coerceIn(0, w - 1)
                val srcY = (y + blockSize / 2).coerceIn(0, h - 1)
                val color = pixels[srcY * w + srcX]
                
                // Add some noise to the color to make it look like a texture
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)
                
                paint.color = color
                canvas.drawRect(x.toFloat(), y.toFloat(), (x + blockSize).toFloat(), (y + blockSize).toFloat(), paint)
                
                // Inner highlight for a 3D block feel
                val highlight = Paint().apply {
                    this.color = Color.argb(40, 255, 255, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                val shadow = Paint().apply {
                    this.color = Color.argb(40, 0, 0, 0)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                
                // Top-Left highlight
                canvas.drawLine(x.toFloat(), y.toFloat(), (x + blockSize).toFloat(), y.toFloat(), highlight)
                canvas.drawLine(x.toFloat(), y.toFloat(), x.toFloat(), (y + blockSize).toFloat(), highlight)
                
                // Bottom-Right shadow
                canvas.drawLine((x + blockSize).toFloat(), (y + blockSize).toFloat(), x.toFloat(), (y + blockSize).toFloat(), shadow)
                canvas.drawLine((x + blockSize).toFloat(), (y + blockSize).toFloat(), (x + blockSize).toFloat(), y.toFloat(), shadow)
                
                // Border
                canvas.drawRect(x.toFloat(), y.toFloat(), (x + blockSize).toFloat(), (y + blockSize).toFloat(), borderPaint)
            }
        }
        
        output
    }
}
