package com.noteapp

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 崩溃日志收集与展示工具
 *
 * 功能：
 * 1. 捕获所有未处理异常（Java/Kotlin 层）
 * 2. 收集设备信息、系统日志、崩溃堆栈
 * 3. 崩溃后自动跳转到 CrashActivity 展示详细日志
 * 4. 支持手动触发崩溃页面（用于调试）
 */
object CrashReporter {

    private const val TAG = "CrashReporter"
    private const val CRASH_LOG_FILE = "crash_report.txt"
    private const val MAX_LOG_LINES = 500

    private var appContext: Context? = null
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    /**
     * 初始化崩溃捕获
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleException(thread, throwable)
            // 让系统默认处理器也处理（生成 tombstone）
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * 处理异常：保存日志 + 启动崩溃页面
     */
    private fun handleException(thread: Thread, throwable: Throwable) {
        try {
            val report = buildCrashReport(thread, throwable)
            saveCrashReport(report)
            Log.e(TAG, "=== CRASH REPORT ===\n$report")

            // 启动崩溃展示页面
            val ctx = appContext ?: return
            val intent = Intent(ctx, CrashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(CrashActivity.EXTRA_CRASH_REPORT, report)
            }
            ctx.startActivity(intent)

            // 等待 Activity 启动
            Thread.sleep(1000)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle crash", e)
        }
    }

    /**
     * 构建完整的崩溃报告
     */
    private fun buildCrashReport(thread: Thread, throwable: Throwable): String {
        val sb = StringBuilder()
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())

        sb.appendLine("═══════════════════════════════════════════════════════════════")
        sb.appendLine("  APP CRASH REPORT")
        sb.appendLine("═══════════════════════════════════════════════════════════════")
        sb.appendLine()
        sb.appendLine("【崩溃时间】$time")
        sb.appendLine("【线程名称】${thread.name} (ID: ${thread.id})")
        sb.appendLine("【进程信息】PID: ${Process.myPid()}, UID: ${Process.myUid()}")
        sb.appendLine()

        // 设备信息
        sb.appendLine("─────────────── 设备信息 ───────────────")
        sb.appendLine("品牌:     ${Build.BRAND}")
        sb.appendLine("型号:     ${Build.MODEL}")
        sb.appendLine("设备:     ${Build.DEVICE}")
        sb.appendLine("Android:  ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("ABI:      ${Build.SUPPORTED_ABIS?.joinToString() ?: Build.CPU_ABI}")
        sb.appendLine()

        // 应用信息
        sb.appendLine("─────────────── 应用信息 ───────────────")
        try {
            val pm = appContext?.packageManager
            val pi = pm?.getPackageInfo(appContext?.packageName ?: "", 0)
            sb.appendLine("包名:     ${appContext?.packageName}")
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pi?.longVersionCode else pi?.versionCode?.toLong()
            sb.appendLine("版本:     ${pi?.versionName} ($versionCode)")
        } catch (_: Exception) {}
        sb.appendLine()

        // 崩溃堆栈
        sb.appendLine("─────────────── 崩溃堆栈 ───────────────")
        sb.appendLine("异常类型: ${throwable.javaClass.name}")
        sb.appendLine("异常消息: ${throwable.message}")
        sb.appendLine()
        sb.appendLine(throwable.stackTraceToString())
        sb.appendLine()

        // 根本原因链
        var cause = throwable.cause
        var depth = 1
        while (cause != null) {
            sb.appendLine("─────────────── Caused by (depth=$depth) ───────────────")
            sb.appendLine("异常类型: ${cause.javaClass.name}")
            sb.appendLine("异常消息: ${cause.message}")
            sb.appendLine()
            sb.appendLine(cause.stackTraceToString())
            sb.appendLine()
            cause = cause.cause
            depth++
        }

        // 系统日志
        sb.appendLine("─────────────── 系统日志 (最后 $MAX_LOG_LINES 行) ───────────────")
        sb.appendLine(getLogcatOutput())
        sb.appendLine()

        sb.appendLine("═══════════════════════════════════════════════════════════════")
        sb.appendLine("  END OF REPORT")
        sb.appendLine("═══════════════════════════════════════════════════════════════")

        return sb.toString()
    }

    /**
     * 保存崩溃报告到文件
     */
    private fun saveCrashReport(report: String) {
        try {
            val ctx = appContext ?: return
            val file = File(ctx.filesDir, CRASH_LOG_FILE)
            file.writeText(report)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash report", e)
        }
    }

    /**
     * 读取最近一次崩溃报告
     */
    fun readLastCrashReport(): String {
        return try {
            val ctx = appContext ?: return ""
            val file = File(ctx.filesDir, CRASH_LOG_FILE)
            if (file.exists()) file.readText() else ""
        } catch (e: Exception) {
            "读取崩溃日志失败: ${e.message}"
        }
    }

    /**
     * 获取系统日志输出
     */
    private fun getLogcatOutput(): String {
        return try {
            val process = Runtime.getRuntime().exec(
                arrayOf("logcat", "-d", "-t", MAX_LOG_LINES.toString(), "*:E")
            )
            process.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "无法获取系统日志: ${e.message}"
        }
    }

    /**
     * 获取所有崩溃日志文件列表
     */
    fun listCrashFiles(context: Context): List<File> {
        return try {
            context.filesDir.listFiles { f -> f.name.startsWith("crash") }?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearCrashFiles(context: Context) {
        listCrashFiles(context).forEach { file ->
            try {
                file.delete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete crash file: ${file.name}", e)
            }
        }
    }
}
