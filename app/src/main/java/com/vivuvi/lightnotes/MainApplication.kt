package com.vivuvi.lightnotes

import android.app.Application
import androidx.room.Room
import com.vivuvi.lightnotes.data.local.NoteDatabase
import com.vivuvi.lightnotes.data.preferences.ThemePreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob())

    companion object {
        lateinit var noteDatabase: NoteDatabase
        lateinit var themePreferences: ThemePreferencesRepository
        lateinit var isDarkModeFlow: StateFlow<Boolean>
    }

    override fun onCreate() {
        super.onCreate()
        noteDatabase = Room.databaseBuilder(
            applicationContext,
            NoteDatabase::class.java,
            NoteDatabase.Name,
        )
            .addMigrations(NoteDatabase.MIGRATION_1_2, NoteDatabase.MIGRATION_2_3, NoteDatabase.MIGRATION_3_4, NoteDatabase.MIGRATION_4_5, NoteDatabase.MIGRATION_5_6)
            .build()
        themePreferences = ThemePreferencesRepository(applicationContext)
        // Eagerly start collecting so the StateFlow has the real value before any screen reads it.
        isDarkModeFlow = themePreferences.isDarkMode.stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )
    }
}
