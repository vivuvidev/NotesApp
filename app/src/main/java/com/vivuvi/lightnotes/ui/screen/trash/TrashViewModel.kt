package com.vivuvi.lightnotes.ui.screen.trash

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.vivuvi.lightnotes.MainApplication
import com.vivuvi.lightnotes.data.local.Note
import com.vivuvi.lightnotes.data.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class TrashViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    val deletedNotes: LiveData<List<Note>>

    init {
        val noteDao = MainApplication.noteDatabase.getNoteDao()
        val mediaDao = MainApplication.noteDatabase.getNoteMediaDao()
        repository = NoteRepository(noteDao, mediaDao)
        deletedNotes = repository.deletedNotes
    }

    fun restoreNote(note: Note) {
        viewModelScope.launch {
            repository.update(note.copy(isDeleted = false, updatedAt = System.currentTimeMillis()))
        }
    }

    fun permanentlyDelete(note: Note) {
        viewModelScope.launch {
            // Fetch media records before the cascade delete removes them.
            val mediaList = repository.getMediaForNote(note.id)
            repository.deleteById(note.id)
            withContext(Dispatchers.IO) {
                mediaList.forEach { media ->
                    val uri = Uri.parse(media.uri)
                    if (uri.scheme == "file") {
                        runCatching { File(uri.path ?: return@runCatching).delete() }
                    }
                }
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            (deletedNotes.value ?: return@launch).forEach { note ->
                permanentlyDelete(note)
            }
        }
    }
}
