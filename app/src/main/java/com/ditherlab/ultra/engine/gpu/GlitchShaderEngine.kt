package com.ditherlab.ultra.engine.gpu

import android.graphics.Bitmap
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import com.ditherlab.ultra.data.model.EffectConfig
import kotlin.math.max

class GlitchShaderEngine : GpuShaderEngine {
    override val engineName: String = "Glitch"

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun createShader(config: EffectConfig): RuntimeShader {
        val agslCode = """
            uniform shader image;
            uniform float2 resolution;
            uniform float intensity;
            uniform float tiltX;
            uniform float tiltY;
            
            // Basic pseudo-random generator
            float rand(float2 co) {
                return fract(sin(dot(co, float2(12.9898, 78.233))) * 43758.5453);
            }

            half4 main(float2 fragCoord) {
                float2 uv = fragCoord / resolution;
                float normIntensity = max(0.05, intensity);
                
                // Base global tilt
                float tiltOffsetX = tiltX * 50.0 * normIntensity;
                float tiltOffsetY = tiltY * 50.0 * normIntensity;
                
                // 1. Shards (Block displacement)
                // Displace horizontal bands randomly based on Y coordinate
                float shardY = floor(uv.y * (10.0 + normIntensity * 20.0));
                float r = rand(float2(shardY, 1.0));
                
                float shiftX = tiltOffsetX;
                float shiftY = tiltOffsetY;
                
                // Random blocks offset
                if (r < normIntensity * 0.5) {
                    shiftX += (rand(float2(shardY, 2.0)) - 0.5) * resolution.x * 0.3 * normIntensity;
                }

                float2 targetCoord = fragCoord + float2(shiftX, shiftY);
                
                // 2. Chromatic Aberration
                float rgbShift = (5.0 + normIntensity * 25.0) * (max(resolution.x, resolution.y) / 1200.0);
                if (r < normIntensity * 0.3) {
                    rgbShift += (rand(float2(shardY, 3.0)) - 0.5) * 90.0 * normIntensity;
                }
                
                // Sample 3 color channels
                half rChannel = image.eval(targetCoord + float2(-rgbShift, 0.0)).r;
                half gChannel = image.eval(targetCoord).g;
                half bChannel = image.eval(targetCoord + float2(rgbShift, 0.0)).b;
                
                half4 finalColor = half4(rChannel, gChannel, bChannel, 1.0);
                
                // 3. Scanline/Speedline simulation on some shards
                if (r < normIntensity * 0.2) {
                    float lineY = mod(fragCoord.y, 4.0);
                    if (lineY < 1.5) {
                        finalColor.rgb = mix(finalColor.rgb, half3(1.0), 0.3);
                    }
                }
                
                return finalColor;
            }
        """.trimIndent()

        val shader = RuntimeShader(agslCode)
        shader.setFloatUniform("intensity", config.glitchIntensity)
        shader.setFloatUniform("tiltX", config.tiltX)
        shader.setFloatUniform("tiltY", config.tiltY)
        return shader
    }
}
