package com.noteapp.data

import com.noteapp.model.Note
import com.noteapp.model.NoteTag
import com.noteapp.model.Tag
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * 笔记仓库 - 内存缓存 + JSON序列化持久化
 * 模拟产品方案中的 SQLite 数据层
 *
 * 改进:
 * - 数据版本管理，支持向前兼容的迁移
 * - 反序列化异常不再静默丢弃，自动备份损坏数据
 * - 防抖持久化：高频 update 操作不立即写盘，由 flushChanges() 统一落盘
 * - 批量查询方法避免 N+1 问题
 * - 标签名去重检查
 */
class NoteRepository {

    companion object {
        const val STORAGE_KEY = "ntnotes_data"
        const val DATA_VERSION = 1

        fun sanitizeFileName(name: String): String {
            return name.trim()
                .replace(Regex("[\\\\/:*?\"<>|\r\n]"), "_")
                .take(80)
                .ifBlank { "无标题笔记" }
        }
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val notes = mutableListOf<Note>()
    private val tags = mutableListOf<Tag>()
    private val noteTags = mutableListOf<NoteTag>()

    /** 标记自上次 flushChanges() 以来是否有数据变更 */
    private var isDirty = false

    /** 记录反序列化是否成功，供外部查询 */
    var lastLoadFailed = false
        private set
    var lastLoadError: String? = null
        private set

    fun load() {
        loadFromStorage()
    }

    private fun loadFromStorage() {
        lastLoadFailed = false
        lastLoadError = null
        val saved = loadPersistentData(STORAGE_KEY)
        if (saved.isNotEmpty()) {
            val ok = deserialize(saved)
            if (!ok) {
                lastLoadFailed = true
                // 自动备份损坏的数据，限制最多保留3个备份
                try {
                    val backupKey = "${STORAGE_KEY}_backup"
                    val timestamps = listOf(
                        loadPersistentData("${backupKey}_1"),
                        loadPersistentData("${backupKey}_2"),
                        loadPersistentData("${backupKey}_3")
                    )
                    val emptySlot = timestamps.indexOfFirst { it.isEmpty() } + 1
                    if (emptySlot > 0) {
                        savePersistentData("${backupKey}_${emptySlot}", saved)
                    } else {
                        // 覆盖最旧的（轮转）
                        savePersistentData("${backupKey}_1", loadPersistentData("${backupKey}_2"))
                        savePersistentData("${backupKey}_2", loadPersistentData("${backupKey}_3"))
                        savePersistentData("${backupKey}_3", saved)
                    }
                } catch (_: Exception) { /* 静默降级 - 备份失败不阻塞启动 */ }
            }
        }
    }

    /**
     * 将所有变更写入持久化存储。
     * 页面离开或应用挂起时应主动调用此方法。
     */
    fun flushChanges() {
        if (isDirty) {
            persistImmediate()
        }
    }

    /** 直接持久化（不走防抖），返回是否成功 */
    private fun persistImmediate(): Boolean {
        return try {
            savePersistentData(STORAGE_KEY, serialize())
            isDirty = false
            true
        } catch (e: Exception) {
            lastLoadError = "保存失败: ${e.message}"
            false
        }
    }

    /** 标记脏数据 - 仅标记，不立即写盘 */
    private fun markDirty() {
        isDirty = true
    }

    /** 标记脏并立即持久化（用于用户明确操作），返回是否成功 */
    private fun markDirtyAndPersist(): Boolean {
        isDirty = true
        return persistImmediate()
    }

    // === Notes CRUD ===

    fun getAllNotes(includeDeleted: Boolean = false): List<Note> {
        return if (includeDeleted) notes.toList()
        else notes.filter { it.deletedAt == null }
    }

    fun getNoteById(id: String): Note? = notes.firstOrNull { it.id == id }

    fun getActiveNoteById(id: String): Note? = notes.firstOrNull { it.id == id && it.deletedAt == null }

    fun insertNote(note: Note) {
        if (notes.any { it.id == note.id }) return
        notes.add(note)
        markDirtyAndPersist()
    }

    /**
     * 更新笔记 - 高频操作，仅更新内存不立即写盘，
     * 由 flushChanges() 统一落盘以避免每次按键都触发全量序列化。
     */
    fun updateNote(note: Note) {
        val idx = notes.indexOfFirst { it.id == note.id }
        if (idx >= 0) {
            notes[idx] = note
            markDirty() // 仅标记，不立即持久化
        }
    }

    fun softDeleteNote(id: String, deletedAt: String) {
        val idx = notes.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val note = notes[idx]
            notes[idx] = note.copy(deletedAt = deletedAt)
            markDirtyAndPersist()
        }
    }

