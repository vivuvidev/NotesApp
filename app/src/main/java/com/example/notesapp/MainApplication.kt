package com.example.notesapp

import android.app.Application
import androidx.room.Room
import com.example.notesapp.data.local.NoteDatabase

class MainApplication: Application() {
    companion object{
        lateinit var noteDatabase: NoteDatabase
    }

    override fun onCreate() {
        super.onCreate()
        noteDatabase= Room.databaseBuilder(
            applicationContext,
            NoteDatabase::class.java,
            NoteDatabase.Name
        ).build()
    }
}