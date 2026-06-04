package com.noteapp.data

import android.annotation.SuppressLint
import android.content.Context

object AndroidContextHolder {
    @SuppressLint("StaticFieldLeak")
    lateinit var applicationContext: Context
        private set

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
}
