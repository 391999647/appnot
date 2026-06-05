package com.noteapp.data

actual fun currentTimeSeconds(): Long = System.currentTimeMillis() / 1000
