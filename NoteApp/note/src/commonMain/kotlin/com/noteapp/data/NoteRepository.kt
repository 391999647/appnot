package com.noteapp.data

import com.noteapp.model.Note
import com.noteapp.model.Tag
import com.noteapp.model.NoteTag
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
        const val STORAGE_KEY = "noteapp_data"
        const val DATA_VERSION = 1
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
        val saved = loadPersistentData(STORAGE_KEY)
        if (saved.isNotEmpty()) {
            val ok = deserialize(saved)
            if (!ok) {
                lastLoadFailed = true
                // 自动备份损坏的数据，防止永久丢失
                try {
                    savePersistentData("${STORAGE_KEY}_backup_${currentTimeSeconds()}", saved)
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

    /** 直接持久化（不走防抖） */
    private fun persistImmediate() {
        savePersistentData(STORAGE_KEY, serialize())
        isDirty = false
    }

    /** 标记脏数据 - 仅标记，不立即写盘 */
    private fun markDirty() {
        isDirty = true
    }

    /** 标记脏并立即持久化（用于用户明确操作） */
    private fun markDirtyAndPersist() {
        isDirty = true
        persistImmediate()
    }

    // === Notes CRUD ===

    fun getAllNotes(includeDeleted: Boolean = false): List<Note> {
        return if (includeDeleted) notes.toList()
        else notes.filter { it.deletedAt == null }
    }

    fun getNoteById(id: String): Note? = notes.firstOrNull { it.id == id }

    fun insertNote(note: Note) {
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

    fun restoreNote(id: String) {
        val idx = notes.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val note = notes[idx]
            notes[idx] = note.copy(deletedAt = null)
            markDirtyAndPersist()
        }
    }

    fun permanentlyDeleteNote(id: String) {
        notes.removeAll { it.id == id }
        noteTags.removeAll { it.noteId == id }
        markDirtyAndPersist()
    }

    fun getDeletedNotes(): List<Note> = notes.filter { it.deletedAt != null }

    // === Tags CRUD ===

    fun getAllTags(): List<Tag> = tags.toList()

    fun getTagById(id: String): Tag? = tags.firstOrNull { it.id == id }

    fun insertTag(tag: Tag) {
        // 防止创建同名标签
        val existing = getTagByName(tag.name)
        if (existing != null) return
        tags.add(tag)
        markDirtyAndPersist()
    }

    fun updateTag(tag: Tag) {
        val idx = tags.indexOfFirst { it.id == tag.id }
        if (idx >= 0) {
            tags[idx] = tag
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

    fun addTagToNote(noteId: String, tagId: String) {
        if (noteTags.none { it.noteId == noteId && it.tagId == tagId }) {
            noteTags.add(NoteTag(noteId, tagId))
            markDirtyAndPersist()
        }
    }

    fun removeTagFromNote(noteId: String, tagId: String) {
        noteTags.removeAll { it.noteId == noteId && it.tagId == tagId }
        markDirtyAndPersist()
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

    // === Serialization ===

    fun serialize(): String {
        val data = mapOf(
            "version" to DATA_VERSION,
            "notes" to notes,
            "tags" to tags,
            "noteTags" to noteTags
        )
        return json.encodeToString(data)
    }

    fun deserialize(dataStr: String): Boolean {
        if (dataStr.isBlank()) return false
        return try {
            val map = json.decodeFromString<Map<String, kotlinx.serialization.json.JsonElement>>(dataStr)

            // 版本检查与迁移入口
            val version = (map["version"]?.toString()?.toIntOrNull() ?: 0)
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
