package com.noteapp.util

actual fun currentTimeString(): String {
    val now = js("new Date()")
    val year = now.getFullYear()
    val month = (now.getMonth() + 1).toString().padStart(2, '0')
    val day = now.getDate().toString().padStart(2, '0')
    val hours = now.getHours().toString().padStart(2, '0')
    val minutes = now.getMinutes().toString().padStart(2, '0')
    val seconds = now.getSeconds().toString().padStart(2, '0')
    return "$year-$month-$day $hours:$minutes:$seconds"
}
