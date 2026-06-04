package com.noteapp.data

import platform.posix.time
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.LongVar

actual fun currentTimeSeconds(): Long {
    return memScoped {
        val t = alloc<LongVar>()
        t.value = time(null)
        t.value
    }
}
