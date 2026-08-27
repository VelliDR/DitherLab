package com.ditherlab.ultra.data.model

import com.ditherlab.ultra.core.LayerCompositor.BlendMode

/**
 * Birden fazla efekti sırayla üst üste bindirmek için 
 * render geçişlerini (pass) tutan veri modeli.
 */
data class LayerPass(
    val id: String,
    val name: String,
    val isEnabled: Boolean = true,
    val blendMode: BlendMode = BlendMode.NORMAL,
    val opacity: Float = 1.0f,
    val engineType: EngineType,
    val targetTarget: TargetLayer = TargetLayer.FULL_SCREEN
) {
    enum class TargetLayer {
        FULL_SCREEN,
        SUBJECT_ONLY,
        BACKGROUND_ONLY
    }

    enum class EngineType {
        DITHER_ERROR_DIFFUSION,
        DITHER_BAYER,
        DITHER_HALFTONE,
        IMPASTO_3D,
        ASCII_MATRIX,
        GLITCH_CRT,
        GLITCH_CHROMATIC,
        GLITCH_PIXEL_SORT,
        GLITCH_SPIDER_VERSE,
        GLITCH_KATANA_VHS,
        PIXEL_DEAD_CELLS,
        TEMPORAL_AFTERIMAGE
    }
}
