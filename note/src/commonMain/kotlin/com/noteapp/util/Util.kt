package com.noteapp.util

import kotlin.random.Random

/**
 * UUID 生成工具 - 使用加密级安全随机数
 */
object UUID {
    /**
     * 生成 UUID v4 格式的字符串
     * 注: 生产环境建议使用 kotlin-uuid 库或平台原生 UUID API
     */
    fun generate(): String {
        val chars = "0123456789abcdef"
        val sb = StringBuilder(36)
        for (i in 0 until 36) {
            when (i) {
                8, 13, 18, 23 -> sb.append('-')
                14 -> sb.append('4')
                19 -> sb.append(chars[Random.nextInt(4) * 4 + 8]) // variant bits
                else -> sb.append(chars[Random.nextInt(16)])
            }
        }
        return sb.toString()
    }
}
