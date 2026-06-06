package com.noteapp.util

import kotlin.random.Random

/**
 * UUID 生成工具 - 使用伪随机数生成器
 */
object UUID {
    /**
     * 生成 UUID v4 格式的字符串
     * 注: 使用 kotlin.random.Random，非加密安全。
     *     如需加密安全 UUID，建议使用平台原生 API（如 java.util.UUID 或 SecureRandom）。
     */
    fun generate(): String {
        val chars = "0123456789abcdef"
        val sb = StringBuilder(36)
        for (i in 0 until 36) {
            when (i) {
                8, 13, 18, 23 -> sb.append('-')
                14 -> sb.append('4')
                19 -> sb.append(chars[8 + Random.nextInt(4)]) // variant bits: 8=8,9,10,11 → '8','9','a','b'
                else -> sb.append(chars[Random.nextInt(16)])
            }
        }
        return sb.toString()
    }
}
