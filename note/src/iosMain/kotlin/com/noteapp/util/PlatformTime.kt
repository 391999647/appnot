package com.noteapp.util

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDate

actual fun currentTimeString(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
    return formatter.stringFromDate(NSDate())
}
