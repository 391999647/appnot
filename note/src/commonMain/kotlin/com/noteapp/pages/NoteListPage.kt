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
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.module.RouterModule
import com.noteapp.AppRepo
import com.noteapp.data.NoteRepository
import com.noteapp.model.Note
import com.noteapp.model.Tag
import com.noteapp.theme.ThemeColors
import com.noteapp.theme.ThemeStyles
import com.noteapp.theme.Icons
import com.noteapp.util.UUID
import com.noteapp.util.currentTimeString
import com.noteapp.util.formatDateTimeOnly
import com.noteapp.data.savePersistentData
import com.noteapp.data.loadPersistentData
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.timer.clearTimeout
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Page("NoteListPage")
internal class NoteListPage : Pager() {

    private var notes by observableList<Note>()
    private var tags by observableList<Tag>()
    private var searchQuery by observable("")
    private var filterTagId by observable<String?>(null)
    private var filterTagName by observable("全部笔记")
    private var showSidebar by observable(false)
    private var showTagManage by observable(false)
    private var errorMsg by observable<String?>(null)

    // Search enhancement
    private var searchHistory by observableList<String>()
    private var searchScope by observable(NoteRepository.SearchScope.TITLE_AND_CONTENT)
    private var showSearchHistory by observable(false)
    private var showSearchScopePicker by observable(false)
    private val searchHistoryKey = "search_history"
    private val sortByKey = "sort_by"
    private val filterTagIdKey = "filter_tag_id"
    private val filterTagNameKey = "filter_tag_name"

    // Sort
    private var sortBy by observable(SortBy.UPDATED_TIME)
    private var showSortPicker by observable(false)

    // Batch operations
    private var isMultiSelectMode by observable(false)
    private var selectedNoteIds by observableList<String>()

    // New tag in sidebar
    private var newTagName by observable("")
    private var showNewTagInput by observable(false)

    // Toast
    private var toastMessage by observable("")
    private var showToast by observable(false)
    private var toastTimerRef: String? = null
    private var searchDebounceRef: String? = null

    // Confirmation dialog
    private var showConfirmDialog by observable(false)
    private var confirmTitle by observable("")
    private var confirmMessage by observable("")
    private var confirmAction by observable<(() -> Unit)?>(null)
    private var cancelAction by observable<(() -> Unit)?>(null)

    // Batch tag picker
    private var showBatchTagPicker by observable(false)

    override fun created() {
        super.created()
        loadSearchHistory()
        loadPersistentSortAndFilter()
        loadData()
    }

    private fun loadPersistentSortAndFilter() {
        val savedSort = loadPersistentData(sortByKey)
        if (savedSort.isNotBlank()) {
            try {
                val ordinal = savedSort.toInt()
                sortBy = SortBy.entries.getOrElse(ordinal) { SortBy.UPDATED_TIME }
            } catch (_: Exception) { }
        }
        val savedTagId = loadPersistentData(filterTagIdKey)
        if (savedTagId.isNotBlank()) {
            filterTagId = savedTagId
            filterTagName = loadPersistentData(filterTagNameKey).ifBlank { "全部笔记" }
        }
    }

    private fun persistSortBy() {
        savePersistentData(sortByKey, sortBy.ordinal.toString())
    }

    private fun persistFilterTag() {
        filterTagId?.let { savePersistentData(filterTagIdKey, it) } ?: savePersistentData(filterTagIdKey, "")
        savePersistentData(filterTagNameKey, filterTagName)
    }

    private fun loadSearchHistory() {
        val saved = loadPersistentData(searchHistoryKey)
        if (saved.isNotEmpty() && saved != "{}") {
            try {
                val list = Json.decodeFromString<List<String>>(saved)
                searchHistory.clear(); searchHistory.addAll(list)
            } catch (_: Exception) { }
        }
    }

    private fun persistSearchHistory() {
        try {
            savePersistentData(searchHistoryKey, Json.encodeToString(searchHistory.toList()))
        } catch (_: Exception) { }
    }

    override fun pageWillDestroy() {
        super.pageWillDestroy()
        toastTimerRef?.let { clearTimeout(it) }
        toastTimerRef = null
        searchDebounceRef?.let { clearTimeout(it) }
        searchDebounceRef = null
    }

    override fun pageDidAppear() {
        super.pageDidAppear()
        refreshNotes()
        checkLoadError()
    }

    private fun loadData() {
        val repo = AppRepo.repo
        tags.clear(); tags.addAll(repo.getAllTags())
        refreshNotes()
        checkLoadError()
    }

    private fun checkLoadError() {
        if (AppRepo.repo.lastLoadFailed) {
            errorMsg = "数据加载失败，已自动备份原数据。请检查浏览器存储空间。"
        }
    }

