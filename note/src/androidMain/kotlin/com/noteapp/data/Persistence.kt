package com.noteapp.data

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import java.io.File

private val appDir: File by lazy {
    val ctx = AndroidContextHolder.applicationContext!!
    File(ctx.filesDir, "noteapp")
}

private val masterKeyAlias: String by lazy {
    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
}

private fun encryptedFile(file: File): EncryptedFile {
    return EncryptedFile.Builder(
        file,
        AndroidContextHolder.applicationContext!!,
        masterKeyAlias,
        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
    ).build()
}

actual fun savePersistentData(key: String, value: String) {
    try {
        val dir = appDir
        dir.mkdirs()
        val file = File(dir, key)
        encryptedFile(file).openFileOutput().use { outputStream ->
            outputStream.write(value.toByteArray(Charsets.UTF_8))
        }
    } catch (e: Exception) {
        Log.e("NoteApp", "savePersistentData failed: key=$key", e)
    }
}

actual fun loadPersistentData(key: String): String {
    return try {
        val file = File(appDir, key)
        if (!file.exists()) return ""
        encryptedFile(file).openFileInput().use { inputStream ->
            inputStream.readBytes().toString(Charsets.UTF_8)
        }
    } catch (e: Exception) {
        Log.e("NoteApp", "loadPersistentData failed: key=$key", e)
        ""
    }
}
