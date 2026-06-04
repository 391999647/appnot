package com.noteapp.util

actual fun currentTimeString(): String {
    val now = System.currentTimeMillis()
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(now))
}
