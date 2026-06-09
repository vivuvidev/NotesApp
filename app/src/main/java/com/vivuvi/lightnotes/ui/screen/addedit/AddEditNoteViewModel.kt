package com.vivuvi.lightnotes.ui.screen.addedit

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vivuvi.lightnotes.MainApplication
import com.vivuvi.lightnotes.data.local.Note
import com.vivuvi.lightnotes.data.local.NoteMedia
import com.vivuvi.lightnotes.data.repository.NoteRepository
import com.vivuvi.lightnotes.util.AddEditEvent
import com.vivuvi.lightnotes.util.FontSize
import com.vivuvi.lightnotes.util.ListType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

class AddEditNoteViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val repository: NoteRepository
    private val noteId: Int = savedStateHandle.get<Int>("noteId") ?: -1

    val isEditMode: Boolean get() = noteId != -1

    var title by mutableStateOf("")
        private set
    var content by mutableStateOf("")
        private set
    var fontSize by mutableStateOf(FontSize.MEDIUM)
        private set
    var backgroundColor by mutableStateOf(0L)
        private set
    var textColor by mutableStateOf(0L)
        private set
    var isBold by mutableStateOf(false)
        private set
    var isItalic by mutableStateOf(false)
        private set
    var isUnderline by mutableStateOf(false)
        private set
    var headingLevel by mutableStateOf(0)
        private set
    var listType by mutableStateOf(ListType.NONE)
        private set
    var isPinned by mutableStateOf(false)
        private set
    var isLocked by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(noteId != -1)
        private set

    private val _mediaItems = MutableStateFlow<List<NoteMedia>>(emptyList())
    val mediaItems: StateFlow<List<NoteMedia>> = _mediaItems.asStateFlow()

    private var existingNote: Note? = null
    private val saveMutex = Mutex()
    private val _events = Channel<AddEditEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // Flips to true once the initial state is settled so auto-save never fires
    // on the loaded/default values before the user has made any change.
    private val _autoSaveEnabled = MutableStateFlow(false)

    init {
        val noteDao = MainApplication.noteDatabase.getNoteDao()
        val mediaDao = MainApplication.noteDatabase.getNoteMediaDao()
        repository = NoteRepository(noteDao, mediaDao)

        if (isEditMode) {
            viewModelScope.launch {
                repository.getById(noteId)?.let { note ->
                    existingNote = note
                    title = note.title
                    content = note.content
                    fontSize = FontSize.entries.find { it.name == note.fontSize } ?: FontSize.MEDIUM
                    backgroundColor = note.backgroundColor
                    textColor = note.textColor
                    isBold = note.isBold
                    isItalic = note.isItalic
                    isUnderline = note.isUnderline
                    headingLevel = note.headingLevel
                    listType = ListType.entries.find { it.name == note.listType } ?: ListType.NONE
                    isPinned = note.isPinned
                    isLocked = note.isLocked
                }
                isLoading = false
                _autoSaveEnabled.value = true  // arm only after load so we don't save initial state
            }
            viewModelScope.launch {
                repository.mediaForNote(noteId).collect { _mediaItems.value = it }
            }
        } else {
            _autoSaveEnabled.value = true  // arm immediately; new note starts blank
        }

        startAutoSave()
    }

    // ── Auto-save ─────────────────────────────────────────────────────────────

    @OptIn(FlowPreview::class)
    private fun startAutoSave() {
        viewModelScope.launch {
            // Wait for initial state to settle before observing changes.
            _autoSaveEnabled.filter { it }.first()

            snapshotFlow { buildStateKey() }
                .drop(1)          // skip the first emission (settled/loaded state)
                .debounce(1_500L) // wait for 1.5 s of inactivity
                .collect { autoSave() }
        }
    }

    // All fields that should trigger an auto-save when changed.
    private fun buildStateKey(): String =
        "$title|$content|$isBold|$isItalic|$isUnderline|$headingLevel" +
        "|${listType.name}|${fontSize.name}|$backgroundColor|$textColor|$isPinned"

    // ── State setters ─────────────────────────────────────────────────────────

    fun onTitleChange(value: String) { title = value }
    fun onContentChange(value: String) { content = value }
    fun onFontSizeChange(size: FontSize) { fontSize = size }
    fun onBackgroundColorChange(color: Long) { backgroundColor = color }
    fun onTextColorChange(color: Long) { textColor = color }
    fun onBoldToggle() { isBold = !isBold }
    fun onItalicToggle() { isItalic = !isItalic }
    fun onUnderlineToggle() { isUnderline = !isUnderline }
    fun onHeadingLevelChange(level: Int) { headingLevel = if (headingLevel == level) 0 else level }
    fun onListTypeChange(type: ListType) { listType = if (listType == type) ListType.NONE else type }
    fun onPinToggle() {
        isPinned = !isPinned
        // Persist immediately — pin state drives list ordering and must not wait for the 1.5 s debounce.
        viewModelScope.launch {
            if (title.isNotBlank() || content.isNotBlank() || existingNote != null || _mediaItems.value.isNotEmpty()) {
                persistNote(buildCurrentNote())
            }
        }
    }

    /** Appends (or sets) speech-recognition output into the note body. */
    fun appendSpeechText(text: String) {
        content = if (content.isBlank()) text else "$content $text"
    }

    // ── Persistence helpers ───────────────────────────────────────────────────

    private fun buildCurrentNote(
        isArchived: Boolean = existingNote?.isArchived ?: false,
        isDeleted: Boolean = existingNote?.isDeleted ?: false,
    ): Note {
        val existing = existingNote
        val now = System.currentTimeMillis()
        return if (existing != null) {
            existing.copy(
                title = title,
                content = content,
                updatedAt = now,
                isArchived = isArchived,
                isDeleted = isDeleted,
                fontSize = fontSize.name,
                backgroundColor = backgroundColor,
                textColor = textColor,
                isBold = isBold,
                isItalic = isItalic,
                isUnderline = isUnderline,
                headingLevel = headingLevel,
                listType = listType.name,
                isPinned = isPinned,
            )
        } else {
            Note(
                id = 0,
                title = title,
                content = content,
                createdAt = Date(),
                updatedAt = now,
                isArchived = isArchived,
                isDeleted = isDeleted,
                fontSize = fontSize.name,
                backgroundColor = backgroundColor,
                textColor = textColor,
                isBold = isBold,
                isItalic = isItalic,
                isUnderline = isUnderline,
                headingLevel = headingLevel,
                listType = listType.name,
                isPinned = isPinned,
            )
        }
    }

    /**
     * Upsert a note to Room.
     * For brand-new notes (id == 0) also flushes any buffered media and starts
     * the live media Flow so subsequent media ops work against the real row.
     */
    private suspend fun persistNote(note: Note): Note = saveMutex.withLock {
        if (note.id == 0 && existingNote == null) {
            val newId = repository.insert(note).toInt()
            val inserted = note.copy(id = newId)
            existingNote = inserted
            // Flush media items buffered before the first DB insert.
            // Use .copy() to preserve mimeType and fileName.
            _mediaItems.value.filter { it.id == 0 }.forEach { media ->
                repository.insertMedia(media.copy(id = 0, noteId = newId))
            }
            // Switch to the live Room Flow for this note's media.
            viewModelScope.launch {
                repository.mediaForNote(newId).collect { _mediaItems.value = it }
            }
            inserted
        } else {
            val resolved = existingNote ?: return@withLock note
            val toSave = resolved.copy(
                title = note.title,
                content = note.content,
                updatedAt = note.updatedAt,
                isArchived = note.isArchived,
                isDeleted = note.isDeleted,
                fontSize = note.fontSize,
                backgroundColor = note.backgroundColor,
                textColor = note.textColor,
                isBold = note.isBold,
                isItalic = note.isItalic,
                isUnderline = note.isUnderline,
                headingLevel = note.headingLevel,
                listType = note.listType,
                isPinned = note.isPinned,
            )
            repository.update(toSave)
            existingNote = toSave
            toSave
        }
    }

    private suspend fun autoSave() {
        if (title.isBlank() && content.isBlank() && _mediaItems.value.isEmpty()) return
        persistNote(buildCurrentNote())
    }

    // ── Media ─────────────────────────────────────────────────────────────────

    private fun addMedia(uri: String, type: String, mimeType: String = "", fileName: String = "") {
        val existing = existingNote
        if (existing != null) {
            viewModelScope.launch {
                repository.insertMedia(
                    NoteMedia(noteId = existing.id, uri = uri, type = type, mimeType = mimeType, fileName = fileName)
                )
            }
        } else {
            _mediaItems.value = _mediaItems.value + NoteMedia(
                noteId = 0, uri = uri, type = type, mimeType = mimeType, fileName = fileName
            )
            // Create the note immediately so media is persisted even if the user leaves without typing.
            viewModelScope.launch { persistNote(buildCurrentNote()) }
        }
    }

    fun addGalleryImage(sourceUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val mimeType = app.contentResolver.getType(sourceUri) ?: "image/jpeg"
            val originalName = resolveDisplayName(app, sourceUri)
            val ext = when (mimeType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val dir = File(app.filesDir, "media").also { it.mkdirs() }
            val dest = File(dir, "img_${System.currentTimeMillis()}.$ext")
            try {
                app.contentResolver.openInputStream(sourceUri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                addMedia(dest.toUri().toString(), "IMAGE", mimeType, originalName.ifBlank { dest.name })
            } catch (_: Exception) {
                addMedia(sourceUri.toString(), "IMAGE", mimeType, originalName)
            }
        }
    }

    /**
     * Creates the output file in internal storage, persists its path to
     * SavedStateHandle (survives process death while the camera app is open),
     * and returns a FileProvider content:// URI the camera app can write to.
     *
     * Must be called on the main thread before launching the camera intent.
     */
    fun createCameraOutputUri(): Uri {
        val app = getApplication<Application>()
        val dir = File(app.filesDir, "media").also { it.mkdirs() }
        val dest = File(dir, "img_${System.currentTimeMillis()}.jpg")
        savedStateHandle["pendingCameraPath"] = dest.absolutePath
        return FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", dest)
    }

    /** Camera returned RESULT_OK — the file is already fully written. */
    fun onCameraSuccess() {
        val path = savedStateHandle.remove<String>("pendingCameraPath") ?: return
        val file = File(path)
        addMedia(file.toUri().toString(), "IMAGE", "image/jpeg", file.name)
    }

    /** Camera was cancelled — delete the empty pre-allocated file. */
    fun onCameraCancel() {
        val path = savedStateHandle.remove<String>("pendingCameraPath") ?: return
        viewModelScope.launch(Dispatchers.IO) { File(path).delete() }
    }

    /**
     * Copies the picked file into app-private internal storage so it remains
     * accessible after the content:// URI permission expires.
     */
    fun addFileMedia(sourceUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val mimeType = app.contentResolver.getType(sourceUri) ?: "application/octet-stream"
            val originalName = resolveDisplayName(app, sourceUri)
            val ext = extensionForMime(mimeType, sourceUri)
            val dir = File(app.filesDir, "attachments").also { it.mkdirs() }
            val dest = File(dir, "file_${System.currentTimeMillis()}.$ext")
            try {
                app.contentResolver.openInputStream(sourceUri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                addMedia(dest.toUri().toString(), "FILE", mimeType, originalName.ifBlank { dest.name })
            } catch (_: Exception) {
                // Copy failed — fall back to the original URI so the user at least
                // sees something for this session (will break after restart).
                addMedia(sourceUri.toString(), "FILE", mimeType, originalName)
            }
        }
    }

    fun removeMedia(media: NoteMedia) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val uri = Uri.parse(media.uri)
                if (uri.scheme == "file") {
                    runCatching { File(uri.path ?: return@runCatching).delete() }
                }
            }
            if (existingNote != null && media.id != 0) {
                repository.deleteMedia(media.id)
            } else {
                _mediaItems.value = _mediaItems.value.filter { it.uri != media.uri }
            }
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /** Save current state with isArchived = true, then navigate back. */
    fun archive() {
        viewModelScope.launch {
            if (title.isBlank() && content.isBlank()) {
                _events.send(AddEditEvent.NavigateBack)
                return@launch
            }
            persistNote(buildCurrentNote(isArchived = true))
            _events.send(AddEditEvent.NavigateBack)
        }
    }

    fun softDelete() {
        viewModelScope.launch {
            val existing = existingNote
            if (existing != null) {
                repository.update(
                    existing.copy(
                        title = title,
                        content = content,
                        isDeleted = true,
                        updatedAt = System.currentTimeMillis(),
                    )
                )
            }
            _events.send(AddEditEvent.NavigateBack)
        }
    }

    fun shareNote() {
        viewModelScope.launch { _events.send(AddEditEvent.Share(title, content)) }
    }

    fun requestGallery() {
        viewModelScope.launch { _events.send(AddEditEvent.LaunchGallery) }
    }

    fun requestCamera() {
        viewModelScope.launch { _events.send(AddEditEvent.LaunchCamera) }
    }

    fun requestFilePicker() {
        viewModelScope.launch { _events.send(AddEditEvent.LaunchFilePicker) }
    }

    /** Navigate to PIN setup screen; persists the note first if it's brand-new. */
    fun requestLockSetup() {
        viewModelScope.launch {
            if (title.isBlank() && content.isBlank() && _mediaItems.value.isEmpty()) return@launch
            if (existingNote == null) persistNote(buildCurrentNote())
            val id = existingNote?.id ?: return@launch
            _events.send(AddEditEvent.ShowPinSetup(id))
        }
    }

    /** Navigate to PIN removal screen. */
    fun requestLockRemove() {
        viewModelScope.launch {
            val id = existingNote?.id ?: return@launch
            _events.send(AddEditEvent.ShowPinRemove(id))
        }
    }

    /** Re-reads lock state from DB after returning from the PIN lock screen. */
    fun refreshLockState() {
        if (!isEditMode && existingNote == null) return
        viewModelScope.launch {
            val id = existingNote?.id ?: return@launch
            val updated = repository.getById(id) ?: return@launch
            existingNote = updated
            isLocked = updated.isLocked
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun resolveDisplayName(app: Application, uri: Uri): String {
        if (uri.scheme == "content") {
            runCatching {
                app.contentResolver.query(
                    uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx)?.takeIf { it.isNotBlank() }?.let { return it }
                    }
                }
            }
        }
        return uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: ""
    }

    private fun extensionForMime(mimeType: String, sourceUri: Uri): String {
        val fromMap = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        if (!fromMap.isNullOrBlank()) return fromMap
        return when (mimeType) {
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx"
            "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "pptx"
            "application/msword" -> "doc"
            "application/vnd.ms-excel" -> "xls"
            "application/vnd.ms-powerpoint" -> "ppt"
            else -> sourceUri.lastPathSegment?.substringAfterLast('.')
                ?.takeIf { it.isNotBlank() && it.length <= 5 } ?: "bin"
        }
    }
}
