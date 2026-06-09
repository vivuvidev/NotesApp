package com.vivuvi.lightnotes.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("""
        SELECT * FROM notes
        WHERE isDeleted = 0 AND isArchived = 0
        ORDER BY isPinned DESC, orderIndex ASC, updatedAt DESC, createdAt DESC
    """)
    fun getAllNotes(): Flow<List<Note>>

    @Query("""
        SELECT * FROM notes
        WHERE isArchived = 1 AND isDeleted = 0
        ORDER BY updatedAt DESC, createdAt DESC
    """)
    fun getArchivedNotes(): LiveData<List<Note>>

    @Query("""
        SELECT * FROM notes
        WHERE isDeleted = 1
        ORDER BY updatedAt DESC, createdAt DESC
    """)
    fun getDeletedNotes(): LiveData<List<Note>>

    @Query("""
        SELECT * FROM notes
        WHERE isDeleted = 0 AND isArchived = 0
        AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun searchNotes(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun getNoteById(noteId: Int): Note?

    @Insert
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Query("UPDATE notes SET orderIndex = :orderIndex WHERE id = :id")
    suspend fun updateOrderIndex(id: Int, orderIndex: Int)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}
