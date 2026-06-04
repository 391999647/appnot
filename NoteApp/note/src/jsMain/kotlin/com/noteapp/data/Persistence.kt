package com.noteapp.data

actual fun savePersistentData(key: String, value: String) {
    js("localStorage.setItem(key, value)")
}

actual fun loadPersistentData(key: String): String {
    val result: String = js("localStorage.getItem(key) || ''")
    return result
}
