package com.ditherlab.ultra.engine.gpu

import android.graphics.Bitmap
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import com.ditherlab.ultra.data.model.EffectConfig

class VanGoghShaderEngine : GpuShaderEngine {
    override val engineName: String = "Van Gogh"

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun createShader(config: EffectConfig): RuntimeShader {
        val agslCode = """
            uniform shader image;
            uniform float2 resolution;
            uniform float stepSize;
            uniform float brushLength;
            uniform int impasto;
            uniform int mode;

            // Helper to get luminance
            float getLuminance(half3 color) {
                return dot(color, half3(0.299, 0.587, 0.114));
            }

            half4 main(float2 fragCoord) {
                float2 uv = fragCoord;
                float2 step = float2(1.0) / resolution;
                
                // 1. Compute Local Gradient (Sobel) to find stroke direction
                float l00 = getLuminance(image.eval(uv + float2(-1.0, -1.0)).rgb);
                float l10 = getLuminance(image.eval(uv + float2( 0.0, -1.0)).rgb);
                float l20 = getLuminance(image.eval(uv + float2( 1.0, -1.0)).rgb);
                float l01 = getLuminance(image.eval(uv + float2(-1.0,  0.0)).rgb);
                float l21 = getLuminance(image.eval(uv + float2( 1.0,  0.0)).rgb);
                float l02 = getLuminance(image.eval(uv + float2(-1.0,  1.0)).rgb);
                float l12 = getLuminance(image.eval(uv + float2( 0.0,  1.0)).rgb);
                float l22 = getLuminance(image.eval(uv + float2( 1.0,  1.0)).rgb);

                float gx = (l20 + 2.0 * l21 + l22) - (l00 + 2.0 * l01 + l02);
                float gy = (l02 + 2.0 * l12 + l22) - (l00 + 2.0 * l10 + l20);
                
                // Tangent direction (perpendicular to gradient)
                float2 dir = normalize(float2(-gy, gx));
                if (length(dir) < 0.01) {
                    dir = float2(1.0, 0.0);
                }

                // Add some turbulence based on mode
                if (mode > 0) {
                    float noise = fract(sin(dot(uv, float2(12.9898, 78.233))) * 43758.5453);
                    float angle = (noise - 0.5) * 0.5;
                    float s = sin(angle);
                    float c = cos(angle);
                    dir = float2(dir.x * c - dir.y * s, dir.x * s + dir.y * c);
                }

                // 2. Directional Blur (Line Integral Convolution style)
                int samples = int(clamp(brushLength * 0.5, 4.0, 16.0));
                half3 sum = half3(0.0);
                float weightSum = 0.0;
                
                float actualStep = max(1.0, stepSize * 0.5);

                for (int i = -samples; i <= samples; i++) {
                    float w = 1.0 - abs(float(i)) / float(samples);
                    float2 offset = dir * (float(i) * actualStep);
                    sum += image.eval(uv + offset).rgb * w;
                    weightSum += w;
                }
                
                half3 finalColor = sum / weightSum;

                // 3. Impasto Effect (Emboss based on luminance gradient of the blurred result)
                if (impasto == 1) {
                    float bl01 = getLuminance(image.eval(uv + float2(-2.0, 0.0)).rgb);
                    float bl21 = getLuminance(image.eval(uv + float2( 2.0, 0.0)).rgb);
                    float bl10 = getLuminance(image.eval(uv + float2( 0.0,-2.0)).rgb);
                    float bl12 = getLuminance(image.eval(uv + float2( 0.0, 2.0)).rgb);
                    
                    float dX = bl21 - bl01;
                    float dY = bl12 - bl10;
                    
                    // Light from top-left
                    float emboss = (dX + dY) * 2.0;
                    finalColor += half3(emboss);
                }

                // Increase saturation slightly to match Van Gogh style
                float lum = getLuminance(finalColor);
                finalColor = mix(half3(lum), finalColor, 1.4);

                return half4(finalColor, 1.0);
            }
        """.trimIndent()

        val shader = RuntimeShader(agslCode)
        shader.setFloatUniform("stepSize", config.vangoghStepSize)
        shader.setFloatUniform("brushLength", config.vangoghMaxLength)
        shader.setIntUniform("impasto", if (config.vangoghImpasto) 1 else 0)
        shader.setIntUniform("mode", config.vangoghMode)
        return shader
    }
    
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun updateShaderUniforms(shader: RuntimeShader, config: EffectConfig) {
        shader.setFloatUniform("stepSize", config.vangoghStepSize)
        shader.setFloatUniform("brushLength", config.vangoghMaxLength)
        shader.setIntUniform("impasto", if (config.vangoghImpasto) 1 else 0)
        shader.setIntUniform("mode", config.vangoghMode)
    }
}
