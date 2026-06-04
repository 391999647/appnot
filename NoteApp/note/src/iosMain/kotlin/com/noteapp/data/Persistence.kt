package com.noteapp.data

import platform.Foundation.NSUserDefaults

actual fun savePersistentData(key: String, value: String) {
    NSUserDefaults.standardUserDefaults.setObject(value, forKey = key)
    NSUserDefaults.standardUserDefaults.synchronize()
}

actual fun loadPersistentData(key: String): String {
    return NSUserDefaults.standardUserDefaults.stringForKey(key) ?: ""
}
