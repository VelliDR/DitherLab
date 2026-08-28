package com.ditherlab.ultra.engine.pwa

import android.graphics.*
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

class PostcardEngineKotlin : VisualEngine {
    override val engineName: String = "Postcard"

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        val w = input.width
        val h = input.height
        val scale = max(w, h) / 1200f
        
        val margin = (config.postcardStampMargin * scale).toInt()
        val totalW = w + margin * 2
        val totalH = h + margin * 2

        val output = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        // Background (off-white paper or bank note paper)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (config.postcardMode == "engraving") {
            paint.color = Color.parseColor("#E8EBE9") // Slightly greenish/bluish for banknote
        } else {
            paint.color = Color.parseColor("#F4F1EA") // Warm paper
        }
        canvas.drawRect(0f, 0f, totalW.toFloat(), totalH.toFloat(), paint)

        // Draw image slightly faded
        val colorMatrix = ColorMatrix()
        if (config.postcardMode == "engraving") {
            colorMatrix.setSaturation(0.2f) // More desaturated for engraving
            // Greenish tint
            val tintMatrix = ColorMatrix(floatArrayOf(
                0.8f, 0f, 0f, 0f, -10f,
                0f, 1.1f, 0f, 0f, 15f,
                0f, 0f, 0.9f, 0f, 5f,
                0f, 0f, 0f, 1f, 0f
            ))
            colorMatrix.postConcat(tintMatrix)
        } else {
            colorMatrix.setSaturation(0.7f) // Desaturate slightly
            // Warm tint
            val tintMatrix = ColorMatrix(floatArrayOf(
                1.1f, 0f, 0f, 0f, 10f,
                0f, 1.0f, 0f, 0f, 5f,
                0f, 0f, 0.9f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            ))
            colorMatrix.postConcat(tintMatrix)
        }
        
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(input, margin.toFloat(), margin.toFloat(), paint)
        paint.colorFilter = null
        
        // Draw Stamp in top right corner
        val stampW = 80f * scale
        val stampH = 100f * scale
        val stampX = totalW - margin - stampW - 20f * scale
        val stampY = margin + 20f * scale
        
        // Stamp border
        paint.color = Color.WHITE
        paint.setShadowLayer(5f * scale, 2f * scale, 2f * scale, Color.argb(100, 0, 0, 0))
        canvas.drawRect(stampX, stampY, stampX + stampW, stampY + stampH, paint)
        paint.clearShadowLayer()
        
        // Stamp inner image (just a colored rect for now)
        paint.color = Color.parseColor("#446688")
        canvas.drawRect(stampX + 10f * scale, stampY + 10f * scale, stampX + stampW - 10f * scale, stampY + stampH - 10f * scale, paint)
        
        // Postmark (Squiggly lines)
        paint.style = Paint.Style.STROKE
        paint.color = Color.argb(150, 20, 20, 20)
        paint.strokeWidth = 3f * scale
        
        // Draw some wavy lines across the stamp
        val path = Path()
        val startY1 = stampY + stampH * 0.4f
        val startY2 = stampY + stampH * 0.6f
        val startY3 = stampY + stampH * 0.8f
        
        for (i in 0..4) {
            val yOffset = startY1 + (i - 2) * 12f * scale
            path.moveTo(stampX - 50f * scale, yOffset)
            path.quadTo(stampX + stampW * 0.3f, yOffset - 20f * scale, stampX + stampW * 1.2f, yOffset)
        }
        canvas.drawPath(path, paint)
        
        // Postmark Circle
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(stampX - 40f * scale, stampY + 30f * scale, 45f * scale, paint)
        canvas.drawCircle(stampX - 40f * scale, stampY + 30f * scale, 40f * scale, paint)
        
        output
    }
}
