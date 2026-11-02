package com.oliver.videoeditor.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import com.oliver.videoeditor.data.*

class EditorViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ProjectStore(app)
    private val prefs = app.getSharedPreferences("oliver", Context.MODE_PRIVATE)
    var projects by mutableStateOf(store.loadAll()); private set
    var project by mutableStateOf<Project?>(null); private set
    var selected by mutableIntStateOf(0)
    var dark by mutableStateOf(prefs.getBoolean("dark", true)); private set
    var watermark by mutableStateOf(prefs.getBoolean("wm", false)); private set
    private val undoS = ArrayDeque<Project>(); private val redoS = ArrayDeque<Project>()
    var historyTick by mutableIntStateOf(0); private set
    val canUndo get() = historyTick >= 0 && undoS.isNotEmpty()
    val canRedo get() = historyTick >= 0 && redoS.isNotEmpty()

    fun setDark(v: Boolean) { dark = v; prefs.edit().putBoolean("dark", v).apply() }
    fun setWatermark(v: Boolean) { watermark = v; prefs.edit().putBoolean("wm", v).apply() }

    fun create(name: String = "Project ${projects.size + 1}", aspect: Int = 0, overlays: List<Overlay> = emptyList()) {
        undoS.clear(); redoS.clear(); selected = 0
        commit(Project(name = name, aspect = aspect, overlays = overlays))
    }
    fun open(p: Project) { undoS.clear(); redoS.clear(); selected = 0; project = p }
    fun close() { project = null; projects = store.loadAll() }
    fun delete(p: Project) { store.delete(p.id); projects = store.loadAll() }
    fun markExported() { project?.let { commit(it.copy(exported = true)) } }

    private fun commit(p: Project) { val n = p.copy(updated = System.currentTimeMillis()); project = n; store.save(n); historyTick++ }
    fun edit(f: (Project) -> Project) {
        val c = project ?: return
        undoS.addLast(c); if (undoS.size > 50) undoS.removeFirst(); redoS.clear(); commit(f(c))
    }
    fun undo() { val c = project ?: return; val p = undoS.removeLastOrNull() ?: return; redoS.addLast(c); commit(p); fixSel() }
    fun redo() { val c = project ?: return; val p = redoS.removeLastOrNull() ?: return; undoS.addLast(c); commit(p); fixSel() }
    private fun fixSel() { selected = selected.coerceIn(0, ((project?.clips?.size ?: 1) - 1).coerceAtLeast(0)) }

    fun updateClip(f: (Clip) -> Clip) = edit { p ->
        p.copy(clips = p.clips.mapIndexed { i, c -> if (i == selected) f(c) else c })
    }
    fun addClips(c: List<Clip>) = edit { it.copy(clips = it.clips + c) }
    fun deleteClip() { edit { p -> p.copy(clips = p.clips.filterIndexed { i, _ -> i != selected }) }; fixSel() }
    fun duplicate() = edit { p ->
        val c = p.clips.getOrNull(selected) ?: return@edit p
        p.copy(clips = p.clips.toMutableList().apply { add(selected + 1, c.copy(id = java.util.UUID.randomUUID().toString())) })
    }
    /** Split selected clip at [posMs], the player position inside the trimmed clip (source ms). */
    fun split(posMs: Long) = edit { p ->
        val c = p.clips.getOrNull(selected) ?: return@edit p
        val at = c.startMs + posMs
        if (c.image || at <= c.startMs + 200 || at >= c.endMs - 200) return@edit p
        p.copy(clips = p.clips.toMutableList().apply {
            set(selected, c.copy(endMs = at))
            add(selected + 1, c.copy(id = java.util.UUID.randomUUID().toString(), startMs = at))
        })
    }
}
