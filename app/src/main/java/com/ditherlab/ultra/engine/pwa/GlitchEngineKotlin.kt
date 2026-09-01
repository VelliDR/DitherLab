package com.ditherlab.ultra.engine.pwa

import android.graphics.*
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GlitchEngineKotlin : VisualEngine {
    override val engineName: String = "Glitch"

    private var benDayBitmap: Bitmap? = null

    @Synchronized
    private fun getBenDayPattern(scale: Float): BitmapShader {
        val size = max(4, (5 * scale).toInt())
        val bm = benDayBitmap
        if (bm == null || bm.width != size) {
            val newBm = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(newBm)
            // Draw ONLY the black dots, leave background transparent
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#111116") }
            c.drawCircle(size / 2f, size / 2f, size / 4.2f, p)
            benDayBitmap?.recycle()
            benDayBitmap = newBm
        }
        return BitmapShader(benDayBitmap!!, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
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
        return floatArrayOf(h * 360f, s * 100f, l * 100f)
    }

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        val w = input.width
        val h = input.height
        val scale = max(w, h) / 1200f
        val normIntensity = (config.glitchIntensity / 100f).coerceIn(0.05f, 1.0f)

        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val ctx = Canvas(output)
        
        val paint = Paint()

        // Gyro Tilt offsets
        val tiltOffsetX = config.tiltX * 50f * scale * normIntensity
        val tiltOffsetY = config.tiltY * 50f * scale * normIntensity

        // 1. True RGB/CMYK Split for Global Aberration (Spider-Verse style)
        val paintRed = Paint().apply {
            colorFilter = ColorMatrixColorFilter(floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 0f, 0f, 0f, 0f,
                0f, 0f, 0f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        val paintCyan = Paint().apply {
            colorFilter = ColorMatrixColorFilter(floatArrayOf(
                0f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        }

        val globalShiftX = (5f + normIntensity * 25f) * scale
        
        // Draw base image as separated red and cyan channels with offset
        ctx.drawBitmap(input, tiltOffsetX - globalShiftX, tiltOffsetY, paintRed)
        ctx.drawBitmap(input, tiltOffsetX + globalShiftX, tiltOffsetY, paintCyan)

        // 2. VHS Style Shards (Horizontal blocks)
        val shardCount = (10 + normIntensity * 35).toInt()
        val maxShift = w * 0.45f * normIntensity

        for (i in 0 until shardCount) {
            val cy = Random.nextFloat() * h
            val shardH = (0.01f + Random.nextFloat() * 0.12f) * h // Ince uzun yatay bloklar
            
            val cx = Random.nextFloat() * w
            val shardW = (0.2f + Random.nextFloat() * 0.7f) * w // Genis bloklar

            val shiftX = (Random.nextFloat() - 0.5f) * maxShift + tiltOffsetX * 2f
            
            ctx.save()
            // Rect clip for VHS block (Yatay yirtilma)
            ctx.clipRect(cx - shardW, cy - shardH, cx + shardW, cy + shardH)
            
            // Draw normal shifted image
            paint.reset()
            ctx.drawBitmap(input, shiftX, 0f, paint)

            // Add extreme color aberration inside the block
            val blockShiftX = (Random.nextFloat() - 0.5f) * 90f * scale * normIntensity
            val useRed = Random.nextBoolean()
            
            val activeColorPaint = if (useRed) paintRed else paintCyan
            activeColorPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            ctx.drawBitmap(input, shiftX + blockShiftX, 0f, activeColorPaint)
            // Reset ADD mode for cyan paint if we changed it to SCREEN above
            paintCyan.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD) 

            // Add BenDay dots overlay ONLY inside the shard, and ONLY over opaque pixels
            if (normIntensity > 0.1f) {
                paint.reset()
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                paint.alpha = (255 * 0.3f).toInt() // Noktalar cok koyu olmasin
                paint.shader = getBenDayPattern(scale)
                ctx.drawRect(cx - shardW, cy - shardH, cx + shardW, cy + shardH, paint)
                paint.shader = null
            }

            // Yatay Speed Lines (VHS Scanline tarzi) ONLY over opaque pixels
            paint.reset()
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
            paint.color = Color.argb((255 * 0.6f).toInt(), 255, 255, 255)
            paint.strokeWidth = max(1f, 1.5f * scale)
            paint.style = Paint.Style.STROKE
            
            val lineCount = 1 + Random.nextInt(4)
            for (l in 0 until lineCount) {
                val ly = cy - shardH + Random.nextFloat() * (shardH * 2)
                ctx.drawLine(cx - shardW, ly, cx + shardW, ly, paint)
            }
            
            // Kose Çizgileri (Neon border) ONLY over opaque pixels
            paint.reset()
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = max(1f, 2f * scale)
            paint.color = if (useRed) Color.parseColor("#ff007f") else Color.parseColor("#00f0ff")
            paint.alpha = (255 * 0.7f).toInt()
            
            if (Random.nextBoolean()) {
                ctx.drawLine(cx - shardW, cy - shardH, cx + shardW, cy - shardH, paint)
            } else {
                ctx.drawLine(cx - shardW, cy + shardH, cx + shardW, cy + shardH, paint)
            }

            ctx.restore() // End of Shard Clip
        }
        output
    }
}
