package com.ditherlab.ultra.data.model

import androidx.compose.ui.graphics.Color

data class ColorPalette(
    val id: String,
    val name: String,
    val colors: List<Color>
) {
    init {
        require(colors.isNotEmpty()) { "Bir palet en az bir renge sahip olmalıdır." }
    }
}
