package com.noteapp.data

import android.content.Context

/**
 * 全局 Android Context 持有者
 * 用于在 commonMain 代码中访问 Android Context
 */
object AndroidContextHolder {
    private var _applicationContext: Context? = null

    val applicationContext: Context
        get() = _applicationContext ?: throw IllegalStateException(
            "AndroidContextHolder not initialized. Call init() first."
        )

    fun init(context: Context) {
        _applicationContext = context.applicationContext
    }
}
