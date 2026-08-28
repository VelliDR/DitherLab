package com.ditherlab.ultra.engine.pwa

import android.graphics.*
import android.os.Build
import androidx.annotation.RequiresApi
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VanGoghBetaEngineKotlin : VisualEngine {
    override val engineName: String = "Van Gogh Beta (GPU)"

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val agslShader = """
        uniform shader image;
        uniform float2 resolution;
        uniform float brushSize;
        uniform float intensity;
        uniform int mode;

        float getLum(float3 c) {
            return dot(c, float3(0.299, 0.587, 0.114));
        }

        float hash(float2 p) {
            return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
        }

        float noise(float2 p) {
            float2 i = floor(p);
            float2 f = fract(p);
            float2 u = f*f*(3.0-2.0*f);
            return mix(mix(hash(i), hash(i+float2(1.0,0.0)), u.x),
                       mix(hash(i+float2(0.0,1.0)), hash(i+float2(1.0,1.0)), u.x), u.y);
        }

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            
            // 1. Structure Tensor (Edge detection)
            float step = 1.0;
            float3 c00 = image.eval(fragCoord + float2(-step, -step)).rgb;
            float3 c10 = image.eval(fragCoord + float2( 0, -step)).rgb;
            float3 c20 = image.eval(fragCoord + float2( step, -step)).rgb;
            float3 c01 = image.eval(fragCoord + float2(-step,  0)).rgb;
            float3 c21 = image.eval(fragCoord + float2( step,  0)).rgb;
            float3 c02 = image.eval(fragCoord + float2(-step,  step)).rgb;
            float3 c12 = image.eval(fragCoord + float2( 0,  step)).rgb;
            float3 c22 = image.eval(fragCoord + float2( step,  step)).rgb;

            float l00 = getLum(c00); float l10 = getLum(c10); float l20 = getLum(c20);
            float l01 = getLum(c01);                          float l21 = getLum(c21);
            float l02 = getLum(c02); float l12 = getLum(c12); float l22 = getLum(c22);

            float gx = (l20 + 2.0*l21 + l22) - (l00 + 2.0*l01 + l02);
            float gy = (l02 + 2.0*l12 + l22) - (l00 + 2.0*l10 + l20);
            
            float2 dir = float2(gx, gy);
            if (length(dir) < 0.001) {
                dir = float2(1.0, 0.0);
            } else {
                dir = normalize(dir);
            }
            
            // Tangent follows the edge contour
            float2 t = float2(-dir.y, dir.x);
            
            // Add turbulence to the stroke direction
            float n1 = noise(uv * 10.0);
            float n2 = noise(uv * 10.0 + float2(5.2, 1.3));
            
            if (mode == 2) {
                // Swirl / chaos
                float angleOffset = (n1 - 0.5) * 3.14159 * 1.5 * intensity;
                float s = sin(angleOffset);
                float c = cos(angleOffset);
                t = float2(t.x * c - t.y * s, t.x * s + t.y * c);
            } else if (mode == 3) {
                // Radial / Perpendicular
                t = mix(t, dir, 0.8 + (n1 - 0.5) * 0.4 * intensity);
            } else {
                // Parallel
                t += float2(n1 - 0.5, n2 - 0.5) * intensity * 1.5;
            }
            
            t = normalize(t);
            
            float2 n = float2(-t.y, t.x); // Normal to the tangent

            float len = min(15.0, max(2.0, brushSize * 1.5));
            float wid = max(1.0, len * 0.3);
            float count = (len + 1.0) * (wid + 1.0);
            
            // 2. Anisotropic Kuwahara Filter
            float3 m0 = float3(0.0); float3 s0 = float3(0.0);
            float3 m1 = float3(0.0); float3 s1 = float3(0.0);
            float3 m2 = float3(0.0); float3 s2 = float3(0.0);
            float3 m3 = float3(0.0); float3 s3 = float3(0.0);
            
            // Quad 0: [-len, 0], [-wid, 0]
            for(int j = -6; j <= 0; j++) {
                if (float(j) < -wid) continue;
                for(int i = -15; i <= 0; i++) {
                    if (float(i) < -len) continue;
                    float3 c = image.eval(fragCoord + t * float(i) + n * float(j)).rgb;
                    m0 += c; s0 += c*c;
                }
            }
            
            // Quad 1: [0, len], [-wid, 0]
            for(int j = -6; j <= 0; j++) {
                if (float(j) < -wid) continue;
                for(int i = 0; i <= 15; i++) {
                    if (float(i) > len) break;
                    float3 c = image.eval(fragCoord + t * float(i) + n * float(j)).rgb;
                    m1 += c; s1 += c*c;
                }
            }
            
            // Quad 2: [-len, 0], [0, wid]
            for(int j = 0; j <= 6; j++) {
                if (float(j) > wid) break;
                for(int i = -15; i <= 0; i++) {
                    if (float(i) < -len) continue;
                    float3 c = image.eval(fragCoord + t * float(i) + n * float(j)).rgb;
                    m2 += c; s2 += c*c;
                }
            }
            
            // Quad 3: [0, len], [0, wid]
            for(int j = 0; j <= 6; j++) {
                if (float(j) > wid) break;
                for(int i = 0; i <= 15; i++) {
                    if (float(i) > len) break;
                    float3 c = image.eval(fragCoord + t * float(i) + n * float(j)).rgb;
                    m3 += c; s3 += c*c;
                }
            }
            
            m0 /= count; s0 = abs(s0 / count - m0*m0);
            m1 /= count; s1 = abs(s1 / count - m1*m1);
            m2 /= count; s2 = abs(s2 / count - m2*m2);
            m3 /= count; s3 = abs(s3 / count - m3*m3);
            
            float var0 = s0.r + s0.g + s0.b;
            float var1 = s1.r + s1.g + s1.b;
            float var2 = s2.r + s2.g + s2.b;
            float var3 = s3.r + s3.g + s3.b;
            
            float min_var = var0;
            float3 final_color = m0;
            
            if (var1 < min_var) { min_var = var1; final_color = m1; }
            if (var2 < min_var) { min_var = var2; final_color = m2; }
            if (var3 < min_var) { min_var = var3; final_color = m3; }
            
            // 3. Impasto / Emboss Lighting
            // Calculate pseudo height map based on luminance gradient + noise
            float hgx = gx * 2.0 + (noise(uv * 60.0) - 0.5) * 0.4;
            float hgy = gy * 2.0 + (noise(uv * 60.0 + float2(1.1, 2.2)) - 0.5) * 0.4;
            
            // Stroke edges have high variance. We can carve valleys at the edges.
            float valley = clamp(min_var * 40.0 * intensity, 0.0, 1.0);
            // Deepen valleys
            final_color *= mix(1.0, 0.6, valley);
            
            float bump_scale = intensity * 1.5;
            float3 normal = normalize(float3(-hgx * bump_scale, -hgy * bump_scale, 1.0));
            float3 lightDir = normalize(float3(-1.0, -1.0, 1.0));
            
            float diff = max(0.0, dot(normal, lightDir));
            float spec = pow(max(0.0, dot(reflect(-lightDir, normal), float3(0.0, 0.0, 1.0))), 8.0);
            
            final_color = final_color * (0.85 + 0.3 * diff) + spec * 0.15 * intensity;
            
            // 4. Color Correction (Slight saturation boost)
            float lum = getLum(final_color);
            final_color = mix(float3(lum), final_color, 1.25);
            
            return half4(final_color, 1.0);
        }
    """

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val w = input.width
            val h = input.height
            
            val shader = RuntimeShader(agslShader)
            shader.setFloatUniform("resolution", w.toFloat(), h.toFloat())
            shader.setFloatUniform("brushSize", config.vangoghBetaBrushSize)
            shader.setFloatUniform("intensity", config.vangoghBetaIntensity)
            shader.setIntUniform("mode", config.vangoghMode)
            
            val bitmapShader = BitmapShader(input, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            shader.setInputShader("image", bitmapShader)
            
            val paint = Paint().apply {
                this.shader = shader
            }
            
            val picture = android.graphics.Picture()
            val canvas = picture.beginRecording(w, h)
            
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
            picture.endRecording()
            
            // Bitmap.createBitmap with Picture creates a hardware bitmap if possible (API 28+)
            // Since API 31+, it explicitly supports RenderNode backing, allowing RuntimeShader
            val hwBitmap = Bitmap.createBitmap(picture, w, h, Bitmap.Config.HARDWARE)
            
            // Convert back to software bitmap so it can be drawn safely on other software canvases
            hwBitmap.copy(Bitmap.Config.ARGB_8888, false) ?: input
        } else {
            input
        }
    }
}
