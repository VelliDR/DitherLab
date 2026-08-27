package com.ditherlab.ultra.data.model

import android.graphics.Bitmap

sealed interface StudioUiState {
    data object Loading : StudioUiState
    
    data class Active(
        val originalImage: Bitmap? = null,
        val processedImage: Bitmap? = null,
        val currentConfig: EffectConfig = EffectConfig(),
        val availablePalettes: List<ColorPalette> = emptyList(),
        val isA_BSplitActive: Boolean = false,
        val splitPosition: Float = 0.5f, // 0.0 to 1.0
        val isExporting: Boolean = false,
        val selectedEngineIndex: Int = 0,
        val subjectMaskBitmap: Bitmap? = null
    ) : StudioUiState
    
    data class Error(val message: String) : StudioUiState
}
