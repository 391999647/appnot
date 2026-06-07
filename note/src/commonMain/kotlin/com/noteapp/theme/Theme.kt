package com.noteapp.theme

import com.tencent.kuikly.core.base.Color

/**
 * 统一主题色常量
 */
object ThemeColors {
    val primary = Color(0xFF2563EBL)
    val primaryLight = Color(0xFFEFF6FFL)

    val background = Color(0xFFF8FAFCL)
    val backgroundLight = Color(0xFFF1F5F9L)
    val surface = Color.WHITE

    val textPrimary = Color(0xFF0F172AL)
    val textSecondary = Color(0xFF334155L)
    val textTertiary = Color(0xFF64748BL)
    val textHint = Color(0xFF94A3B8L)
    val textPlaceholder = Color(0xFFCBD5E1L)
    val textLight = Color(0xFF94A3B8L)

    val border = Color(0xFFE2E8F0L)
    val chipBg = Color(0xFFF1F5F9L)

    val danger = Color(0xFFDC2626L)
    val dangerLight = Color(0xFFFEF2F2L)
    val success = Color(0xFF16A34AL)
    val successLight = Color(0xFFF0FDF4L)

    val warning = Color(0xFFF59E0BL)
    val warningLight = Color(0xFFFFFBEBL)
    val info = Color(0xFF0284C7L)
    val infoLight = Color(0xFFE0F2FEL)

    val overlay = Color(0x80000000L)
    val transparent = Color.TRANSPARENT

    // 深色模式颜色
    val darkBackground = Color(0xFF121212L)
    val darkSurface = Color(0xFF1E1E1EL)
    val darkTextPrimary = Color(0xFFE0E0E0L)
    val darkTextSecondary = Color(0xFFBDBDBDL)
    val darkTextTertiary = Color(0xFF9E9E9EL)
    val darkBorder = Color(0xFF424242L)
    val darkChipBg = Color(0xFF2C2C2CL)
    val darkDangerLight = Color(0xFF3D1F1FL)
    val darkSuccessLight = Color(0xFF1F3D1FL)
    val darkWarningLight = Color(0xFF3D2F00L)
    val darkInfoLight = Color(0xFF0D2137L)
}

/**
 * 统一样式常量
 */
object ThemeStyles {
    // 圆角规范
    const val borderRadiusButton = 12f
    const val borderRadiusCard = 18f
    const val borderRadiusTag = 999f
    const val borderRadiusInput = 14f
    const val borderRadiusChip = 999f

    // 间距规范
    const val spacingPageHorizontal = 16f
    const val spacingPageVertical = 16f
    const val spacingComponentVertical = 8f
    const val spacingComponentHorizontal = 8f
    const val spacingInnerPadding = 14f

    // 字体大小规范
    const val fontSizeTitle = 22f
    const val fontSizeSubtitle = 17f
    const val fontSizeBody = 15f
    const val fontSizeCaption = 12f
    const val fontSizeSmall = 11f

    // 阴影
    const val shadowColor = 0x1A000000L
    const val shadowRadius = 4f
    const val shadowOffsetY = 2f

    // 尺寸常量
    const val fabSize = 56f
    const val sidebarWidth = 220f
    const val toastHeight = 44f
    const val batchBarHeight = 56f
    const val searchScopeWidth = 140f

    // 动画时长 (ms)
    const val animationFast = 150
    const val animationNormal = 300
    const val animationSlow = 500

    // 手势阈值
    const val swipeThreshold = 80f
    const val longPressDuration = 500L

    // 自动保存间隔 (ms)
    const val autoSaveInterval = 30_000L
}

/**
 * 主题模式
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * 窗口尺寸信息，用于响应式布局
 */
data class WindowInfo(val width: Float, val height: Float) {
    val isCompact: Boolean get() = width < 600f
}

/**
 * 颜色方案数据类，用于深色/浅色主题切换
 */
data class ThemeColorScheme(
    val primary: Color,
    val primaryLight: Color,
    val background: Color,
    val backgroundLight: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textHint: Color,
    val textPlaceholder: Color,
    val textLight: Color,
    val border: Color,
    val chipBg: Color,
    val danger: Color,
    val dangerLight: Color,
    val success: Color,
    val successLight: Color,
    val warning: Color,
    val warningLight: Color,
    val info: Color,
    val infoLight: Color,
    val overlay: Color,
    val transparent: Color
)

/**
 * 浅色主题颜色方案（基于现有常量）
 */
val ThemeColors.light: ThemeColorScheme
    get() = ThemeColorScheme(
        primary = primary,
        primaryLight = primaryLight,
        background = background,
        backgroundLight = backgroundLight,
        surface = surface,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textTertiary = textTertiary,
        textHint = textHint,
        textPlaceholder = textPlaceholder,
        textLight = textLight,
        border = border,
        chipBg = chipBg,
        danger = danger,
        dangerLight = dangerLight,
        success = success,
        successLight = successLight,
        warning = warning,
        warningLight = warningLight,
        info = info,
        infoLight = infoLight,
        overlay = overlay,
        transparent = transparent
    )

/**
 * 深色主题颜色方案
 */
val ThemeColors.dark: ThemeColorScheme
    get() = ThemeColorScheme(
        primary = primary,
        primaryLight = Color(0xFF1A3A5CL),
        background = darkBackground,
        backgroundLight = darkSurface,
        surface = darkSurface,
        textPrimary = darkTextPrimary,
        textSecondary = darkTextSecondary,
        textTertiary = darkTextTertiary,
        textHint = darkTextTertiary,
        textPlaceholder = Color(0xFF555555L),
        textLight = Color(0xFF777777L),
        border = darkBorder,
        chipBg = darkChipBg,
        danger = danger,
        dangerLight = darkDangerLight,
        success = success,
        successLight = darkSuccessLight,
        warning = warning,
        warningLight = darkWarningLight,
        info = info,
        infoLight = darkInfoLight,
        overlay = overlay,
        transparent = transparent
    )
