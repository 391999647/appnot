package com.noteapp.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.TextArea
import com.tencent.kuikly.core.module.RouterModule
import com.noteapp.AppRepo
import com.noteapp.data.exportFile
import com.noteapp.data.savePersistentData
import com.noteapp.data.loadPersistentData
import com.noteapp.model.Note
import com.noteapp.model.Tag
import com.noteapp.theme.ThemeColors
import com.noteapp.theme.ThemeStyles
import com.noteapp.theme.Icons
import com.noteapp.util.Sanitizer
import com.noteapp.util.UUID
import com.noteapp.util.currentTimeString
import com.noteapp.util.formatTimeOnly
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.timer.clearTimeout

@Page("NoteEditPage")
internal class NoteEditPage : Pager() {

    private var noteId by observable("")
    private var title by observable("无标题笔记")
    private var content by observable("")
    private var createdAt by observable("")
    private var noteTags by observableList<Tag>()
    private var allTags by observableList<Tag>()
    private var saveStatus by observable("已保存")
    private var showPreview by observable(true)
    private var showExportMenu by observable(false)
    private var showTagPicker by observable(false)
    private var newTagName by observable("")

    // Auto-save
    private var autoSaveTimerRef: String? = null
    private var hasUnsavedChanges by observable(false)
    private var isDraft by observable(false)

    // Toast
    private var toastMessage by observable("")
    private var showToast by observable(false)
    private var toastTimerRef: String? = null

    // Confirmation dialog
    private var showConfirmDialog by observable(false)
    private var confirmTitle by observable("")
    private var confirmMessage by observable("")
    private var confirmAction by observable<(() -> Unit)?>(null)

    // Loading state
    private var isLoading by observable(false)

    override fun created() {
        super.created()
        noteId = pagerData.params.optString("noteId", "")
        loadNote()
        loadTags()
        tryRestoreDraft()
    }

    override fun pageDidAppear() {
        super.pageDidAppear()
        startAutoSave()
    }

    override fun pageDidDisappear() {
        super.pageDidDisappear()
        cancelAutoSave()
        AppRepo.repo.flushChanges()
    }

    override fun pageWillDestroy() {
        super.pageWillDestroy()
        cancelAutoSave()
        saveDraft()
    }

    private fun loadNote() {
        val note = AppRepo.repo.getNoteById(noteId) ?: return
        title = note.title.ifBlank { "无标题笔记" }
        content = note.content
        createdAt = note.createdAt
        noteTags.clear(); noteTags.addAll(AppRepo.repo.getTagsForNote(note.id))
        isDraft = false
    }

    private fun loadTags() {
        allTags.clear(); allTags.addAll(AppRepo.repo.getAllTags())
    }

    /** Try to restore a draft if the note wasn't properly saved */
    private fun tryRestoreDraft() {
        val draftKey = "draft_$noteId"
        val draftData = loadPersistentData(draftKey)
        if (draftData.isNotEmpty() && draftData != "{}") {
            val existing = AppRepo.repo.getNoteById(noteId)
            if (existing != null) {
                try {
                    val draftJson = Json { ignoreUnknownKeys = true }
                    val draftNote = draftJson.decodeFromString(Note.serializer(), draftData)
                    if (draftNote.updatedAt != existing.updatedAt) {
                        title = draftNote.title.ifBlank { "无标题笔记" }
                        content = draftNote.content
                        isDraft = true
                        showToast("已恢复未保存的内容")
                    }
                } catch (_: Exception) { }
            }
        }
    }

    private fun saveDraft() {
        if (noteId.isBlank()) return
        val draftKey = "draft_$noteId"
        val draftNote = Note(
            id = noteId, title = title, content = content,
            createdAt = if (createdAt.isBlank()) currentTimeString() else createdAt,
            updatedAt = currentTimeString()
        )
        try {
            val draftJson = Json { prettyPrint = false }
            savePersistentData(draftKey, draftJson.encodeToString(Note.serializer(), draftNote))
        } catch (_: Exception) { }
    }

    private fun startAutoSave() {
        cancelAutoSave()
        autoSaveTimerRef = setTimeout(ThemeStyles.autoSaveInterval.toInt()) {
            if (hasUnsavedChanges) {
                doSave()
                saveDraft()
                hasUnsavedChanges = false
            }
            startAutoSave()
        }
    }

