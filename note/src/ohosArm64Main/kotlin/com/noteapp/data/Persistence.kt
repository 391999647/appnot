package com.noteapp.data

import kotlinx.cinterop.memScoped
import kotlinx.cinterop.allocArray
import platform.posix.*

actual fun savePersistentData(key: String, value: String) {
    try {
        val dir = "/data/storage/el2/base/files/ntnotes/"
        mkdir(dir, 0x1C0.toUShort())
        val filePath = "$dir$key"
        val fd = open(filePath, O_WRONLY or O_CREAT or O_TRUNC, 0x180.toUShort())
        if (fd < 0) return
        val bytes = value.encodeToByteArray()
        memScoped {
            val buf = allocArray<ByteVar>(bytes.size)
            for (i in bytes.indices) buf[i] = bytes[i]
            write(fd, buf, bytes.size.toLong())
        }
        close(fd)
    } catch (_: Exception) { }
}

actual fun loadPersistentData(key: String): String {
    return try {
        val filePath = "/data/storage/el2/base/files/ntnotes/$key"
        val fd = open(filePath, O_RDONLY)
        if (fd < 0) return ""
        val size = lseek(fd, 0, SEEK_END)
        if (size <= 0L) { close(fd); return "" }
        lseek(fd, 0, SEEK_SET)
        val result = memScoped {
            val buf = allocArray<ByteVar>(size.toInt())
            read(fd, buf, size.toLong())
            ByteArray(size.toInt()) { buf[it].toByte() }.decodeToString()
        }
        close(fd)
        result
    } catch (_: Exception) { "" }
}
