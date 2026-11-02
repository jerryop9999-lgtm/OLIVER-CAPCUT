@file:OptIn(UnstableApi::class)
package com.oliver.videoeditor.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import androidx.media3.common.*
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.*
import androidx.media3.transformer.*
import com.google.common.collect.ImmutableList
import com.oliver.videoeditor.data.ASPECTS
import com.oliver.videoeditor.data.Project
import java.io.File

object Exporter {
    fun start(ctx: Context, p: Project, height: Int, fps: Int, watermark: Boolean,
              onDone: (Uri) -> Unit, onError: (String) -> Unit): Transformer {
        val out = File(ctx.cacheDir, "export_${System.currentTimeMillis()}.mp4")
        val items = p.clips.map { c ->
            val mi = if (c.image) MediaItem.fromUri(c.uri) else MediaItem.Builder().setUri(c.uri)
                .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(c.startMs).setEndPositionMs(c.endMs).build()).build()
            val audio = mutableListOf<AudioProcessor>(); val video = mutableListOf<Effect>()
            if (c.rot != 0) video += ScaleAndRotateTransformation.Builder().setRotationDegrees(c.rot.toFloat()).build()
            if (!c.image && c.speed != 1f) {
                video += SpeedChangeEffect(c.speed)
                audio += SonicAudioProcessor().apply { setSpeed(c.speed) }
            }
            if (c.vol != 1f) audio += volume(c.vol)
            EditedMediaItem.Builder(mi).apply {
                if (c.image) { setDurationUs((c.endMs - c.startMs) * 1000); setFrameRate(30) }
                setEffects(Effects(audio, video))
            }.build()
        }
        val totalMs = p.clips.sumOf { ((it.endMs - it.startMs) / it.speed).toLong() }
        val seqs = mutableListOf(EditedMediaItemSequence(items))
        p.music?.let { m ->
            val mi = MediaItem.Builder().setUri(m).setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder().setEndPositionMs(totalMs).build()).build()
            seqs += EditedMediaItemSequence(listOf(EditedMediaItem.Builder(mi).setRemoveVideo(true)
                .setEffects(Effects(listOf(volume(p.musicVol)), emptyList())).build()))
        }
        val fx = mutableListOf<Effect>()
        ASPECTS[p.aspect].second?.let { fx += Presentation.createForAspectRatio(it, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP) }
        fx += Presentation.createForHeight(height)
        when (p.filter) {
            1 -> fx += RgbFilter.createGrayscaleFilter()
            2 -> fx += RgbAdjustment.Builder().setRedScale(1.15f).setBlueScale(0.88f).build()
            3 -> fx += RgbAdjustment.Builder().setBlueScale(1.15f).setRedScale(0.9f).build()
            4 -> fx += Brightness(0.15f)
            5 -> fx += Contrast(0.4f)
        }
        val ov = p.overlays.map { o ->
            val s = SpannableString(o.text)
            s.setSpan(ForegroundColorSpan(o.color), 0, s.length, 0); s.setSpan(AbsoluteSizeSpan(o.size), 0, s.length, 0)
            TextOverlay.createStaticTextOverlay(s, OverlaySettings.Builder()
                .setBackgroundFrameAnchor(0f, listOf(0.75f, 0f, -0.75f)[o.pos]).build()) as TextureOverlay
        }.toMutableList()
        if (watermark) ov += TextOverlay.createStaticTextOverlay(SpannableString("OLIVER"),
            OverlaySettings.Builder().setBackgroundFrameAnchor(0.8f, -0.92f).build())
        if (ov.isNotEmpty()) fx += OverlayEffect(ImmutableList.copyOf(ov))
        fx += FrameDropEffect.createDefaultFrameDropEffect(fps.toFloat())

        val comp = Composition.Builder(seqs).setEffects(Effects(emptyList(), fx))
            .experimentalSetForceAudioTrack(true).build()
        val t = Transformer.Builder(ctx).addListener(object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                runCatching { onDone(save(ctx, out)) }.onFailure { onError(it.message ?: "Save failed") }
            }
            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                out.delete(); onError(exportException.message ?: "Export failed")
            }
        }).build()
        t.start(comp, out.absolutePath)
        return t
    }

    private fun volume(v: Float) = ChannelMixingAudioProcessor().apply {
        for (ch in 1..2) putChannelMixingMatrix(ChannelMixingMatrix.create(ch, 2).scaleBy(v))
    }

    private fun save(ctx: Context, f: File): Uri {
        val v = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "OLIVER_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/OLIVER")
        }
        val uri = ctx.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, v) ?: error("Cannot create file")
        ctx.contentResolver.openOutputStream(uri)!!.use { o -> f.inputStream().use { it.copyTo(o) } }
        f.delete(); return uri
    }
}
