package com.noteapp.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.module.RouterModule
import com.noteapp.AppRepo
import com.noteapp.model.Note
import com.noteapp.theme.ThemeColors
import com.noteapp.theme.ThemeStyles
import com.noteapp.theme.Icons
import com.noteapp.util.formatDateTimeOnly
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.timer.clearTimeout

@Page("RecycleBinPage")
internal class RecycleBinPage : Pager() {

    private var deletedNotes by observableList<Note>()

    // Toast
    private var toastMessage by observable("")
    private var showToast by observable(false)
    private var toastTimerRef: String? = null

    // Confirmation dialog
    private var showConfirmDialog by observable(false)
    private var confirmTitle by observable("")
    private var confirmMessage by observable("")
    private var confirmAction by observable<(() -> Unit)?>(null)

    override fun created() {
        super.created()
        loadDeletedNotes()
    }

    private fun loadDeletedNotes() {
        deletedNotes.clear()
        deletedNotes.addAll(AppRepo.repo.getDeletedNotes())
    }

    private fun showToast(message: String) {
        toastMessage = message
        showToast = true
        toastTimerRef?.let { clearTimeout(it) }
        toastTimerRef = setTimeout(2500) {
            showToast = false
            toastMessage = ""
        }
    }

    private fun showConfirm(title: String, message: String, action: () -> Unit) {
        confirmTitle = title
        confirmMessage = message
        confirmAction = action
        showConfirmDialog = true
    }

    private fun dismissConfirm() {
        showConfirmDialog = false
        confirmTitle = ""
        confirmMessage = ""
        confirmAction = null
    }

    private fun emptyTrash() {
        val count = deletedNotes.size
        if (count == 0) return
        showConfirm("清空回收站", "确定要永久删除 $count 篇笔记吗？此操作不可恢复。") {
            val ids = deletedNotes.map { it.id }.toList()
            for (id in ids) {
                AppRepo.repo.permanentlyDeleteNote(id)
            }
            loadDeletedNotes()
            showToast("已永久删除 $count 篇笔记")
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { flex(1f); flexDirectionColumn(); backgroundColor(ThemeColors.background) }

            // 顶部导航栏
            View {
                attr {
                    flexDirectionRow(); alignItemsCenter()
                    padding(top = 12f, left = 16f, bottom = 12f, right = 16f)
                    backgroundColor(ThemeColors.surface)
                }
                View {
                    attr { padding(right = 16f) }
                    event { click { ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage() } }
                    Text { attr { text("${Icons.BACK} 返回"); fontSize(ThemeStyles.fontSizeBody); color(ThemeColors.primary) } }
                }
                Text { attr { text("${Icons.DELETE} 回收站"); fontSize(ThemeStyles.fontSizeSubtitle); fontWeightBold(); flex(1f) } }
                if (ctx.deletedNotes.isNotEmpty()) {
                    View {
                        attr { padding(top = 6f, left = 12f, bottom = 6f, right = 12f); backgroundColor(ThemeColors.dangerLight); borderRadius(ThemeStyles.borderRadiusChip) }
                        event { click { ctx.emptyTrash() } }
                        Text { attr { text("清空"); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.danger) } }
                    }
                }
                Text { attr { text("共 ${ctx.deletedNotes.size} 篇"); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.textHint); marginLeft(8f) } }
            }

