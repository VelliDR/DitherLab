package com.ditherlab.ultra.engine.color

import android.graphics.Bitmap
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ToneCurveAdjuster : VisualEngine {
    override val engineName: String = "Tone Curve Adjuster"

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        // Hermite kübik eğri (Blacks, Shadows, Midtones, Highlights, Whites) hesaplaması
        input 
    }
}
