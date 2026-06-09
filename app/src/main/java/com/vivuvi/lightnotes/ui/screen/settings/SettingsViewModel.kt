package com.vivuvi.lightnotes.ui.screen.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vivuvi.lightnotes.MainApplication
import com.vivuvi.lightnotes.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SettingsDialog { DeveloperInfo, ConfirmClearAll }

data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val activeDialog: SettingsDialog? = null,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val themePreferences = MainApplication.themePreferences
    private val repository: NoteRepository

    init {
        val noteDao = MainApplication.noteDatabase.getNoteDao()
        val mediaDao = MainApplication.noteDatabase.getNoteMediaDao()
        repository = NoteRepository(noteDao, mediaDao)
    }

    private val _activeDialog = MutableStateFlow<SettingsDialog?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        themePreferences.isDarkMode,
        _activeDialog,
    ) { isDark, dialog ->
        SettingsUiState(isDarkMode = isDark, activeDialog = dialog)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        // isDarkModeFlow has been eagerly collecting since app start, so .value is the
        // real persisted preference by the time the user navigates to Settings.
        initialValue = SettingsUiState(isDarkMode = MainApplication.isDarkModeFlow.value),
    )

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setDarkMode(enabled) }
    }

    fun showDialog(dialog: SettingsDialog) {
        _activeDialog.value = dialog
    }

    fun dismissDialog() {
        _activeDialog.value = null
    }

    fun deleteAllNotes() {
        viewModelScope.launch {
            repository.deleteAllNotes()
            _activeDialog.value = null
        }
    }
}
