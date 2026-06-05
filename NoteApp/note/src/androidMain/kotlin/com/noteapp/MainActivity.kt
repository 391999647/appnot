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

/**
 * NoteApp Android 入口 Activity
 *
 * 使用 KuiklyRenderView 渲染 Kuikly 页面
 */
class MainActivity : ComponentActivity() {

    private lateinit var kuiklyRenderViewDelegator: KuiklyRenderViewBaseDelegator
    private lateinit var container: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            initKuikly()
        } catch (e: Throwable) {
            Log.e("NoteApp", "CRASH in onCreate", e)
            android.widget.Toast.makeText(this, "Crash: " + e.message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun initKuikly() {
        AndroidContextHolder.init(this)
        AppRepo.initialize()

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

        kuiklyRenderViewDelegator = KuiklyRenderViewBaseDelegator(delegate)

        val pageData = mapOf<String, Any>(
            "appId" to 1,
            "debug" to 1
        )
        kuiklyRenderViewDelegator.onAttach(
            container,
            "",
            "NoteListPage",
            pageData
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!kuiklyRenderViewDelegator.onBackPressed()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        kuiklyRenderViewDelegator.onResume()
    }

    override fun onPause() {
        super.onPause()
        kuiklyRenderViewDelegator.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        kuiklyRenderViewDelegator.onDetach()
    }
}
