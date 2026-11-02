package com.oliver.videoeditor.media

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.oliver.videoeditor.data.Clip

object MediaUtil {
    fun toClip(ctx: Context, uri: Uri): Clip? {
        runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val image = ctx.contentResolver.getType(uri)?.startsWith("image") == true
        if (image) return Clip(uri = uri.toString(), image = true, srcDurMs = 3000)
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(ctx, uri)
            val d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: return null
            Clip(uri = uri.toString(), image = false, srcDurMs = d)
        } catch (e: Exception) { null } finally { r.release() }
    }
    fun fmt(ms: Long): String { val s = ms / 1000; return "%d:%02d".format(s / 60, s % 60) }
}
