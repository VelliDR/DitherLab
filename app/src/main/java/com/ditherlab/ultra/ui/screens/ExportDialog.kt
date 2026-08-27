package com.ditherlab.ultra.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExportReady: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dışa Aktar") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("İşleme arka plan iş parçacığında tam çözünürlükte yapılacaktır.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { onExportReady("PNG") }) { Text("Kayıpsız PNG") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onExportReady("WEBP") }) { Text("Animasyonlu WebP") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onExportReady("MP4") }) { Text("H.264 Video") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    )
}