    private fun cancelAutoSave() {
        autoSaveTimerRef?.let { clearTimeout(it) }
        autoSaveTimerRef = null
    }

    private fun markChanged() {
        hasUnsavedChanges = true
    }

    private fun doSave() {
        val note = Note(
            id = noteId, title = title, content = content,
            createdAt = if (createdAt.isBlank()) currentTimeString() else createdAt,
            updatedAt = currentTimeString()
        )
        val existing = AppRepo.repo.getNoteById(noteId)
        if (existing != null) {
            AppRepo.repo.updateNote(note)
        } else {
            AppRepo.repo.insertNote(note)
        }
        saveStatus = "已保存 ${formatTimeOnly(currentTimeString())}"
        isDraft = false
        // Clear draft after successful save
        savePersistentData("draft_$noteId", "")
    }

    private fun doSaveAndClose() {
        cancelAutoSave()
        doSave()
        AppRepo.repo.flushChanges()
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
    }

    private fun doDelete() {
        showConfirmDialog("删除笔记", "确定要删除「${title.ifBlank { "无标题笔记" }}」吗？删除后可在回收站恢复。") {
            cancelAutoSave()
            AppRepo.repo.softDeleteNote(noteId, currentTimeString())
            savePersistentData("draft_$noteId", "")
            acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
        }
    }

    private fun doExportFormat(format: String) {
        val safeTitle = title.ifBlank { "无标题笔记" }
        when (format) {
            "Markdown (.md)" -> exportFile("${safeTitle}.md", "# $title\n\n$content", "text/markdown")
            "纯文本 (.txt)" -> exportFile("${safeTitle}.txt", "$title\n\n$content", "text/plain")
            "JSON (.json)" -> {
                val note = AppRepo.repo.getNoteById(noteId) ?: return
                val prettyJson = Json { prettyPrint = true }
                val noteJson = prettyJson.encodeToString(Note.serializer(), note)
                exportFile("${safeTitle}.json", noteJson, "application/json")
            }
        }
        showExportMenu = false
        showToast("已导出为 $format")
    }

    private fun doTitleChange(newTitle: String) {
        title = newTitle
        markChanged()
        doSave()
    }

