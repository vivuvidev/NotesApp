package com.example.notesapp.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update


@Dao
interface NoteDao{

    @Query("select * from notes order by createdAt desc")
    fun getAllNotes(): LiveData<List<Note>>

    @Insert
   suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
   suspend fun deleteNote(note:Note)


}