package com.vivuvi.lightnotes.ui.screen.pinlock

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vivuvi.lightnotes.MainApplication
import com.vivuvi.lightnotes.data.repository.NoteRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest

enum class PinMode { SETUP, UNLOCK, REMOVE }

sealed class PinLockEvent {
    object Success : PinLockEvent()
    data class UnlockSuccess(val noteId: Int) : PinLockEvent()
}

class PinLockViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val repository: NoteRepository

    val noteId: Int = savedStateHandle.get<Int>("noteId") ?: -1
    val mode: PinMode = runCatching {
        PinMode.valueOf(savedStateHandle.get<String>("mode") ?: "UNLOCK")
    }.getOrDefault(PinMode.UNLOCK)

    var pin by mutableStateOf("")
        private set
    var isConfirmStep by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val _events = Channel<PinLockEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // Holds the first PIN entry during the setup confirm flow.
    private var firstPin = ""

    init {
        val noteDao = MainApplication.noteDatabase.getNoteDao()
        val mediaDao = MainApplication.noteDatabase.getNoteMediaDao()
        repository = NoteRepository(noteDao, mediaDao)
    }

    fun appendDigit(digit: Int) {
        if (pin.length >= 6) return
        pin += digit.toString()
        errorMessage = null
    }

    fun deleteLastDigit() {
        if (pin.isNotEmpty()) pin = pin.dropLast(1)
        errorMessage = null
    }

    fun confirm() {
        if (pin.length < 4) {
            errorMessage = "PIN must be 4–6 digits"
            return
        }
        viewModelScope.launch {
            when (mode) {
                PinMode.SETUP -> handleSetup()
                PinMode.UNLOCK -> handleUnlock()
                PinMode.REMOVE -> handleRemove()
            }
        }
    }

    private suspend fun handleSetup() {
        if (!isConfirmStep) {
            firstPin = pin
            pin = ""
            isConfirmStep = true
        } else {
            if (pin == firstPin) {
                repository.setNoteLock(noteId, hashPin(pin))
                _events.send(PinLockEvent.Success)
            } else {
                // Mismatch — restart setup from the first step.
                pin = ""
                firstPin = ""
                isConfirmStep = false
                errorMessage = "PINs don't match. Try again."
            }
        }
    }

    private suspend fun handleUnlock() {
        val storedHash = repository.getById(noteId)?.pin
        if (storedHash != null && hashPin(pin) == storedHash) {
            _events.send(PinLockEvent.UnlockSuccess(noteId))
        } else {
            pin = ""
            errorMessage = "Incorrect PIN. Try again."
        }
    }

    private suspend fun handleRemove() {
        val storedHash = repository.getById(noteId)?.pin
        if (storedHash != null && hashPin(pin) == storedHash) {
            repository.removeNoteLock(noteId)
            _events.send(PinLockEvent.Success)
        } else {
            pin = ""
            errorMessage = "Incorrect PIN. Try again."
        }
    }

    private fun hashPin(raw: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
