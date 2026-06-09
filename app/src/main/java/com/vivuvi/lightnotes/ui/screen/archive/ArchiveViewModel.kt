package com.vivuvi.lightnotes.ui.screen.archive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.vivuvi.lightnotes.MainApplication
import com.vivuvi.lightnotes.data.local.Note
import com.vivuvi.lightnotes.data.repository.NoteRepository
import kotlinx.coroutines.launch

class ArchiveViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    val archivedNotes: LiveData<List<Note>>

    init {
        val noteDao = MainApplication.noteDatabase.getNoteDao()
        val mediaDao = MainApplication.noteDatabase.getNoteMediaDao()
        repository = NoteRepository(noteDao, mediaDao)
        archivedNotes = repository.archivedNotes
    }

    fun restoreNote(note: Note) {
        viewModelScope.launch {
            repository.update(note.copy(isArchived = false, updatedAt = System.currentTimeMillis()))
        }
    }

    fun sendToTrash(note: Note) {
        viewModelScope.launch {
            repository.update(note.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
        }
    }
}
