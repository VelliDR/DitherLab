package com.ditherlab.ultra.engine.pwa

import android.graphics.*
import androidx.core.graphics.ColorUtils
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.data.model.PointF
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.*
import kotlin.random.Random

class VanGoghEngineKotlin : VisualEngine {
    override val engineName: String = "Van Gogh"

    // Noise permutation table for streamline turbulence
    private val p = IntArray(512)
    init {
        val perm = intArrayOf(
            151,160,137,91,90,15,131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,
            8,99,37,240,21,10,23,190,6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,
            35,11,32,57,177,33,88,237,149,56,87,174,20,125,136,171,168,68,175,74,165,71,
            134,139,48,27,166,77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,
            55,46,245,40,244,102,143,54,65,25,63,161,1,216,80,73,209,76,132,187,208,89,
            18,169,200,196,135,130,116,188,159,86,164,100,109,198,173,186,3,64,52,217,226,
            250,124,123,5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,
            189,28,42,223,183,170,213,119,248,152,2,44,154,163,70,221,153,101,155,167,43,
            172,9,129,22,39,253,19,98,108,110,79,113,224,232,178,185,112,104,218,246,97,
            228,251,34,242,193,238,210,144,12,191,179,162,241,81,51,145,235,249,14,239,
            107,49,192,214,31,181,199,106,157,184,84,204,176,115,121,50,45,127,4,150,254,
            138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180
        )
        for (i in 0 until 256) {
            p[i] = perm[i]
            p[256 + i] = perm[i]
        }
    }

    private fun fade(t: Double): Double = t * t * t * (t * (t * 6 - 15) + 10)
    private fun lerp(t: Double, a: Double, b: Double): Double = a + t * (b - a)
    private fun grad(hash: Int, x: Double, y: Double): Double {
        val h = hash and 15
        val u = if (h < 8) x else y
        val v = if (h < 4) y else if (h == 12 || h == 14) x else 0.0
        return (if (h and 1 == 0) u else -u) + (if (h and 2 == 0) v else -v)
    }

    private fun noise2D(x: Double, y: Double): Float {
        var xi = floor(x).toInt() and 255
        var yi = floor(y).toInt() and 255
        val xf = x - floor(x)
        val yf = y - floor(y)
        val u = fade(xf)
        val v = fade(yf)
        val aa = p[p[xi] + yi]
        val ab = p[p[xi] + yi + 1]
        val ba = p[p[xi + 1] + yi]
        val bb = p[p[xi + 1] + yi + 1]
        val x1 = lerp(u, grad(aa, xf, yf), grad(ba, xf - 1, yf))
        val x2 = lerp(u, grad(ab, xf, yf - 1), grad(bb, xf - 1, yf - 1))
        return lerp(v, x1, x2).toFloat()
    }

    private fun rgbToHsl(r: Int, g: Int, b: Int): FloatArray {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        var h = 0f
        var s = 0f
        val l = (max + min) / 2f

        if (max != min) {
            val d = max - min
            s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
            h = when (max) {
                rf -> (gf - bf) / d + (if (gf < bf) 6f else 0f)
                gf -> (bf - rf) / d + 2f
                bf -> (rf - gf) / d + 4f
                else -> 0f
            }
            h /= 6f
        }
        return floatArrayOf(h * 360f, s, l)
    }

    private data class StreamlineConfig(
        val density: Float,
        val stepLength: Float,
        val maxSteps: Int,
        val brushWidth: Float,
        val bristleCount: Int,
        val alpha: Float,
        val turbulence: Float,
        val impasto: Boolean,
        val highlightOnly: Boolean = false,
        val vibration: Boolean = true
    )

    private class TensorField(
        val angles: FloatArray,
        val coherence: FloatArray,
        val sw: Int,
        val sh: Int
    )

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        val w = input.width
        val h = input.height
        val maxDim = max(w, h)
        val scale = maxDim / 1200f

        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val targetCtx = Canvas(output)
        
