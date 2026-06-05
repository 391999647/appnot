package com.noteapp.data

actual fun currentTimeSeconds(): Long {
    return (kotlin.js.Date().getTime() / 1000).toLong()
}