    private fun doContentChange(newContent: String) {
        content = newContent
        markChanged()
        doSave()
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

    private fun showConfirmDialog(title: String, message: String, action: () -> Unit) {
        confirmTitle = title
        confirmMessage = message
        confirmAction = action
        showConfirmDialog = true
    }

    private fun dismissConfirmDialog() {
        showConfirmDialog = false
        confirmTitle = ""
        confirmMessage = ""
        confirmAction = null
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { flex(1f); flexDirectionColumn(); backgroundColor(ThemeColors.backgroundLight) }

            // ========== 顶部工具栏 ==========
            View {
                attr {
                    flexDirectionRow(); alignItemsCenter(); justifyContentSpaceBetween()
                    padding(top = 10f, left = 16f, bottom = 10f, right = 16f)
                    backgroundColor(ThemeColors.surface)
                }

                View {
                    attr { padding(right = 16f) }
                    event { click { ctx.doSaveAndClose() } }
                    Text { attr { text("${Icons.BACK} 返回"); fontSize(ThemeStyles.fontSizeBody); color(ThemeColors.primary) } }
                }

                View { attr { flexDirectionRow(); alignItemsCenter() }
                    if (ctx.isDraft) {
                        Text { attr { text("草稿"); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.warning); marginRight(6f) } }
                    }
                    Text { attr { text(ctx.saveStatus); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.textHint) } }
                }

                View { attr { flexDirectionRow(); alignItemsCenter() }
                    View {
                        attr { padding(top = 6f, left = 12f, bottom = 6f, right = 12f)
                            backgroundColor(ThemeColors.chipBg); borderRadius(ThemeStyles.borderRadiusChip); marginRight(8f) }
                        event { click { ctx.showTagPicker = true; ctx.loadTags() } }
                        Text { attr { text("${Icons.TAG} 标签"); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.textTertiary) } }
                    }
                    View {
                        attr { padding(top = 6f, left = 12f, bottom = 6f, right = 12f)
                            backgroundColor(ThemeColors.chipBg); borderRadius(ThemeStyles.borderRadiusChip); marginRight(8f) }
                        event { click { ctx.showPreview = !ctx.showPreview } }
                        Text { attr { text("${Icons.PREVIEW} ${if (ctx.showPreview) "隐藏预览" else "显示预览"}")
                            fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.textTertiary) } }
                    }
                    View {
                        attr { padding(top = 6f, left = 12f, bottom = 6f, right = 12f)
                            backgroundColor(ThemeColors.chipBg); borderRadius(ThemeStyles.borderRadiusChip); marginRight(8f) }
                        event { click { ctx.showExportMenu = true } }
                        Text { attr { text("${Icons.EXPORT} 导出"); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.textTertiary) } }
                    }
                    View {
                        attr { padding(top = 6f, left = 12f, bottom = 6f, right = 12f)
                            backgroundColor(ThemeColors.dangerLight); borderRadius(ThemeStyles.borderRadiusChip) }
                        event { click { ctx.doDelete() } }
                        Text { attr { text("${Icons.DELETE} 删除"); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.danger) } }
                    }
                }
            }

            // ========== 编辑区 + 预览区 ==========
            View {
                attr { flex(1f); flexDirectionRow() }

                // 编辑区
                View {
                    attr { flex(1f); flexDirectionColumn(); padding(top = 16f, left = 20f, right = 10f) }

                    Input {
                        attr {
                            text(ctx.title); fontSize(28f); fontWeightBold(); color(ThemeColors.textPrimary)
                            placeholder("输入标题..."); placeholderColor(ThemeColors.textPlaceholder); marginBottom(16f)
                        }
                        event { textDidChange { ctx.doTitleChange(it.text) } }
                    }

                    TextArea {
                        attr {
                            flex(1f); text(ctx.content); fontSize(ThemeStyles.fontSizeBody); color(Color(0xFF444444L))
                            placeholder("开始写笔记... 支持 Markdown 语法\n\n# 一级标题\n## 二级标题\n- 列表项\n**粗体** *斜体*")
                            placeholderColor(ThemeColors.textPlaceholder)
                        }
                        event { textDidChange { ctx.doContentChange(it.text) } }
                    }
                }

                // Markdown 预览区
                if (ctx.showPreview) {
                    View {
                        attr { flex(1f); flexDirectionColumn(); backgroundColor(ThemeColors.surface)
                            padding(top = 16f, left = 16f, bottom = 16f, right = 16f) }
                        Text { attr { text("预览"); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.textLight); marginBottom(8f) } }
                        List {
                            attr { flex(1f) }
                            val safeContent = Sanitizer.stripHtml(ctx.content)
                            val lines = safeContent.split("\n")
                            for (line in lines) {
                                when {
                                    line.startsWith("# ") -> Text {
                                        attr { text(line.removePrefix("# ")); fontSize(28f); fontWeightBold()
                                            marginTop(16f); marginBottom(8f); color(ThemeColors.textPrimary) } }
                                    line.startsWith("## ") -> Text {
                                        attr { text(line.removePrefix("## ")); fontSize(24f); fontWeightBold()
                                            marginTop(12f); marginBottom(6f); color(Color(0xFF444444L)) } }
                                    line.startsWith("### ") -> Text {
                                        attr { text(line.removePrefix("### ")); fontSize(20f); fontWeightBold()
                                            marginTop(10f); marginBottom(4f); color(ThemeColors.textSecondary) } }
                                    line.startsWith("- ") || line.startsWith("* ") -> Text {
                                        attr { text("  • ${line.removePrefix("- ").removePrefix("* ")}")
                                            fontSize(ThemeStyles.fontSizeBody); marginBottom(4f); color(ThemeColors.textSecondary) } }
                                    line.startsWith("> ") -> Text {
                                        attr { text(line.removePrefix("> ")); fontSize(ThemeStyles.fontSizeBody)
                                            color(Color(0xFF888888L)); marginBottom(4f) } }
                                    line.isBlank() -> View { attr { height(12f) } }
                                    else -> Text {
                                        attr { text(line); fontSize(ThemeStyles.fontSizeBody); color(ThemeColors.textSecondary); marginBottom(4f) } }
                                }
                            }
                        }
                    }
                }
            }

            // ========== 底部标签栏 ==========
            View {
                attr { flexDirectionRow(); alignItemsCenter(); justifyContentSpaceBetween()
                    padding(top = 8f, left = 16f, bottom = 8f, right = 16f); backgroundColor(ThemeColors.surface) }

                View { attr { flexDirectionRow(); flex(1f); flexWrapWrap() }
                    for (tag in ctx.noteTags) {
                        View {
                            attr { flexDirectionRow(); alignItemsCenter(); padding(top = 4f, left = 8f, bottom = 4f, right = 4f)
                                backgroundColor(ThemeColors.primaryLight); borderRadius(ThemeStyles.borderRadiusTag); marginRight(6f) }
                            Text { attr { text(tag.name); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.primary); marginRight(4f) } }
                            View {
                                attr { padding(top = 2f, left = 2f, bottom = 2f, right = 2f) }
                                event { click { AppRepo.repo.removeTagFromNote(ctx.noteId, tag.id); ctx.loadNote(); ctx.loadTags() } }
                                Text { attr { text(Icons.CLOSE); fontSize(ThemeStyles.fontSizeSmall); color(ThemeColors.textHint) } }
                            }
                        }
                    }
                }
                Text { attr { text("Markdown: # 标题  **粗体**  *斜体*  - 列表"); fontSize(ThemeStyles.fontSizeSmall); color(ThemeColors.textPlaceholder) } }
            }

            // ========== 标签选择浮层 ==========
            if (ctx.showTagPicker) {
                View {
                    attr { width(pagerData.pageViewWidth); height(pagerData.pageViewHeight)
                        positionAbsolute(); top(0f); left(0f); backgroundColor(ThemeColors.overlay)
                        justifyContentCenter(); alignItemsCenter() }
                    event { click { ctx.showTagPicker = false } }
                    View {
                        attr { width(320f); backgroundColor(ThemeColors.surface); borderRadius(ThemeStyles.borderRadiusCard)
                            padding(top = 20f, left = 20f, bottom = 20f, right = 20f) }
                        Text { attr { text("选择标签"); fontSize(ThemeStyles.fontSizeSubtitle); fontWeightBold(); marginBottom(16f) } }
                        for (tag in ctx.allTags) {
                            val isSelected = ctx.noteTags.any { it.id == tag.id }
                            View {
                                attr { flexDirectionRow(); alignItemsCenter(); padding(top = 8f, bottom = 8f) }
                                event { click { if (isSelected) AppRepo.repo.removeTagFromNote(ctx.noteId, tag.id)
                                    else AppRepo.repo.addTagToNote(ctx.noteId, tag.id); ctx.loadNote(); ctx.loadTags() } }
                                View { attr { width(18f); height(18f); borderRadius(9f)
                                    backgroundColor(if (isSelected) ThemeColors.primary else ThemeColors.border); marginRight(10f) } }
                                Text { attr { text(tag.name); fontSize(ThemeStyles.fontSizeBody); flex(1f) } }
                            }
                        }
                        View { attr { flexDirectionRow(); marginTop(12f) }
                            View {
                                attr { flex(1f); backgroundColor(Color(0xFFF5F5F5L)); borderRadius(ThemeStyles.borderRadiusChip)
                                    padding(top = 6f, left = 8f, bottom = 6f, right = 8f) }
                                Input {
                                    attr { flex(1f); text(ctx.newTagName); fontSize(ThemeStyles.fontSizeBody)
                                        placeholder("新建标签..."); placeholderColor(ThemeColors.textLight) }
                                    event { textDidChange { ctx.newTagName = it.text } }
                                }
                            }
                            View {
                                attr { padding(top = 6f, left = 12f, bottom = 6f, right = 12f)
                                    backgroundColor(ThemeColors.primary); borderRadius(ThemeStyles.borderRadiusChip); marginLeft(8f) }
                                event { click {
                                    if (ctx.newTagName.isNotBlank()) {
                                        val existingTag = AppRepo.repo.getTagByName(ctx.newTagName)
                                        val tagId = existingTag?.id ?: UUID.generate()
                                        if (existingTag == null) {
                                            val newTag = Tag(id = tagId, name = ctx.newTagName)
                                            AppRepo.repo.insertTag(newTag)
                                        }
                                        AppRepo.repo.addTagToNote(ctx.noteId, tagId)
                                        ctx.loadNote(); ctx.loadTags(); ctx.newTagName = ""
                                    } } }
                                Text { attr { text("添加"); fontSize(ThemeStyles.fontSizeCaption); color(Color.WHITE) } }
                            }
                        }
                        View { attr { marginTop(16f); alignSelfStretch(); padding(top = 10f, bottom = 10f)
                            backgroundColor(ThemeColors.primary); borderRadius(ThemeStyles.borderRadiusButton); alignItemsCenter() }
                            event { click { ctx.showTagPicker = false } }
                            Text { attr { text("完成"); fontSize(ThemeStyles.fontSizeBody); color(Color.WHITE) } }
                        }
                    }
                }
            }

            // ========== 导出菜单 ==========
            if (ctx.showExportMenu) {
                View {
                    attr { width(pagerData.pageViewWidth); height(pagerData.pageViewHeight)
                        positionAbsolute(); top(0f); left(0f); backgroundColor(ThemeColors.overlay)
                        justifyContentCenter(); alignItemsCenter() }
                    event { click { ctx.showExportMenu = false } }
                    View {
                        attr { width(280f); backgroundColor(ThemeColors.surface); borderRadius(ThemeStyles.borderRadiusCard)
                            padding(top = 20f, left = 20f, bottom = 20f, right = 20f) }
                        Text { attr { text("导出笔记"); fontSize(ThemeStyles.fontSizeSubtitle); fontWeightBold(); marginBottom(16f) } }
                        for (format in listOf("Markdown (.md)", "纯文本 (.txt)", "JSON (.json)")) { View {
                            attr { padding(top = 12f, bottom = 12f); flexDirectionRow(); alignItemsCenter() }
                            event { click { ctx.doExportFormat(format) } }
                            Text { attr { text(format); fontSize(ThemeStyles.fontSizeBody); flex(1f); color(ThemeColors.textPrimary) } }
                            Text { attr { text(Icons.FORWARD); fontSize(ThemeStyles.fontSizeBody); color(ThemeColors.textPlaceholder) } }
                        } }
                        View { attr { marginTop(16f); alignSelfStretch(); padding(top = 10f, bottom = 10f)
                            backgroundColor(ThemeColors.border); borderRadius(ThemeStyles.borderRadiusButton); alignItemsCenter() }
                            event { click { ctx.showExportMenu = false } }
                            Text { attr { text("取消"); fontSize(ThemeStyles.fontSizeBody); color(ThemeColors.textTertiary) } }
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
                    event { click { ctx.dismissConfirmDialog() } }
                    View {
                        attr { width(300f); backgroundColor(ThemeColors.surface); borderRadius(ThemeStyles.borderRadiusCard)
                            padding(top = 24f, left = 24f, bottom = 24f, right = 24f) }
                        Text { attr { text(ctx.confirmTitle); fontSize(ThemeStyles.fontSizeSubtitle); fontWeightBold(); marginBottom(8f); color(ThemeColors.textPrimary) } }
                        Text { attr { text(ctx.confirmMessage); fontSize(ThemeStyles.fontSizeBody); color(ThemeColors.textSecondary); marginBottom(24f) } }
                        View { attr { flexDirectionRow(); justifyContentFlexEnd() }
                            View {
                                attr { padding(top = 8f, left = 20f, bottom = 8f, right = 20f); backgroundColor(ThemeColors.chipBg); borderRadius(ThemeStyles.borderRadiusButton); marginRight(12f) }
                                event { click { ctx.dismissConfirmDialog() } }
                                Text { attr { text("取消"); fontSize(ThemeStyles.fontSizeBody); color(ThemeColors.textTertiary) } }
                            }
                            View {
                                attr { padding(top = 8f, left = 20f, bottom = 8f, right = 20f); backgroundColor(ThemeColors.danger); borderRadius(ThemeStyles.borderRadiusButton) }
                                event { click { val action = ctx.confirmAction; ctx.dismissConfirmDialog(); action?.invoke() } }
                                Text { attr { text("确认"); fontSize(ThemeStyles.fontSizeBody); color(Color.WHITE) } }
                            }
                        }
                    }
                }
            }
        }
    }
}
