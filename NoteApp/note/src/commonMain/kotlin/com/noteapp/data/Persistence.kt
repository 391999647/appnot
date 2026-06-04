package com.noteapp.data

expect fun savePersistentData(key: String, value: String)
expect fun loadPersistentData(key: String): String
