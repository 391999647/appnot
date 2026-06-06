package com.noteapp.data

import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

actual fun exportFile(filename: String, content: String, mimeType: String) {
    try {
        val ctx = AndroidContextHolder.applicationContext!!
        val dir = File(ctx.filesDir, "ntnotes_exports")
        dir.mkdirs()
        val file = File(dir, filename)
        file.writeText(content)
        val uri = FileProvider.getUriForFile(ctx, "com.noteapp.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(Intent.createChooser(intent, "分享 $filename"))
    } catch (e: Exception) {
        Log.e("NTnotes", "exportFile failed: filename=$filename", e)
    }
}
