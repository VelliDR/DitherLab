package com.ditherlab.ultra.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ditherlab.ultra.data.model.StudioUiState
import com.ditherlab.ultra.ui.components.CanvasViewport
import com.ditherlab.ultra.ui.components.MatteSlider
import com.ditherlab.ultra.ui.theme.*
import com.ditherlab.ultra.ui.viewmodel.StudioViewModel

// ─────────────────────────────────────────
// Kadraj Reposundan Alınan Şablon Listesi
// ─────────────────────────────────────────
private data class ShapeItem(val id: String, val emoji: String, val label: String)

private val ALL_SHAPES = listOf(
    ShapeItem("none", "🚫", "Yok"),
    ShapeItem("fluidRiver", "🌊", "Akışkan Nehir"),
    ShapeItem("fluidCorners", "🌓", "Karşıt Köşeler"),
    ShapeItem("blob", "💧", "Akışkan Leke"),
    ShapeItem("waveTop", "〰️", "Dalgalı Tepe"),
    ShapeItem("arch", "🏛️", "Antik Kemer"),
    ShapeItem("doubleArch", "🚪", "İkili Portal"),
    ShapeItem("cinemaFrame", "📺", "Retro Ekran"),
    ShapeItem("ticket", "🎟️", "Sinema Bileti"),
    ShapeItem("torn1", "📜", "Dikey Yırtık"),
    ShapeItem("torn2", "📄", "Alt Yırtık"),
    ShapeItem("tornWindow", "🕳️", "Yırtık Pencere"),
    ShapeItem("stamp", "📮", "Posta Pulu"),
    ShapeItem("puzzle", "🧩", "Puzzle"),
    ShapeItem("diagonal", "⚡", "Çapraz Kesim"),
    ShapeItem("diagonalSplit", "📐", "Çapraz Şerit"),
    ShapeItem("hexagon", "⬡", "Altıgen"),
    ShapeItem("diamond", "💠", "Elmas Kesim"),
    ShapeItem("badge", "🎖️", "Rozet"),
    ShapeItem("circle", "⭕", "Daire"),
    ShapeItem("ellipse", "🥚", "Elips"),
    ShapeItem("windowGrid", "🪟", "4'lü Pencere")
)

