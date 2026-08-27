package com.ditherlab.ultra.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Kullanıcının Yapay Işıklandırma (Global Relighting) için ışık rengini
 * seçeceği serbest renk tekeri (Color Wheel) mantığı.
 * 48 Ana renkten (Hue) ve onların farklı doygunluk (Neon, Pastel, Mat) 
 * varyasyonlarından oluşur.
 */
object LightColorWheel {

    /**
     * Toplam 48 renk oluşturur. (16 Renk Açısı x 3 Varyasyon)
     * Kullanıcı arayüzünde koyu mat ve pastel tonları aynı anda görebilmek için
     * doygunluk (Saturation) ve parlaklık (Value) eş zamanlı hesaplanır.
     */
    fun generateRelightingPalette(): List<Color> {
        val colors = mutableListOf<Color>()
        val hueSteps = 16 // 16 farklı renk tonu açısı (Kırmızıdan Mora 360 derece)
        
        // 1. Neon/Parlak (Sat: 1.0, Val: 1.0)
        // 2. Pastel (Sat: 0.5, Val: 0.9)
        // 3. Koyu/Mat (Sat: 0.8, Val: 0.4)
        
        val variations = listOf(
            Pair(1.0f, 1.0f),  // Neon
            Pair(0.5f, 0.9f),  // Pastel
            Pair(0.8f, 0.4f)   // Mat/Koyu
        )
        
        for (variation in variations) {
            val (sat, value) = variation
            for (i in 0 until hueSteps) {
                val hue = (i.toFloat() / hueSteps.toFloat()) * 360f
                colors.add(Color.hsv(hue, sat, value))
            }
        }
        
        // 16 * 3 = Tam olarak 48 Renk Dondürür
        return colors
    }
}
