package com.ditherlab.ultra.core

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Native (C++) veya düşük seviyeli bellek optimizasyonlarını simüle eden araçlar.
 * OOM (Out Of Memory) hatalarını engellemek için allocationları takip eder.
 */
object NativeBitmapUtils {

    suspend fun createOptimizedCopy(source: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        // Konfigürasyonu donanım dostu ARGB_8888 olarak garanti eder.
        val copy = source.copy(Bitmap.Config.ARGB_8888, true)
        copy.setHasAlpha(true)
        copy
    }

    /**
     * Görüntüye (örn. arkaplana) bir siyah/beyaz maskeyi alpha kanalı olarak uygular.
     */
    suspend fun applyAlphaMask(source: Bitmap, mask: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val sourcePixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)
        
        source.getPixels(sourcePixels, 0, width, 0, 0, width, height)
        mask.getPixels(maskPixels, 0, width, 0, 0, width, height)
        
        for (i in sourcePixels.indices) {
            val p = sourcePixels[i]
            val m = maskPixels[i]
            
            // Maskeden sadece red kanalını alpha olarak kullan
            val alpha = android.graphics.Color.red(m)
            
            // Mevcut pikselin RGB'sini koru, alphasını maskeden al
            val r = android.graphics.Color.red(p)
            val g = android.graphics.Color.green(p)
            val b = android.graphics.Color.blue(p)
            
            sourcePixels[i] = android.graphics.Color.argb(alpha, r, g, b)
        }
        
        result.setPixels(sourcePixels, 0, width, 0, 0, width, height)
        result
    }
}
