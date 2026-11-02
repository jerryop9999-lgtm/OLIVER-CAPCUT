package com.oliver.videoeditor.ui.screens

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.oliver.videoeditor.data.*
import com.oliver.videoeditor.media.MediaUtil
import com.oliver.videoeditor.viewmodel.EditorViewModel

private fun previewItem(c: Clip): MediaItem =
    if (c.image) MediaItem.Builder().setUri(c.uri).setImageDurationMs(c.endMs - c.startMs).build()
    else MediaItem.Builder().setUri(c.uri).setClippingConfiguration(
        MediaItem.ClippingConfiguration.Builder().setStartPositionMs(c.startMs).setEndPositionMs(c.endMs).build()).build()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: EditorViewModel) {
    val p = vm.project ?: return
    val ctx = LocalContext.current
    var tool by remember { mutableIntStateOf(0) }
    var exporting by remember { mutableStateOf(false) }
    val player = remember { ExoPlayer.Builder(ctx).build() }
    DisposableEffect(Unit) { onDispose { player.release() } }
    val sel = p.clips.getOrNull(vm.selected)
    LaunchedEffect(p.clips) {
        val idx = player.currentMediaItemIndex; val pos = player.currentPosition
        player.setMediaItems(p.clips.map(::previewItem)); player.prepare()
        if (idx < p.clips.size) player.seekTo(idx, pos)
    }
    LaunchedEffect(sel?.speed) { player.setPlaybackSpeed(sel?.speed ?: 1f) }

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val clips = uris.mapNotNull { MediaUtil.toClip(ctx, it) }
        if (clips.isNotEmpty()) vm.addClips(clips)
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { runCatching { ctx.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            vm.edit { pr -> pr.copy(music = it.toString()) } }
    }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton({ vm.close() }) { Text("Back") }
            Spacer(Modifier.weight(1f))
            TextButton({ vm.undo() }, enabled = vm.canUndo) { Text("↶ Undo") }
            TextButton({ vm.redo() }, enabled = vm.canRedo) { Text("Redo ↷") }
            Button({ exporting = true }, enabled = p.clips.isNotEmpty(), shape = RoundedCornerShape(50)) { Text("Export") }
        }
        // Preview
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            if (p.clips.isEmpty()) Text("Tap “+ Add” in the timeline to import videos or photos", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else {
                var m = Modifier.clip(RoundedCornerShape(12.dp)).fillMaxHeight()
                ASPECTS[p.aspect].second?.let { m = m.aspectRatio(it) }
                AndroidView(factory = { c -> (LayoutInflater.from(c).inflate(androidx.media3.ui.R.layout.exo_player_view, null) as PlayerView)
                    .also { it.player = player } }, modifier = m.rotate(sel?.rot?.toFloat() ?: 0f))
            }
        }
        // Timeline
        LazyRow(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(p.clips, key = { _, c -> c.id }) { i, c ->
                val w = (((c.endMs - c.startMs) / c.speed) / 60f).coerceIn(70f, 260f).dp
                Box(Modifier.size(w, 56.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(if (i == vm.selected) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                    .clickable { vm.selected = i; player.seekTo(i, 0) }.padding(6.dp)) {
                    Text((if (c.image) "IMG " else "VID ") + MediaUtil.fmt(((c.endMs - c.startMs) / c.speed).toLong()) +
                        (if (c.speed != 1f) "\n${c.speed}x" else ""), fontSize = 11.sp)
                }
            }
            item { Box(Modifier.size(70.dp, 56.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary)
                .clickable { mediaPicker.launch(arrayOf("video/*", "image/*")) }, contentAlignment = Alignment.Center) { Text("+ Add", color = MaterialTheme.colorScheme.onPrimary) } }
        }
        // Tool panel
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            Column(Modifier.padding(12.dp).heightIn(min = 150.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Clip", "Canvas", "Audio", "Text", "Filters").forEachIndexed { i, n -> FilterChip(tool == i, { tool = i }, { Text(n) }) }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                    when (tool) {
                        0 -> ClipTools(vm, sel, player)
                        1 -> Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ASPECTS.forEachIndexed { i, a -> FilterChip(p.aspect == i, { vm.edit { it.copy(aspect = i) } }, { Text(a.first) }) } }
                        2 -> AudioTools(vm, p, sel) { audioPicker.launch(arrayOf("audio/*")) }
                        3 -> TextTools(vm, p)
                        4 -> Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FILTERS.forEachIndexed { i, n -> FilterChip(p.filter == i, { vm.edit { it.copy(filter = i) } }, { Text(n) }) } }
                    }
                }
            }
        }
    }
    if (exporting) ExportDialog(vm, p) { exporting = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClipTools(vm: EditorViewModel, c: Clip?, player: ExoPlayer) {
    if (c == null) { Text("Select a clip in the timeline"); return }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton({ vm.split(player.currentPosition) }, enabled = !c.image) { Text("Split") }
        OutlinedButton({ vm.duplicate() }) { Text("Duplicate") }
        OutlinedButton({ vm.deleteClip() }) { Text("Delete") }
        OutlinedButton({ vm.updateClip { it.copy(rot = (it.rot + 90) % 360) } }) { Text("Rotate 90°") }
    }
    if (!c.image) {
        Text("Speed", fontSize = 12.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0.5f, 1f, 1.5f, 2f, 3f).forEach { s -> FilterChip(c.speed == s, { vm.updateClip { it.copy(speed = s) } }, { Text("${s}x") }) } }
    }
    var range by remember(c.id, c.startMs, c.endMs) { mutableStateOf(c.startMs.toFloat()..c.endMs.toFloat()) }
    Text(if (c.image) "Duration: ${MediaUtil.fmt((range.endInclusive - range.start).toLong())}" else
        "Trim: ${MediaUtil.fmt(range.start.toLong())} – ${MediaUtil.fmt(range.endInclusive.toLong())}", fontSize = 12.sp)
    RangeSlider(range, { range = it }, valueRange = 0f..(if (c.image) 10000f else c.srcDurMs.toFloat()),
        onValueChangeFinished = {
            if (range.endInclusive - range.start >= 300f) vm.updateClip { it.copy(startMs = range.start.toLong(), endMs = range.endInclusive.toLong()) }
            else range = c.startMs.toFloat()..c.endMs.toFloat()
        })
}

