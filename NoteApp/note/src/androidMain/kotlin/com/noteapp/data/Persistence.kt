package com.noteapp.data

import android.content.Context
import android.util.Log
import java.io.File

private fun getAppDir(): File {
    val ctx = AndroidContextHolder.applicationContext
    return File(ctx.filesDir, "noteapp").also { it.mkdirs() }
}

actual fun savePersistentData(key: String, value: String) {
    try {
        val dir = getAppDir()
        File(dir, key).writeText(value)
    } catch (e: Exception) {
        Log.e("NoteApp", "savePersistentData failed: key=$key", e)
    }
}

actual fun loadPersistentData(key: String): String {
    return try {
        val file = File(getAppDir(), key)
        if (file.exists()) file.readText() else ""
    } catch (e: Exception) {
        Log.e("NoteApp", "loadPersistentData failed: key=$key", e)
        ""
    }
}