    private fun refreshNotes() {
        val repo = AppRepo.repo
        var result = repo.getAllNotes()
        filterTagId?.let {
            val tagNoteIds = repo.getNotesForTag(it).map { n -> n.id }.toSet()
            result = result.filter { note -> note.id in tagNoteIds }
        }
        if (searchQuery.isNotBlank()) {
            result = repo.searchNotes(searchQuery, searchScope).filter { note -> result.any { it.id == note.id } }
        }
        result = when (sortBy) {
            SortBy.UPDATED_TIME -> result.sortedByDescending { it.updatedAt }
            SortBy.CREATED_TIME -> result.sortedByDescending { it.createdAt }
            SortBy.TITLE -> result.sortedBy { it.title.lowercase() }
        }
        notes.clear(); notes.addAll(result)
    }

    private fun performSearch(query: String, saveHistory: Boolean = false) {
        searchQuery = query
        if (saveHistory && query.isNotBlank()) {
            searchHistory.removeAll { it == query }
            searchHistory.add(0, query)
            if (searchHistory.size > 10) {
                searchHistory.removeAt(searchHistory.lastIndex)
            }
            persistSearchHistory()
        }
        showSearchHistory = false
        refreshNotes()
    }

    private fun debouncedSearch(query: String) {
        searchDebounceRef?.let { clearTimeout(it) }
        searchDebounceRef = setTimeout(300) {
            performSearch(query, saveHistory = false)
            searchDebounceRef = null
        }
    }

    private fun clearSearchHistory() {
        searchHistory.clear()
        persistSearchHistory()
    }

    private fun toggleMultiSelectMode() {
        isMultiSelectMode = !isMultiSelectMode
        if (!isMultiSelectMode) {
            selectedNoteIds.clear()
        }
    }

    private fun toggleNoteSelection(noteId: String) {
        if (selectedNoteIds.contains(noteId)) {
            selectedNoteIds.removeAll { it == noteId }
        } else {
            selectedNoteIds.add(noteId)
        }
        if (selectedNoteIds.isEmpty()) {
            isMultiSelectMode = false
        }
    }

    private fun selectAllNotes() {
        selectedNoteIds.clear()
        selectedNoteIds.addAll(notes.map { it.id })
    }

    private fun deselectAllNotes() {
        selectedNoteIds.clear()
    }

    private fun batchDelete() {
        if (selectedNoteIds.isEmpty()) return
        showConfirm(
            "批量删除",
            "确定要删除选中的 ${selectedNoteIds.size} 篇笔记吗？",
            { doBatchDeleteConfirmed() },
            null
        )
    }

    private fun doBatchDeleteConfirmed() {
        val ids = selectedNoteIds.toList()
        AppRepo.repo.softDeleteNotes(ids, currentTimeString())
        selectedNoteIds.clear()
        isMultiSelectMode = false
        refreshNotes()
        showToast("已删除 ${ids.size} 篇笔记")
    }

    private fun showBatchTagDialog() {
        showBatchTagPicker = true
    }

    private fun doBatchTag(tagId: String) {
        val ids = selectedNoteIds.toList()
        AppRepo.repo.addTagToNotes(ids, tagId)
        showBatchTagPicker = false
        selectedNoteIds.clear()
        isMultiSelectMode = false
        refreshNotes()
        showToast("已为 ${ids.size} 篇笔记添加标签")
    }

