package com.ditherlab.ultra.engine.gpu

import android.graphics.Bitmap
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import com.ditherlab.ultra.data.model.EffectConfig

class CrtTvShaderEngine : GpuShaderEngine {
    override val engineName: String = "CrtTv"

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun createShader(config: EffectConfig): RuntimeShader {
        val agslCode = """
            uniform shader image;
            uniform float2 resolution;
            uniform float scanlineGap;
            uniform int phosphorGlow;

            // Simplified CRT effect for GPU
            half4 main(float2 fragCoord) {
                // 1. Shift for RGB split (chromatic aberration)
                float shiftPx = max(1.0, max(resolution.x, resolution.y) / 600.0);
                float2 uv = fragCoord;
                
                half4 colorBase = image.eval(uv);
                half4 colorShifted = image.eval(float2(uv.x - shiftPx, uv.y));
                
                // Screen blend for chromatic aberration
                half4 blended = half4(1.0) - (half4(1.0) - colorBase) * (half4(1.0) - colorShifted * 0.35);
                
                // 2. Phosphor Glow (Approximated by sampling neighbors)
                if (phosphorGlow == 1) {
                    float blurOffset = 4.0;
                    half4 glow1 = image.eval(float2(uv.x - blurOffset, uv.y - blurOffset));
                    half4 glow2 = image.eval(float2(uv.x + blurOffset, uv.y + blurOffset));
                    half4 glow3 = image.eval(float2(uv.x - blurOffset, uv.y + blurOffset));
                    half4 glow4 = image.eval(float2(uv.x + blurOffset, uv.y - blurOffset));
                    half4 glowSum = (glow1 + glow2 + glow3 + glow4) * 0.25;
                    blended = half4(1.0) - (half4(1.0) - blended) * (half4(1.0) - glowSum * 0.30);
                }

                // 3. Scanlines
                float lineH = max(1.0, scanlineGap * 0.45);
                float modY = mod(fragCoord.y, scanlineGap);
                if (modY < lineH) {
                    blended.rgb = mix(blended.rgb, half3(0.0), 0.40);
                }

                // 4. Vignette
                float2 center = resolution / 2.0;
                float dist = distance(uv, center);
                float maxDist = max(resolution.x, resolution.y) * 0.72;
                float t = clamp(dist / maxDist, 0.0, 1.0);
                
                // Polynomial smoothstep for vignette gradient
                float vignetteVal = smoothstep(0.7, 1.0, t);
                blended.rgb = mix(blended.rgb, half3(0.0), vignetteVal * 0.85);

                return half4(blended.rgb, 1.0);
            }
        """.trimIndent()

        val shader = RuntimeShader(agslCode)
        shader.setFloatUniform("scanlineGap", config.crtScanlineGap)
        shader.setIntUniform("phosphorGlow", if (config.crtPhosphorGlow) 1 else 0)
        return shader
    }
    
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun updateShaderUniforms(shader: RuntimeShader, config: EffectConfig) {
        shader.setFloatUniform("scanlineGap", config.crtScanlineGap)
        shader.setIntUniform("phosphorGlow", if (config.crtPhosphorGlow) 1 else 0)
    }
}
