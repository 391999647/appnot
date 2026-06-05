package com.noteapp

import android.app.Application
import android.util.Log
import com.noteapp.data.AndroidContextHolder
import java.io.File

class NoteApp : Application() {
    private val crashLogFile: File by lazy { File(filesDir, "crash_log.txt") }

    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logCrash("Uncaught in ${thread.name}", throwable)
        }

        try {
            AndroidContextHolder.init(this)
            AppRepo.initialize()
            logToFile("NoteApp init OK")
        } catch (e: Throwable) {
            logCrash("NoteApp.onCreate", e)
            throw e
        }
    }

    private fun logCrash(tag: String, e: Throwable) {
        val msg = "$tag: ${e.javaClass.name}: ${e.message}\n${e.stackTrace.joinToString("\n") { "    $it" }}"
        Log.e("NoteApp-CRASH", msg)
        try {
            crashLogFile.appendText("$msg\n\n")
        } catch (_: Exception) {}
    }

    private fun logToFile(msg: String) {
        try { crashLogFile.appendText("$msg\n") } catch (_: Exception) {}
    }
}
