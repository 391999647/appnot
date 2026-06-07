package com.noteapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

/**
 * 崩溃日志展示页面
 *
 * 当应用发生未捕获异常时自动启动，展示完整的崩溃信息：
 * - 崩溃时间、线程、进程信息
 * - 设备型号、Android 版本
 * - 完整的异常堆栈（含 Caused by 链）
 * - 系统日志（logcat）
 *
 * 提供操作：
 * - 复制日志到剪贴板
 * - 重启应用
 * - 清空日志
 */
class CrashActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CRASH_REPORT = "crash_report"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val report = intent.getStringExtra(EXTRA_CRASH_REPORT)
            ?: CrashReporter.readLastCrashReport()
            ?: "未找到崩溃日志"

        buildUi(report)
    }

    private fun buildUi(report: String) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FF121212"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 标题栏
        val titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(16), dp(16), dp(12))
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleText = TextView(this).apply {
            text = "💥 应用崩溃"
            setTextColor(Color.parseColor("#FFE06060"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeBtn = Button(this).apply {
            text = "关闭"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#FF333333"))
            setOnClickListener { finishAffinity() }
        }

        titleBar.addView(titleText)
        titleBar.addView(closeBtn)
        root.addView(titleBar)

        // 分割线
        val divider = View(this).apply {
            setBackgroundColor(Color.parseColor("#FF333333"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        }
        root.addView(divider)

        // 提示信息
        val hintText = TextView(this).apply {
            text = "应用发生了一个未处理的异常。以下是详细的崩溃日志，可长按文本复制或点击下方按钮复制全部内容。"
            setTextColor(Color.parseColor("#FFAAAAAA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(16), dp(12), dp(16), dp(8))
        }
        root.addView(hintText)

        // 日志内容（可滚动）
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setPadding(dp(16), 0, dp(16), 0)
        }

        val logText = TextView(this).apply {
            text = report
            setTextColor(Color.parseColor("#FFE0E0E0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setLineSpacing(2f, 1.2f)
            typeface = android.graphics.Typeface.MONOSPACE
            movementMethod = ScrollingMovementMethod()
            isLongClickable = true
            setOnLongClickListener {
                copyToClipboard(report)
                true
            }
        }

        scrollView.addView(logText)
        root.addView(scrollView)

        // 底部按钮栏
        val buttonBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(12), dp(16), dp(24))
            gravity = Gravity.CENTER
        }

        val copyBtn = createActionButton("📋 复制日志") {
            copyToClipboard(report)
        }

        val restartBtn = createActionButton("🔄 重启应用") {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finishAffinity()
        }

        val clearBtn = createActionButton("🗑️ 清空日志") {
            CrashReporter.clearCrashFiles(this@CrashActivity)
            Toast.makeText(this@CrashActivity, "日志已清空", Toast.LENGTH_SHORT).show()
            finishAffinity()
        }

        buttonBar.addView(copyBtn)
        buttonBar.addView(restartBtn)
        buttonBar.addView(clearBtn)
        root.addView(buttonBar)

        setContentView(root)
    }

    private fun createActionButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setBackgroundColor(Color.parseColor("#FF4A90D9"))
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(8)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("崩溃日志", text))
        Toast.makeText(this, "日志已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    private fun dp(px: Int): Int {
        return (px * resources.displayMetrics.density).toInt()
    }
}
