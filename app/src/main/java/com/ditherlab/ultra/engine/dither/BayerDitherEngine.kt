package com.ditherlab.ultra.engine.dither

import android.graphics.Bitmap
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BayerDitherEngine : VisualEngine {
    override val engineName: String = "Bayer Ordered Dither"

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        // AGSL RenderEffect entegrasyonu RenderNode üzerinden burada yapılacaktır.
        input 
    }
}
