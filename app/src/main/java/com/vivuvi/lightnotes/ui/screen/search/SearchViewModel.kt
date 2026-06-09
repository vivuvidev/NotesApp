package com.vivuvi.lightnotes.ui.screen.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vivuvi.lightnotes.MainApplication
import com.vivuvi.lightnotes.data.local.Note
import com.vivuvi.lightnotes.data.repository.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val results: StateFlow<List<Note>> = _query
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList())
            else repository.searchNotes(q)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    init {
        val noteDao = MainApplication.noteDatabase.getNoteDao()
        val mediaDao = MainApplication.noteDatabase.getNoteMediaDao()
        repository = NoteRepository(noteDao, mediaDao)
    }

    fun onQueryChange(q: String) {
        _query.value = q
    }
}
