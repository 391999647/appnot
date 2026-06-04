package com.noteapp.util

import kotlinx.cinterop.memScoped
import kotlinx.cinterop.alloc
import kotlinx.cinterop.ptr
import kotlinx.cinterop.LongVar
import platform.posix.time
import platform.posix.localtime_r
import platform.posix.tm

actual fun currentTimeString(): String {
    return memScoped {
        val t = alloc<LongVar>()
        t.value = time(null)
        val tm = alloc<tm>()
        localtime_r(t.ptr, tm.ptr)
        "%04d-%02d-%02d %02d:%02d:%02d".format(
            tm.tm_year + 1900, tm.tm_mon + 1, tm.tm_mday,
            tm.tm_hour, tm.tm_min, tm.tm_sec
        )
    }
}
