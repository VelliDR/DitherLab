package com.ditherlab.ultra.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Farklı işlem geçişlerini (dither, impasto, ascii) donanım hızlandırmalı
 * Canvas operasyonları veya özel blending modlarıyla birleştiren (composite) modül.
 */
object LayerCompositor {

    enum class BlendMode {
        NORMAL,
        MULTIPLY,
        SCREEN,
        OVERLAY
    }

    /**
     * İki bitmap'i belirtilen Harmanlama Modu (Blend Mode) ile üst üste bindirir.
     */
    suspend fun composite(
        base: Bitmap,
        overlay: Bitmap,
        blendMode: BlendMode = BlendMode.NORMAL,
        alpha: Float = 1.0f
    ): Bitmap = withContext(Dispatchers.Default) {
        val result = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Alt katmanı çiz
        canvas.drawBitmap(base, 0f, 0f, null)

        // Üst katman için Paint ayarla
        val paint = Paint().apply {
            this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
            
            this.xfermode = when (blendMode) {
                BlendMode.NORMAL -> PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
                BlendMode.MULTIPLY -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                BlendMode.SCREEN -> PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                BlendMode.OVERLAY -> PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            }
        }

        // Üst katmanı çiz
        canvas.drawBitmap(overlay, 0f, 0f, paint)

        result
    }

    /**
     * A/B Perde (Split View) için iki katmanı dikey olarak böler.
     * [splitPosition] 0.0 ile 1.0 arasındadır.
     */
    suspend fun compositeSplitView(
        layerA: Bitmap,
        layerB: Bitmap,
        splitPosition: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        val result = Bitmap.createBitmap(layerA.width, layerA.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val splitX = (layerA.width * splitPosition).toInt().coerceIn(0, layerA.width)

        // Sol taraf: layerA
        val srcRectA = android.graphics.Rect(0, 0, splitX, layerA.height)
        val dstRectA = android.graphics.Rect(0, 0, splitX, layerA.height)
        canvas.drawBitmap(layerA, srcRectA, dstRectA, null)

        // Sağ taraf: layerB
        val srcRectB = android.graphics.Rect(splitX, 0, layerB.width, layerB.height)
        val dstRectB = android.graphics.Rect(splitX, 0, layerB.width, layerB.height)
        canvas.drawBitmap(layerB, srcRectB, dstRectB, null)

        result
    }
}
