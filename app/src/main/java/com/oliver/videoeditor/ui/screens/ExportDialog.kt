package com.oliver.videoeditor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.oliver.videoeditor.data.Project
import com.oliver.videoeditor.export.Exporter
import com.oliver.videoeditor.viewmodel.EditorViewModel
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(vm: EditorViewModel, p: Project, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var height by remember { mutableIntStateOf(720) }; var fps by remember { mutableIntStateOf(30) }
    var running by remember { mutableStateOf(false) }; var progress by remember { mutableFloatStateOf(0f) }
    var result by remember { mutableStateOf<String?>(null) }; var error by remember { mutableStateOf<String?>(null) }
    var tr by remember { mutableStateOf<Transformer?>(null) }
    LaunchedEffect(running) {
        val holder = ProgressHolder()
        while (running) {
            tr?.let { if (it.getProgress(holder) != Transformer.PROGRESS_STATE_NOT_STARTED) progress = holder.progress / 100f }
            delay(300)
        }
    }
    DisposableEffect(Unit) { onDispose { if (running) tr?.cancel() } }
    AlertDialog(onDismissRequest = { if (!running) onClose() },
        title = { Text("Export MP4") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Resolution")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(720, 1080).forEach { h -> FilterChip(height == h, { if (!running) height = h }, { Text("${h}p") }) } }
            Text("Frame rate")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(30, 60).forEach { f -> FilterChip(fps == f, { if (!running) fps = f }, { Text("$f FPS") }) } }
            if (running) { LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth()); Text("${(progress * 100).toInt()}%") }
            result?.let { Text("Saved to Movies/OLIVER") }
            error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = {
            if (result == null) Button(enabled = !running, onClick = {
                running = true; error = null; progress = 0f
                try {
                    tr = Exporter.start(ctx, p, height, fps, vm.watermark,
                        onDone = { running = false; result = it.toString(); vm.markExported() },
                        onError = { running = false; error = it })
                } catch (e: Exception) { running = false; error = e.message ?: "Could not start export" }
            }) { Text(if (error != null) "Retry" else "Start export") }
            else TextButton(onClose) { Text("Done") }
        },
        dismissButton = { TextButton({ if (running) { tr?.cancel(); running = false }; onClose() }) { Text(if (running) "Cancel" else "Close") } })
}
