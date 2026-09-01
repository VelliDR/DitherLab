package com.ditherlab.ultra.engine.pwa

import android.graphics.Bitmap
import com.ditherlab.ultra.data.model.EffectConfig
import com.ditherlab.ultra.engine.base.VisualEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OrijinalEngineKotlin : VisualEngine {
    override val engineName: String = "Orijinal"

    override suspend fun process(input: Bitmap, config: EffectConfig): Bitmap = withContext(Dispatchers.Default) {
        // Return the input unchanged
        input
    }
}
