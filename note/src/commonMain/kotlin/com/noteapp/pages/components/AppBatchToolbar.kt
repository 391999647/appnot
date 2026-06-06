package com.noteapp.pages.components

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.noteapp.theme.ThemeColors
import com.noteapp.theme.ThemeStyles

/**
 * 批量操作按钮样式
 */
enum class BatchActionStyle {
    Default,
    Primary,
    Danger,
    Success
}

/**
 * 批量操作项
 * @param icon 图标资源名
 * @param label 按钮文字
 * @param style 按钮样式
 * @param onClick 点击回调
 */
data class BatchAction(
    val icon: String,
    val label: String,
    val style: BatchActionStyle = BatchActionStyle.Default,
    val onClick: () -> Unit
)

/**
 * 批量操作工具栏
 *
 * 当已选数量大于 0 时显示，左侧展示"已选 x / 共 y 篇"，
 * 右侧渲染传入的操作按钮列表。
 *
 * @param selectedCount 已选数量
 * @param totalCount 总数量
 * @param actions 操作按钮列表，从左到右依次排列
 */
fun ViewContainer<*, *>.AppBatchToolbar(
    selectedCount: Int,
    totalCount: Int,
    actions: List<BatchAction>
) {
    if (selectedCount <= 0) return

    View {
        attr {
            flexDirectionRow()
            alignItemsCenter()
            justifyContentSpaceBetween()
            padding(top = 8f, left = 16f, bottom = 8f, right = 16f)
            backgroundColor(ThemeColors.surface)
        }

        Text {
            attr {
                text("已选 $selectedCount / 共 $totalCount 篇")
                fontSize(ThemeStyles.fontSizeBody)
                color(ThemeColors.textPrimary)
            }
        }

        View {
            attr {
                flexDirectionRow()
                alignItemsCenter()
            }
            for ((index, action) in actions.withIndex()) {
                val isLast = index == actions.lastIndex
                val bgColor = when (action.style) {
                    BatchActionStyle.Primary -> ThemeColors.primaryLight
                    BatchActionStyle.Danger -> ThemeColors.dangerLight
                    BatchActionStyle.Success -> ThemeColors.successLight
                    BatchActionStyle.Default -> ThemeColors.chipBg
                }
                val textColor = when (action.style) {
                    BatchActionStyle.Primary -> ThemeColors.primary
                    BatchActionStyle.Danger -> ThemeColors.danger
                    BatchActionStyle.Success -> ThemeColors.success
                    BatchActionStyle.Default -> ThemeColors.textTertiary
                }
                View {
                    attr {
                        padding(top = 6f, left = 12f, bottom = 6f, right = 12f)
                        backgroundColor(bgColor)
                        borderRadius(ThemeStyles.borderRadiusChip)
                        if (!isLast) marginRight(8f)
                    }
                    event { click { action.onClick() } }
                    View {
                        attr {
                            flexDirectionRow()
                            alignItemsCenter()
                        }
                        Image {
                            attr {
                                src(action.icon)
                                width(14f)
                                height(14f)
                                marginRight(4f)
                            }
                        }
                        Text {
                            attr {
                                text(action.label)
                                fontSize(ThemeStyles.fontSizeCaption)
                                color(textColor)
                            }
                        }
                    }
                }
            }
        }
    }
}
