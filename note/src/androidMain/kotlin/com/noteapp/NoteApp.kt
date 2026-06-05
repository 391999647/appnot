package com.noteapp

import android.app.Application
import com.noteapp.data.AndroidContextHolder

class NoteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidContextHolder.init(this)
        AppRepo.initialize()
    }
}
