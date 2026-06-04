package com.noteapp.data

import platform.Foundation.NSDate
import platform.Foundation.NSTimeInterval

actual fun currentTimeSeconds(): Long = NSDate().timeIntervalSince1970.toLong()
