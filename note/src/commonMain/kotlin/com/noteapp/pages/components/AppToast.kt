package com.noteapp.pages.components

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.clearTimeout
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.noteapp.theme.ThemeColors
import com.noteapp.theme.ThemeStyles

/**
 * Toast 状态管理器，封装消息、显示状态与自动隐藏 timer
 * 页面需持有此实例，并在 pageWillDestroy 中调用 clear()
 */
class AppToastHost {
    var message by observable("")
    var visible by observable(false)
    private var timerRef: String? = null

    /**
     * 显示 Toast，2.5s 后自动消失
     */
    fun show(msg: String) {
        message = msg
        visible = true
        timerRef?.let { clearTimeout(it) }
        timerRef = setTimeout(2500) {
            visible = false
            message = ""
        }
    }

    /**
     * 手动关闭当前 Toast
     */
    fun dismiss() {
        timerRef?.let { clearTimeout(it) }
        timerRef = null
        visible = false
        message = ""
    }

    /**
     * 清理 timer，防止页面销毁后回调
     */
    fun clear() {
        timerRef?.let { clearTimeout(it) }
        timerRef = null
    }
}

/**
 * Toast 组件（使用 AppToastHost 管理状态）
 * @param host Toast 状态管理器
 * @param hasFab 是否存在 FAB，存在时底部抬高避免遮挡
 */
fun ViewContainer<*, *>.AppToast(
    host: AppToastHost,
    hasFab: Boolean = false
) {
    AppToast(host.message, host.visible, hasFab)
}

/**
 * Toast 组件（纯渲染，状态由外部管理）
 * @param message 显示文本
 * @param visible 是否可见
 * @param hasFab 是否存在 FAB，存在时底部抬高避免遮挡
 */
fun ViewContainer<*, *>.AppToast(
    message: String,
    visible: Boolean,
    hasFab: Boolean = false
) {
    if (visible) {
        val bottomOffset = if (hasFab) 88f else 28f
        View {
            attr {
                positionAbsolute()
                bottom(bottomOffset)
                left(24f)
                right(24f)
                alignItemsCenter()
            }
            View {
                attr {
                    backgroundColor(Color(0xEE0F172AL))
                    borderRadius(ThemeStyles.borderRadiusChip)
                    padding(top = 12f, left = 22f, bottom = 12f, right = 22f)
                }
                Text {
                    attr {
                        text(message)
                        fontSize(ThemeStyles.fontSizeBody)
                        color(Color.WHITE)
                    }
                }
            }
        }
    }
}
