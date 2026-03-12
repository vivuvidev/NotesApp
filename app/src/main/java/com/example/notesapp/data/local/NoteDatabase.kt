package com.example.notesapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.notesapp.util.Converters


@Database(entities = [Note::class], version = 1)
@TypeConverters(Converters::class)
abstract class NoteDatabase: RoomDatabase() {
    companion object{
        const val Name = "notes_DB"
    }

    abstract fun getNoteDao(): NoteDao

}