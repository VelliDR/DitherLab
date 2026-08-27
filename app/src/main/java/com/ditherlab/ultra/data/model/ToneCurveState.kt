package com.ditherlab.ultra.data.model

data class PointF(val x: Float, val y: Float)

data class ToneCurveState(
    // 5 Noktalı Hermite Kübik Eğrisi kontrol noktaları
    val points: List<PointF> = listOf(
        PointF(0.0f, 0.0f),
        PointF(0.25f, 0.25f),
        PointF(0.5f, 0.5f),
        PointF(0.75f, 0.75f),
        PointF(1.0f, 1.0f)
    )
) {
    init {
        require(points.size == 5) { "ToneCurve tam olarak 5 kontrol noktasına sahip olmalıdır." }
    }
}
