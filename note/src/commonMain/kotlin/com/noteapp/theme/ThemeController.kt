package com.noteapp.theme

import com.noteapp.data.savePersistentData
import com.noteapp.data.loadPersistentData

/**
 * 主题控制器，管理应用主题模式及持久化
 */
object ThemeController {
    private const val KEY_THEME_MODE = "theme_mode"

    var mode: ThemeMode = ThemeMode.LIGHT
        private set

    var isSystemDark: Boolean = false
        private set

    /**
     * 当前应使用的颜色方案
     */
    val colors: ThemeColorScheme
        get() = when (mode) {
            ThemeMode.LIGHT -> ThemeColors.light
            ThemeMode.DARK -> ThemeColors.dark
            ThemeMode.SYSTEM -> if (isSystemDark) ThemeColors.dark else ThemeColors.light
        }

    /**
     * 初始化，从持久化加载主题模式
     */
    fun init() {
        mode = when (loadPersistentData(KEY_THEME_MODE)) {
            "dark" -> ThemeMode.DARK
            "system" -> ThemeMode.SYSTEM
            else -> ThemeMode.LIGHT
        }
    }

    /**
     * 设置主题模式并持久化
     */
    fun setMode(newMode: ThemeMode) {
        mode = newMode
        savePersistentData(KEY_THEME_MODE, newMode.name.lowercase())
    }
}
