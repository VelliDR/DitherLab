package com.ditherlab.ultra.engine.base

import android.graphics.Bitmap
import com.ditherlab.ultra.data.model.EffectConfig

/**
 * Represents a dynamic fine-tuning parameter for a specific engine.
 */
data class EngineParameter(
    val key: String,
    val name: String,
    val minValue: Float,
    val maxValue: Float,
    val defaultValue: Float,
    val step: Float = 0.01f
)

/**
 * Tüm efekt motorlarının (Glitch, Dither, Impasto vb.) türeyeceği ortak Base (Temel) arayüz.
 * LayerPass içerisinde polimorfik olarak her motoru sırayla çalıştırabilmemizi sağlar.
 */
interface VisualEngine {
    
    val engineName: String
    
    /**
     * Motorun desteklediği ince ayar parametrelerini döner.
     */
    fun getSupportedParameters(): List<EngineParameter> = emptyList()
    
    /**
     * Motorun çalışıp çalışmadığını asenkron olarak döner.
     */
    suspend fun process(input: Bitmap, config: EffectConfig): Bitmap
}
