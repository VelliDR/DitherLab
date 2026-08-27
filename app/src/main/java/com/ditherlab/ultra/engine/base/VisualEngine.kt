package com.ditherlab.ultra.engine.base

import android.graphics.Bitmap
import com.ditherlab.ultra.data.model.EffectConfig

/**
 * Tüm efekt motorlarının (Glitch, Dither, Impasto vb.) türeyeceği ortak Base (Temel) arayüz.
 * LayerPass içerisinde polimorfik olarak her motoru sırayla çalıştırabilmemizi sağlar.
 */
interface VisualEngine {
    
    val engineName: String
    
    /**
     * Motorun çalışıp çalışmadığını asenkron olarak döner.
     */
    suspend fun process(input: Bitmap, config: EffectConfig): Bitmap
}
