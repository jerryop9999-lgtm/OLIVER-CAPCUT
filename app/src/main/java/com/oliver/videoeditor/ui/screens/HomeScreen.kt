package com.oliver.videoeditor.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oliver.videoeditor.data.Overlay
import com.oliver.videoeditor.viewmodel.EditorViewModel
import java.text.DateFormat
import java.util.Date

private data class Template(val name: String, val aspect: Int, val text: String)
private val templates = listOf(
    Template("Vertical Story", 2, "Your Story"), Template("Landscape Vlog", 1, "My Vlog"),
    Template("Square Post", 3, "New Post"), Template("Portrait 4:5", 4, "Hello"))

@Composable
fun HomeScreen(vm: EditorViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    var settings by remember { mutableStateOf(false) }
    val list = if (tab == 0) vm.projects else vm.projects.filter { !it.exported }
    LazyColumn(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("OLIVER", fontSize = 34.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { settings = true }, shape = RoundedCornerShape(50)) { Text("Settings") }
            }
            Text("Video editor", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Button(onClick = { vm.create() }, Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(30.dp)) {
                Text("+  New Project", fontSize = 18.sp)
            }
        }
        item { Text("Templates", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(templates) { t ->
                    Card(Modifier.size(130.dp, 90.dp).clickable { vm.create(t.name, t.aspect, listOf(Overlay(text = t.text))) },
                        shape = RoundedCornerShape(18.dp)) {
                        Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.BottomStart) { Text(t.name, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(tab == 0, { tab = 0 }, { Text("Recent Projects") })
            FilterChip(tab == 1, { tab = 1 }, { Text("Drafts") }) } }
        if (list.isEmpty()) item { Text("Nothing here yet. Tap New Project to start.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(list, key = { it.id }) { p ->
            Card(Modifier.fillMaxWidth().clickable { vm.open(p) }, shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.name, fontWeight = FontWeight.Bold)
                        Text("${p.clips.size} clips · " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(p.updated)) +
                            if (p.exported) " · exported" else " · draft", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { vm.delete(p) }) { Text("Delete") }
                }
            }
        }
    }
    if (settings) AlertDialog(onDismissRequest = { settings = false }, confirmButton = { TextButton({ settings = false }) { Text("Done") } },
        title = { Text("Settings") }, text = { Column {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Dark theme", Modifier.weight(1f)); Switch(vm.dark, { vm.setDark(it) }) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Watermark on exports", Modifier.weight(1f)); Switch(vm.watermark, { vm.setWatermark(it) }) }
        } })
}