        // 1. Astar Katmanı
        // Simulate blur + saturate + contrast by drawing it scaled down and back up
        val blurW = max(1, (w / (12f * scale)).toInt())
        val blurH = max(1, (h / (12f * scale)).toInt())
        val blurredBase = Bitmap.createScaledBitmap(input, blurW, blurH, true)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cm = ColorMatrix().apply {
            setSaturation(1.45f)
            // Contrast 110%
            val c = 1.1f
            val t = (1f - c) * 255f / 2f
            postConcat(ColorMatrix(floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        paint.colorFilter = ColorMatrixColorFilter(cm)
        targetCtx.drawBitmap(Bitmap.createScaledBitmap(blurredBase, w, h, true), 0f, 0f, paint)

        val pixels = IntArray(w * h)
        input.getPixels(pixels, 0, w, 0, 0, w, h)

        // 2. Tensör Alanı Hesabı
        val tensorField = computePaddedStructureTensor(pixels, w, h, scale)

        val mode = config.vangoghMode
        val stepSize = config.vangoghStepSize * scale
        val minLen = config.vangoghMinLength * scale
        val maxLen = config.vangoghMaxLength * scale
        val impasto = config.vangoghImpasto

        // 3. Katman: Kütlesel Zemin Fırçaları
        renderStreamlinePass(targetCtx, pixels, w, h, tensorField, mode, scale, StreamlineConfig(
            density = 0.014f,
            stepLength = stepSize * 0.35f,
            maxSteps = 12,
            brushWidth = minLen * 0.58f,
            bristleCount = 6,
            alpha = 0.58f,
            turbulence = 0.12f,
            impasto = false,
            vibration = true
        ))

        // 4. Katman: Ana Anatomik Fırçalar
        val mainMaxSteps = max(4, (maxLen / (stepSize * 0.25f)).toInt())
        renderStreamlinePass(targetCtx, pixels, w, h, tensorField, mode, scale, StreamlineConfig(
            density = 0.042f,
            stepLength = stepSize * 0.25f,
            maxSteps = mainMaxSteps,
            brushWidth = minLen * 0.35f,
            bristleCount = 8,
            alpha = 0.92f,
            turbulence = 0.20f,
            impasto = impasto,
            vibration = true
        ))

        // 5. Katman: Highlight & Detay Vurguları
        val detailMaxSteps = max(3, ((minLen * 0.6f) / (stepSize * 0.18f)).toInt())
        renderStreamlinePass(targetCtx, pixels, w, h, tensorField, mode, scale, StreamlineConfig(
            density = 0.020f,
            stepLength = stepSize * 0.18f,
            maxSteps = detailMaxSteps,
            brushWidth = minLen * 0.18f,
            bristleCount = 4,
            alpha = 0.95f,
            turbulence = 0.08f,
            impasto = impasto,
            highlightOnly = true,
            vibration = false
        ))

        output
    }

    private suspend fun computePaddedStructureTensor(pixels: IntArray, width: Int, height: Int, scale: Float): TensorField = coroutineScope {
        val sw = max(100, (width / scale).roundToInt())
        val sh = max(100, (height / scale).roundToInt())
        val size = sw * sh

        val Jxx = FloatArray(size)
        val Jyy = FloatArray(size)
        val Jxy = FloatArray(size)

        val numChunks = 8
        val yRanges = (1 until sh - 1).chunked(max(1, (sh - 2) / numChunks))
        
        yRanges.map { yRange ->
            async {
                for (y in yRange) {
                    for (x in 1 until sw - 1) {
                        val srcX = (x * scale).toInt()
                        val srcY = (y * scale).toInt()

                        fun getLum(dx: Int, dy: Int): Float {
                            val px = (srcX + dx).coerceIn(0, width - 1)
                            val py = (srcY + dy).coerceIn(0, height - 1)
                            val c = pixels[py * width + px]
                            val r = Color.red(c)
                            val g = Color.green(c)
                            val b = Color.blue(c)
                            return r * 0.299f + g * 0.587f + b * 0.114f
                        }

                        val l00 = getLum(-1, -1); val l10 = getLum(0, -1); val l20 = getLum(1, -1)
                        val l01 = getLum(-1, 0);                           val l21 = getLum(1, 0)
                        val l02 = getLum(-1, 1);  val l12 = getLum(0, 1);  val l22 = getLum(1, 1)

                        val gx = (l20 + 2 * l21 + l22) - (l00 + 2 * l01 + l02)
                        val gy = (l02 + 2 * l12 + l22) - (l00 + 2 * l10 + l20)

                        val iIdx = y * sw + x
                        Jxx[iIdx] = gx * gx
                        Jyy[iIdx] = gy * gy
                        Jxy[iIdx] = gx * gy
                    }
                }
            }
        }.awaitAll()

        val angles = FloatArray(size)
        val coherence = FloatArray(size)
        val radius = 3
        
        val yRanges2 = (radius until sh - radius).chunked(max(1, (sh - 2 * radius) / numChunks))
        yRanges2.map { yRange ->
            async {
                for (y in yRange) {
                    for (x in radius until sw - radius) {
                        var sxx = 0f; var syy = 0f; var sxy = 0f

                        for (dy in -radius..radius) {
                            val baseNIdx = (y + dy) * sw
                            for (dx in -radius..radius) {
                                val nIdx = baseNIdx + (x + dx)
                                sxx += Jxx[nIdx]
                                syy += Jyy[nIdx]
                                sxy += Jxy[nIdx]
                            }
                        }

                        val iIdx = y * sw + x
                        angles[iIdx] = (0.5 * atan2(2.0 * sxy, (sxx - syy).toDouble()) + PI / 2.0).toFloat()

                        val num = (sxx - syy) * (sxx - syy) + 4 * sxy * sxy
                        val den = (sxx + syy) * (sxx + syy) + 1e-5f
                        coherence[iIdx] = sqrt(num) / den
                    }
                }
            }
        }.awaitAll()

        TensorField(angles, coherence, sw, sh)
    }

    private suspend fun renderStreamlinePass(targetCtx: Canvas, pixels: IntArray, width: Int, height: Int, tensorField: TensorField, mode: Int, scale: Float, config: StreamlineConfig) {
        val referenceArea = (width / scale) * (height / scale)
        val totalSeeds = (referenceArea * config.density).toInt()

        for (i in 0 until totalSeeds) {
            if (i % 500 == 0) kotlinx.coroutines.yield()
            
            val rx = Random.nextFloat() * width
            val ry = Random.nextFloat() * height
            
            val px = rx.toInt().coerceIn(0, width - 1)
            val py = ry.toInt().coerceIn(0, height - 1)
            val c = pixels[py * width + px]
            val a = Color.alpha(c) / 255f
            
            if (a < 0.1f) continue

            val hsl = rgbToHsl(Color.red(c), Color.green(c), Color.blue(c))
            if (config.highlightOnly && hsl[2] < 0.55f) continue

            val pathPoints = traceStreamline(rx, ry, width, height, tensorField, mode, scale, config)
            if (pathPoints.size < 2) continue

            drawMultiBristleStroke(targetCtx, pathPoints, hsl, scale, config)
        }
    }

    private fun traceStreamline(startX: Float, startY: Float, width: Int, height: Int, tensorField: TensorField, mode: Int, scale: Float, config: StreamlineConfig): List<PointF> {
        val points = mutableListOf(PointF(startX, startY))
        var currX = startX
        var currY = startY

        for (step in 0 until config.maxSteps) {
            if (currX < 2f || currX >= width - 2f || currY < 2f || currY >= height - 2f) break

            val tx = (currX / scale).toInt().coerceIn(0, tensorField.sw - 1)
            val ty = (currY / scale).toInt().coerceIn(0, tensorField.sh - 1)
            val idx = ty * tensorField.sw + tx

            var baseAngle = tensorField.angles[idx]
            val coh = tensorField.coherence[idx]

            val noiseX = (currX / scale).toDouble() * 0.003
            val noiseY = (currY / scale).toDouble() * 0.003
            val noise = noise2D(noiseX, noiseY)

            if (mode == 2) {
                baseAngle += (noise * PI * 1.5).toFloat()
            } else if (mode == 3) {
                baseAngle = (baseAngle * coh) + ((PI.toFloat() / 2f + noise * 0.5f) * (1f - coh))
            } else {
                baseAngle += (noise - 0.5f) * config.turbulence * (1.2f - coh)
            }

            val vx = cos(baseAngle) * config.stepLength
            val vy = sin(baseAngle) * config.stepLength

            currX += vx
            currY += vy
            points.add(PointF(currX, currY))
        }

        return points
    }

    private fun drawMultiBristleStroke(ctx: Canvas, points: List<PointF>, baseHsl: FloatArray, scale: Float, config: StreamlineConfig) {
        val bristleCount = config.bristleCount
        val primarySat = (baseHsl[1] * 1.3f).coerceAtMost(1f)
        val primaryLum = baseHsl[2]
        
        var hueShiftBase = 0f
        if (config.vibration && Random.nextFloat() < 0.15f) {
            hueShiftBase = if (Random.nextBoolean()) 20f else -20f
        }

        val baseLineWidth = max(0.7f * scale, (config.brushWidth / bristleCount) * 1.2f)
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            style = Paint.Style.STROKE
        }
        
        val androidPaths = Array(bristleCount) { Path() }
        
        for (b in 0 until bristleCount) {
            val offsetFactor = (b.toFloat() / (bristleCount - 1) - 0.5f) * config.brushWidth
            val isMain = b == bristleCount / 2
            
            val h = (baseHsl[0] + hueShiftBase + (Random.nextFloat() - 0.5f) * 10f + 360f) % 360f
            val s = (primarySat + (Random.nextFloat() - 0.5f) * 0.15f).coerceIn(0f, 1f)
            val l = (primaryLum + if (isMain) 0.05f else (Random.nextFloat() - 0.5f) * 0.1f).coerceIn(0f, 1f)
            
            val bristleColor = ColorUtils.HSLToColor(floatArrayOf(h, s, l))
            val bristleAlpha = (config.alpha * 255 * (0.7f + Random.nextFloat() * 0.3f)).toInt().coerceIn(0, 255)
            
            paint.color = bristleColor
            paint.alpha = bristleAlpha
            
            var strokeW = baseLineWidth * (0.8f + Random.nextFloat() * 0.4f)
            
            val path = androidPaths[b]
            path.reset()
            
            if (points.isNotEmpty()) {
                val pt = points[0]
                val nx = if (points.size > 1) -(points[1].y - pt.y) else 0f
                val ny = if (points.size > 1) (points[1].x - pt.x) else 0f
                val mag = sqrt(nx * nx + ny * ny).takeIf { it > 0 } ?: 1f
                val dirX = (nx / mag) * offsetFactor
                val dirY = (ny / mag) * offsetFactor
                path.moveTo(pt.x + dirX, pt.y + dirY)
                
                for (i in 1 until points.size) {
                    val cpt = points[i]
                    val cnx = if (i < points.size - 1) -(points[i+1].y - cpt.y) else nx
                    val cny = if (i < points.size - 1) (points[i+1].x - cpt.x) else ny
                    val cmag = sqrt(cnx * cnx + cny * cny).takeIf { it > 0 } ?: 1f
                    val cdirX = (cnx / cmag) * offsetFactor
                    val cdirY = (cny / cmag) * offsetFactor
                    
                    path.lineTo(cpt.x + cdirX, cpt.y + cdirY)
                }
            }
            
            // Average taper factor of 0.8
            var finalStrokeW = strokeW * 0.8f
            var effectiveAlpha = bristleAlpha
            
            if (finalStrokeW < 1.0f) {
                effectiveAlpha = (bristleAlpha * finalStrokeW).toInt().coerceIn(0, 255)
                finalStrokeW = 1.0f
            }
            
            paint.strokeWidth = finalStrokeW
            paint.alpha = effectiveAlpha
            
            if (config.impasto && isMain) {
                val shadowPaint = Paint(paint).apply {
                    color = Color.argb(120, 0, 0, 0)
                }
                ctx.save()
                ctx.translate(config.brushWidth * 0.2f, config.brushWidth * 0.2f)
                ctx.drawPath(path, shadowPaint)
                ctx.restore()
                
                val highlightPaint = Paint(paint).apply {
                    color = Color.argb(60, 255, 255, 255)
                }
                ctx.save()
                ctx.translate(-config.brushWidth * 0.1f, -config.brushWidth * 0.1f)
                ctx.drawPath(path, highlightPaint)
                ctx.restore()
            }
            
            ctx.drawPath(path, paint)
        }
    }
}
