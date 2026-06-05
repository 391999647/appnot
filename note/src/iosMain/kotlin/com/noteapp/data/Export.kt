package com.noteapp.data

import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.writeToFile
import platform.Foundation.NSFileManager
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSSearchPathForDirectoriesInDomains

actual fun exportFile(filename: String, content: String, mimeType: String) {
    try {
        // iOS: 优先写到 Documents 目录，用户可通过 Files.app 访问
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true
        )
        val dir = if (paths.isNotEmpty()) paths.first() as String
                  else NSTemporaryDirectory() ?: "/tmp"
        val path = "$dir/$filename"
        (content as NSString).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    } catch (_: Exception) { }
}
