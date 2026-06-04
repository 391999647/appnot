package com.noteapp.theme

import com.tencent.kuikly.core.base.Color

/**
 * 统一主题色常量
 */
object ThemeColors {
    val primary = Color(0xFF4A90D9L)
    val primaryLight = Color(0xFFE8F0FEL)

    val background = Color(0xFFF5F5F5L)
    val backgroundLight = Color(0xFFFAFAFAL)
    val surface = Color.WHITE

    val textPrimary = Color(0xFF333333L)
    val textSecondary = Color(0xFF555555L)
    val textTertiary = Color(0xFF666666L)
    val textHint = Color(0xFF999999L)
    val textPlaceholder = Color(0xFFCCCCCCL)
    val textLight = Color(0xFFBBBBBBL)

    val border = Color(0xFFE0E0E0L)
    val chipBg = Color(0xFFF0F0F0L)

    val danger = Color(0xFFE06060L)
    val dangerLight = Color(0xFFFFF0F0L)
    val success = Color(0xFF50C050L)
    val successLight = Color(0xFFE8FCE8L)

    val warning = Color(0xFFFF9500L)
    val warningLight = Color(0xFFFFF8E1L)
    val info = Color(0xFF2196F3L)
    val infoLight = Color(0xFFE3F2FDL)

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
    const val borderRadiusButton = 8f
    const val borderRadiusCard = 12f
    const val borderRadiusTag = 10f
    const val borderRadiusInput = 8f
    const val borderRadiusChip = 6f

    // 间距规范
    const val spacingPageHorizontal = 16f
    const val spacingPageVertical = 16f
    const val spacingComponentVertical = 8f
    const val spacingComponentHorizontal = 8f
    const val spacingInnerPadding = 12f

    // 字体大小规范
    const val fontSizeTitle = 20f
    const val fontSizeSubtitle = 18f
    const val fontSizeBody = 14f
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