            // 笔记列表
            List {
                attr { flex(1f); padding(top = 16f, left = 16f, bottom = 16f, right = 16f) }
                if (ctx.deletedNotes.isEmpty()) {
                    View {
                        attr { flex(1f); alignItemsCenter(); justifyContentCenter(); paddingTop(100f) }
                        Text { attr { text(Icons.EMPTY_TRASH); fontSize(80f) } }
                        Text { attr { text("回收站为空"); fontSize(ThemeStyles.fontSizeSubtitle); color(ThemeColors.textLight); marginTop(12f) } }
                        Text { attr { text("已删除的笔记将显示在这里"); fontSize(ThemeStyles.fontSizeBody); color(ThemeColors.textPlaceholder); marginTop(8f) } }
                    }
                } else {
                    for (note in ctx.deletedNotes) {
                        View {
                            attr {
                                flexDirectionRow(); justifyContentSpaceBetween(); alignItemsCenter()
                                backgroundColor(ThemeColors.surface); borderRadius(ThemeStyles.borderRadiusCard)
                                padding(top = 12f, left = 16f, bottom = 12f, right = 16f); marginBottom(8f)
                            }
                            View {
                                attr { flex(1f); flexDirectionColumn() }
                                Text {
                                    attr { text(note.title.ifBlank { "无标题笔记" }); fontSize(ThemeStyles.fontSizeBody)
                                        fontWeightBold(); color(ThemeColors.textPrimary) } }
                                Text {
                                    attr { text("删除于 ${note.deletedAt?.let { formatDateTimeOnly(it) } ?: ""}")
                                        fontSize(ThemeStyles.fontSizeSmall); color(ThemeColors.textLight); marginTop(4f) } }
                            }
                            View {
                                attr { padding(top = 6f, left = 12f, bottom = 6f, right = 12f)
                                    backgroundColor(ThemeColors.successLight); borderRadius(ThemeStyles.borderRadiusChip); marginRight(8f) }
                                event { click { AppRepo.repo.restoreNote(note.id); ctx.loadDeletedNotes(); ctx.showToast("已恢复「${note.title.ifBlank { "无标题笔记" }}」") } }
                                Text { attr { text("${Icons.RESTORE} 恢复"); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.success) } }
                            }
                            View {
                                attr { padding(top = 6f, left = 12f, bottom = 6f, right = 12f)
                                    backgroundColor(ThemeColors.dangerLight); borderRadius(ThemeStyles.borderRadiusChip) }
                                event { click { ctx.showConfirm("永久删除", "确定要永久删除「${note.title.ifBlank { "无标题笔记" }}」吗？此操作不可恢复。") { AppRepo.repo.permanentlyDeleteNote(note.id); ctx.loadDeletedNotes(); ctx.showToast("已永久删除") } } }
                                Text { attr { text("${Icons.DELETE} 删除"); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.danger) } }
                            }
                        }
                    }
                }
            }

            // ========== Toast ==========
            if (ctx.showToast) {
                View {
                    attr { positionAbsolute(); bottom(80f); left(32f); right(32f); alignItemsCenter() }
                    View {
                        attr { backgroundColor(Color(0xE0000000L)); borderRadius(ThemeStyles.borderRadiusCard); padding(top = 10f, left = 20f, bottom = 10f, right = 20f) }
                        Text { attr { text(ctx.toastMessage); fontSize(ThemeStyles.fontSizeBody); color(Color.WHITE) } }
                    }
                }
            }

            // ========== 确认对话框 ==========
            if (ctx.showConfirmDialog) {
                View {
                    attr { width(pagerData.pageViewWidth); height(pagerData.pageViewHeight)
                        positionAbsolute(); top(0f); left(0f); backgroundColor(ThemeColors.overlay)
                        justifyContentCenter(); alignItemsCenter() }
                    event { click { ctx.dismissConfirm() } }
                    View {
                        attr { width(300f); backgroundColor(ThemeColors.surface); borderRadius(ThemeStyles.borderRadiusCard)
                            padding(top = 24f, left = 24f, bottom = 24f, right = 24f) }
                        Text { attr { text(ctx.confirmTitle); fontSize(ThemeStyles.fontSizeSubtitle); fontWeightBold(); marginBottom(8f); color(ThemeColors.textPrimary) } }
                        Text { attr { text(ctx.confirmMessage); fontSize(ThemeStyles.fontSizeBody); color(ThemeColors.textSecondary); marginBottom(24f) } }
                        View { attr { flexDirectionRow(); justifyContentFlexEnd() }
                            View {
                                attr { padding(top = 8f, left = 20f, bottom = 8f, right = 20f); backgroundColor(ThemeColors.chipBg); borderRadius(ThemeStyles.borderRadiusButton); marginRight(12f) }
                                event { click { ctx.dismissConfirm() } }
                                Text { attr { text("取消"); fontSize(ThemeStyles.fontSizeBody); color(ThemeColors.textTertiary) } }
                            }
                            View {
                                attr { padding(top = 8f, left = 20f, bottom = 8f, right = 20f); backgroundColor(ThemeColors.danger); borderRadius(ThemeStyles.borderRadiusButton) }
                                event { click { val action = ctx.confirmAction; ctx.dismissConfirm(); action?.invoke() } }
                                Text { attr { text("确认"); fontSize(ThemeStyles.fontSizeBody); color(Color.WHITE) } }
                            }
                        }
                    }
                }
            }
        }
    }
}
