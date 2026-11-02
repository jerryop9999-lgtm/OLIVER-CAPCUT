package com.oliver.videoeditor.data

import android.content.Context
import com.google.gson.Gson
import java.io.File
import java.util.UUID

data class Clip(
    val id: String = UUID.randomUUID().toString(), val uri: String, val image: Boolean,
    val srcDurMs: Long, val startMs: Long = 0, val endMs: Long = srcDurMs,
    val speed: Float = 1f, val rot: Int = 0, val vol: Float = 1f
)
data class Overlay(val id: String = UUID.randomUUID().toString(), val text: String,
    val color: Int = 0xFFFFFFFF.toInt(), val size: Int = 64, val pos: Int = 2)
data class Project(
    val id: String = UUID.randomUUID().toString(), val name: String = "Untitled",
    val updated: Long = System.currentTimeMillis(), val clips: List<Clip> = emptyList(),
    val music: String? = null, val musicVol: Float = 1f, val overlays: List<Overlay> = emptyList(),
    val filter: Int = 0, val aspect: Int = 0, val exported: Boolean = false
)

val ASPECTS = listOf<Pair<String, Float?>>("Original" to null, "16:9" to 16f / 9, "9:16" to 9f / 16, "1:1" to 1f, "4:5" to 4f / 5)
val FILTERS = listOf("None", "B&W", "Warm", "Cool", "Bright", "Contrast")

class ProjectStore(ctx: Context) {
    private val dir = File(ctx.filesDir, "projects").apply { mkdirs() }
    private val gson = Gson()
    fun save(p: Project) = runCatching { File(dir, "${p.id}.json").writeText(gson.toJson(p)) }
    fun delete(id: String) { File(dir, "$id.json").delete() }
    fun loadAll(): List<Project> = (dir.listFiles() ?: emptyArray()).mapNotNull {
        runCatching { gson.fromJson(it.readText(), Project::class.java) }.getOrNull()
    }.sortedByDescending { it.updated }
}
