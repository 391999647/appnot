package com.noteapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.noteapp.data.AndroidContextHolder
import com.noteapp.pages.NoteEditPage
import com.noteapp.pages.NoteListPage
import com.noteapp.pages.RecycleBinPage
import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.render.android.IKuiklyRenderExport
import com.tencent.kuikly.core.render.android.context.KuiklyRenderCoreExecuteModeBase
import com.tencent.kuikly.core.render.android.exception.ErrorReason
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegator
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegatorDelegate
import com.tencent.kuikly.core.render.android.performace.KRMonitorType
import java.io.File

/**
 * MainActivity - 应用主入口
 *
 * 职责：
 * 1. 初始化 Kuikly 渲染引擎并加载 NoteListPage
 * 2. 捕获所有初始化异常，崩溃时跳转到 CrashActivity
 * 3. 处理返回键事件，传递给 Kuikly 页面栈
 */
class MainActivity : ComponentActivity() {

    private lateinit var kuiklyRenderViewDelegator: KuiklyRenderViewBaseDelegator
    private lateinit var container: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            initKuikly()
        } catch (e: Throwable) {
            val msg = "CRASH in MainActivity.onCreate: ${e.javaClass.name}: ${e.message}\n${e.stackTraceToString()}"
            Log.e("NoteApp-CRASH", msg, e)
            logToFile("MainActivity.onCreate: $msg")

            // 启动崩溃展示页面
            startCrashActivity(msg)
        }
    }

    /**
     * 初始化 Kuikly 渲染引擎
     */
    private fun initKuikly() {
        // 手动注册所有页面
        Log.d("NoteApp", "Registering pages...")
        Log.d("NoteApp", "NoteListPage exists before reg? ${BridgeManager.isPageExist("NoteListPage")}")
        BridgeManager.registerPageRouter("NoteListPage") { NoteListPage() }
        BridgeManager.registerPageRouter("NoteEditPage") { NoteEditPage() }
        BridgeManager.registerPageRouter("RecycleBinPage") { RecycleBinPage() }
        Log.d("NoteApp", "NoteListPage exists after reg? ${BridgeManager.isPageExist("NoteListPage")}")
        Log.d("NoteApp", "Pages registered. BridgeManager.init=${BridgeManager.isDidInit()}")
        logToFile("Pages registered manually")

        val bundleDir = File(filesDir, "noteapp").also { it.mkdirs() }

        container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(container)

        val delegate = object : KuiklyRenderViewBaseDelegatorDelegate {
            override fun coreExecuteModeX(): KuiklyRenderCoreExecuteModeBase {
                return KuiklyRenderCoreExecuteModeBase.JVM
            }

            override fun performanceMonitorTypes(): List<KRMonitorType> {
                return listOf(KRMonitorType.LAUNCH, KRMonitorType.FRAME, KRMonitorType.MEMORY)
            }

            override fun registerExternalModule(kuiklyRenderExport: IKuiklyRenderExport) {
                super.registerExternalModule(kuiklyRenderExport)
            }

            override fun onUnhandledException(
                throwable: Throwable,
                errorReason: ErrorReason,
                executeMode: KuiklyRenderCoreExecuteModeBase
            ) {
                val msg = "Kuikly Unhandled: ${errorReason.name}\n${throwable.stackTraceToString()}"
                Log.e("NoteApp", msg, throwable)
                logToFile(msg)

                // Kuikly 内部异常也展示崩溃页面
                startCrashActivity(msg)
            }

            override fun onPageLoadComplete(
                isSucceed: Boolean,
                errorReason: ErrorReason?,
                executeMode: KuiklyRenderCoreExecuteModeBase
            ) {
                val status = if (isSucceed) "SUCCESS" else "FAILED"
                Log.d("NoteApp", "Page load $status: errorReason=${errorReason?.name}")

                if (!isSucceed) {
                    val msg = "页面加载失败: ${errorReason?.name}"
                    logToFile(msg)
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                }
            }
        }

        try {
            kuiklyRenderViewDelegator = KuiklyRenderViewBaseDelegator(delegate)
            logToFile("KuiklyRenderViewBaseDelegator created OK")
        } catch (e: Throwable) {
            val msg = "KuiklyRenderViewBaseDelegator init failed: ${e.javaClass.name}: ${e.message}"
            Log.e("NoteApp-CRASH", msg, e)
            logToFile(msg)
            throw e
        }

        val pageData = mapOf<String, Any>(
            "appId" to 1,
            "debug" to 1
        )

        try {
            kuiklyRenderViewDelegator.onAttach(
                container,
                bundleDir.absolutePath,
                "NoteListPage",
                pageData
            )
            logToFile("onAttach OK - page=NoteListPage")
        } catch (e: Throwable) {
            val msg = "onAttach failed: ${e.javaClass.name}: ${e.message}"
            Log.e("NoteApp-CRASH", msg, e)
            logToFile(msg)
            throw e
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!kuiklyRenderViewDelegator.onBackPressed()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    /**
     * 启动崩溃展示页面
     */
    private fun startCrashActivity(report: String) {
        val intent = Intent(this, CrashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(CrashActivity.EXTRA_CRASH_REPORT, report)
        }
        startActivity(intent)
        finishAffinity()
    }

    private fun logToFile(msg: String) {
        try {
            File(filesDir, "crash_log.txt").appendText("$msg\n")
        } catch (_: Exception) {
        }
    }

    override fun onResume() {
        super.onResume()
        if (::kuiklyRenderViewDelegator.isInitialized) {
            kuiklyRenderViewDelegator.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::kuiklyRenderViewDelegator.isInitialized) {
            kuiklyRenderViewDelegator.onPause()
        }
    }

    override fun onDestroy() {
        if (::kuiklyRenderViewDelegator.isInitialized) {
            kuiklyRenderViewDelegator.onDetach()
        }
        super.onDestroy()
        AndroidContextHolder.cleanup()
    }
}
