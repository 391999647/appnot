package com.noteapp.data

import android.content.Context
import android.util.Log
import java.io.File

private val appDir: File by lazy {
    val ctx = AndroidContextHolder.applicationContext
    File(ctx.filesDir, "noteapp")
}

actual fun savePersistentData(key: String, value: String) {
    try {
        val dir = appDir
        dir.mkdirs()
        File(dir, key).writeText(value)
    } catch (e: Exception) {
        Log.e("NoteApp", "savePersistentData failed: key=$key", e)
    }
}

actual fun loadPersistentData(key: String): String {
    return try {
        val file = File(appDir, key)
        if (file.exists()) file.readText() else ""
    } catch (e: Exception) {
        Log.e("NoteApp", "loadPersistentData failed: key=$key", e)
        ""
    }
}