// Drawer Sekmeleri
private enum class DrawerTab(val label: String, val emoji: String) {
    MOTOR("Motor", "⚙️"),
    AYAR("Ayar", "🎛️"),
    SABLON("Şablon", "📐"),
    GOLGE("Gölge", "🎭")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
    viewModel: StudioViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val pickImage = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                val bitmap = android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
                viewModel.setOriginalImage(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val pickVideo = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setOriginalVideo(context, uri)
        }
    }

    var isDrawerOpen by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(DrawerTab.MOTOR) }

    Scaffold(
        containerColor = DeepCanvasBlack,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MatteOrange,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("v3", color = DeepCanvasBlack, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Text("DitherLab", color = PrimaryLightSage, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = PrimaryMatteGreen
                ),
                actions = {
                    if (uiState is StudioUiState.Active) {
                        val activeState = uiState as StudioUiState.Active
                        TextButton(onClick = { pickImage.launch("image/*") }) {
                            Text("📷", fontSize = 16.sp)
                        }
                        TextButton(onClick = { pickVideo.launch("video/*") }) {
                            Text("🎥", fontSize = 16.sp)
                        }
                        if (activeState.isExporting || activeState.isProcessingVideo) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp).size(20.dp),
                                color = MatteOrange,
                                strokeWidth = 2.dp
                            )
                        } else {
                            if (activeState.originalVideoUri != null) {
                                TextButton(onClick = { viewModel.exportVideo(context) }) {
                                    Text("⬇️V", color = PrimaryMatteGreen, fontSize = 12.sp)
                                }
                            } else {
                                TextButton(onClick = { viewModel.exportImage(context) }) {
                                    Text("⬇️", fontSize = 16.sp)
                                }
                            }
                        }
                        TextButton(onClick = { viewModel.toggleSplitView() }) {
                            val st = activeState
                            Text(if (st.isA_BSplitActive) "A|B" else "A/B", color = if (st.isA_BSplitActive) MatteOrange else OnSurfaceMutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { isDrawerOpen = !isDrawerOpen }) {
                            Text(if (isDrawerOpen) "✕" else "🎨", fontSize = 18.sp)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is StudioUiState.Loading -> {
                    Text("Yükleniyor...", color = PrimaryMatteGreen, modifier = Modifier.align(Alignment.Center))
                }
                is StudioUiState.Error -> {
                    Text("Hata: ${state.message}", color = Color.Red, modifier = Modifier.align(Alignment.Center))
                }
                is StudioUiState.Active -> {
                    // ── YENİ: Video ise Sol Menü ve Ana Tuval ──
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Sol Menü: Video Zaman Çizelgesi
                        if (state.originalVideoUri != null) {
                            com.ditherlab.ultra.ui.components.VideoTimelineSidebar(state, viewModel)
                        }
                        
                        // ── ANA TUVAL (Kalan alanı kaplar) ──
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(DeepCanvasBlack)
                        ) {
                            if (state.originalImage == null && state.originalVideoUri == null) {
                                // Boş durum: seçim butonları
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable { pickImage.launch("image/*") }.padding(16.dp)
                                    ) {
                                        Text("📷", fontSize = 48.sp)
                                        Spacer(Modifier.height(8.dp))
                                        Text("Fotoğraf", color = Color.White, fontSize = 14.sp)
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable { pickVideo.launch("video/*") }.padding(16.dp)
                                    ) {
                                        Text("🎥", fontSize = 48.sp)
                                        Spacer(Modifier.height(8.dp))
                                        Text("Video", color = Color.White, fontSize = 14.sp)
                                    }
                                }
                            }
                        } else if (state.originalImage != null && state.originalVideoUri == null) {
                            // Fotoğraf modu
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceDark)
                            ) {
                                CanvasViewport(
                                    uiState = state,
                                    onBrushPointAdded = { pt, isNew -> viewModel.addBrushPoint(pt, isNew) },
                                    onBrushPathFinished = { viewModel.finishBrushPath() }
                                )
                            }
                        } else if (state.originalVideoUri != null) {
                            // Video modu
                            val engine = viewModel.availableEngines.getOrNull(state.backgroundEngineIndex)
                                ?: viewModel.availableEngines.first()
                            val isGpuEngine = engine is com.ditherlab.ultra.engine.gpu.GpuShaderEngine

                            val exoPlayer = remember(context) {
                                androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                                    setMediaItem(androidx.media3.common.MediaItem.fromUri(state.originalVideoUri))
                                    repeatMode = androidx.media3.exoplayer.ExoPlayer.REPEAT_MODE_ALL
                                    prepare()
                                    play()
                                }
                            }

                            var videoAspectRatio by remember { mutableFloatStateOf(16f / 9f) }

                            DisposableEffect(exoPlayer) {
                                val listener = object : androidx.media3.common.Player.Listener {
                                    override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                                        if (videoSize.width > 0 && videoSize.height > 0) {
                                            videoAspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                                        }
                                    }
                                }
                                exoPlayer.addListener(listener)
                                onDispose {
                                    exoPlayer.removeListener(listener)
                                    exoPlayer.release()
                                }
                            }

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.ui.viewinterop.AndroidView(
                                    factory = { ctx ->
                                        android.view.TextureView(ctx).apply {
                                            exoPlayer.setVideoTextureView(this)
                                        }
                                    },
                                    modifier = Modifier
                                        .aspectRatio(videoAspectRatio)
                                        .then(
                                            if (isGpuEngine && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                                val gpuEngine = engine as com.ditherlab.ultra.engine.gpu.GpuShaderEngine
                                                val shader = remember(engine) { gpuEngine.createShader(state.currentConfig) }
                                                Modifier.graphicsLayer {
                                                    gpuEngine.updateShaderUniforms(shader, state.currentConfig)
                                                    shader.setFloatUniform("resolution", this.size.width, this.size.height)
                                                    val renderEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "image")
                                                    this.renderEffect = renderEffect.asComposeRenderEffect()
                                                    this.clip = true
                                                }
                                            } else {
                                                Modifier
                                            }
                                        )
                                )
                                
                                val fgEngine = viewModel.availableEngines.getOrNull(state.foregroundEngineIndex)
                                val isFgGpu = fgEngine is com.ditherlab.ultra.engine.gpu.GpuShaderEngine
                                if (!isGpuEngine || !isFgGpu || state.currentConfig.targetLayer != com.ditherlab.ultra.data.model.TargetLayer.ALL || state.maskShape.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Canlı önizleme (Video)\nSadece GPU destekli basit motorlarda çalışır.\n\nKarmaşık maskeler veya CPU motorları için \nsonucu görmek üzere 'Dışa Aktar' butonunu kullanın.",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(24.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Video progress bar
                        if (state.isProcessingVideo && state.videoProgress > 0f) {
                            LinearProgressIndicator(
                                progress = { state.videoProgress },
                                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                                color = MatteOrange,
                                trackColor = SurfaceDark
                            )
                        }
                    } // <-- Box (Ana Tuval) kapaniyor
                    } // <-- YENI: Row kapaniyor

                    // ── SAĞ DRAWER (Overlay) ──
                    val drawerAlpha by animateFloatAsState(
                        targetValue = if (state.sliderIsDragging) 0.1f else 1f,
                        label = "drawerAlpha"
                    )

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        // Arka plan dokunma alanı (drawer'ı kapatmak için)
                        if (isDrawerOpen) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { isDrawerOpen = false }
                                    .background(Color.Black.copy(alpha = 0.3f))
                            )
                        }

                        AnimatedVisibility(
                            visible = isDrawerOpen,
                            enter = slideInHorizontally(initialOffsetX = { it }),
                            exit = slideOutHorizontally(targetOffsetX = { it })
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.72f)
                                    .alpha(drawerAlpha)
                            ) {
                                // Sol: Sekme Çubuğu (Kadraj dock-sidebar benzeri)
                                Column(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(56.dp)
                                        .background(SurfaceDark.copy(alpha = 0.97f))
                                        .padding(vertical = 8.dp),
                                    verticalArrangement = Arrangement.Top,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    DrawerTab.entries.forEach { tab ->
                                        val isActive = activeTab == tab
                                        Surface(
                                            modifier = Modifier
                                                .padding(vertical = 2.dp)
                                                .clickable { activeTab = tab },
                                            color = if (isActive) SurfaceHighlight else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(tab.emoji, fontSize = 14.sp)
                                                Text(
                                                    tab.label,
                                                    color = if (isActive) MatteOrange else OnSurfaceMutedText,
                                                    fontSize = 9.sp,
                                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }

                                // Sağ: İçerik Paneli
                                Column(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f)
                                        .background(DeepCanvasBlack.copy(alpha = 0.93f))
                                        .verticalScroll(rememberScrollState())
                                        .padding(12.dp)
                                ) {
                                    when (activeTab) {
                                        DrawerTab.MOTOR -> MotorTabContent(viewModel, state)
                                        DrawerTab.AYAR -> AyarTabContent(viewModel, state)
                                        DrawerTab.SABLON -> SablonTabContent(viewModel, state)
                                        DrawerTab.GOLGE -> GolgeTabContent(viewModel, state)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════
//  MOTOR SEKMESİ — Arka Plan + Özne motor seçicileri
// ═══════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MotorTabContent(viewModel: StudioViewModel, state: StudioUiState.Active) {
    // Katman seçimi
    Text("HEDEF KATMAN", fontSize = 10.sp, color = OnSurfaceMutedText, letterSpacing = 1.sp)
    Spacer(Modifier.height(6.dp))
    val targetOptions = listOf("Tümü", "Özne", "Arka")
    val selectedTargetIndex = when (state.currentConfig.targetLayer) {
        com.ditherlab.ultra.data.model.TargetLayer.ALL -> 0
        com.ditherlab.ultra.data.model.TargetLayer.SUBJECT -> 1
        com.ditherlab.ultra.data.model.TargetLayer.BACKGROUND -> 2
    }
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        targetOptions.forEachIndexed { index, label ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = targetOptions.size),
                onClick = {
                    val t = when (index) {
                        0 -> com.ditherlab.ultra.data.model.TargetLayer.ALL
                        1 -> com.ditherlab.ultra.data.model.TargetLayer.SUBJECT
                        else -> com.ditherlab.ultra.data.model.TargetLayer.BACKGROUND
                    }
                    viewModel.updateConfig { it.copy(targetLayer = t) }
                },
                selected = index == selectedTargetIndex,
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MatteOrange,
                    inactiveContainerColor = SurfaceDark
                )
            ) { Text(label, color = if (index == selectedTargetIndex) DeepCanvasBlack else Color.LightGray, fontSize = 11.sp) }
        }
    }

    Spacer(Modifier.height(12.dp))

    // Arka Plan Motoru
    Text("ARKA PLAN MOTORU", fontSize = 10.sp, color = OnSurfaceMutedText, letterSpacing = 1.sp)
    Spacer(Modifier.height(4.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        viewModel.availableEngines.forEachIndexed { index, engine ->
            val isSelected = state.backgroundEngineIndex == index
            Surface(
                modifier = Modifier.clickable { viewModel.setBackgroundEngine(index) },
                color = if (isSelected) MatteOrange.copy(alpha = 0.2f) else SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) MatteOrange else SurfaceVariantDark),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    engine.engineName,
                    color = if (isSelected) MatteOrange else Color.LightGray,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    // Özne Motoru
    Text("ÖZNE MOTORU", fontSize = 10.sp, color = OnSurfaceMutedText, letterSpacing = 1.sp)
    Spacer(Modifier.height(4.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        viewModel.availableEngines.forEachIndexed { index, engine ->
            val isSelected = state.foregroundEngineIndex == index
            Surface(
                modifier = Modifier.clickable { viewModel.setForegroundEngine(index) },
                color = if (isSelected) PrimaryMatteGreen.copy(alpha = 0.2f) else SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) PrimaryMatteGreen else SurfaceVariantDark),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    engine.engineName,
                    color = if (isSelected) PrimaryMatteGreen else Color.LightGray,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    // Fırça modu
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Fırça Modu", color = Color.White, fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = state.currentConfig.isBrushModeActive,
                onCheckedChange = { viewModel.toggleBrushMode() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = DeepCanvasBlack,
                    checkedTrackColor = MatteOrange
                ),
                modifier = Modifier.height(24.dp)
            )
        }
        if (state.currentConfig.brushPaths.isNotEmpty()) {
            TextButton(
                onClick = { viewModel.clearBrushPaths() },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Sil", color = WarningMutedRust, fontSize = 12.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════
//  AYAR SEKMESİ — Seçili motorun ince ayar barları
// ═══════════════════════════════════════════════════
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AyarTabContent(viewModel: StudioViewModel, state: StudioUiState.Active) {
    if (state.backgroundEngineIndex == state.foregroundEngineIndex) {
        val engine = viewModel.availableEngines.getOrNull(state.backgroundEngineIndex)
        Text("${engine?.engineName?.uppercase() ?: "MOTOR"} AYARLARI", fontSize = 10.sp, color = OnSurfaceMutedText, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        EngineSettings(state.backgroundEngineIndex, viewModel, state)
    } else {
        val bgEngine = viewModel.availableEngines.getOrNull(state.backgroundEngineIndex)
        Text("ARKA PLAN: ${bgEngine?.engineName?.uppercase() ?: "MOTOR"} AYARLARI", fontSize = 10.sp, color = OnSurfaceMutedText, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        EngineSettings(state.backgroundEngineIndex, viewModel, state)

        Spacer(Modifier.height(16.dp))
        androidx.compose.material3.HorizontalDivider(color = SurfaceVariantDark)
        Spacer(Modifier.height(16.dp))

        val fgEngine = viewModel.availableEngines.getOrNull(state.foregroundEngineIndex)
        Text("ÖZNE: ${fgEngine?.engineName?.uppercase() ?: "MOTOR"} AYARLARI", fontSize = 10.sp, color = OnSurfaceMutedText, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        EngineSettings(state.foregroundEngineIndex, viewModel, state)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EngineSettings(engineIndex: Int, viewModel: StudioViewModel, state: StudioUiState.Active) {
    val engine = viewModel.availableEngines.getOrNull(engineIndex)
    val engineName = engine?.javaClass?.simpleName ?: ""
    when (engineName) {
        "PixelArtEngineKotlin", "PixelArtGpuEngine" -> { // PixelArt
            val palettes = listOf("gameboy" to "Gameboy", "gb-pocket" to "GB Pocket", "cga" to "CGA", "c64" to "C64", "cyberpunk" to "Cyberpunk", "vaporwave" to "Vapor")
            Text("Renk Paleti", color = Color.White, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                palettes.forEach { (key, label) ->
                    val sel = state.currentConfig.paletteKey == key
                    Surface(
                        modifier = Modifier.clickable { viewModel.updateConfig { it.copy(paletteKey = key) } },
                        color = if (sel) MatteOrange else SurfaceDark,
                        shape = RoundedCornerShape(16.dp)
                    ) { Text(label, color = if (sel) DeepCanvasBlack else Color.LightGray, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            SliderRow("Piksel Boyutu", "${state.currentConfig.pixelSize.toInt()}px",
                value = (state.currentConfig.pixelSize - 32f) / (256f - 32f),
                onValueChange = { v -> viewModel.updateConfig { it.copy(pixelSize = 32f + v * (256f - 32f)) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
        }
        "GlitchEngineKotlin", "GlitchGpuEngine" -> { // Glitch
            SliderRow("Glitch Yoğunluğu", "${state.currentConfig.glitchIntensity.toInt()}%",
                value = (state.currentConfig.glitchIntensity - 5f) / (100f - 5f),
                onValueChange = { v -> viewModel.updateConfig { it.copy(glitchIntensity = 5f + v * (100f - 5f)) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
            CheckboxRow("Jiroskop Paralaks", state.currentConfig.gyroEffect) { v -> viewModel.updateConfig { it.copy(gyroEffect = v) } }
        }
        "VanGoghEngineKotlin", "VanGoghShaderEngine" -> { // VanGogh
            ModeSelector("Fırça Akış", listOf("Paralel", "Girdap", "Radyal"), state.currentConfig.vangoghMode - 1) { i -> viewModel.updateConfig { it.copy(vangoghMode = i + 1) } }
            SliderRow("Yoğunluk", "${state.currentConfig.vangoghStepSize.toInt()}",
                value = (state.currentConfig.vangoghStepSize - 6f) / (24f - 6f),
                onValueChange = { v -> viewModel.updateConfig { it.copy(vangoghStepSize = 6f + v * (24f - 6f)) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
            SliderRow("Min Fırça", "${state.currentConfig.vangoghMinLength.toInt()}px",
                value = (state.currentConfig.vangoghMinLength - 5f) / (20f - 5f),
                onValueChange = { v -> viewModel.updateConfig { it.copy(vangoghMinLength = 5f + v * (20f - 5f)) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
            SliderRow("Max Fırça", "${state.currentConfig.vangoghMaxLength.toInt()}px",
                value = (state.currentConfig.vangoghMaxLength - 15f) / (50f - 15f),
                onValueChange = { v -> viewModel.updateConfig { it.copy(vangoghMaxLength = 15f + v * (50f - 15f)) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
            CheckboxRow("3D Boya Kabartma", state.currentConfig.vangoghImpasto) { v -> viewModel.updateConfig { it.copy(vangoghImpasto = v) } }
        }
        "MinecraftEngineKotlin" -> { // Minecraft
            SliderRow("Blok Boyutu", "${state.currentConfig.minecraftBlockSize.toInt()}px",
                value = (state.currentConfig.minecraftBlockSize - 8f) / (48f - 8f),
                onValueChange = { v -> viewModel.updateConfig { it.copy(minecraftBlockSize = 8f + v * (48f - 8f)) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
        }
        "PostcardEngineKotlin" -> { // Postcard
            ModeSelector("Doku Modu", listOf("Halftone", "Banknot"), if (state.currentConfig.postcardMode == "engraving") 1 else 0) { i ->
                viewModel.updateConfig { it.copy(postcardMode = if (i == 1) "engraving" else "halftone_paper") }
            }
            SliderRow("Pul Kenarı", "${state.currentConfig.postcardStampMargin.toInt()}px",
                value = (state.currentConfig.postcardStampMargin - 12f) / (60f - 12f),
                onValueChange = { v -> viewModel.updateConfig { it.copy(postcardStampMargin = 12f + v * (60f - 12f)) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
        }
        "ThermalPaperEngineKotlin" -> { // ThermalPaper
            ModeSelector("Kağıt Tipi", listOf("Eski", "Yeni"), if (state.currentConfig.thermalPaperType == "fresh") 1 else 0) { i ->
                viewModel.updateConfig { it.copy(thermalPaperType = if (i == 1) "fresh" else "aged") }
            }
            SliderRow("Aşınma", "${state.currentConfig.thermalWear.toInt()}%",
                value = state.currentConfig.thermalWear / 100f,
                onValueChange = { v -> viewModel.updateConfig { it.copy(thermalWear = v * 100f) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
            CheckboxRow("Yırtık Kenar", state.currentConfig.thermalTornEdge) { v -> viewModel.updateConfig { it.copy(thermalTornEdge = v) } }
        }
        "NoirComicEngineKotlin" -> {
            ModeSelector("Renk Modu", listOf("Noir B&W", "Spider-Red", "Canlı"), state.currentConfig.noirColorMode) { i ->
                viewModel.updateConfig { it.copy(noirColorMode = i) }
            }
            ModeSelector("Nokta Rengi", listOf("Siyah", "Kırmızı", "Lacivert"), when(state.currentConfig.noirDotColor) { "red" -> 1; "navy" -> 2; else -> 0 }) { i ->
                viewModel.updateConfig { it.copy(noirDotColor = when(i) { 1 -> "red"; 2 -> "navy"; else -> "black" }) }
            }
            SliderRow("Nokta Boyutu", "${state.currentConfig.noirDotSize.toInt()}px",
                value = (state.currentConfig.noirDotSize - 2f) / (20f - 2f),
                onValueChange = { v -> viewModel.updateConfig { it.copy(noirDotSize = 2f + v * (20f - 2f)) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
            SliderRow("Kontrast", String.format("%.1f", state.currentConfig.noirContrast),
                value = (state.currentConfig.noirContrast - 0.5f) / (5.0f - 0.5f),
                onValueChange = { v -> viewModel.updateConfig { it.copy(noirContrast = 0.5f + v * (5.0f - 0.5f)) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
            SliderRow("Gazete Dokusu", "${(state.currentConfig.noirTextureDensity * 100).toInt()}%",
                value = state.currentConfig.noirTextureDensity,
                onValueChange = { v -> viewModel.updateConfig { it.copy(noirTextureDensity = v) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
        }
        "AsciiMatrixEngineKotlin", "AsciiArtEngineKotlin" -> { // AsciiMatrix
            ModeSelector("Karakter Seti", listOf("Klasik", "Binary", "Hex"),
                when (state.currentConfig.asciiCharSetKey) { "binary" -> 1; "hex" -> 2; else -> 0 }
            ) { i -> viewModel.updateConfig { it.copy(asciiCharSetKey = when(i) { 1 -> "binary"; 2 -> "hex"; else -> "density" }) } }
            ModeSelector("Renk Modu", listOf("Matrix", "Amber", "Renkli"),
                when (state.currentConfig.asciiColorMode) { "amber" -> 1; "fullcolor" -> 2; else -> 0 }
            ) { i -> viewModel.updateConfig { it.copy(asciiColorMode = when(i) { 1 -> "amber"; 2 -> "fullcolor"; else -> "matrix" }) } }
            SliderRow("Yazı Boyutu", "${state.currentConfig.asciiFontSize.toInt()}px",
                value = (state.currentConfig.asciiFontSize - 6f) / (32f - 6f),
                onValueChange = { v -> viewModel.updateConfig { it.copy(asciiFontSize = 6f + v * (32f - 6f)) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
        }
        "CrtTvEngineKotlin", "CrtTvGpuEngine", "CrtTvShaderEngine" -> { // CrtTv
            SliderRow("Tarama Çizgisi", "${state.currentConfig.crtScanlineGap.toInt()}px",
                value = (state.currentConfig.crtScanlineGap - 1f) / (10f - 1f),
                onValueChange = { v -> viewModel.updateConfig { it.copy(crtScanlineGap = 1f + v * (10f - 1f)) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
            CheckboxRow("Fosfor Parlaması", state.currentConfig.crtPhosphorGlow) { v -> viewModel.updateConfig { it.copy(crtPhosphorGlow = v) } }
        }
        "FlirThermalEngineKotlin" -> { // FlirThermal
            ModeSelector("Termal Palet", listOf("Ironbow", "Gökkuşağı", "Beyaz"),
                when (state.currentConfig.flirMode) { "rainbow" -> 1; "whitehot" -> 2; else -> 0 }
            ) { i -> viewModel.updateConfig { it.copy(flirMode = when(i) { 1 -> "rainbow"; 2 -> "whitehot"; else -> "ironbow" }) } }
        }
        "VanGoghBetaEngineKotlin" -> { // VanGoghBeta
            ModeSelector("Fırça Akış", listOf("Paralel", "Girdap", "Radyal"), state.currentConfig.vangoghMode - 1) { i -> viewModel.updateConfig { it.copy(vangoghMode = i + 1) } }
            SliderRow("Fırça Boyutu", "${state.currentConfig.vangoghBetaBrushSize.toInt()}",
                value = (state.currentConfig.vangoghBetaBrushSize - 2f) / (50f - 2f),
                onValueChange = { v -> viewModel.updateConfig { it.copy(vangoghBetaBrushSize = 2f + v * (50f - 2f)) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
            SliderRow("Yoğunluk", "${(state.currentConfig.vangoghBetaIntensity * 100).toInt()}%",
                value = (state.currentConfig.vangoghBetaIntensity - 0.1f) / (1f - 0.1f),
                onValueChange = { v -> viewModel.updateConfig { it.copy(vangoghBetaIntensity = 0.1f + v * (1f - 0.1f)) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
        }
        "CmykOffsetEngineKotlin" -> { // CmykOffset
            SliderRow("Kayma", "${state.currentConfig.cmykOffsetPx.toInt()}px",
                value = state.currentConfig.cmykOffsetPx / 20f,
                onValueChange = { v -> viewModel.updateConfig { it.copy(cmykOffsetPx = v * 20f) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
            SliderRow("Nokta Boyutu", "${state.currentConfig.cmykDotSize.toInt()}px",
                value = (state.currentConfig.cmykDotSize - 3f) / (32f - 3f),
                onValueChange = { v -> viewModel.updateConfig { it.copy(cmykDotSize = 3f + v * (32f - 3f)) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
        }
        "PunkFanzineEngineKotlin" -> { // PunkFanzine
            SliderRow("Kontrast", "${state.currentConfig.punkContrastBoost}",
                value = (state.currentConfig.punkContrastBoost - 1f) / (5f - 1f),
                onValueChange = { v -> viewModel.updateConfig { it.copy(punkContrastBoost = 1f + v * (5f - 1f)) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
            SliderRow("Toner Gürültüsü", "${state.currentConfig.punkTonerNoise.toInt()}",
                value = state.currentConfig.punkTonerNoise / 100f,
                onValueChange = { v -> viewModel.updateConfig { it.copy(punkTonerNoise = v * 100f) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
        }
        "ColorClashEngineKotlin" -> { // ColorClash
            SliderRow("Blok Boyutu", "${state.currentConfig.colorClashBlockSize.toInt()}px",
                value = (state.currentConfig.colorClashBlockSize - 4f) / (64f - 4f),
                onValueChange = { v -> viewModel.updateConfig { it.copy(colorClashBlockSize = 4f + v * (64f - 4f)) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
        }
        "TextGlitchEngineKotlin" -> { // TextGlitch
            SliderRow("Yazı Boyutu", "${state.currentConfig.textGlitchFontSize.toInt()}px",
                value = (state.currentConfig.textGlitchFontSize - 8f) / (120f - 8f),
                onValueChange = { v -> viewModel.updateConfig { it.copy(textGlitchFontSize = 8f + v * (120f - 8f)) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
            ModeSelector("Glitch Stili", listOf("VHS", "RGB Kayması", "Damga"),
                when (state.currentConfig.textGlitchStyle) { "rgb_shift" -> 1; "stamp" -> 2; else -> 0 }
            ) { i -> viewModel.updateConfig { it.copy(textGlitchStyle = when(i) { 1 -> "rgb_shift"; 2 -> "stamp"; else -> "vhs" }) } }
        }
        "SensorCorruptEngineKotlin" -> { // SensorCorrupt
            ModeSelector("Mod", listOf("Kaos", "Standart"), if (state.currentConfig.sensorCorruptMode == "standard") 1 else 0) { i ->
                viewModel.updateConfig { it.copy(sensorCorruptMode = if (i == 1) "standard" else "chaos") }
            }
            SliderRow("Gürültü", "${(state.currentConfig.sensorNoiseIntensity * 100).toInt()}%",
                value = state.currentConfig.sensorNoiseIntensity,
                onValueChange = { v -> viewModel.updateConfig { it.copy(sensorNoiseIntensity = v) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
            SliderRow("Kaos", "${(state.currentConfig.sensorChaosLevel * 100).toInt()}%",
                value = state.currentConfig.sensorChaosLevel,
                onValueChange = { v -> viewModel.updateConfig { it.copy(sensorChaosLevel = v) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
            CheckboxRow("Satır Titremesi", state.currentConfig.sensorLineJitter) { v -> viewModel.updateConfig { it.copy(sensorLineJitter = v) } }
            CheckboxRow("Bit Kayması", state.currentConfig.sensorBitShift) { v -> viewModel.updateConfig { it.copy(sensorBitShift = v) } }
        }
        "ChromaticAberrationEngineKotlin" -> { // ChromaticAberration
            SliderRow("Kromatik Sapma", "${state.currentConfig.chromaticAberrationAmount.toInt()}",
                value = state.currentConfig.chromaticAberrationAmount / 30f,
                onValueChange = { v -> viewModel.updateConfig { it.copy(chromaticAberrationAmount = v * 30f) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
        }
        "SwirlyBokehEngineKotlin" -> { // SwirlyBokeh
            SliderRow("Girdap Yoğunluğu", "${state.currentConfig.swirlyBokehIntensity.toInt()}",
                value = state.currentConfig.swirlyBokehIntensity / 100f,
                onValueChange = { v -> viewModel.updateConfig { it.copy(swirlyBokehIntensity = v * 100f) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
        }
        "DarkroomEngineKotlin" -> { // Darkroom
            val presets = listOf("sb" to "SB", "analog" to "Analog", "reze" to "Reze", "vampir" to "Vampir", "gotik" to "Gotik", "nordic" to "Nordic", "cinestill" to "Cinestill")
            Text("Preset", color = Color.White, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                presets.forEach { (key, label) ->
                    val sel = state.currentConfig.darkroomPreset == key
                    Surface(
                        modifier = Modifier.clickable { viewModel.updateConfig { it.copy(darkroomPreset = key) } },
                        color = if (sel) MatteOrange else SurfaceDark,
                        shape = RoundedCornerShape(16.dp)
                    ) { Text(label, color = if (sel) DeepCanvasBlack else Color.LightGray, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            SliderRow("Yoğunluk", "${(state.currentConfig.darkroomIntensity * 100).toInt()}%",
                value = state.currentConfig.darkroomIntensity,
                onValueChange = { v -> viewModel.updateConfig { it.copy(darkroomIntensity = v) } },
                onDraggingChanged = { viewModel.setSliderDragging(it) }
            )
        }
        else -> {
            Text("Bu motor için ayar bulunmuyor.", color = OnSurfaceMutedText, fontSize = 11.sp)
        }
    }
}

// ═══════════════════════════════════════════════════
//  ŞABLON SEKMESİ — Kadraj'dan gelen 21 kırpma şekli
// ═══════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SablonTabContent(viewModel: StudioViewModel, state: StudioUiState.Active) {
    Text("KIRPMA ŞEKLİ", fontSize = 10.sp, color = OnSurfaceMutedText, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ALL_SHAPES.forEach { shape ->
            val isSelected = state.maskShape == shape.id
            Surface(
                modifier = Modifier.clickable { viewModel.setMaskShape(shape.id) },
                color = if (isSelected) PrimaryMatteGreen.copy(alpha = 0.2f) else SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) PrimaryMatteGreen else SurfaceVariantDark
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "${shape.emoji} ${shape.label}",
                    color = if (isSelected) PrimaryMatteGreen else Color.LightGray,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    // Şeffaflık seçeneği
    CheckboxRow("Şekil Dışı Şeffaf", state.currentConfig.maskIsTransparent) { v ->
        viewModel.updateConfig { it.copy(maskIsTransparent = v) }
    }
    
    Spacer(Modifier.height(12.dp))
    
    // Şekil Hedef Katman
    Text("ŞEKİL HEDEFİ (POP-OUT EFEKTİ)", fontSize = 10.sp, color = OnSurfaceMutedText, letterSpacing = 1.sp)
    Spacer(Modifier.height(6.dp))
    val targetOptions = listOf("Tümü", "Özne", "Arka")
    val selectedTargetIndex = when (state.currentConfig.shapeTargetLayer) {
        com.ditherlab.ultra.data.model.TargetLayer.ALL -> 0
        com.ditherlab.ultra.data.model.TargetLayer.SUBJECT -> 1
        com.ditherlab.ultra.data.model.TargetLayer.BACKGROUND -> 2
    }
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        targetOptions.forEachIndexed { index, label ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = targetOptions.size),
                onClick = {
                    val t = when (index) {
                        0 -> com.ditherlab.ultra.data.model.TargetLayer.ALL
                        1 -> com.ditherlab.ultra.data.model.TargetLayer.SUBJECT
                        else -> com.ditherlab.ultra.data.model.TargetLayer.BACKGROUND
                    }
                    viewModel.updateConfig { it.copy(shapeTargetLayer = t) }
                },
                selected = index == selectedTargetIndex,
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MatteOrange,
                    inactiveContainerColor = SurfaceDark
                )
            ) { Text(label, color = if (index == selectedTargetIndex) DeepCanvasBlack else Color.LightGray, fontSize = 11.sp) }
        }
    }
}

// ═══════════════════════════════════════════════════
//  GÖLGE SEKMESİ — Gölge kontrolleri (placeholder)
// ═══════════════════════════════════════════════════
@Composable
private fun GolgeTabContent(viewModel: StudioViewModel, state: StudioUiState.Active) {
    Text("GÖLGE AYARLARI", fontSize = 10.sp, color = OnSurfaceMutedText, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))
    Text("Gölge kontrolleri henüz aktif değil.", color = OnSurfaceMutedText, fontSize = 11.sp)
    Text("(Kadraj reposundaki gölge sistemi yakında eklenecek)", color = OnSurfaceMutedText, fontSize = 9.sp)
}

// ═══════════════════════════════════════════════════
//  YARDIMCI BİLEŞENLER
// ═══════════════════════════════════════════════════

@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onDraggingChanged: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.White, fontSize = 11.sp)
            Text(valueText, color = MatteOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        MatteSlider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            onDraggingChanged = onDraggingChanged
        )
    }
}

@Composable
private fun CheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = MatteOrange)
        )
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun ModeSelector(label: String, options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Text(label, color = Color.White, fontSize = 11.sp)
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { i, title ->
            val selected = i == selectedIndex
            Button(
                onClick = { onSelect(i) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) MatteOrange else SurfaceDark
                ),
                modifier = Modifier.height(30.dp),
                contentPadding = PaddingValues(horizontal = 10.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(title, color = if (selected) DeepCanvasBlack else Color.White, fontSize = 10.sp)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}
