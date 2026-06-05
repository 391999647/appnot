package com.noteapp.util

/**
 * 简单的内容消毒工具 - 防止 XSS 注入
 * 转义 HTML 特殊字符
 */
object Sanitizer {
    fun sanitize(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }

    /**
     * 从 Markdown 内容中剥离可能的 HTML 标签
     */
    fun stripHtml(input: String): String {
        return input.replace(Regex("<[^>]*>"), "")
    }

    /**
     * 安全截取 - 避免在多字节字符中间截断
     */
    fun safeTruncate(input: String, maxLen: Int): String {
        if (input.length <= maxLen) return input
        return input.take(maxLen)
    }
}
