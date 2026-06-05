package com.noteapp

import android.app.Application
import android.util.Log
import com.noteapp.data.AndroidContextHolder
import java.io.File

/**
 * NoteApp Application
 *
 * 初始化顺序：
 * 1. 注册全局崩溃捕获器（CrashReporter）
 * 2. 初始化 AndroidContextHolder（供其他模块使用 Context）
 * 3. 初始化数据仓库
 */
class NoteApp : Application() {

    private val crashLogFile: File by lazy { File(filesDir, "crash_log.txt") }

    override fun onCreate() {
        super.onCreate()

        // 1. 首先注册崩溃捕获器 —— 越早注册越好，确保能捕获后续所有异常
        CrashReporter.init(this)

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
