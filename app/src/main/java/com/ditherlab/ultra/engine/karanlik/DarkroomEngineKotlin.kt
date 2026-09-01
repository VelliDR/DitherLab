package com.ditherlab.ultra.engine.karanlik

import android.graphics.*
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DarkroomEngineKotlin : VisualEngine {
    override val engineName: String = "Darkroom"

    private fun createMatrix(saturation: Float, contrast: Float, brightness: Float): ColorMatrix {
        val cScale = contrast
        val cTranslate = (-0.5f * cScale + 0.5f) * 255f
        val contrastMatrix = ColorMatrix(floatArrayOf(
            cScale, 0f, 0f, 0f, cTranslate,
            0f, cScale, 0f, 0f, cTranslate,
            0f, 0f, cScale, 0f, cTranslate,
            0f, 0f, 0f, 1f, 0f
        ))
        
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(saturation)
        
        val bScale = brightness
        val brightMatrix = ColorMatrix(floatArrayOf(
            bScale, 0f, 0f, 0f, 0f,
            0f, bScale, 0f, 0f, 0f,
            0f, 0f, bScale, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        
        val result = ColorMatrix(contrastMatrix)
        result.postConcat(satMatrix)
        result.postConcat(brightMatrix)
        return result
    }

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        val w = input.width
        val h = input.height

        val resultLayer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultLayer)

        // Hardcoding the presets from PRESETS js object
        val presets = listOf(
            Preset("sb", createMatrix(0f, 1.65f, 0.92f), null, 0f, PorterDuff.Mode.SRC_OVER),
            Preset("analog", createMatrix(0.85f, 1.02f, 0.98f), 0xfff5e6cc.toInt(), 0.12f, PorterDuff.Mode.MULTIPLY),
            Preset("reze", createMatrix(1.08f, 0.95f, 1.02f), 0xffffebd8.toInt(), 0.22f, PorterDuff.Mode.SCREEN),
            Preset("vampir", createMatrix(2.20f, 1.50f, 0.85f), 0xffff1100.toInt(), 0.25f, PorterDuff.Mode.MULTIPLY), // fallback to MULTIPLY for COLOR_BURN
            Preset("gotik", createMatrix(0.25f, 1.35f, 0.80f), 0xff05121a.toInt(), 0.30f, PorterDuff.Mode.MULTIPLY),
            Preset("nordic", createMatrix(0.88f, 0.92f, 1.02f), 0xff002244.toInt(), 0.14f, PorterDuff.Mode.OVERLAY), // fallback to OVERLAY for SOFT_LIGHT
            Preset("cinestill", createMatrix(1.15f, 1.08f, 0.95f), 0xff003366.toInt(), 0.10f, PorterDuff.Mode.MULTIPLY)
        )
        
        val presetKey = config.darkroomPreset.ifEmpty { "sb" }
        val preset = presets.find { it.name == presetKey } ?: presets[0]

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(preset.colorMatrix)
        }
        canvas.drawBitmap(input, 0f, 0f, paint)

        preset.overlayColor?.let { colorVal ->
            if (preset.overlayOpacity > 0f) {
                val overlayPaint = Paint().apply {
                    color = colorVal
                    alpha = (preset.overlayOpacity * 255).toInt()
                    xfermode = PorterDuffXfermode(preset.blendMode)
                }
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), overlayPaint)
            }
        }

        resultLayer
    }

    data class Preset(
        val name: String,
        val colorMatrix: ColorMatrix,
        val overlayColor: Int?,
        val overlayOpacity: Float,
        val blendMode: PorterDuff.Mode
    )
}
