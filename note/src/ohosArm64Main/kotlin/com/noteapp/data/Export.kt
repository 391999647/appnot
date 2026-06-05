package com.noteapp.data

import kotlinx.cinterop.memScoped
import kotlinx.cinterop.allocArray
import platform.posix.*

actual fun exportFile(filename: String, content: String, mimeType: String) {
    try {
        val dir = "/data/storage/el2/base/files/noteapp_exports/"
        mkdir(dir, 0x1C0.toUShort()) // 0700
        val filePath = "$dir$filename"
        val fd = open(filePath, O_WRONLY or O_CREAT or O_TRUNC, 0x180.toUShort()) // 0600
        if (fd < 0) return
        val bytes = content.encodeToByteArray()
        memScoped {
            val buf = allocArray<ByteVar>(bytes.size)
            for (i in bytes.indices) buf[i] = bytes[i]
            write(fd, buf, bytes.size.toLong())
        }
        close(fd)
    } catch (_: Exception) { }
}
