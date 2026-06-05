package com.noteapp

import android.util.Log
import androidx.activity.ComponentActivity
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import com.noteapp.data.AndroidContextHolder
import com.tencent.kuikly.core.render.android.context.KuiklyRenderCoreExecuteModeBase
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegator
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegatorDelegate
import com.tencent.kuikly.core.render.android.IKuiklyRenderExport
import com.tencent.kuikly.core.render.android.exception.ErrorReason
import com.tencent.kuikly.core.render.android.performace.KRMonitorType
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var kuiklyRenderViewDelegator: KuiklyRenderViewBaseDelegator
    private lateinit var container: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            initKuikly()
        } catch (e: Throwable) {
            val msg = "CRASH in onCreate: ${e.javaClass.name}: ${e.message}\n${e.stackTrace.joinToString("\n") { "    $it" }}"
            Log.e("NoteApp-CRASH", msg)
            try {
                File(filesDir, "crash_log.txt").appendText("MainActivity.onCreate: $msg\n\n")
            } catch (_: Exception) {}
            android.widget.Toast.makeText(this, "Crash: ${e.javaClass.simpleName}: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun initKuikly() {
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
                Log.e("NoteApp", "Unhandled: ${errorReason.name}", throwable)
            }

            override fun onPageLoadComplete(
                isSucceed: Boolean,
                errorReason: ErrorReason?,
                executeMode: KuiklyRenderCoreExecuteModeBase
            ) {
                Log.d("NoteApp", "Page load: isSucceed=$isSucceed errorReason=${errorReason?.name}")
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
            logToFile("onAttach OK")
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

    private fun logToFile(msg: String) {
        try { File(filesDir, "crash_log.txt").appendText("$msg\n") } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        if (::kuiklyRenderViewDelegator.isInitialized) kuiklyRenderViewDelegator.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (::kuiklyRenderViewDelegator.isInitialized) kuiklyRenderViewDelegator.onPause()
    }

    override fun onDestroy() {
        if (::kuiklyRenderViewDelegator.isInitialized) kuiklyRenderViewDelegator.onDetach()
        super.onDestroy()
        AndroidContextHolder.cleanup()
    }
}
