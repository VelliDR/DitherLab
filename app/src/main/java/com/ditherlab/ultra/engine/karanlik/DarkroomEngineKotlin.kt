package com.ditherlab.ultra.engine.karanlik

import android.graphics.*
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

class DarkroomEngineKotlin : VisualEngine {
    override val engineName: String = "Darkroom"

    private fun createColorFilter(saturation: Float, contrast: Float, brightness: Float, hueShiftDeg: Float = 0f): ColorMatrixColorFilter {
        val cm = ColorMatrix()
        
        // 1. Saturation
        cm.setSaturation(saturation)

        // 2. Contrast
        if (contrast != 1f) {
            val scale = contrast
            val translate = (-0.5f * scale + 0.5f) * 255f
            val contrastMatrix = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(contrastMatrix)
        }

        // 3. Brightness
        if (brightness != 1f) {
            val brightMatrix = ColorMatrix(floatArrayOf(
                brightness, 0f, 0f, 0f, 0f,
                0f, brightness, 0f, 0f, 0f,
                0f, 0f, brightness, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(brightMatrix)
        }

        // 4. Hue shift if applicable
        if (hueShiftDeg != 0f) {
            val rad = Math.toRadians(hueShiftDeg.toDouble())
            val cos = Math.cos(rad).toFloat()
            val sin = Math.sin(rad).toFloat()
            val lumR = 0.213f
            val lumG = 0.715f
            val lumB = 0.072f
            val hueMatrix = ColorMatrix(floatArrayOf(
                lumR + cos * (1f - lumR) + sin * (-lumR), lumG + cos * (-lumG) + sin * (-lumG), lumB + cos * (-lumB) + sin * (1f - lumB), 0f, 0f,
                lumR + cos * (-lumR) + sin * (0.143f), lumG + cos * (1f - lumG) + sin * (0.140f), lumB + cos * (-lumB) + sin * (-0.283f), 0f, 0f,
                lumR + cos * (-lumR) + sin * (-(1f - lumR)), lumG + cos * (-lumG) + sin * (lumG), lumB + cos * (1f - lumB) + sin * (lumB), 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(hueMatrix)
        }

        return ColorMatrixColorFilter(cm)
    }

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        val intensity = config.darkroomIntensity.coerceIn(0f, 1f)
        if (intensity <= 0f) return@withContext input

        val w = input.width
        val h = input.height

        val presets = listOf(
            Preset("sb", saturation = 0f, contrast = 1.65f, brightness = 0.92f, overlayColor = null, overlayOpacity = 0f, blendMode = BlendMode.SRC_OVER, noise = 0.16f),
            Preset("analog", saturation = 0.85f, contrast = 1.02f, brightness = 0.98f, overlayColor = 0xfff5e6cc.toInt(), overlayOpacity = 0.12f, blendMode = BlendMode.MULTIPLY, noise = 0.08f),
            Preset("reze", saturation = 1.08f, contrast = 0.95f, brightness = 1.02f, overlayColor = 0xffffebd8.toInt(), overlayOpacity = 0.22f, blendMode = BlendMode.SCREEN, noise = 0.04f),
            Preset("vampir", saturation = 2.20f, contrast = 1.50f, brightness = 0.85f, overlayColor = 0xffff1100.toInt(), overlayOpacity = 0.25f, blendMode = BlendMode.COLOR_BURN, noise = 0.12f),
            Preset("gotik", saturation = 0.25f, contrast = 1.35f, brightness = 0.80f, overlayColor = 0xff05121a.toInt(), overlayOpacity = 0.30f, blendMode = BlendMode.MULTIPLY, noise = 0.08f),
            Preset("nordic", saturation = 0.88f, contrast = 0.92f, brightness = 1.02f, overlayColor = 0xff002244.toInt(), overlayOpacity = 0.14f, blendMode = BlendMode.SOFT_LIGHT, noise = 0.05f),
            Preset("cinestill", saturation = 1.15f, contrast = 1.08f, brightness = 0.95f, hueShift = -8f, overlayColor = 0xff003366.toInt(), overlayOpacity = 0.10f, blendMode = BlendMode.COLOR_BURN, noise = 0.14f)
        )

        val presetKey = config.darkroomPreset.ifEmpty { "sb" }
        val preset = presets.find { it.name == presetKey } ?: presets[0]

        // 1. Render processed preset layer
        val processed = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(processed)

        val paint = Paint().apply {
            colorFilter = createColorFilter(preset.saturation, preset.contrast, preset.brightness, preset.hueShift)
        }
        canvas.drawBitmap(input, 0f, 0f, paint)

        // 2. Blend Overlay Color with native Android BlendMode
        preset.overlayColor?.let { colorVal ->
            if (preset.overlayOpacity > 0f) {
                val overlayPaint = Paint().apply {
                    color = colorVal
                    alpha = (preset.overlayOpacity * 255).toInt()
                    blendMode = preset.blendMode
                }
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), overlayPaint)
            }
        }

        // 3. Apply Film Grain Noise if needed
        val noiseAmount = preset.noise * intensity
        if (noiseAmount > 0.01f) {
            val pixels = IntArray(w * h)
            processed.getPixels(pixels, 0, w, 0, 0, w, h)
            val noiseFactor = (noiseAmount * 60).toInt()
            val rng = Random(42)
            
            for (i in pixels.indices) {
                val p = pixels[i]
                val a = (p shr 24) and 0xff
                if (a == 0) continue
                var r = (p shr 16) and 0xff
                var g = (p shr 8) and 0xff
                var b = p and 0xff

                val noise = rng.nextInt(-noiseFactor, noiseFactor)
                r = (r + noise).coerceIn(0, 255)
                g = (g + noise).coerceIn(0, 255)
                b = (b + noise).coerceIn(0, 255)

                pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
            processed.setPixels(pixels, 0, w, 0, 0, w, h)
        }

        // 4. Intensity Blending (Input vs Processed)
        if (intensity >= 0.99f) {
            processed
        } else {
            val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val resCanvas = Canvas(result)
            resCanvas.drawBitmap(input, 0f, 0f, null)
            val blendPaint = Paint().apply {
                alpha = (intensity * 255).toInt()
            }
            resCanvas.drawBitmap(processed, 0f, 0f, blendPaint)
            processed.recycle()
            result
        }
    }

    data class Preset(
        val name: String,
        val saturation: Float,
        val contrast: Float,
        val brightness: Float,
        val hueShift: Float = 0f,
        val overlayColor: Int?,
        val overlayOpacity: Float,
        val blendMode: BlendMode,
        val noise: Float
    )
}
