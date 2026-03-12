package com.example.notesapp.data.repository

import androidx.lifecycle.LiveData
import com.example.notesapp.data.local.Note
import com.example.notesapp.data.local.NoteDao

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: LiveData<List<Note>> = noteDao.getAllNotes()


    suspend fun insert(note: Note){
        noteDao.insertNote(note)
    }

    suspend fun update(note: Note){
        noteDao.updateNote(note)
    }

    suspend fun delete(note: Note){
        noteDao.deleteNote(note)
    }

}