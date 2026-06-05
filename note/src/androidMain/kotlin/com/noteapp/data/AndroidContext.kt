package com.noteapp.data

import android.annotation.SuppressLint
import android.content.Context
import java.lang.ref.WeakReference

object AndroidContextHolder {
    private var weakContext: WeakReference<Context>? = null

    val applicationContext: Context?
        get() = weakContext?.get()

    fun init(context: Context) {
        weakContext = WeakReference(context.applicationContext)
    }

    fun cleanup() {
        weakContext?.clear()
        weakContext = null
    }
}