    private fun doBatchExport() {
        val ids = selectedNoteIds.toList()
        val sb = StringBuilder()
        for (id in ids) {
            val note = AppRepo.repo.getNoteById(id) ?: continue
            sb.appendLine("# ${note.title}")
            sb.appendLine()
            sb.appendLine(note.content)
            sb.appendLine("\n---\n")
        }
        val content = sb.toString()
        val filename = "ntnotes_export_${currentTimeString().replace(":", "-")}.md"
        com.noteapp.data.exportFile(filename, content, "text/markdown")
        selectedNoteIds.clear()
        isMultiSelectMode = false
        showToast("已导出 ${ids.size} 篇笔记")
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

    private fun showConfirm(title: String, message: String, onConfirm: (() -> Unit)?, onCancel: (() -> Unit)?) {
        confirmTitle = title
        confirmMessage = message
        confirmAction = onConfirm
        cancelAction = onCancel
        showConfirmDialog = true
    }

    private fun dismissConfirm() {
        showConfirmDialog = false
        confirmTitle = ""
        confirmMessage = ""
        confirmAction = null
        cancelAction = null
    }

    private fun confirmConfirmed() {
        val action = confirmAction
        dismissConfirm()
        action?.invoke()
    }

    private fun confirmCancelled() {
        val action = cancelAction
        dismissConfirm()
        action?.invoke()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { flex(1f); flexDirectionColumn(); backgroundColor(ThemeColors.background) }

            // ========== 顶部导航栏 ==========
            View {
                attr {
                    flexDirectionRow(); alignItemsCenter()
                    padding(top = 12f, left = 16f, bottom = 12f, right = 16f)
                    backgroundColor(ThemeColors.surface)
                }

                View {
                    attr { padding(right = 12f) }
                    event { click { ctx.showSidebar = !ctx.showSidebar } }
                    Image { attr { src(if (ctx.showSidebar) Icons.CLOSE else Icons.MENU); width(22f); height(22f) } }
                }

                if (ctx.isMultiSelectMode) {
                    View {
                        attr { flex(1f); flexDirectionRow(); alignItemsCenter() }
                        Text { attr { text("已选 ${ctx.selectedNoteIds.size}/${ctx.notes.size}"); fontSize(ThemeStyles.fontSizeBody); color(ThemeColors.textPrimary); flex(1f) } }
                        View {
                            attr { padding(top = 4f, left = 8f, bottom = 4f, right = 8f); backgroundColor(ThemeColors.chipBg); borderRadius(ThemeStyles.borderRadiusChip); marginRight(6f) }
                            event { click { if (ctx.selectedNoteIds.size == ctx.notes.size) ctx.deselectAllNotes() else ctx.selectAllNotes() } }
                            Text { attr { text(if (ctx.selectedNoteIds.size == ctx.notes.size) "取消全选" else "全选"); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.textTertiary) } }
                        }
                        View {
                            attr { padding(top = 4f, left = 8f, bottom = 4f, right = 8f); backgroundColor(ThemeColors.chipBg); borderRadius(ThemeStyles.borderRadiusChip) }
                            event { click { ctx.toggleMultiSelectMode() } }
                            Text { attr { text("取消"); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.textTertiary) } }
                        }
                    }
                } else {
                    View {
                        attr {
                            flex(1f); flexDirectionRow(); alignItemsCenter()
                            backgroundColor(ThemeColors.chipBg); borderRadius(ThemeStyles.borderRadiusInput)
                            padding(top = 6f, left = 12f, bottom = 6f, right = 12f)
                        }
                        event { click { ctx.showSearchHistory = !ctx.showSearchHistory; ctx.showSearchScopePicker = false } }
                        Input {
                            attr {
                                text(ctx.searchQuery); fontSize(ThemeStyles.fontSizeBody); flex(1f)
                                placeholder("搜索笔记..."); placeholderColor(ThemeColors.textHint)
                                color(ThemeColors.textPrimary)
                            }
                            event { textDidChange { ctx.debouncedSearch(it.text) } }
                        }
                    }

                    View {
                        attr { padding(top = 4f, left = 4f, bottom = 4f, right = 4f); marginLeft(4f) }
                        event { click { ctx.showSearchScopePicker = !ctx.showSearchScopePicker; ctx.showSearchHistory = false } }
                        Text { attr { text(ctx.searchScope.displayName()); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.textTertiary) } }
                    }
                }
            }

            // ========== 搜索范围选择器浮层 ==========
            if (ctx.showSearchScopePicker) {
                View {
                    attr { positionAbsolute(); top(52f); left(16f); backgroundColor(ThemeColors.surface); borderRadius(ThemeStyles.borderRadiusCard); padding(top = 4f, bottom = 4f); zIndex(100) }
                    for (scope in NoteRepository.SearchScope.values()) {
                        View {
                            attr { padding(top = 10f, left = 16f, bottom = 10f, right = 16f); flexDirectionRow(); alignItemsCenter() }
                            event { click { ctx.searchScope = scope; ctx.showSearchScopePicker = false; ctx.refreshNotes() } }
                            Text { attr { text(scope.displayName()); fontSize(ThemeStyles.fontSizeBody); color(if (ctx.searchScope == scope) ThemeColors.primary else ThemeColors.textPrimary); marginRight(8f) } }
                            if (ctx.searchScope == scope) {
                                Image { attr { src(Icons.CHECK); width(16f); height(16f) } }
                            }
                        }
                    }
                }
            }

            // ========== 搜索历史浮层 ==========
            if (ctx.showSearchHistory && ctx.searchHistory.isNotEmpty()) {
                View {
                    attr { positionAbsolute(); top(52f); left(16f); right(16f); backgroundColor(ThemeColors.surface); borderRadius(ThemeStyles.borderRadiusCard); padding(top = 8f, bottom = 8f); zIndex(100) }
                    View {
                        attr { flexDirectionRow(); justifyContentSpaceBetween(); alignItemsCenter(); padding(top = 4f, left = 16f, bottom = 8f, right = 16f) }
                        Text { attr { text("搜索历史"); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.textHint) } }
                        View {
                            attr { padding(top = 2f, left = 6f, bottom = 2f, right = 6f) }
                            event { click { ctx.clearSearchHistory() } }
                            Text { attr { text("清除"); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.textTertiary) } }
                        }
                    }
                    for (query in ctx.searchHistory) {
                        View {
                            attr { padding(top = 8f, left = 16f, bottom = 8f, right = 16f); flexDirectionRow(); alignItemsCenter() }
                            event { click { ctx.performSearch(query, saveHistory = true) } }
                            Text { attr { text(query); fontSize(ThemeStyles.fontSizeBody); color(ThemeColors.textPrimary); flex(1f) } }
                            Image { attr { src(Icons.HISTORY); width(16f); height(16f) } }
                        }
                    }
                }
            }

            // ========== 错误提示条 ==========
            ctx.errorMsg?.let { msg ->
                View {
                    attr {
                        padding(top = 8f, left = 16f, bottom = 8f, right = 16f)
                        backgroundColor(ThemeColors.dangerLight)
                    }
                    Text {
                        attr { text(msg); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.danger) }
                    }
                }
            }

            // ========== 主体: 侧边栏 + 笔记列表 ==========
            View {
                attr { flex(1f); flexDirectionRow() }

                // 侧边栏
                if (ctx.showSidebar) {
                    View {
                        attr { width(ThemeStyles.sidebarWidth); flexDirectionColumn(); backgroundColor(ThemeColors.surface); padding(top = 8f) }

                        View {
                            attr { flexDirectionRow(); justifyContentSpaceBetween(); alignItemsCenter()
                                padding(top = 12f, left = 16f, bottom = 12f, right = 16f) }
                            Text { attr { text("标签"); fontSize(ThemeStyles.fontSizeSubtitle); fontWeightBold(); color(ThemeColors.textPrimary) } }
                            View {
                                attr { padding(top = 4f, left = 8f, bottom = 4f, right = 8f)
                                    backgroundColor(ThemeColors.chipBg); borderRadius(ThemeStyles.borderRadiusChip) }
                                event { click { ctx.showTagManage = true } }
                                Text { attr { text("管理"); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.textTertiary) } }
                            }
                        }

                        View {
                            attr { padding(top = 10f, left = 16f, bottom = 10f, right = 16f)
                                backgroundColor(if (ctx.filterTagId == null) ThemeColors.primaryLight else ThemeColors.transparent) }
                            event { click { ctx.filterTagId = null; ctx.filterTagName = "全部笔记"; ctx.persistFilterTag(); ctx.refreshNotes() } }
                            Text { attr { text("全部笔记 (${AppRepo.repo.activeNoteCount()})"); fontSize(ThemeStyles.fontSizeBody)
                                color(if (ctx.filterTagId == null) ThemeColors.primary else ThemeColors.textPrimary) } }
                        }

                        for (tag in ctx.tags) {
                            View {
                                attr { padding(top = 10f, left = 16f, bottom = 10f, right = 16f)
                                    backgroundColor(if (ctx.filterTagId == tag.id) ThemeColors.primaryLight else ThemeColors.transparent) }
                                event { click { ctx.filterTagId = tag.id; ctx.filterTagName = tag.name; ctx.persistFilterTag(); ctx.refreshNotes() } }
                                Text { attr { text("${tag.name} (${AppRepo.repo.getNotesForTag(tag.id).size})"); fontSize(ThemeStyles.fontSizeBody)
                                    color(if (ctx.filterTagId == tag.id) ThemeColors.primary else ThemeColors.textPrimary) } }
                            }
                        }

                        View { attr { height(1f); backgroundColor(ThemeColors.border); marginTop(8f); marginBottom(8f) } }

                        View {
                            attr { padding(top = 12f, left = 16f, bottom = 12f, right = 16f); flexDirectionRow(); alignItemsCenter() }
                            event { click { ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage("RecycleBinPage", com.tencent.kuikly.core.nvi.serialization.json.JSONObject()) } }
                            View {
                                attr { flexDirectionRow(); alignItemsCenter() }
                                Image { attr { src(Icons.EMPTY_TRASH); width(16f); height(16f); marginRight(4f) } }
                                Text { attr { text("回收站"); fontSize(ThemeStyles.fontSizeBody); color(ThemeColors.textTertiary) } }
                            }
                        }
                    }
                } // end sidebar

                // 笔记列表区
                View {
                    attr { flex(1f); flexDirectionColumn() }

                    // 侧边栏遮罩层
                    if (ctx.showSidebar) {
                        View {
                            attr { positionAbsolute(); top(0f); left(0f); right(0f); bottom(0f)
                                backgroundColor(ThemeColors.overlay); zIndex(50) }
                            event { click { ctx.showSidebar = false } }
                        }
                    }

                    View {
                        attr { padding(top = 16f, left = 16f, bottom = 8f, right = 16f)
                            flexDirectionRow(); justifyContentSpaceBetween(); alignItemsCenter() }
                        Text { attr { text(ctx.filterTagName); fontSize(ThemeStyles.fontSizeTitle); fontWeightBold(); color(ThemeColors.textPrimary) } }
                        View { attr { flexDirectionRow(); alignItemsCenter() }
                            Text { attr { text("共 ${ctx.notes.size} 篇"); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.textHint); marginRight(12f) } }
                            View {
                                attr { padding(top = 4f, left = 8f, bottom = 4f, right = 8f); backgroundColor(ThemeColors.chipBg); borderRadius(ThemeStyles.borderRadiusChip); marginRight(8f) }
                                event { click { ctx.showSortPicker = !ctx.showSortPicker } }
                                Text { attr { text(ctx.sortBy.displayName); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.textTertiary) } }
                            }
                            if (!ctx.isMultiSelectMode && ctx.notes.isNotEmpty()) {
                                View {
                                    attr { padding(top = 4f, left = 8f, bottom = 4f, right = 8f); backgroundColor(ThemeColors.chipBg); borderRadius(ThemeStyles.borderRadiusChip) }
                                    event { click { ctx.toggleMultiSelectMode() } }
                                    Image { attr { src(Icons.SELECT_ALL); width(18f); height(18f) } }
                                }
                            }
                        }
                    }

                    // ========== 排序选择器浮层 ==========
                    if (ctx.showSortPicker) {
                        View {
                            attr { positionAbsolute(); top(52f); right(16f); backgroundColor(ThemeColors.surface); borderRadius(ThemeStyles.borderRadiusCard); padding(top = 4f, bottom = 4f); zIndex(100) }
                            for (sort in SortBy.entries) {
                                View {
                                    attr { padding(top = 10f, left = 16f, bottom = 10f, right = 16f); flexDirectionRow(); alignItemsCenter() }
                                    event { click { ctx.sortBy = sort; ctx.persistSortBy(); ctx.showSortPicker = false; ctx.refreshNotes() } }
                                    Text { attr { text(sort.displayName); fontSize(ThemeStyles.fontSizeBody); color(if (ctx.sortBy == sort) ThemeColors.primary else ThemeColors.textPrimary); marginRight(8f) } }
                                    if (ctx.sortBy == sort) {
                                        Image { attr { src(Icons.CHECK); width(16f); height(16f) } }
                                    }
                                }
                            }
                        }
                    }

                    List {
                        attr { flex(1f); padding(top = 0f, left = 16f, bottom = 0f, right = 16f) }
                        if (ctx.notes.isEmpty()) {
                            View {
                                attr { flex(1f); alignItemsCenter(); justifyContentCenter(); paddingTop(80f) }
                                View {
                                    attr { width(96f); height(96f); borderRadius(48f)
                                        backgroundColor(ThemeColors.chipBg); justifyContentCenter(); alignItemsCenter() }
                                    Image { attr { src(Icons.NOTE_EMPTY); width(64f); height(64f) } }
                                }
                                Text { attr { text(if (ctx.searchQuery.isNotBlank()) "未找到相关笔记" else "暂无笔记，点击右下角创建")
                                    fontSize(ThemeStyles.fontSizeBody); color(ThemeColors.textLight); marginTop(16f); lineHeight(22f) } }
                            }
                        } else {
                            val tagsMap = AppRepo.repo.getTagsForNotes(ctx.notes.map { it.id })
                            for (note in ctx.notes) {
                                val noteTags = tagsMap[note.id] ?: emptyList()
                                val contentPreview = com.noteapp.util.Sanitizer.safeTruncate(
                                    note.content.replace("\n", " "), 100
                                )
                                val isSelected = ctx.selectedNoteIds.contains(note.id)
                                View {
                                    attr { flexDirectionRow(); alignItemsCenter() }
                                    if (ctx.isMultiSelectMode) {
                                        View {
                                            attr { padding(right = 10f) }
                                            event { click { ctx.toggleNoteSelection(note.id) } }
                                            Image { attr { src(if (isSelected) Icons.SELECTED else Icons.SELECT_ALL); width(22f); height(22f) } }
                                        }
                                    }
                                    View {
                                        attr { flex(1f); flexDirectionColumn(); backgroundColor(ThemeColors.surface); borderRadius(ThemeStyles.borderRadiusCard)
                                            padding(top = 14f, left = 16f, bottom = 14f, right = 16f); marginBottom(10f) }
                                        if (ctx.isMultiSelectMode) {
                                            event { click { ctx.toggleNoteSelection(note.id) } }
                                        } else {
                                            event { click { ctx.openNoteEditor(note.id) } }
                                        }

                                        View {
                                            attr { flexDirectionRow(); justifyContentSpaceBetween(); alignItemsCenter() }
                                            Text {
                                                val displayTitle = note.title.ifBlank { "无标题笔记" }
                                                val titleText = if (ctx.searchQuery.isNotBlank()) {
                                                    ctx.buildHighlightedText(displayTitle, ctx.searchQuery)
                                                } else {
                                                    displayTitle
                                                }
                                                attr { text(titleText); fontSize(ThemeStyles.fontSizeSubtitle); fontWeightBold()
                                                    color(ThemeColors.textPrimary); flex(1f) }
                                            }
                                            Text { attr { text(formatDateTimeOnly(note.updatedAt)); fontSize(ThemeStyles.fontSizeSmall)
                                                color(ThemeColors.textLight) } }
                                        }

                                        if (contentPreview.isNotBlank()) {
                                            Text {
                                                val previewText = if (ctx.searchQuery.isNotBlank()) {
                                                    ctx.buildHighlightedText(contentPreview + if (note.content.length > 100) "..." else "", ctx.searchQuery)
                                                } else {
                                                    contentPreview + if (note.content.length > 100) "..." else ""
                                                }
                                                attr { text(previewText); fontSize(ThemeStyles.fontSizeBody); color(ThemeColors.textHint); marginTop(4f); lines(2) }
                                            }
                                        }

                                        if (noteTags.isNotEmpty()) { View {
                                            attr { flexDirectionRow(); marginTop(8f); flexWrapWrap() }
                                            for (tag in noteTags) { View {
                                                attr { padding(top = 2f, left = 8f, bottom = 2f, right = 8f)
                                                    backgroundColor(ThemeColors.primaryLight); borderRadius(ThemeStyles.borderRadiusTag); marginRight(6f); marginTop(4f) }
                                                Text { attr { text(tag.name); fontSize(ThemeStyles.fontSizeSmall); color(ThemeColors.primary) } }
                                            } }
                                        } }
                                    }
                                }
                            }
                        }
                    }
                }
            } // end main body

            // ========== 批量操作工具栏 ==========
            if (ctx.isMultiSelectMode && ctx.selectedNoteIds.isNotEmpty()) {
                View {
                    attr { flexDirectionRow(); alignItemsCenter(); justifyContentSpaceBetween()
                        padding(top = 8f, left = 16f, bottom = 8f, right = 16f)
                        backgroundColor(ThemeColors.surface) }
                    View { attr { flexDirectionRow(); alignItemsCenter() }
                        View {
                            attr { padding(top = 6f, left = 12f, bottom = 6f, right = 12f); backgroundColor(ThemeColors.dangerLight); borderRadius(ThemeStyles.borderRadiusChip); marginRight(8f) }
                            event { click { ctx.batchDelete() } }
                            View {
                                attr { flexDirectionRow(); alignItemsCenter() }
                                Image { attr { src(Icons.DELETE); width(14f); height(14f); marginRight(4f) } }
                                Text { attr { text("删除"); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.danger) } }
                            }
                        }
                        View {
                            attr { padding(top = 6f, left = 12f, bottom = 6f, right = 12f); backgroundColor(ThemeColors.primaryLight); borderRadius(ThemeStyles.borderRadiusChip); marginRight(8f) }
                            event { click { ctx.showBatchTagDialog() } }
                            View {
                                attr { flexDirectionRow(); alignItemsCenter() }
                                Image { attr { src(Icons.TAG); width(14f); height(14f); marginRight(4f) } }
                                Text { attr { text("标签"); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.primary) } }
                            }
                        }
                        View {
                            attr { padding(top = 6f, left = 12f, bottom = 6f, right = 12f); backgroundColor(ThemeColors.chipBg); borderRadius(ThemeStyles.borderRadiusChip) }
                            event { click { ctx.doBatchExport() } }
                            View {
                                attr { flexDirectionRow(); alignItemsCenter() }
                                Image { attr { src(Icons.EXPORT); width(14f); height(14f); marginRight(4f) } }
                                Text { attr { text("导出"); fontSize(ThemeStyles.fontSizeCaption); color(ThemeColors.textTertiary) } }
                            }
                        }
                    }
                }
            }

            // ========== FAB ==========
            if (!ctx.isMultiSelectMode) {
                View {
                    attr { positionAbsolute(); bottom(24f); right(24f); width(ThemeStyles.fabSize); height(ThemeStyles.fabSize); borderRadius(28f)
                        backgroundColor(ThemeColors.primary); justifyContentCenter(); alignItemsCenter() }
                    event {
                        click {
                            ctx.openNoteEditor("")
                        }
                    }
                    Image { attr { src(Icons.ADD); width(28f); height(28f) } }
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
                    View {
                        attr { width(300f); backgroundColor(ThemeColors.surface); borderRadius(ThemeStyles.borderRadiusCard)
                            padding(top = 24f, left = 24f, bottom = 24f, right = 24f) }
                        Text { attr { text(ctx.confirmTitle); fontSize(ThemeStyles.fontSizeSubtitle); fontWeightBold(); marginBottom(8f); color(ThemeColors.textPrimary) } }
                        Text { attr { text(ctx.confirmMessage); fontSize(ThemeStyles.fontSizeBody); color(ThemeColors.textSecondary); marginBottom(24f) } }
                        View { attr { flexDirectionRow(); justifyContentFlexEnd() }
                            View {
                                attr { padding(top = 8f, left = 20f, bottom = 8f, right = 20f); backgroundColor(ThemeColors.chipBg); borderRadius(ThemeStyles.borderRadiusButton); marginRight(12f) }
                                event { click { ctx.confirmCancelled() } }
                                Text { attr { text("取消"); fontSize(ThemeStyles.fontSizeBody); color(ThemeColors.textTertiary) } }
                            }
                            View {
                                attr { padding(top = 8f, left = 20f, bottom = 8f, right = 20f); backgroundColor(ThemeColors.danger); borderRadius(ThemeStyles.borderRadiusButton) }
                                event { click { ctx.confirmConfirmed() } }
                                Text { attr { text("确认"); fontSize(ThemeStyles.fontSizeBody); color(Color.WHITE) } }
                            }
                        }
                    }
                }
            }

            // ========== 标签管理浮层 ==========
            if (ctx.showTagManage) {
                View {
                    attr {
                        width(pagerData.pageViewWidth); height(pagerData.pageViewHeight)
                        positionAbsolute(); top(0f); left(0f); backgroundColor(ThemeColors.overlay)
                        justifyContentCenter(); alignItemsCenter()
                    }
                    View {
                        attr { width(320f); backgroundColor(ThemeColors.surface); borderRadius(ThemeStyles.borderRadiusCard)
                            padding(top = 20f, left = 20f, bottom = 20f, right = 20f) }
                        Text { attr { text("管理标签"); fontSize(ThemeStyles.fontSizeSubtitle); fontWeightBold(); marginBottom(16f) } }
                        for (tag in ctx.tags) { View {
                            attr { flexDirectionRow(); justifyContentSpaceBetween(); alignItemsCenter()
                                padding(top = 8f, bottom = 8f) }
                            Text { attr { text(tag.name); fontSize(ThemeStyles.fontSizeBody); flex(1f) } }
                            View {
                                attr { padding(top = 6f, left = 6f, bottom = 6f, right = 6f) }
                                event { click {
                                    ctx.showConfirm(
                                        "删除标签",
                                        "确定删除标签「${tag.name}」吗？已有笔记不会被删除，但会移除此标签关联。",
                                        {
                                            AppRepo.repo.deleteTag(tag.id)
                                            if (ctx.filterTagId == tag.id) {
                                                ctx.filterTagId = null
                                                ctx.filterTagName = "全部笔记"
                                            }
                                            ctx.loadData(); ctx.refreshNotes()
                                        },
                                        null
                                    )
                                } }
                                Image { attr { src(Icons.CLOSE); width(18f); height(18f) } }
                            }
                        } }
                        if (ctx.tags.isEmpty()) {
                            Text { attr { text("暂无标签"); fontSize(ThemeStyles.fontSizeBody); color(ThemeColors.textLight); marginTop(8f); marginBottom(8f); alignSelfStretch() } }
                        }
                        // 新建标签
                        View {
                            attr { flexDirectionRow(); marginTop(12f); alignItemsCenter() }
                            View {
                                attr { flex(1f); backgroundColor(ThemeColors.backgroundLight); borderRadius(ThemeStyles.borderRadiusChip)
                                    padding(top = 6f, left = 8f, bottom = 6f, right = 8f) }
                                Input {
                                    attr { flex(1f); text(ctx.newTagName); fontSize(ThemeStyles.fontSizeBody)
                                        placeholder("新建标签名称..."); placeholderColor(ThemeColors.textLight) }
                                    event { textDidChange { ctx.newTagName = it.text } }
                                }
                            }
                            View {
                                attr { padding(top = 6f, left = 12f, bottom = 6f, right = 12f)
                                    backgroundColor(ThemeColors.primary); borderRadius(ThemeStyles.borderRadiusChip); marginLeft(8f) }
                                event { click {
                                    if (ctx.newTagName.isNotBlank()) {
                                        val existing = AppRepo.repo.getTagByName(ctx.newTagName)
                                        if (existing == null) {
                                            AppRepo.repo.insertTag(Tag(id = UUID.generate(), name = ctx.newTagName))
                                            ctx.newTagName = ""
                                            ctx.loadData(); ctx.refreshNotes()
                                        } else {
                                            ctx.showToast("标签「${ctx.newTagName}」已存在")
                                        }
                                    }
                                } }
                                Text { attr { text("添加"); fontSize(ThemeStyles.fontSizeCaption); color(Color.WHITE) } }
                            }
                        }
                        View {
                            attr { marginTop(16f); alignSelfStretch(); padding(top = 10f, bottom = 10f)
                                backgroundColor(ThemeColors.primary); borderRadius(ThemeStyles.borderRadiusButton); alignItemsCenter() }
                            event { click { ctx.showTagManage = false } }
                            Text { attr { text("关闭"); fontSize(ThemeStyles.fontSizeBody); color(Color.WHITE) } }
                        }
                    }
                }
            }

            // ========== 批量标签选择浮层 ==========
            if (ctx.showBatchTagPicker) {
                View {
                    attr { width(pagerData.pageViewWidth); height(pagerData.pageViewHeight)
                        positionAbsolute(); top(0f); left(0f); backgroundColor(ThemeColors.overlay)
                        justifyContentCenter(); alignItemsCenter() }
                    View {
                        attr { width(320f); backgroundColor(ThemeColors.surface); borderRadius(ThemeStyles.borderRadiusCard)
                            padding(top = 20f, left = 20f, bottom = 20f, right = 20f) }
                        Text { attr { text("为选中笔记添加标签"); fontSize(ThemeStyles.fontSizeSubtitle); fontWeightBold(); marginBottom(16f) } }
                        for (tag in ctx.tags) {
                            View {
                                attr { flexDirectionRow(); alignItemsCenter(); padding(top = 8f, bottom = 8f) }
                                event { click { ctx.doBatchTag(tag.id) } }
                                View { attr { width(18f); height(18f); borderRadius(9f); backgroundColor(ThemeColors.primary); marginRight(10f) } }
                                Text { attr { text(tag.name); fontSize(ThemeStyles.fontSizeBody); flex(1f) } }
                            }
                        }
                        if (ctx.tags.isEmpty()) {
                            Text { attr { text("暂无标签，请先在侧边栏创建"); fontSize(ThemeStyles.fontSizeBody); color(ThemeColors.textLight); marginTop(8f); marginBottom(8f) } }
                        }
                        View { attr { marginTop(16f); alignSelfStretch(); padding(top = 10f, bottom = 10f)
                            backgroundColor(ThemeColors.border); borderRadius(ThemeStyles.borderRadiusButton); alignItemsCenter() }
                            event { click { ctx.showBatchTagPicker = false } }
                            Text { attr { text("取消"); fontSize(ThemeStyles.fontSizeBody); color(ThemeColors.textTertiary) } }
                        }
                    }
                }
            }
        }
    }

    private fun openNoteEditor(noteId: String) {
        val params = com.tencent.kuikly.core.nvi.serialization.json.JSONObject().apply { put("noteId", noteId) }
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage("NoteEditPage", params)
    }

    /**
     * 高亮搜索匹配文本 — 将所有匹配关键词用「」包裹
     * 由于 Kuikly Text 组件不支持 Span/富文本，采用包裹标记方式
     */
    private fun buildHighlightedText(text: String, query: String): String {
        if (query.isBlank()) return text
        val lowerQuery = query.lowercase()
        val lowerText = text.lowercase()
        val sb = StringBuilder()
        var start = 0
        while (true) {
            val idx = lowerText.indexOf(lowerQuery, start)
            if (idx < 0) {
                sb.append(text.substring(start))
                break
            }
            sb.append(text.substring(start, idx))
            sb.append("「")
            sb.append(text.substring(idx, idx + query.length.coerceAtMost(text.length - idx)))
            sb.append("」")
            start = idx + query.length
        }
        return sb.toString()
    }
}

/** Display name for search scope */
private fun NoteRepository.SearchScope.displayName(): String = when (this) {
    NoteRepository.SearchScope.TITLE -> "标题"
    NoteRepository.SearchScope.CONTENT -> "内容"
    NoteRepository.SearchScope.TITLE_AND_CONTENT -> "全部"
}

/** 笔记排序方式 */
private enum class SortBy(val displayName: String) {
    UPDATED_TIME("更新时间"),
    CREATED_TIME("创建时间"),
    TITLE("标题")
}
