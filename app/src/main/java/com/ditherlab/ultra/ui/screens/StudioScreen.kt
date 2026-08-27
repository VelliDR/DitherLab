package com.ditherlab.ultra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ditherlab.ultra.data.model.StudioUiState
import com.ditherlab.ultra.ui.components.CanvasViewport
import com.ditherlab.ultra.ui.components.MatteSlider
import com.ditherlab.ultra.ui.theme.DeepCanvasBlack
import com.ditherlab.ultra.ui.theme.PrimaryLightSage
import com.ditherlab.ultra.ui.theme.PrimaryMatteGreen
import com.ditherlab.ultra.ui.theme.SurfaceDark
import com.ditherlab.ultra.ui.theme.SurfaceVariantDark
import com.ditherlab.ultra.ui.viewmodel.StudioViewModel

fun Modifier.simpleVerticalScrollbar(
    state: ScrollState,
    width: androidx.compose.ui.unit.Dp = 4.dp,
    color: Color = Color.Gray.copy(alpha = 0.5f)
): Modifier = this.drawWithContent {
    drawContent()
    val totalHeight = state.maxValue + size.height
    if (totalHeight > size.height && size.height > 0) {
        val scrollbarHeight = (size.height / totalHeight) * size.height
        val scrollbarY = (state.value / totalHeight.toFloat()) * size.height
        drawRect(
            color = color,
            topLeft = Offset(size.width - width.toPx(), scrollbarY),
            size = Size(width.toPx(), scrollbarHeight)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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

    fun getEngineEmoji(name: String): String {
        return when {
            name.contains("Pixel", ignoreCase = true) -> "👾"
            name.contains("Glitch", ignoreCase = true) -> "🌀"
            name.contains("Gogh", ignoreCase = true) -> "🎨"
            name.contains("Minecraft", ignoreCase = true) -> "🎮"
            name.contains("Postcard", ignoreCase = true) -> "📮"
            else -> "⚙️"
        }
    }

    Scaffold(
        containerColor = DeepCanvasBlack,
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = PrimaryMatteGreen,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("v2.08", color = DeepCanvasBlack, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Column {
                            Text("Koca Bir Saçmalık", color = PrimaryLightSage, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            Text("Hitorie - One Me Two Heart", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepCanvasBlack.copy(alpha = 0.95f),
                    titleContentColor = PrimaryMatteGreen
                ),
                actions = {
                    if (uiState is StudioUiState.Active) {
                        val activeState = uiState as StudioUiState.Active
                        TextButton(onClick = { pickImage.launch("image/*") }) {
                            Text("YENİ", color = PrimaryMatteGreen)
                        }
                        if (activeState.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 16.dp).size(24.dp),
                                color = PrimaryMatteGreen
                            )
                        } else {
                            TextButton(onClick = { viewModel.exportImage(context) }) {
                                Text("İNDİR", color = PrimaryMatteGreen)
                            }
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
                    Text(text = "Loading Laboratory...", color = PrimaryMatteGreen, modifier = Modifier.align(Alignment.Center))
                }
                is StudioUiState.Error -> {
                    Text(text = "Error: ${state.message}", color = Color.Red, modifier = Modifier.align(Alignment.Center))
                }
                is StudioUiState.Active -> {
                    Column(Modifier.fillMaxSize()) {
                        
                        // Üst kısım: Viewport Önizleme (Esnek Yükseklik)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceDark)
                        ) {
                            if (state.originalImage == null) {
                                Column(
                                    modifier = Modifier.fillMaxSize().clickable { pickImage.launch("image/*") },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("📷", fontSize = 48.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Fotoğraf Yüklemek İçin Dokunun", color = Color.White, fontSize = 14.sp)
                                    Text("JPG, PNG, WEBP desteklenir", color = Color.Gray, fontSize = 10.sp)
                                }
                            } else {
                                CanvasViewport(
                                    uiState = state,
                                    onBrushPointAdded = { pt, isNew -> viewModel.addBrushPoint(pt, isNew) },
                                    onBrushPathFinished = { viewModel.finishBrushPath() }
                                )
                            }
                        }
                        
                        // Alt kısım: Kontroller (Kaydırılabilir)
                        if (state.originalImage != null) {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f) // Limit height so it doesn't squish the canvas
                                    .simpleVerticalScrollbar(scrollState, color = PrimaryMatteGreen.copy(alpha = 0.5f))
                                    .verticalScroll(scrollState)
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp)
                            ) {
                                Text(
                                    text = "EFEKT MOTORU",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                // Motor Seçici (Izgara/Flow)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    viewModel.availableEngines.forEachIndexed { index, engine ->
                                        val isSelected = state.selectedEngineIndex == index
                                        Surface(
                                            modifier = Modifier
                                                .clickable { viewModel.setEngine(index) },
                                            color = if (isSelected) PrimaryMatteGreen.copy(alpha = 0.15f) else SurfaceDark,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) PrimaryMatteGreen else SurfaceVariantDark),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(getEngineEmoji(engine.engineName) + " ", fontSize = 12.sp)
                                                Text(
                                                    text = engine.engineName,
                                                    color = if (isSelected) PrimaryMatteGreen else Color.LightGray,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Parametre Kartı
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceVariantDark),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        // Katman Seçimi
                                        val targetOptions = listOf("Tümü", "Özne", "Arka")
                                        val selectedTargetIndex = when (state.currentConfig.targetLayer) {
                                            com.ditherlab.ultra.data.model.TargetLayer.ALL -> 0
                                            com.ditherlab.ultra.data.model.TargetLayer.SUBJECT -> 1
                                            com.ditherlab.ultra.data.model.TargetLayer.BACKGROUND -> 2
                                        }
                                        SingleChoiceSegmentedButtonRow(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        ) {
                                            targetOptions.forEachIndexed { index, label ->
                                                SegmentedButton(
                                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = targetOptions.size),
                                                    onClick = {
                                                        val newTarget = when (index) {
                                                            0 -> com.ditherlab.ultra.data.model.TargetLayer.ALL
                                                            1 -> com.ditherlab.ultra.data.model.TargetLayer.SUBJECT
                                                            else -> com.ditherlab.ultra.data.model.TargetLayer.BACKGROUND
                                                        }
                                                        viewModel.updateConfig { it.copy(targetLayer = newTarget) }
                                                    },
                                                    selected = index == selectedTargetIndex,
                                                    colors = SegmentedButtonDefaults.colors(
                                                        activeContainerColor = PrimaryMatteGreen,
                                                        inactiveContainerColor = DeepCanvasBlack
                                                    )
                                                ) {
                                                    Text(label, color = if (index == selectedTargetIndex) DeepCanvasBlack else Color.LightGray, fontSize = 12.sp)
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Fırça Modu", color = Color.White, fontSize = 14.sp)
                                                Spacer(Modifier.width(8.dp))
                                                Switch(
                                                    checked = state.currentConfig.isBrushModeActive,
                                                    onCheckedChange = { viewModel.toggleBrushMode() },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = DeepCanvasBlack,
                                                        checkedTrackColor = PrimaryMatteGreen
                                                    ),
                                                    modifier = Modifier.height(24.dp)
                                                )
                                            }
                                            
                                            if (state.currentConfig.brushPaths.isNotEmpty()) {
                                                TextButton(
                                                    onClick = { viewModel.clearBrushPaths() },
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("Sil", color = Color.Red, fontSize = 14.sp)
                                                }
                                            }
                                        }

                                        // Dinamik Ayarlar
                                        when (state.selectedEngineIndex) {
                                            0 -> { // PixelArt
                                                val palettes = listOf(
                                                    "gameboy" to "Gameboy",
                                                    "gb-pocket" to "GB Pocket",
                                                    "cga" to "CGA",
                                                    "c64" to "C64",
                                                    "cyberpunk" to "Cyberpunk",
                                                    "vaporwave" to "Vapor"
                                                )
                                                val selectedPaletteIndex = palettes.indexOfFirst { it.first == state.currentConfig.paletteKey }.takeIf { it >= 0 } ?: 0
                                                Text("Renk Paleti", color = Color.White, fontSize = 12.sp)
                                                
                                                // Bölünmüş 2 satırda göstermek için (sığması için)
                                                val rows = palettes.chunked(3)
                                                rows.forEachIndexed { rowIndex, rowPalettes ->
                                                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                                        rowPalettes.forEachIndexed { colIndex, (key, label) ->
                                                            val i = rowIndex * 3 + colIndex
                                                            SegmentedButton(
                                                                shape = SegmentedButtonDefaults.itemShape(index = colIndex, count = rowPalettes.size),
                                                                onClick = { viewModel.updateConfig { it.copy(paletteKey = key) } },
                                                                selected = i == selectedPaletteIndex,
                                                                colors = SegmentedButtonDefaults.colors(
                                                                    activeContainerColor = PrimaryMatteGreen,
                                                                    inactiveContainerColor = DeepCanvasBlack
                                                                )
                                                            ) { Text(label, color = if(i == selectedPaletteIndex) DeepCanvasBlack else Color.LightGray, fontSize = 10.sp) }
                                                        }
                                                    }
                                                }
                                                Text("Piksel Boyutu: ${state.currentConfig.pixelSize.toInt()}px", color = Color.White, fontSize = 12.sp)
                                                MatteSlider(
                                                    value = (state.currentConfig.pixelSize - 32f) / (256f - 32f),
                                                    onValueChange = { v -> viewModel.updateConfig { it.copy(pixelSize = 32f + v * (256f - 32f)) } }
                                                )
                                            }
                                            1 -> { // Glitch
                                                Text("Glitch Yoğunluğu: ${state.currentConfig.glitchIntensity.toInt()}%", color = Color.White, fontSize = 12.sp)
                                                MatteSlider(
                                                    value = (state.currentConfig.glitchIntensity - 5f) / (100f - 5f),
                                                    onValueChange = { v -> viewModel.updateConfig { it.copy(glitchIntensity = 5f + v * (100f - 5f)) } }
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    androidx.compose.material3.Checkbox(
                                                        checked = state.currentConfig.gyroEffect,
                                                        onCheckedChange = { v -> viewModel.updateConfig { it.copy(gyroEffect = v) } },
                                                        colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = PrimaryMatteGreen)
                                                    )
                                                    Text("Jiroskop Paralaks", color = Color.White, fontSize = 12.sp)
                                                }
                                            }
                                            2 -> { // VanGogh
                                                Text("Fırça Akış Modu", color = Color.White, fontSize = 12.sp)
                                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                    val modes = listOf("Paralel", "Girdap", "Radyal")
                                                    modes.forEachIndexed { i, title ->
                                                        val selected = state.currentConfig.vangoghMode == i + 1
                                                        Button(
                                                            onClick = { viewModel.updateConfig { it.copy(vangoghMode = i + 1) } },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = if (selected) PrimaryMatteGreen else DeepCanvasBlack
                                                            ),
                                                            modifier = Modifier.height(32.dp),
                                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                                        ) {
                                                            Text(title, color = if(selected) DeepCanvasBlack else Color.White, fontSize = 10.sp)
                                                        }
                                                    }
                                                }
                                                Spacer(Modifier.height(8.dp))
                                                Text("Yoğunluk: ${state.currentConfig.vangoghStepSize.toInt()}", color = Color.White, fontSize = 12.sp)
                                                MatteSlider(
                                                    value = (state.currentConfig.vangoghStepSize - 6f) / (24f - 6f),
                                                    onValueChange = { v -> viewModel.updateConfig { it.copy(vangoghStepSize = 6f + v * (24f - 6f)) } }
                                                )
                                                Text("Min Fırça: ${state.currentConfig.vangoghMinLength.toInt()}px", color = Color.White, fontSize = 12.sp)
                                                MatteSlider(
                                                    value = (state.currentConfig.vangoghMinLength - 5f) / (20f - 5f),
                                                    onValueChange = { v -> viewModel.updateConfig { it.copy(vangoghMinLength = 5f + v * (20f - 5f)) } }
                                                )
                                                Text("Max Fırça: ${state.currentConfig.vangoghMaxLength.toInt()}px", color = Color.White, fontSize = 12.sp)
                                                MatteSlider(
                                                    value = (state.currentConfig.vangoghMaxLength - 15f) / (50f - 15f),
                                                    onValueChange = { v -> viewModel.updateConfig { it.copy(vangoghMaxLength = 15f + v * (50f - 15f)) } }
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    androidx.compose.material3.Checkbox(
                                                        checked = state.currentConfig.vangoghImpasto,
                                                        onCheckedChange = { v -> viewModel.updateConfig { it.copy(vangoghImpasto = v) } },
                                                        colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = PrimaryMatteGreen)
                                                    )
                                                    Text("3D Boya Kabartma (Impasto)", color = Color.White, fontSize = 12.sp)
                                                }
                                            }
                                            3 -> { // Minecraft
                                                Text("Blok Boyutu: ${state.currentConfig.minecraftBlockSize.toInt()}px", color = Color.White, fontSize = 12.sp)
                                                MatteSlider(
                                                    value = (state.currentConfig.minecraftBlockSize - 8f) / (48f - 8f),
                                                    onValueChange = { v -> viewModel.updateConfig { it.copy(minecraftBlockSize = 8f + v * (48f - 8f)) } }
                                                )
                                            }
                                            4 -> { // Postcard
                                                Text("Doku Modu", color = Color.White, fontSize = 12.sp)
                                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                    val m1 = "halftone_paper"
                                                    val m2 = "engraving"
                                                    Button(
                                                        onClick = { viewModel.updateConfig { it.copy(postcardMode = m1) } },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (state.currentConfig.postcardMode == m1) PrimaryMatteGreen else DeepCanvasBlack
                                                        )
                                                    ) { Text("Halftone", color = if(state.currentConfig.postcardMode == m1) DeepCanvasBlack else Color.White, fontSize = 10.sp) }
                                                    Button(
                                                        onClick = { viewModel.updateConfig { it.copy(postcardMode = m2) } },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (state.currentConfig.postcardMode == m2) PrimaryMatteGreen else DeepCanvasBlack
                                                        )
                                                    ) { Text("Banknot", color = if(state.currentConfig.postcardMode == m2) DeepCanvasBlack else Color.White, fontSize = 10.sp) }
                                                }
                                                Spacer(Modifier.height(8.dp))
                                                Text("Pul Kenarı: ${state.currentConfig.postcardStampMargin.toInt()}px", color = Color.White, fontSize = 12.sp)
                                                MatteSlider(
                                                    value = (state.currentConfig.postcardStampMargin - 12f) / (60f - 12f),
                                                    onValueChange = { v -> viewModel.updateConfig { it.copy(postcardStampMargin = 12f + v * (60f - 12f)) } }
                                                )
                                            }
                                            5 -> { // ThermalPaper
                                                Text("Kağıt Tipi", color = Color.White, fontSize = 12.sp)
                                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                    val m1 = "aged"
                                                    val m2 = "fresh"
                                                    Button(
                                                        onClick = { viewModel.updateConfig { it.copy(thermalPaperType = m1) } },
                                                        colors = ButtonDefaults.buttonColors(containerColor = if (state.currentConfig.thermalPaperType == m1) PrimaryMatteGreen else DeepCanvasBlack)
                                                    ) { Text("Eski", color = if(state.currentConfig.thermalPaperType == m1) DeepCanvasBlack else Color.White, fontSize = 10.sp) }
                                                    Button(
                                                        onClick = { viewModel.updateConfig { it.copy(thermalPaperType = m2) } },
                                                        colors = ButtonDefaults.buttonColors(containerColor = if (state.currentConfig.thermalPaperType == m2) PrimaryMatteGreen else DeepCanvasBlack)
                                                    ) { Text("Yeni", color = if(state.currentConfig.thermalPaperType == m2) DeepCanvasBlack else Color.White, fontSize = 10.sp) }
                                                }
                                                Spacer(Modifier.height(8.dp))
                                                Text("Aşınma (Wear): ${state.currentConfig.thermalWear.toInt()}%", color = Color.White, fontSize = 12.sp)
                                                MatteSlider(
                                                    value = state.currentConfig.thermalWear / 100f,
                                                    onValueChange = { v -> viewModel.updateConfig { it.copy(thermalWear = v * 100f) } }
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    androidx.compose.material3.Checkbox(
                                                        checked = state.currentConfig.thermalTornEdge,
                                                        onCheckedChange = { v -> viewModel.updateConfig { it.copy(thermalTornEdge = v) } },
                                                        colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = PrimaryMatteGreen)
                                                    )
                                                    Text("Yırtık Kenar", color = Color.White, fontSize = 12.sp)
                                                }
                                            }
                                            6 -> { // AsciiMatrix
                                                Text("Karakter Seti", color = Color.White, fontSize = 12.sp)
                                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                    val sets = listOf("density" to "Klasik", "binary" to "Binary", "hex" to "Hex")
                                                    sets.forEach { (key, label) ->
                                                        val isSelected = state.currentConfig.asciiCharSetKey == key
                                                        Button(
                                                            onClick = { viewModel.updateConfig { it.copy(asciiCharSetKey = key) } },
                                                            colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) PrimaryMatteGreen else DeepCanvasBlack)
                                                        ) { Text(label, color = if(isSelected) DeepCanvasBlack else Color.White, fontSize = 10.sp) }
                                                    }
                                                }
                                                val isCustom = state.currentConfig.asciiCharSetKey !in listOf("density", "binary", "hex")
                                                androidx.compose.material3.OutlinedTextField(
                                                    value = if (isCustom) state.currentConfig.asciiCharSetKey else "",
                                                    onValueChange = { newVal ->
                                                        val finalVal = newVal.ifEmpty { "density" }
                                                        viewModel.updateConfig { it.copy(asciiCharSetKey = finalVal) }
                                                    },
                                                    label = { Text("Özel Metin", color = Color.LightGray, fontSize = 10.sp) },
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = PrimaryMatteGreen,
                                                        unfocusedBorderColor = Color.DarkGray,
                                                        focusedTextColor = Color.White,
                                                        unfocusedTextColor = Color.White,
                                                        cursorColor = PrimaryMatteGreen
                                                    ),
                                                    singleLine = true
                                                )
                                                Text("Renk Modu", color = Color.White, fontSize = 12.sp)
                                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                    val modes = listOf("matrix" to "Matrix", "amber" to "Amber", "fullcolor" to "Renkli")
                                                    modes.forEach { (key, label) ->
                                                        Button(
                                                            onClick = { viewModel.updateConfig { it.copy(asciiColorMode = key) } },
                                                            colors = ButtonDefaults.buttonColors(containerColor = if (state.currentConfig.asciiColorMode == key) PrimaryMatteGreen else DeepCanvasBlack)
                                                        ) { Text(label, color = if(state.currentConfig.asciiColorMode == key) DeepCanvasBlack else Color.White, fontSize = 10.sp) }
                                                    }
                                                }
                                                Text("Yazı Tipi Boyutu: ${state.currentConfig.asciiFontSize.toInt()}px", color = Color.White, fontSize = 12.sp)
                                                MatteSlider(
                                                    value = (state.currentConfig.asciiFontSize - 6f) / (32f - 6f),
                                                    onValueChange = { v -> viewModel.updateConfig { it.copy(asciiFontSize = 6f + v * (32f - 6f)) } }
                                                )
                                            }
                                            7 -> { // CrtTv
                                                Text("Tarama Çizgisi Boşluğu: ${state.currentConfig.crtScanlineGap.toInt()}px", color = Color.White, fontSize = 12.sp)
                                                MatteSlider(
                                                    value = (state.currentConfig.crtScanlineGap - 1f) / (10f - 1f),
                                                    onValueChange = { v -> viewModel.updateConfig { it.copy(crtScanlineGap = 1f + v * (10f - 1f)) } }
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    androidx.compose.material3.Checkbox(
                                                        checked = state.currentConfig.crtPhosphorGlow,
                                                        onCheckedChange = { v -> viewModel.updateConfig { it.copy(crtPhosphorGlow = v) } },
                                                        colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = PrimaryMatteGreen)
                                                    )
                                                    Text("Fosfor Parlaması", color = Color.White, fontSize = 12.sp)
                                                }
                                            }
                                            8 -> { // FlirThermal
                                                Text("Termal Palet", color = Color.White, fontSize = 12.sp)
                                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                    val modes = listOf("ironbow" to "Ironbow", "rainbow" to "Gökkuşağı", "whitehot" to "Beyaz Sıcak")
                                                    modes.forEach { (key, label) ->
                                                        Button(
                                                            onClick = { viewModel.updateConfig { it.copy(flirMode = key) } },
                                                            colors = ButtonDefaults.buttonColors(containerColor = if (state.currentConfig.flirMode == key) PrimaryMatteGreen else DeepCanvasBlack)
                                                        ) { Text(label, color = if(state.currentConfig.flirMode == key) DeepCanvasBlack else Color.White, fontSize = 10.sp) }
                                                    }
                                                }
                                            }
                                            9 -> { // VanGoghBeta
                                                Text("Fırça Akış Modu", color = Color.White, fontSize = 12.sp)
                                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                    val modes = listOf("Paralel", "Girdap", "Radyal")
                                                    modes.forEachIndexed { i, title ->
                                                        val selected = state.currentConfig.vangoghMode == i + 1
                                                        Button(
                                                            onClick = { viewModel.updateConfig { it.copy(vangoghMode = i + 1) } },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = if (selected) PrimaryMatteGreen else DeepCanvasBlack
                                                            ),
                                                            modifier = Modifier.height(32.dp),
                                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                                        ) {
                                                            Text(title, color = if(selected) DeepCanvasBlack else Color.White, fontSize = 10.sp)
                                                        }
                                                    }
                                                }
                                                Spacer(Modifier.height(8.dp))
                                                Text("Fırça Boyutu: ${state.currentConfig.vangoghBetaBrushSize.toInt()}", color = Color.White, fontSize = 12.sp)
                                                MatteSlider(
                                                    value = (state.currentConfig.vangoghBetaBrushSize - 2f) / (50f - 2f),
                                                    onValueChange = { v -> viewModel.updateConfig { it.copy(vangoghBetaBrushSize = 2f + v * (50f - 2f)) } }
                                                )
                                                Text("Yoğunluk: ${(state.currentConfig.vangoghBetaIntensity * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
                                                MatteSlider(
                                                    value = (state.currentConfig.vangoghBetaIntensity - 0.1f) / (1f - 0.1f),
                                                    onValueChange = { v -> viewModel.updateConfig { it.copy(vangoghBetaIntensity = 0.1f + v * (1f - 0.1f)) } }
                                                )
                                            }
                                            10 -> { // CmykOffset
                                                Text("Kayma (Offset): ${state.currentConfig.cmykOffsetPx.toInt()}px", color = Color.White, fontSize = 12.sp)
                                                MatteSlider(
                                                    value = (state.currentConfig.cmykOffsetPx - 0f) / (20f - 0f),
                                                    onValueChange = { v -> viewModel.updateConfig { it.copy(cmykOffsetPx = v * 20f) } }
                                                )
                                                Text("Nokta Boyutu: ${state.currentConfig.cmykDotSize.toInt()}px", color = Color.White, fontSize = 12.sp)
                                                MatteSlider(
                                                    value = (state.currentConfig.cmykDotSize - 3f) / (32f - 3f),
                                                    onValueChange = { v -> viewModel.updateConfig { it.copy(cmykDotSize = 3f + v * (32f - 3f)) } }
                                                )
                                            }
                                            11 -> { // PunkFanzine
                                                Text("Kontrast Artışı: ${state.currentConfig.punkContrastBoost}", color = Color.White, fontSize = 12.sp)
                                                MatteSlider(
                                                    value = (state.currentConfig.punkContrastBoost - 1f) / (5f - 1f),
                                                    onValueChange = { v -> viewModel.updateConfig { it.copy(punkContrastBoost = 1f + v * (5f - 1f)) } }
                                                )
                                                Text("Toner Gürültüsü: ${state.currentConfig.punkTonerNoise.toInt()}", color = Color.White, fontSize = 12.sp)
                                                MatteSlider(
                                                    value = (state.currentConfig.punkTonerNoise - 0f) / (100f - 0f),
                                                    onValueChange = { v -> viewModel.updateConfig { it.copy(punkTonerNoise = v * 100f) } }
                                                )
                                            }
                                            12 -> { // ColorClash
                                                Text("Blok Boyutu: ${state.currentConfig.colorClashBlockSize.toInt()}px", color = Color.White, fontSize = 12.sp)
                                                MatteSlider(
                                                    value = (state.currentConfig.colorClashBlockSize - 4f) / (64f - 4f),
                                                    onValueChange = { v -> viewModel.updateConfig { it.copy(colorClashBlockSize = 4f + v * (64f - 4f)) } }
                                                )
                                            }
                                            13 -> { // TextGlitch
                                                Text("Yazı Boyutu: ${state.currentConfig.textGlitchFontSize.toInt()}px", color = Color.White, fontSize = 12.sp)
                                                MatteSlider(
                                                    value = (state.currentConfig.textGlitchFontSize - 8f) / (120f - 8f),
                                                    onValueChange = { v -> viewModel.updateConfig { it.copy(textGlitchFontSize = 8f + v * (120f - 8f)) } }
                                                )
                                                Text("Glitch Stili", color = Color.White, fontSize = 12.sp)
                                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                    val styles = listOf("vhs" to "VHS", "rgb_shift" to "RGB Kayması", "stamp" to "Damga")
                                                    styles.forEach { (key, label) ->
                                                        Button(
                                                            onClick = { viewModel.updateConfig { it.copy(textGlitchStyle = key) } },
                                                            colors = ButtonDefaults.buttonColors(containerColor = if (state.currentConfig.textGlitchStyle == key) PrimaryMatteGreen else DeepCanvasBlack)
                                                        ) { Text(label, color = if(state.currentConfig.textGlitchStyle == key) DeepCanvasBlack else Color.White, fontSize = 10.sp) }
                                                    }
                                                }
                                            }
                                            14 -> { // SensorCorrupt
                                                Text("Mod", color = Color.White, fontSize = 12.sp)
                                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                    val modes = listOf("chaos" to "Kaos", "standard" to "Standart")
                                                    modes.forEach { (key, label) ->
                                                        Button(
                                                            onClick = { viewModel.updateConfig { it.copy(sensorCorruptMode = key) } },
                                                            colors = ButtonDefaults.buttonColors(containerColor = if (state.currentConfig.sensorCorruptMode == key) PrimaryMatteGreen else DeepCanvasBlack)
                                                        ) { Text(label, color = if(state.currentConfig.sensorCorruptMode == key) DeepCanvasBlack else Color.White, fontSize = 10.sp) }
                                                    }
                                                }
                                                Text("Gürültü Yoğunluğu: ${(state.currentConfig.sensorNoiseIntensity*100).toInt()}%", color = Color.White, fontSize = 12.sp)
                                                MatteSlider(
                                                    value = state.currentConfig.sensorNoiseIntensity,
                                                    onValueChange = { v -> viewModel.updateConfig { it.copy(sensorNoiseIntensity = v) } }
                                                )
                                                Text("Kaos Seviyesi: ${(state.currentConfig.sensorChaosLevel*100).toInt()}%", color = Color.White, fontSize = 12.sp)
                                                MatteSlider(
                                                    value = state.currentConfig.sensorChaosLevel,
                                                    onValueChange = { v -> viewModel.updateConfig { it.copy(sensorChaosLevel = v) } }
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    androidx.compose.material3.Checkbox(
                                                        checked = state.currentConfig.sensorLineJitter,
                                                        onCheckedChange = { v -> viewModel.updateConfig { it.copy(sensorLineJitter = v) } },
                                                        colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = PrimaryMatteGreen)
                                                    )
                                                    Text("Satır Titremesi (Jitter)", color = Color.White, fontSize = 12.sp)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    androidx.compose.material3.Checkbox(
                                                        checked = state.currentConfig.sensorBitShift,
                                                        onCheckedChange = { v -> viewModel.updateConfig { it.copy(sensorBitShift = v) } },
                                                        colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = PrimaryMatteGreen)
                                                    )
                                                    Text("Bit Kayması (Shift)", color = Color.White, fontSize = 12.sp)
                                                }
                                            }
                                            else -> {
                                                // Fallback
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { viewModel.toggleSplitView() },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepCanvasBlack),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryMatteGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text(if (state.isA_BSplitActive) "A/B Perdesini Kapat" else "A/B Perdesini Aç", color = PrimaryMatteGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
