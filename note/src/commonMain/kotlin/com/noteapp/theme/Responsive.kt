package com.noteapp.theme

/**
 * 响应式断点常量
 */
object BreakPoints {
    const val COMPACT = 600f
}

/**
 * 根据窗口尺寸返回响应式水平内边距
 */
fun responsivePadding(window: WindowInfo): Float = when {
    window.isCompact -> 12f
    else -> 16f
}
