package com.ditherlab.ultra.data.model

data class EffectConfig(
    val selectedPaletteId: String = "default",
    val toneCurve: ToneCurveState = ToneCurveState(),
    val ditherStrength: Float = 1.0f, // 0.0f to 1.0f
    val useBayer: Boolean = true,
    val lightAngle: Float = 0.0f, // Impasto 3D ışık yönü (radyan)
    val lightElevation: Float = 45.0f, // Işığın ufuk/tepe yüksekliği (derece)
    val lightColor: Int = android.graphics.Color.WHITE, // Işık rengi (RGB)
    val impastoDepth: Float = 0.5f,
    val chromaticAberrationAmount: Float = 0.0f,
    val crtDistortionAmount: Float = 0.0f,
    val darkroomPreset: String = "sb",
    val darkroomIntensity: Float = 1.0f,
    val swirlyBokehIntensity: Float = 50f,
    val maskIsTransparent: Boolean = false,
    val asciiEnabled: Boolean = false,
    val pixelSize: Float = 128f,
    val paletteKey: String = "gameboy",
    // Glitch
    val glitchIntensity: Float = 50f,
    val gyroEffect: Boolean = false,
    val tiltX: Float = 0f,
    val tiltY: Float = 0f,
    
    // Van Gogh
    val vangoghIntensity: Float = 0.5f,
    val vangoghMode: Int = 1,
    val vangoghStepSize: Float = 12f,
    val vangoghMinLength: Float = 10f,
    val vangoghMaxLength: Float = 30f,
    val vangoghImpasto: Boolean = true,
    
    // Van Gogh Beta (GPU AGSL)
    val vangoghBetaIntensity: Float = 1.0f,
    val vangoghBetaBrushSize: Float = 8f,
    
    // Minecraft
    val minecraftBlockSize: Float = 16f,
    
    // Postcard
    val postcardMode: String = "halftone_paper",
    // AsciiMatrix Engine
    val asciiCharSetKey: String = "density",
    val asciiColorMode: String = "matrix",
    val asciiFontSize: Float = 10f,
    
    // CmykOffset Engine
    val cmykOffsetPx: Float = 5f,
    val cmykDotSize: Float = 8f,
    
    // ColorClash Engine
    val colorClashBlockSize: Float = 8f,
    
    // CrtTv Engine
    val crtScanlineGap: Float = 3f,
    val crtPhosphorGlow: Boolean = true,
    
    // FlirThermal Engine
    val flirMode: String = "ironbow",
    
    // PunkFanzine Engine
    val punkContrastBoost: Float = 2.5f,
    val punkTonerNoise: Float = 40f,
    
    // TextGlitch Engine
    val textGlitchText: String = "SYSTEM ERROR",
    val textGlitchStyle: String = "rgb_shift",
    val textGlitchFontSize: Float = 42f,
    
    // ThermalPaper Engine
    val thermalPaperType: String = "aged",
    val thermalWear: Float = 30f,
    val thermalTornEdge: Boolean = true,
    
    // SensorCorrupt Engine
    val sensorCorruptMode: String = "chaos",
    val sensorNoiseIntensity: Float = 0.5f,
    val sensorChaosLevel: Float = 0.5f,
    val sensorLineJitter: Boolean = true,
    val sensorBitShift: Boolean = true,
    
    // NoirComic Engine
    val noirDotSize: Float = 6f,
    val noirContrast: Float = 2.0f,
    val noirTextureDensity: Float = 0.3f,
    val noirColorMode: Int = 0, // 0: Noir B&W, 1: Spider-Red Pop, 2: Full Color
    val noirDotColor: String = "black", // "black", "red", "navy"
    
    val postcardStampMargin: Float = 24f,
    val postcardIntensity: Float = 0.5f,
    
    val crtIntensity: Float = 0.5f,
    
    val afterimageAngle: Float = 0.0f, // Radyan cinsinden (0-2PI)
    val afterimageSpread: Float = 20.0f, // Dağılma / Trail uzunluğu
    val lightIntensity: Float = 1.0f, // Işık Parlaklığı (0.0 - 2.0)
    val targetLayer: TargetLayer = TargetLayer.ALL, // Hangi katmanın etkileneceği (Motorlar için)
    val shapeTargetLayer: TargetLayer = TargetLayer.ALL, // Hangi katmanın maskeleneceği (Şekiller için)
    val resolvedPalette: ColorPalette? = null, // ViewModel'den motora aktarılan aktif palet
    
    // Fırça Maskesi Ayarları
    val isBrushModeActive: Boolean = false,
    val brushSize: Float = 0.05f,
    val brushPaths: List<BrushPath> = emptyList(),
    
    // Dynamic Fine-Tuning Parameters (used by UI sliders to override engine defaults)
    val customParams: Map<String, Float> = emptyMap(),
    
    // Video Timeline / Effect Range Parameters
    val effectStartTimeMs: Long = 0L,
    val effectEndTimeMs: Long = -1L,
    val trimVideoToEffect: Boolean = false
)

data class BrushPath(
    val points: List<PointF>,
    val strokeWidth: Float
)

enum class TargetLayer {
    ALL,
    SUBJECT,
    BACKGROUND
}
