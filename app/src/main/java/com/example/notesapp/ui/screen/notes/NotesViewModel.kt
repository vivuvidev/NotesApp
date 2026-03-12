package com.example.notesapp.ui.screen.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.notesapp.MainApplication
import com.example.notesapp.data.local.Note
import com.example.notesapp.data.repository.NoteRepository
import kotlinx.coroutines.launch

class NotesViewModel(application: Application): AndroidViewModel(application) {
    private val repository: NoteRepository
    val allNotes: LiveData<List<Note>>

    init {
        val noteDao = MainApplication.noteDatabase.getNoteDao()
         repository= NoteRepository(noteDao)
        allNotes = repository.allNotes
    }


    fun addNote(note: Note){
        viewModelScope.launch {
            repository.insert(note)
        }
    }

    fun updateNote(note: Note){
        viewModelScope.launch {
            repository.update(note)
        }
    }

    fun deleteNote(note: Note){
        viewModelScope.launch {
            repository.delete(note)
        }
    }



}