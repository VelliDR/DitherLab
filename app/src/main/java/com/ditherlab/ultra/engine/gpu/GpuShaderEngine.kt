package com.ditherlab.ultra.engine.gpu

import android.graphics.Bitmap
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine

interface GpuShaderEngine : VisualEngine {
    
    /**
     * Creates and configures a RuntimeShader for Android 13+ (AGSL).
     * @param config The current user-selected configuration for the effect.
     * @return A configured RuntimeShader ready to be applied as a RenderEffect.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createShader(config: EffectConfig): RuntimeShader

    /**
     * Applies the shader to the input bitmap and returns the processed bitmap.
     * On Android 13+, this uses Skia's highly optimized RuntimeShader implementation.
     */
    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val shader = createShader(config)
            shader.setFloatUniform("resolution", input.width.toFloat(), input.height.toFloat())

            val bitmapShader = android.graphics.BitmapShader(
                input, 
                android.graphics.Shader.TileMode.CLAMP, 
                android.graphics.Shader.TileMode.CLAMP
            )
            shader.setInputShader("image", bitmapShader)
            
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                this.shader = shader
            }

            // Create HardwareBuffer backed ImageReader
            val imageReader = android.media.ImageReader.newInstance(
                input.width, 
                input.height, 
                android.graphics.PixelFormat.RGBA_8888, 
                1, 
                android.hardware.HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or android.hardware.HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
            )
            
            val surface = imageReader.surface
            val renderer = android.graphics.HardwareRenderer()
            renderer.setSurface(surface)
            
            val renderNode = android.graphics.RenderNode("gpu_shader_node")
            renderNode.setPosition(0, 0, input.width, input.height)
            val canvas = renderNode.beginRecording()
            canvas.drawRect(0f, 0f, input.width.toFloat(), input.height.toFloat(), paint)
            renderNode.endRecording()
            
            renderer.setContentRoot(renderNode)
            renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()
            
            val image = imageReader.acquireNextImage()
            if (image == null) {
                // If it's still null, return original input to prevent crash
                renderer.destroy()
                surface.release()
                imageReader.close()
                return@withContext input
            }
            
            val hardwareBuffer = image.hardwareBuffer
            val hardwareBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer!!, null)
            
            val outputBitmap = hardwareBitmap?.copy(Bitmap.Config.ARGB_8888, true) ?: input
            
            hardwareBitmap?.recycle()
            hardwareBuffer.close()
            image.close()
            renderer.destroy()
            surface.release()
            imageReader.close()
            
            outputBitmap
        } else {
            // Fallback for older devices (could just return input, or implement a slow CPU loop)
            input
        }
    }
}
