package com.ditherlab.ultra.data.repository

import androidx.compose.ui.graphics.Color
import com.ditherlab.ultra.data.model.ColorPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * .HEX ve JASC-PAL dosyalarını ayrıştırıp [ColorPalette] listesi döndüren repository.
 */
class PaletteRepository {

    /**
     * Satır başına bir hex kodun yer aldığı basit .HEX formatını ayrıştırır.
     */
    suspend fun parseHexPalette(id: String, name: String, hexContent: String): ColorPalette =
        withContext(Dispatchers.Default) {
            val colors = hexContent.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith(";") && !it.startsWith("//") }
                .map { line ->
                    val hexStr = if (line.startsWith("#")) line else "#$line"
                    // Parse as 32-bit color, assuming #RRGGBB or #AARRGGBB
                    Color(android.graphics.Color.parseColor(hexStr))
                }
            
            ColorPalette(id, name, colors)
        }

    /**
     * Standart JASC-PAL (Paint Shop Pro) palet dosyasını ayrıştırır.
     */
    suspend fun parseJascPal(id: String, name: String, jascContent: String): ColorPalette =
        withContext(Dispatchers.Default) {
            val lines = jascContent.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size < 3 || lines[0] != "JASC-PAL") {
                throw IllegalArgumentException("Geçersiz JASC-PAL formatı")
            }

            // lines[1] versiyon (örn. "0100")
            // lines[2] renk sayısı
            val colorCount = lines[2].toIntOrNull() 
                ?: throw IllegalArgumentException("JASC-PAL dosyasında renk sayısı okunamadı")

            val colors = mutableListOf<Color>()
            val startIndex = 3
            val endIndex = minOf(startIndex + colorCount, lines.size)

            for (i in startIndex until endIndex) {
                val components = lines[i].split(Regex("\\s+"))
                if (components.size >= 3) {
                    val r = components[0].toIntOrNull()?.coerceIn(0, 255) ?: 0
                    val g = components[1].toIntOrNull()?.coerceIn(0, 255) ?: 0
                    val b = components[2].toIntOrNull()?.coerceIn(0, 255) ?: 0
                    colors.add(Color(red = r, green = g, blue = b, alpha = 255))
                }
            }

            ColorPalette(id, name, colors)
        }
        
    /**
     * Uygulama içi önceden tanımlanmış (default) paletleri döner.
     */
    fun getDefaultPalettes(): List<ColorPalette> {
        return listOf(
            ColorPalette(
                id = "gameboy",
                name = "GameBoy Classic",
                colors = listOf(
                    Color(0xFF0F380F),
                    Color(0xFF306230),
                    Color(0xFF8BAC0F),
                    Color(0xFF9BBC0F)
                )
            ),
            ColorPalette(
                id = "bw",
                name = "Black & White",
                colors = listOf(
                    Color.Black,
                    Color.White
                )
            ),
            ColorPalette(
                id = "cga",
                name = "CGA Palette 1",
                colors = listOf(
                    Color(0xFF000000), // Black
                    Color(0xFF00AAAA), // Cyan
                    Color(0xFFAA00AA), // Magenta
                    Color(0xFFAAAAAA)  // Light Gray
                )
            )
        )
    }
}
