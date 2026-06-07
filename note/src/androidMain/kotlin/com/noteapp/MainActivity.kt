package com.noteapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import com.tencent.kuikly.core.render.android.adapter.HRImageLoadOption
import com.tencent.kuikly.core.render.android.adapter.IKRImageAdapter
import com.tencent.kuikly.core.render.android.adapter.IKRRouterAdapter
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.context.KuiklyRenderCoreExecuteModeBase
import com.tencent.kuikly.core.render.android.exception.ErrorReason
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegator
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegatorDelegate
import com.tencent.kuikly.core.render.android.performace.KRMonitorType
import org.json.JSONObject
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

    private data class PageState(val pageName: String, val pageData: Map<String, Any>)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            initKuikly()
        } catch (e: Throwable) {
            val msg = "CRASH in MainActivity.onCreate: ${e.javaClass.name}: ${e.message}\n${e.stackTraceToString()}"
            Log.e("NTnotes-CRASH", msg, e)
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
        Log.d("NTnotes", "Registering pages...")
        Log.d("NTnotes", "NoteListPage exists before reg? ${BridgeManager.isPageExist("NoteListPage")}")
        BridgeManager.registerPageRouter("NoteListPage") { NoteListPage() }
        BridgeManager.registerPageRouter("NoteEditPage") { NoteEditPage() }
        BridgeManager.registerPageRouter("RecycleBinPage") { RecycleBinPage() }
        Log.d("NTnotes", "NoteListPage exists after reg? ${BridgeManager.isPageExist("NoteListPage")}")
        Log.d("NTnotes", "Pages registered. BridgeManager.init=${BridgeManager.isDidInit()}")
        logToFile("Pages registered manually")

        val bundleDir = File(filesDir, "ntnotes").also { it.mkdirs() }

        container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(container)

        val initialPageData = mapOf<String, Any>(
            "appId" to 1,
            "debug" to 1
        )
        val pageStack = mutableListOf<PageState>()
        var currentPage = PageState("NoteListPage", initialPageData)

        lateinit var delegate: KuiklyRenderViewBaseDelegatorDelegate

        fun attachPage(pageName: String, pageData: Map<String, Any>) {
            if (::kuiklyRenderViewDelegator.isInitialized) {
                kuiklyRenderViewDelegator.onDetach()
                container.removeAllViews()
            }
            kuiklyRenderViewDelegator = KuiklyRenderViewBaseDelegator(delegate)
            kuiklyRenderViewDelegator.onAttach(
                container,
                bundleDir.absolutePath,
                pageName,
                pageData
            )
            currentPage = PageState(pageName, pageData)
            logToFile("onAttach OK - page=$pageName")
        }

        fun openKuiklyPage(pageName: String, pageData: Map<String, Any>) {
            pageStack.add(currentPage)
            attachPage(pageName, pageData)
        }

        fun closeKuiklyPage() {
            val previous = pageStack.removeLastOrNull() ?: PageState("NoteListPage", initialPageData)
            attachPage(previous.pageName, previous.pageData)
        }

        registerKuiklyAdapters(::openKuiklyPage, ::closeKuiklyPage)

        delegate = object : KuiklyRenderViewBaseDelegatorDelegate {
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
                Log.e("NTnotes", msg, throwable)
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
                Log.d("NTnotes", "Page load $status: errorReason=${errorReason?.name}")

                if (!isSucceed) {
                    val msg = "页面加载失败: ${errorReason?.name}"
                    logToFile(msg)
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                }
            }
        }

        try {
            attachPage(currentPage.pageName, currentPage.pageData)
        } catch (e: Throwable) {
            val msg = "Kuikly page attach failed: ${e.javaClass.name}: ${e.message}"
            Log.e("NTnotes-CRASH", msg, e)
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

    private fun registerKuiklyAdapters(
        openPage: (String, Map<String, Any>) -> Unit,
        closePage: () -> Unit
    ) {
        KuiklyRenderAdapterManager.krImageAdapter = object : IKRImageAdapter {
            override fun fetchDrawable(
                options: HRImageLoadOption,
                callback: (Drawable?) -> Unit
            ) {
                fetchDrawable(options, null, callback)
            }

            override fun fetchDrawable(
                options: HRImageLoadOption,
                imageParams: JSONObject?,
                callback: (Drawable?) -> Unit
            ) {
                callback(loadDrawable(options.src))
            }

            override val shouldWaitViewDidLoad: Boolean = false
        }

        KuiklyRenderAdapterManager.krRouterAdapter = object : IKRRouterAdapter {
            override fun openPage(context: android.content.Context, pageName: String, pageData: JSONObject) {
                openPage(pageName, pageData.toMap())
            }

            override fun closePage(context: android.content.Context) {
                closePage()
            }
        }
    }

    private fun loadDrawable(src: String): Drawable? {
        return try {
            val bitmap = when {
                src.startsWith("assets://") -> {
                    val assetPath = src.removePrefix("assets://")
                    assets.open(assetPath).use { BitmapFactory.decodeStream(it) }
                }
                src.startsWith("file://") -> BitmapFactory.decodeFile(src.removePrefix("file://"))
                else -> null
            } ?: return null
            BitmapDrawable(resources, bitmap)
        } catch (e: Throwable) {
            Log.e("NTnotes", "Failed to load image: $src", e)
            null
        }
    }

    private fun JSONObject.toMap(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = opt(key)
            if (value != null && value != JSONObject.NULL) {
                result[key] = value
            }
        }
        return result
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
