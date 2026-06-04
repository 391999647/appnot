package com.noteapp.model

import kotlinx.serialization.Serializable

/**
 * 笔记数据模型
 */
@Serializable
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null
)

/**
 * 标签数据模型
 */
@Serializable
data class Tag(
    val id: String,
    val name: String
)

/**
 * 笔记-标签关联
 */
@Serializable
data class NoteTag(
    val noteId: String,
    val tagId: String
)
