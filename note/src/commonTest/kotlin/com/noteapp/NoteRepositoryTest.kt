package com.noteapp

import com.noteapp.data.NoteRepository
import com.noteapp.model.Note
import com.noteapp.model.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NoteRepositoryTest {

    @Test
    fun testInsertAndGetNote() {
        val repo = NoteRepository()
        val note = Note(
            id = "test-1",
            title = "Test Note",
            content = "Test Content",
            createdAt = "2024-01-01 00:00:00",
            updatedAt = "2024-01-01 00:00:00"
        )
        repo.insertNote(note)
        
        val retrieved = repo.getNoteById("test-1")
        assertNotNull(retrieved)
        assertEquals("Test Note", retrieved.title)
        assertEquals("Test Content", retrieved.content)
    }

    @Test
    fun testUpdateNote() {
        val repo = NoteRepository()
        val note = Note(
            id = "test-2",
            title = "Original",
            content = "Original Content",
            createdAt = "2024-01-01 00:00:00",
            updatedAt = "2024-01-01 00:00:00"
        )
        repo.insertNote(note)
        
        val updated = note.copy(title = "Updated", updatedAt = "2024-01-02 00:00:00")
        repo.updateNote(updated)
        repo.flushChanges()
        
        val retrieved = repo.getNoteById("test-2")
        assertNotNull(retrieved)
        assertEquals("Updated", retrieved.title)
    }

    @Test
    fun testSoftDeleteAndRestore() {
        val repo = NoteRepository()
        val note = Note(
            id = "test-3",
            title = "Delete Me",
            content = "Content",
            createdAt = "2024-01-01 00:00:00",
            updatedAt = "2024-01-01 00:00:00"
        )
        repo.insertNote(note)
        assertEquals(1, repo.activeNoteCount())
        
        repo.softDeleteNote("test-3", "2024-01-02 00:00:00")
        assertEquals(0, repo.activeNoteCount())
        assertEquals(1, repo.deletedNoteCount())
        
        repo.restoreNote("test-3")
        assertEquals(1, repo.activeNoteCount())
        assertEquals(0, repo.deletedNoteCount())
    }

    @Test
    fun testSearchNotes() {
        val repo = NoteRepository()
        repo.insertNote(Note("1", "Kotlin", "Kotlin is great", "2024-01-01", "2024-01-01"))
        repo.insertNote(Note("2", "Java", "Java is okay", "2024-01-01", "2024-01-01"))
        repo.insertNote(Note("3", "Swift", "Swift for iOS", "2024-01-01", "2024-01-01"))
        
        val results = repo.searchNotes("Kotlin")
        assertEquals(1, results.size)
        assertEquals("Kotlin", results[0].title)
    }

    @Test
    fun testTagManagement() {
        val repo = NoteRepository()
        val tag = Tag("tag-1", "Important")
        repo.insertTag(tag)
        
        assertEquals(1, repo.getAllTags().size)
        assertNotNull(repo.getTagByName("Important"))
        
        // Duplicate tag should be ignored
        repo.insertTag(Tag("tag-2", "Important"))
        assertEquals(1, repo.getAllTags().size)
    }
}
