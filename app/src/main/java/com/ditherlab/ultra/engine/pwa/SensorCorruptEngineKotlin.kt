package com.ditherlab.ultra.engine.pwa

import android.graphics.Bitmap
import android.graphics.Color
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class SensorCorruptEngineKotlin : VisualEngine {
    override val engineName: String = "SensorCorrupt"

    private fun getPRNG(seed: Long): () -> Double {
        var s = seed.toUInt()
        return {
            s = 1664525u * s + 1013904223u
            s.toDouble() / 4294967296.0
        }
    }

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        val w = input.width
        val h = input.height
        val maxDim = max(w, h)
        val scaleFactor = maxDim / 1200f

        val outBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val srcPixels = IntArray(w * h)
        input.getPixels(srcPixels, 0, w, 0, 0, w, h)

        val mode = config.sensorCorruptMode // or "standard"
        val noiseIntensity = config.sensorNoiseIntensity
        val chaosLevel = config.sensorChaosLevel
        val seed = 12345L // keep fixed for stability unless specified
        val lineJitter = config.sensorLineJitter
        val bitShift = config.sensorBitShift

        val random = getPRNG(seed)

        if (mode == "chaos") {
            val numBlocks = floor(8 * chaosLevel).toInt() + 1
            val blockBuffer = IntArray(w * floor(26 * scaleFactor).toInt() * 4) // Max size approximation

            for (b in 0 until numBlocks) {
                val blockH = max(1, floor((random() * 20 + 6) * scaleFactor).toInt())
                val srcY = floor(random() * max(1, h - blockH)).toInt()
                val destY = floor(random() * max(1, h - blockH)).toInt()
                val shiftX = floor((random() - 0.5) * w * 0.3 * chaosLevel).toInt()

                val srcStart = srcY * w

                for (y in 0 until blockH) {
                    val destYPos = destY + y
                    if (destYPos < 0 || destYPos >= h) continue

                    for (x in 0 until w) {
                        val srcIdx = srcY * w + y * w + x
                        val destX = x + shiftX
                        if (destX < 0 || destX >= w) continue

                        val destIdx = destYPos * w + destX
                        
                        // Bounds check
                        if(srcIdx < srcPixels.size && destIdx < srcPixels.size) {
                            val srcCol = srcPixels[srcIdx]
                            val destCol = srcPixels[destIdx]
                            
                            val srcR = (srcCol shr 16) and 0xFF
                            val srcG = (srcCol shr 8) and 0xFF
                            val srcB = srcCol and 0xFF
                            
                            val destR = (destCol shr 16) and 0xFF
                            val destG = (destCol shr 8) and 0xFF
                            val destB = destCol and 0xFF
                            
                            val newR = (srcG * 0.7 + destR * 0.3).toInt()
                            val newG = (srcR * 0.7 + destG * 0.3).toInt()
                            val newB = srcB
                            
                            srcPixels[destIdx] = Color.argb(255, newR, newG, newB)
                        }
                    }
                }
            }

            var chaosSeed = (random() * 0xFFFFFF).toInt()
            val noiseThreshold = noiseIntensity * 0.8f

            for (i in srcPixels.indices) {
                chaosSeed = (chaosSeed * 16807 + 31) and 0xFFFFFF
                val seedByte = chaosSeed and 0xFF

                if (random() < noiseThreshold) {
                    val selector = (seedByte + i) % 3
                    val col = srcPixels[i]
                    val r = (col shr 16) and 0xFF
                    val g = (col shr 8) and 0xFF
                    val b = col and 0xFF

                    val newCol = if (selector == 0) {
                        Color.argb(255, g, b, r)
                    } else if (selector == 1) {
                        Color.argb(255, (r xor (seedByte and 0x3F)) and 0xFF, min(255, (g * (1.0 + chaosLevel)).toInt()), b)
                    } else {
                        Color.argb(255, (r shr 2) shl 2, (g shr 2) shl 2, (b shr 2) shl 2)
                    }
                    srcPixels[i] = newCol
                }
            }

        } else {
            // Standard mode
            if (lineJitter) {
                for (y in 0 until h) {
                    if (random() < (0.04 + noiseIntensity * 0.25)) {
                        val shiftX = floor((random() - 0.5) * (8 + noiseIntensity * 35) * scaleFactor).toInt()
                        val rowStart = y * w
                        if (shiftX > 0) {
                            System.arraycopy(srcPixels, rowStart, srcPixels, rowStart + shiftX, w - shiftX)
                        } else if (shiftX < 0) {
                            System.arraycopy(srcPixels, rowStart - shiftX, srcPixels, rowStart, w + shiftX)
                        }
                    }
                }
            }

            val noiseAmplitude = 380 * noiseIntensity * scaleFactor
            val thresholdOverdrive = min(1.0, noiseIntensity * 0.9).toFloat()
            val thresholdBitShift = noiseIntensity * 0.5f

            for (i in srcPixels.indices) {
                val origCol = srcPixels[i]
                val origR = (origCol shr 16) and 0xFF
                val origG = (origCol shr 8) and 0xFF
                val origB = origCol and 0xFF
                val origLum = 0.299 * origR + 0.587 * origG + 0.114 * origB

                var r = origR.toDouble()
                var g = origG.toDouble()
                var b = origB.toDouble()

                if (random() < thresholdOverdrive) {
                    r += (random() - 0.5) * noiseAmplitude
                    g += (random() - 0.5) * noiseAmplitude
                    b += (random() - 0.5) * noiseAmplitude
                }

                if (bitShift && origLum >= 50 && random() < thresholdBitShift) {
                    var ri = r.toInt()
                    var gi = g.toInt()
                    var bi = b.toInt()
                    if (gi > 80) {
                        ri = (ri xor 0x44) and 0xFF
                        gi = min(255, (gi * 1.4).toInt())
                    }
                    if (ri > 100) {
                        bi = (bi or 0xAA) and 0xFF
                    }
                    r = ri.toDouble()
                    g = gi.toDouble()
                    b = bi.toDouble()
                }

                r = max(0.0, r)
                g = max(0.0, g)
                b = max(0.0, b)

                val newLum = 0.299 * r + 0.587 * g + 0.114 * b
                if (newLum > 1) {
                    val targetLum = origLum * 0.75 + newLum * 0.25
                    val lumRatio = targetLum / newLum
                    r = min(255.0, r * lumRatio)
                    g = min(255.0, g * lumRatio)
                    b = min(255.0, b * lumRatio)
                }

                srcPixels[i] = Color.argb(255, r.toInt(), g.toInt(), b.toInt())
            }
        }

        outBitmap.setPixels(srcPixels, 0, w, 0, 0, w, h)
        outBitmap
    }
}
