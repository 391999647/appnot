package com.noteapp.util

/** 格式化时间，仅显示时分秒 */
fun formatTimeOnly(timeStr: String): String {
    // 格式: "YYYY-MM-DD HH:MM:SS" → "HH:MM:SS"
    return if (timeStr.length >= 19) timeStr.substring(11, 19) else timeStr
}

/** 格式化时间，仅显示月日时分秒 */
fun formatDateTimeOnly(timeStr: String): String {
    // 格式: "YYYY-MM-DD HH:MM:SS" → "MM-DD HH:MM:SS"
    return if (timeStr.length >= 19) timeStr.substring(5, 19) else timeStr
}