    fun softDeleteNotes(ids: List<String>, deletedAt: String) {
        if (ids.isEmpty()) return
        val idSet = ids.toSet()
        var changed = false
        for (i in notes.indices) {
            val note = notes[i]
            if (note.id in idSet) {
                notes[i] = note.copy(deletedAt = deletedAt)
                changed = true
            }
        }
        if (changed) markDirtyAndPersist()
    }

    fun restoreNote(id: String) {
        val idx = notes.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val note = notes[idx]
            notes[idx] = note.copy(deletedAt = null)
            markDirtyAndPersist()
        }
    }

    fun restoreNotes(ids: List<String>) {
        if (ids.isEmpty()) return
        val idSet = ids.toSet()
        var changed = false
        for (i in notes.indices) {
            val note = notes[i]
            if (note.id in idSet) {
                notes[i] = note.copy(deletedAt = null)
                changed = true
            }
        }
        if (changed) markDirtyAndPersist()
    }

    fun permanentlyDeleteNote(id: String) {
        val removed = notes.removeAll { it.id == id }
        noteTags.removeAll { it.noteId == id }
        if (removed) markDirtyAndPersist()
    }

    fun permanentlyDeleteNotes(ids: List<String>) {
        if (ids.isEmpty()) return
        val idSet = ids.toSet()
        val removed = notes.removeAll { it.id in idSet }
        noteTags.removeAll { it.noteId in idSet }
        if (removed) markDirtyAndPersist()
    }

    fun getDeletedNotes(): List<Note> = notes.filter { it.deletedAt != null }

    fun clearDraft(noteId: String) {
        removePersistentData("draft_$noteId")
    }

    // === Tags CRUD ===

    fun getAllTags(): List<Tag> = tags.toList()

    fun getTagById(id: String): Tag? = tags.firstOrNull { it.id == id }

    fun insertTag(tag: Tag) {
        // 防止创建同名标签（trim 后比较，大小写不敏感）
        val normalized = tag.name.trim()
        if (normalized.isEmpty()) return
        val existing = tags.firstOrNull { it.name.trim().equals(normalized, ignoreCase = true) }
        if (existing != null) return
        tags.add(tag.copy(name = normalized))
        markDirtyAndPersist()
    }

    fun normalizeTagName(name: String): String = name.trim()

    fun updateTag(tag: Tag) {
        val idx = tags.indexOfFirst { it.id == tag.id }
        if (idx >= 0) {
            val normalized = tag.name.trim()
            if (normalized.isEmpty()) return
            val duplicate = tags.firstOrNull {
                it.id != tag.id && it.name.trim().equals(normalized, ignoreCase = true)
            }
            if (duplicate != null) return
            tags[idx] = tag.copy(name = normalized)
            markDirtyAndPersist()
        }
    }

    fun deleteTag(id: String) {
        tags.removeAll { it.id == id }
        noteTags.removeAll { it.tagId == id }
        markDirtyAndPersist()
    }

    fun getTagByName(name: String): Tag? = tags.firstOrNull { it.name == name }

    // === Note-Tag Association ===

    fun getTagsForNote(noteId: String): List<Tag> {
        val tagIds = noteTags.filter { it.noteId == noteId }.map { it.tagId }.toSet()
        return tags.filter { it.id in tagIds }
    }

    /**
     * 批量获取多篇笔记的标签映射 — 避免 N+1 查询
     */
    fun getTagsForNotes(noteIds: List<String>): Map<String, List<Tag>> {
        if (noteIds.isEmpty()) return emptyMap()
        val noteIdSet = noteIds.toSet()
        val ntMap = noteTags.filter { it.noteId in noteIdSet }
            .groupBy({ it.noteId }) { it.tagId }
        val tagMap = tags.associateBy { it.id }
        return ntMap.mapValues { (_, tagIds) -> tagIds.mapNotNull { tagMap[it] } }
    }

    fun getNotesForTag(tagId: String): List<Note> {
        val noteIds = noteTags.filter { it.tagId == tagId }.map { it.noteId }.toSet()
        return notes.filter { it.id in noteIds && it.deletedAt == null }
    }

    /**
     * 聚合统计每个标签的活跃笔记数量 — 避免 N+1 查询
     */
    fun getTagCounts(): Map<String, Int> {
        val activeNoteIds = notes.filter { it.deletedAt == null }.map { it.id }.toSet()
        return noteTags
            .filter { it.noteId in activeNoteIds }
            .groupingBy { it.tagId }
            .eachCount()
    }

    fun addTagToNote(noteId: String, tagId: String) {
        if (notes.none { it.id == noteId }) return
        if (tags.none { it.id == tagId }) return
        if (noteTags.none { it.noteId == noteId && it.tagId == tagId }) {
            noteTags.add(NoteTag(noteId, tagId))
            markDirtyAndPersist()
        }
    }

    fun addTagToNotes(noteIds: List<String>, tagId: String) {
        if (noteIds.isEmpty()) return
        var changed = false
        for (noteId in noteIds) {
            if (noteTags.none { it.noteId == noteId && it.tagId == tagId }) {
                noteTags.add(NoteTag(noteId, tagId))
                changed = true
            }
        }
        if (changed) markDirtyAndPersist()
    }

    fun removeTagFromNote(noteId: String, tagId: String) {
        val removed = noteTags.removeAll { it.noteId == noteId && it.tagId == tagId }
        if (removed) markDirtyAndPersist()
    }

    fun setTagsForNote(noteId: String, tagIds: List<String>) {
        noteTags.removeAll { it.noteId == noteId }
        tagIds.forEach { tagId -> noteTags.add(NoteTag(noteId, tagId)) }
        markDirtyAndPersist()
    }

    // === Search ===

    /**
     * 统一搜索入口：按标题或内容搜索
     */
    fun searchNotes(query: String, scope: SearchScope = SearchScope.TITLE_AND_CONTENT): List<Note> {
        if (query.isBlank()) return getAllNotes()
        val allNotes = getAllNotes()
        return when (scope) {
            SearchScope.TITLE -> allNotes.filter { it.title.contains(query, ignoreCase = true) }
            SearchScope.CONTENT -> allNotes.filter { it.content.contains(query, ignoreCase = true) }
            SearchScope.TITLE_AND_CONTENT -> allNotes.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true)
            }
        }
    }

    enum class SearchScope { TITLE, CONTENT, TITLE_AND_CONTENT }

    // === 排序/筛选持久化 ===

    fun persistSortBy(sortBy: String) = savePersistentData("sort_by", sortBy)

    fun loadSortBy(): String = loadPersistentData("sort_by")

    fun persistFilterTag(tagId: String?, tagName: String) {
        if (tagId == null) {
            removePersistentData("filter_tag_id")
            removePersistentData("filter_tag_name")
        } else {
            savePersistentData("filter_tag_id", tagId)
            savePersistentData("filter_tag_name", tagName)
        }
    }

    fun loadFilterTagId(): String = loadPersistentData("filter_tag_id")

    fun loadFilterTagName(): String = loadPersistentData("filter_tag_name").ifEmpty { "全部笔记" }

    // === Serialization ===

    @Serializable
    private data class StorageData(
        val version: Int,
        val notes: List<Note>,
        val tags: List<Tag>,
        val noteTags: List<NoteTag>
    )

    fun serialize(): String {
        val data = StorageData(DATA_VERSION, notes.toList(), tags.toList(), noteTags.toList())
        return json.encodeToString(data)
    }

    fun deserialize(dataStr: String): Boolean {
        if (dataStr.isBlank()) return false
        return try {
            val map = json.decodeFromString<Map<String, kotlinx.serialization.json.JsonElement>>(dataStr)

            // 版本检查与迁移入口
            val version = map["version"]?.jsonPrimitive?.intOrNull ?: 0
            if (version > DATA_VERSION) {
                lastLoadError = "Unsupported data version: $version (max supported: $DATA_VERSION)"
                return false
            }

            val notesArr = json.decodeFromString<List<Note>>(map["notes"].toString())
            val tagsArr = json.decodeFromString<List<Tag>>(map["tags"].toString())
            val ntArr = json.decodeFromString<List<NoteTag>>(map["noteTags"].toString())

            notes.clear(); notes.addAll(notesArr)
            tags.clear(); tags.addAll(tagsArr)
            noteTags.clear(); noteTags.addAll(ntArr)

            // 旧版本数据迁移（示例）
            if (version < 1) {
                // migrateV0toV1()
            }

            true
        } catch (e: Exception) {
            lastLoadError = e.message ?: "Unknown deserialization error"
            false
        }
    }

    // === 统计 ===

    fun totalNoteCount(): Int = notes.size
    fun activeNoteCount(): Int = notes.count { it.deletedAt == null }
    fun deletedNoteCount(): Int = notes.count { it.deletedAt != null }
    fun tagCount(): Int = tags.size
}

/** 跨平台获取秒级时间戳（用于备份文件名） */
internal expect fun currentTimeSeconds(): Long
