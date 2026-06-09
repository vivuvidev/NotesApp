package com.vivuvi.lightnotes.data.repository

import androidx.lifecycle.LiveData
import com.vivuvi.lightnotes.data.local.Note
import com.vivuvi.lightnotes.data.local.NoteDao
import com.vivuvi.lightnotes.data.local.NoteMedia
import com.vivuvi.lightnotes.data.local.NoteMediaDao
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val noteDao: NoteDao,
    private val mediaDao: NoteMediaDao,
) {
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()
    val archivedNotes: LiveData<List<Note>> = noteDao.getArchivedNotes()
    val deletedNotes: LiveData<List<Note>> = noteDao.getDeletedNotes()

    suspend fun insert(note: Note): Long = noteDao.insertNote(note)
    suspend fun update(note: Note) = noteDao.updateNote(note)
    suspend fun getById(noteId: Int): Note? = noteDao.getNoteById(noteId)
    suspend fun deleteById(id: Int) = noteDao.deleteNoteById(id)
    suspend fun deleteAllNotes() = noteDao.deleteAllNotes()

    suspend fun persistOrder(notes: List<Note>) {
        notes.forEachIndexed { index, note ->
            noteDao.updateOrderIndex(note.id, index)
        }
    }

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    suspend fun setNoteLock(noteId: Int, hashedPin: String) {
        val note = getById(noteId) ?: return
        update(note.copy(isLocked = true, pin = hashedPin))
    }

    suspend fun removeNoteLock(noteId: Int) {
        val note = getById(noteId) ?: return
        update(note.copy(isLocked = false, pin = null))
    }

    fun mediaForNote(noteId: Int): Flow<List<NoteMedia>> = mediaDao.getByNoteId(noteId)
    suspend fun getMediaForNote(noteId: Int): List<NoteMedia> = mediaDao.getByNoteIdOnce(noteId)
    suspend fun insertMedia(media: NoteMedia) = mediaDao.insert(media)
    suspend fun deleteMedia(id: Int) = mediaDao.deleteById(id)
}
