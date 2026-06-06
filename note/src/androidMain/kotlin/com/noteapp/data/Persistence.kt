package com.noteapp.data

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import java.io.File

private val appDir: File?
    get() {
        val ctx = AndroidContextHolder.applicationContext ?: return null
        return File(ctx.filesDir, "ntnotes")
    }

private val masterKeyAlias: String? by lazy {
    try {
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    } catch (e: Exception) {
        println("NTnotes Failed to create master key: ${e.message}")
        null
    }
}

private fun encryptedFile(file: File): EncryptedFile? {
    val ctx = AndroidContextHolder.applicationContext ?: return null
    val keyAlias = masterKeyAlias ?: return null
    return try {
        EncryptedFile.Builder(
            file,
            ctx,
            keyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    } catch (e: Exception) {
        println("NTnotes Failed to build EncryptedFile: ${e.message}")
        null
    }
}

actual fun savePersistentData(key: String, value: String) {
    try {
        val dir = appDir ?: run {
            println("NTnotes savePersistentData: Context not available")
            return
        }
        dir.mkdirs()
        val file = File(dir, key)
        file.delete() // EncryptedFile.openFileOutput requires the file NOT to exist
        val ef = encryptedFile(file) ?: run {
            println("NTnotes savePersistentData: Failed to create EncryptedFile")
            return
        }
        ef.openFileOutput().use { outputStream ->
            outputStream.write(value.toByteArray(Charsets.UTF_8))
        }
    } catch (e: Exception) {
        println("NTnotes savePersistentData failed: key=$key, error=${e.message}")
    }
}

actual fun loadPersistentData(key: String): String {
    return try {
        val dir = appDir ?: run {
            println("NTnotes loadPersistentData: Context not available")
            return ""
        }
        val file = File(dir, key)
        if (!file.exists()) return ""
        val ef = encryptedFile(file) ?: run {
            println("NTnotes loadPersistentData: Failed to create EncryptedFile")
            return ""
        }
        ef.openFileInput().use { inputStream ->
            inputStream.readBytes().toString(Charsets.UTF_8)
        }
    } catch (e: Exception) {
        println("NTnotes loadPersistentData failed: key=$key, error=${e.message}")
        ""
    }
}

actual fun removePersistentData(key: String) {
    try {
        val dir = appDir ?: return
        val file = File(dir, key)
        if (file.exists()) {
            file.delete()
        }
    } catch (e: Exception) {
        println("NTnotes removePersistentData failed: key=$key, error=${e.message}")
    }
}
