package com.noteapp.pages.components

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.noteapp.theme.ThemeColors
import com.noteapp.theme.ThemeStyles

/**
 * 确认按钮样式
 */
enum class DialogConfirmStyle {
    Normal,
    Danger
}

/**
 * 确认对话框组件
 *
 * 注意：遮罩层不绑定 click 事件，防止误触关闭。
 * 调用方需在 onCancel / onConfirm 回调中自行控制显隐状态。
 *
 * @param visible 是否显示
 * @param title 标题
 * @param message 提示内容
 * @param confirmText 确认按钮文字，默认"确认"
 * @param cancelText 取消按钮文字，默认"取消"
 * @param confirmStyle 确认按钮样式，危险操作用 Danger（红色）
 * @param onCancel 取消回调
 * @param onConfirm 确认回调
 */
fun ViewContainer<*, *>.AppConfirmDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmText: String = "确认",
    cancelText: String = "取消",
    confirmStyle: DialogConfirmStyle = DialogConfirmStyle.Normal,
    onCancel: (() -> Unit)? = null,
    onConfirm: () -> Unit
) {
    if (visible) {
        View {
            attr {
                positionAbsolute()
                top(0f)
                left(0f)
                right(0f)
                bottom(0f)
                backgroundColor(ThemeColors.overlay)
                justifyContentCenter()
                alignItemsCenter()
            }
            // 遮罩层不绑定 click 事件，防止误触关闭
            View {
                attr {
                    width(300f)
                    backgroundColor(ThemeColors.surface)
                    borderRadius(ThemeStyles.borderRadiusCard)
                    padding(top = 24f, left = 24f, bottom = 24f, right = 24f)
                }
                Text {
                    attr {
                        text(title)
                        fontSize(ThemeStyles.fontSizeSubtitle)
                        fontWeightBold()
                        marginBottom(8f)
                        color(ThemeColors.textPrimary)
                    }
                }
                Text {
                    attr {
                        text(message)
                        fontSize(ThemeStyles.fontSizeBody)
                        color(ThemeColors.textSecondary)
                        marginBottom(24f)
                    }
                }
                View {
                    attr {
                        flexDirectionRow()
                        justifyContentFlexEnd()
                    }
                    View {
                        attr {
                            padding(top = 8f, left = 20f, bottom = 8f, right = 20f)
                            backgroundColor(ThemeColors.chipBg)
                            borderRadius(ThemeStyles.borderRadiusButton)
                            marginRight(12f)
                        }
                        event { click { onCancel?.invoke() } }
                        Text {
                            attr {
                                text(cancelText)
                                fontSize(ThemeStyles.fontSizeBody)
                                color(ThemeColors.textTertiary)
                            }
                        }
                    }
                    View {
                        attr {
                            padding(top = 8f, left = 20f, bottom = 8f, right = 20f)
                            backgroundColor(
                                if (confirmStyle == DialogConfirmStyle.Danger) ThemeColors.danger else ThemeColors.primary
                            )
                            borderRadius(ThemeStyles.borderRadiusButton)
                        }
                        event { click { onConfirm() } }
                        Text {
                            attr {
                                text(confirmText)
                                fontSize(ThemeStyles.fontSizeBody)
                                color(Color.WHITE)
                            }
                        }
                    }
                }
            }
        }
    }
}
