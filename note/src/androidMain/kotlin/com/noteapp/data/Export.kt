package com.noteapp.data

import android.util.Log
import java.io.File

actual fun exportFile(filename: String, content: String, mimeType: String) {
    try {
        val ctx = AndroidContextHolder.applicationContext
        val dir = File(ctx.filesDir, "noteapp_exports")
        dir.mkdirs()
        File(dir, filename).writeText(content)
    } catch (e: Exception) {
        Log.e("NoteApp", "exportFile failed: filename=$filename", e)
    }
}
