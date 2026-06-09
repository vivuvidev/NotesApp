package com.vivuvi.lightnotes.ui.screen.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vivuvi.lightnotes.MainApplication
import com.vivuvi.lightnotes.data.local.Note
import com.vivuvi.lightnotes.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    // True while a drag gesture is active — suppresses DB emissions so the in-memory
    // reorder is not overwritten before it is persisted.
    private var isDragging = false

    init {
        val noteDao = MainApplication.noteDatabase.getNoteDao()
        val mediaDao = MainApplication.noteDatabase.getNoteMediaDao()
        repository = NoteRepository(noteDao, mediaDao)

        viewModelScope.launch {
            repository.allNotes.collect { dbNotes ->
                if (!isDragging) _notes.value = dbNotes
            }
        }
    }

    fun onDragStart() {
        isDragging = true
    }

    fun moveNote(fromIndex: Int, toIndex: Int) {
        val current = _notes.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        current.add(toIndex, current.removeAt(fromIndex))
        _notes.value = current
    }

    fun onDragEnd() {
        viewModelScope.launch {
            try {
                repository.persistOrder(_notes.value)
            } finally {
                isDragging = false
            }
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch { repository.update(note) }
    }
}
