import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Picture
import android.graphics.RuntimeShader
import android.os.Build

fun renderWithPicture(w: Int, h: Int): Bitmap? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null

    val shaderStr = """
        uniform float2 resolution;
        half4 main(float2 fragCoord) {
            return half4(0.0, 1.0, 0.0, 1.0);
        }
    """
    val shader = RuntimeShader(shaderStr)
    shader.setFloatUniform("resolution", w.toFloat(), h.toFloat())

    val paint = Paint().apply { this.shader = shader }

    val picture = Picture()
    val canvas = picture.beginRecording(w, h)
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    picture.endRecording()
    
    // API 28+ creates a hardware bitmap!
    val hwBitmap = Bitmap.createBitmap(picture, w, h, Bitmap.Config.HARDWARE)
    val swBitmap = hwBitmap.copy(Bitmap.Config.ARGB_8888, false)
    
    return swBitmap
}