@Composable
private fun AudioTools(vm: EditorViewModel, p: Project, c: Clip?, pick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(pick) { Text(if (p.music == null) "Add music" else "Change music") }
        if (p.music != null) OutlinedButton({ vm.edit { it.copy(music = null) } }) { Text("Remove") }
    }
    if (p.music != null) {
        var v by remember(p.musicVol) { mutableFloatStateOf(p.musicVol) }
        Text("Music volume ${(v * 100).toInt()}%", fontSize = 12.sp)
        Slider(v, { v = it }, valueRange = 0f..2f, onValueChangeFinished = { vm.edit { it.copy(musicVol = v) } })
    }
    if (c != null && !c.image) {
        var v by remember(c.id, c.vol) { mutableFloatStateOf(c.vol) }
        Text("Clip volume ${(v * 100).toInt()}%", fontSize = 12.sp)
        Slider(v, { v = it }, valueRange = 0f..2f, onValueChangeFinished = { vm.updateClip { it.copy(vol = v) } })
    }
}

private val COLORS = listOf(0xFFFFFFFF, 0xFFFFD60A, 0xFFFF453A, 0xFF30D158, 0xFF0A84FF).map { it.toInt() }
private val STICKERS = listOf("😀", "🔥", "❤️", "⭐", "🎉", "👍", "😎", "🎬")

@Composable
private fun TextTools(vm: EditorViewModel, p: Project) {
    var text by remember { mutableStateOf("") }
    var pos by remember { mutableIntStateOf(2) }; var size by remember { mutableIntStateOf(64) }; var color by remember { mutableIntStateOf(COLORS[0]) }
    OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text("Text") }, singleLine = true)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf("Top", "Middle", "Bottom").forEachIndexed { i, n -> FilterChip(pos == i, { pos = i }, { Text(n) }) }
        listOf("S" to 40, "M" to 64, "L" to 96).forEach { (n, s) -> FilterChip(size == s, { size = s }, { Text(n) }) }
        COLORS.forEach { col -> Box(Modifier.size(30.dp).clip(RoundedCornerShape(50)).background(androidx.compose.ui.graphics.Color(col))
            .border(if (color == col) 3.dp else 1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50)).clickable { color = col }) }
    }
    Button({ if (text.isNotBlank()) { vm.edit { it.copy(overlays = it.overlays + Overlay(text = text, color = color, size = size, pos = pos)) }; text = "" } }) { Text("Add text") }
    Text("Stickers", fontSize = 12.sp)
    Row(Modifier.horizontalScroll(rememberScrollState())) { STICKERS.forEach { s ->
        TextButton({ vm.edit { it.copy(overlays = it.overlays + Overlay(text = s, size = 120, pos = 1)) } }) { Text(s, fontSize = 26.sp) } } }
    p.overlays.forEach { o -> Row(verticalAlignment = Alignment.CenterVertically) {
        Text(o.text, Modifier.weight(1f)); TextButton({ vm.edit { pr -> pr.copy(overlays = pr.overlays.filter { it.id != o.id }) } }) { Text("Remove") } } }
}
