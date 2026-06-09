package com.vivuvi.lightnotes.ui.screen.addedit

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.compose.NotesAppTheme
import com.vivuvi.lightnotes.data.local.NoteMedia
import com.vivuvi.lightnotes.util.AddEditEvent
import com.vivuvi.lightnotes.util.FontSize
import com.vivuvi.lightnotes.util.ListType
import com.vivuvi.lightnotes.util.NoteColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Entry point ─────────────────────────────────────────────────────────────

@Composable
fun AddEditNoteScreen(
    navController: NavController,
    viewModel: AddEditNoteViewModel,
) {
    val context = LocalContext.current
    val mediaItems by viewModel.mediaItems.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Activity result launchers ────────────────────────────────────────────
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.addGalleryImage(it) } }

    // Full-resolution capture: FileProvider URI is written by the camera app directly.
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) viewModel.onCameraSuccess() else viewModel.onCameraCancel()
    }

    // Defined before cameraPermLauncher so its reference is valid inside the callback.
    fun launchCamera() {
        cameraLauncher.launch(viewModel.createCameraOutputUri())
    }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {}
            viewModel.addFileMedia(it)
        }
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) launchCamera() }

    // Speech recognition — delegates to the system recognizer (no RECORD_AUDIO
    // permission needed; the system app owns the microphone during recognition).
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!text.isNullOrBlank()) viewModel.appendSpeechText(text)
        }
    }

    fun launchSpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your note…")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: android.content.ActivityNotFoundException) {
            // Device has no speech recognizer; silently ignore.
        }
    }

    // ── One-time event handler ───────────────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddEditEvent.Share -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, event.title)
                        putExtra(Intent.EXTRA_TEXT, event.content)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Note"))
                }
                AddEditEvent.NavigateBack -> navController.popBackStack()
                AddEditEvent.LaunchGallery -> galleryLauncher.launch("image/*")
                AddEditEvent.LaunchCamera -> {
                    val hasPerm = context.checkSelfPermission(Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED
                    if (hasPerm) launchCamera()
                    else cameraPermLauncher.launch(Manifest.permission.CAMERA)
                }
                AddEditEvent.LaunchFilePicker -> fileLauncher.launch(
                    arrayOf(
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "text/plain",
                    )
                )
                AddEditEvent.NoteSaved -> { /* saved silently */ }
                is AddEditEvent.ShowPinSetup -> {
                    navController.navigate("pin_lock?noteId=${event.noteId}&mode=SETUP")
                }
                is AddEditEvent.ShowPinRemove -> {
                    navController.navigate("pin_lock?noteId=${event.noteId}&mode=REMOVE")
                }
            }
        }
    }

    // Refresh lock state when returning from pin_lock screen (ON_RESUME fires on pop-back).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshLockState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AddEditContent(
        title = viewModel.title,
        content = viewModel.content,
        isEditMode = viewModel.isEditMode,
        isPinned = viewModel.isPinned,
        isBold = viewModel.isBold,
        isItalic = viewModel.isItalic,
        isUnderline = viewModel.isUnderline,
        headingLevel = viewModel.headingLevel,
        listType = viewModel.listType,
        fontSize = viewModel.fontSize,
        backgroundColor = viewModel.backgroundColor,
        textColor = viewModel.textColor,
        mediaItems = mediaItems,
        snackbarHostState = snackbarHostState,
        onTitleChange = viewModel::onTitleChange,
        onContentChange = viewModel::onContentChange,
        onBack = { navController.popBackStack() },
        onPinToggle = viewModel::onPinToggle,
        onArchive = viewModel::archive,
        onMicClick = ::launchSpeechRecognizer,
        onBoldToggle = viewModel::onBoldToggle,
        onItalicToggle = viewModel::onItalicToggle,
        onUnderlineToggle = viewModel::onUnderlineToggle,
        onHeadingLevelChange = viewModel::onHeadingLevelChange,
        onListTypeChange = viewModel::onListTypeChange,
        onFontSizeChange = viewModel::onFontSizeChange,
        onBackgroundColorChange = viewModel::onBackgroundColorChange,
        onTextColorChange = viewModel::onTextColorChange,
        onRequestGallery = viewModel::requestGallery,
        onRequestCamera = viewModel::requestCamera,
        onRequestFilePicker = viewModel::requestFilePicker,
        onShare = viewModel::shareNote,
        onSoftDelete = viewModel::softDelete,
        isLocked = viewModel.isLocked,
        onRequestLockSetup = viewModel::requestLockSetup,
        onRequestLockRemove = viewModel::requestLockRemove,
        onRemoveMedia = viewModel::removeMedia,
        onMediaClick = { media ->
            val encodedUri = Uri.encode(media.uri)
            val encodedName = Uri.encode(
                media.fileName.ifBlank { Uri.parse(media.uri).lastPathSegment ?: "" }
            )
            navController.navigate("file_preview?uri=$encodedUri&name=$encodedName")
        },
    )
}

// ─── Screen-specific TopBar ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditTopBar(
    isEditMode: Boolean,
    isPinned: Boolean,
    onBack: () -> Unit,
    onPinToggle: () -> Unit,
    onArchive: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = if (isEditMode) "Edit Note" else "Add Note",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
        },
        actions = {
            IconButton(onClick = onPinToggle) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = if (isPinned) "Unpin" else "Pin",
                    tint = if (isPinned) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onArchive) {
                Icon(
                    imageVector = Icons.Filled.Archive,
                    contentDescription = "Archive",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

// ─── Stateless content ───────────────────────────────────────────────────────

@Composable
private fun AddEditContent(
    title: String,
    content: String,
    isEditMode: Boolean,
    isPinned: Boolean,
    isBold: Boolean,
    isItalic: Boolean,
    isUnderline: Boolean,
    headingLevel: Int,
    listType: ListType,
    fontSize: FontSize,
    backgroundColor: Long,
    textColor: Long,
    mediaItems: List<NoteMedia>,
    snackbarHostState: SnackbarHostState,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onBack: () -> Unit,
    onPinToggle: () -> Unit,
    onArchive: () -> Unit,
    onMicClick: () -> Unit,
    onBoldToggle: () -> Unit,
    onItalicToggle: () -> Unit,
    onUnderlineToggle: () -> Unit,
    onHeadingLevelChange: (Int) -> Unit,
    onListTypeChange: (ListType) -> Unit,
    onFontSizeChange: (FontSize) -> Unit,
    onBackgroundColorChange: (Long) -> Unit,
    onTextColorChange: (Long) -> Unit,
    onRequestGallery: () -> Unit,
    onRequestCamera: () -> Unit,
    onRequestFilePicker: () -> Unit,
    onShare: () -> Unit,
    onSoftDelete: () -> Unit,
    isLocked: Boolean,
    onRequestLockSetup: () -> Unit,
    onRequestLockRemove: () -> Unit,
    onRemoveMedia: (NoteMedia) -> Unit,
    onMediaClick: (NoteMedia) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (!isEditMode) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    val editTime = remember { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) }
    val cursorColor = SolidColor(MaterialTheme.colorScheme.primary)
    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    val resolvedBg = if (backgroundColor != 0L) Color(backgroundColor)
    else MaterialTheme.colorScheme.background

    val resolvedTextColor = if (textColor != 0L) Color(textColor)
    else MaterialTheme.colorScheme.onBackground

    val effectiveFontSize = when (headingLevel) {
        1 -> 32.sp
        2 -> 26.sp
        3 -> 22.sp
        else -> fontSize.sp
    }
    val contentTextStyle = TextStyle(
        color = resolvedTextColor,
        fontSize = effectiveFontSize,
        fontWeight = if (isBold || headingLevel > 0) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None,
        lineHeight = (effectiveFontSize.value * 1.55f).sp,
    )

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            AddEditTopBar(
                isEditMode = isEditMode,
                isPinned = isPinned,
                onBack = onBack,
                onPinToggle = onPinToggle,
                onArchive = onArchive,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onMicClick,
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(imageVector = Icons.Filled.Mic, contentDescription = "Voice note")
            }
        },
        bottomBar = {
            EditorToolbar(
                isBold = isBold,
                isItalic = isItalic,
                isUnderline = isUnderline,
                headingLevel = headingLevel,
                listType = listType,
                fontSize = fontSize,
                backgroundColor = backgroundColor,
                textColor = textColor,
                onBoldToggle = onBoldToggle,
                onItalicToggle = onItalicToggle,
                onUnderlineToggle = onUnderlineToggle,
                onHeadingLevelChange = onHeadingLevelChange,
                onListTypeChange = onListTypeChange,
                onFontSizeChange = onFontSizeChange,
                onBackgroundColorChange = onBackgroundColorChange,
                onTextColorChange = onTextColorChange,
                onRequestGallery = onRequestGallery,
                onRequestCamera = onRequestCamera,
                onRequestFilePicker = onRequestFilePicker,
                onShare = onShare,
                onSoftDelete = onSoftDelete,
                isLocked = isLocked,
                onRequestLockSetup = onRequestLockSetup,
                onRequestLockRemove = onRequestLockRemove,
            )
        },
        containerColor = resolvedBg,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // ── Note header: title, timestamp, attachments ────────────────────
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp),
            ) {
                val titleStyle = MaterialTheme.typography.displayLarge.copy(
                    color = resolvedTextColor,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None,
                )
                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    textStyle = titleStyle,
                    cursorBrush = cursorColor,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (title.isEmpty()) {
                            Text("Title", style = titleStyle, color = placeholderColor)
                        }
                        inner()
                    },
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Edited $editTime",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                    Text(
                        text = "${content.length} characters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }

                if (mediaItems.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    MediaSection(
                        mediaItems = mediaItems,
                        onRemove = onRemoveMedia,
                        onMediaClick = onMediaClick,
                    )
                }

                Spacer(Modifier.height(8.dp))
            }

            // ── Separator ─────────────────────────────────────────────────────
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp,
            )

            // ── Writing surface ───────────────────────────────────────────────
            // Subtle background lift signals "this is the writing area" even on
            // notes with no custom background color.
            val editorBg = if (backgroundColor == 0L)
                MaterialTheme.colorScheme.surfaceContainerLow
            else
                Color.Transparent

            val showEmptyHint = !isEditMode &&
                title.isBlank() && content.isBlank() && mediaItems.isEmpty()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(editorBg),
            ) {
                val prefixedContent = buildContentWithListMarkers(content, listType)

                BasicTextField(
                    value = if (listType == ListType.NONE) content else prefixedContent,
                    onValueChange = { raw ->
                        onContentChange(stripListMarkers(raw, listType))
                    },
                    textStyle = contentTextStyle,
                    cursorBrush = cursorColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp, bottom = 24.dp)
                        .focusRequester(focusRequester),
                )

                // Centered empty-state hint — visible until the user types anything.
                // Rendered on top of the text field so it doesn't block input events.
                if (showEmptyHint) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Tap anywhere to start writing",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

// ─── Media section ────────────────────────────────────────────────────────────

@Composable
private fun MediaSection(
    mediaItems: List<NoteMedia>,
    onRemove: (NoteMedia) -> Unit,
    onMediaClick: (NoteMedia) -> Unit,
) {
    val images = mediaItems.filter { it.isImage }
    val files = mediaItems.filter { !it.isImage }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (images.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(images, key = { it.uri }) { media ->
                    Box {
                        AsyncImage(
                            model = Uri.parse(media.uri),
                            contentDescription = "Attached image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onMediaClick(media) },
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
                                .clickable { onRemove(media) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove image",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }
        }

        files.forEach { media ->
            val fileName = media.fileName.ifBlank {
                Uri.parse(media.uri).lastPathSegment ?: "Attachment"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onMediaClick(media) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = { onRemove(media) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove file",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

// ─── Helpers for list markers ─────────────────────────────────────────────────

private fun buildContentWithListMarkers(content: String, listType: ListType): String {
    if (listType == ListType.NONE || content.isBlank()) return content
    return content.lines().mapIndexed { i, line ->
        when (listType) {
            ListType.BULLET -> "• $line"
            ListType.NUMBERED -> "${i + 1}. $line"
            ListType.NONE -> line
        }
    }.joinToString("\n")
}

private fun stripListMarkers(raw: String, listType: ListType): String {
    if (listType == ListType.NONE) return raw
    return raw.lines().joinToString("\n") { line ->
        when {
            listType == ListType.BULLET && line.startsWith("• ") -> line.removePrefix("• ")
            listType == ListType.NUMBERED -> line.replaceFirst(Regex("^\\d+\\.\\s"), "")
            else -> line
        }
    }
}

// ─── Editor Toolbar ───────────────────────────────────────────────────────────

@Composable
private fun EditorToolbar(
    isBold: Boolean,
    isItalic: Boolean,
    isUnderline: Boolean,
    headingLevel: Int,
    listType: ListType,
    fontSize: FontSize,
    backgroundColor: Long,
    textColor: Long,
    onBoldToggle: () -> Unit,
    onItalicToggle: () -> Unit,
    onUnderlineToggle: () -> Unit,
    onHeadingLevelChange: (Int) -> Unit,
    onListTypeChange: (ListType) -> Unit,
    onFontSizeChange: (FontSize) -> Unit,
    onBackgroundColorChange: (Long) -> Unit,
    onTextColorChange: (Long) -> Unit,
    onRequestGallery: () -> Unit,
    onRequestCamera: () -> Unit,
    onRequestFilePicker: () -> Unit,
    onShare: () -> Unit,
    onSoftDelete: () -> Unit,
    isLocked: Boolean,
    onRequestLockSetup: () -> Unit,
    onRequestLockRemove: () -> Unit,
) {
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant
    var showAttachMenu by remember { mutableStateOf(false) }
    var showColorMenu by remember { mutableStateOf(false) }
    var showFontSizeMenu by remember { mutableStateOf(false) }
    var showFormatMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete note?") },
            text = { Text("This note will be moved to Trash and can be restored from there.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; onSoftDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    Column(modifier = Modifier.navigationBarsPadding()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {

                // ── Attach ──────────────────────────────────────────────────
                Box {
                    IconButton(onClick = { showAttachMenu = true }) {
                        Icon(Icons.Filled.AddBox, contentDescription = "Attach", tint = iconColor)
                    }
                    DropdownMenu(
                        expanded = showAttachMenu,
                        onDismissRequest = { showAttachMenu = false },
                    ) {
                        ToolbarDropdownItem("Add Image from Gallery", Icons.Filled.AddBox) {
                            showAttachMenu = false; onRequestGallery()
                        }
                        ToolbarDropdownItem("Take Photo with Camera", Icons.Filled.AddBox) {
                            showAttachMenu = false; onRequestCamera()
                        }
                        ToolbarDropdownItem("Attach File (PDF, DOC, TXT)", Icons.Filled.AddBox) {
                            showAttachMenu = false; onRequestFilePicker()
                        }
                    }
                }

                // ── Color ───────────────────────────────────────────────────
                Box {
                    IconButton(onClick = { showColorMenu = true }) {
                        Icon(Icons.Filled.Palette, contentDescription = "Color", tint = iconColor)
                    }
                    DropdownMenu(
                        expanded = showColorMenu,
                        onDismissRequest = { showColorMenu = false },
                    ) {
                        Text(
                            text = "Background",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            NoteColor.backgroundOptions.forEach { (color, _) ->
                                ColorCircle(
                                    color = color,
                                    isSelected = color == backgroundColor,
                                    onClick = { onBackgroundColorChange(color); showColorMenu = false },
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = "Text Color",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            NoteColor.textOptions.forEach { (color, _) ->
                                ColorCircle(
                                    color = color,
                                    isSelected = color == textColor,
                                    onClick = { onTextColorChange(color); showColorMenu = false },
                                )
                            }
                        }
                    }
                }

                // ── Font size ───────────────────────────────────────────────
                Box {
                    IconButton(onClick = { showFontSizeMenu = true }) {
                        Icon(Icons.Filled.FormatSize, contentDescription = "Text size", tint = iconColor)
                    }
                    DropdownMenu(
                        expanded = showFontSizeMenu,
                        onDismissRequest = { showFontSizeMenu = false },
                    ) {
                        FontSize.entries.forEach { size ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = size.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = size.sp),
                                    )
                                },
                                onClick = { onFontSizeChange(size); showFontSizeMenu = false },
                                trailingIcon = {
                                    if (size == fontSize) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                },
                            )
                        }
                    }
                }

                // ── Text format ─────────────────────────────────────────────
                Box {
                    IconButton(onClick = { showFormatMenu = true }) {
                        Icon(Icons.Filled.TextFormat, contentDescription = "Text format", tint = iconColor)
                    }
                    DropdownMenu(
                        expanded = showFormatMenu,
                        onDismissRequest = { showFormatMenu = false },
                    ) {
                        FormatToggleItem("Bold", Icons.Filled.FormatBold, isBold) {
                            onBoldToggle(); showFormatMenu = false
                        }
                        FormatToggleItem("Italic", Icons.Filled.FormatItalic, isItalic) {
                            onItalicToggle(); showFormatMenu = false
                        }
                        FormatToggleItem("Underline", Icons.Filled.FormatUnderlined, isUnderline) {
                            onUnderlineToggle(); showFormatMenu = false
                        }
                        HorizontalDivider()
                        FormatToggleItem("H1", null, headingLevel == 1, label = "H1") {
                            onHeadingLevelChange(1); showFormatMenu = false
                        }
                        FormatToggleItem("H2", null, headingLevel == 2, label = "H2") {
                            onHeadingLevelChange(2); showFormatMenu = false
                        }
                        FormatToggleItem("H3", null, headingLevel == 3, label = "H3") {
                            onHeadingLevelChange(3); showFormatMenu = false
                        }
                        HorizontalDivider()
                        FormatToggleItem(
                            "Bullet List",
                            Icons.AutoMirrored.Filled.FormatListBulleted,
                            listType == ListType.BULLET,
                        ) {
                            onListTypeChange(ListType.BULLET); showFormatMenu = false
                        }
                        FormatToggleItem(
                            "Numbered List",
                            Icons.Filled.FormatListNumbered,
                            listType == ListType.NUMBERED,
                        ) {
                            onListTypeChange(ListType.NUMBERED); showFormatMenu = false
                        }
                    }
                }
            }

            // ── More ────────────────────────────────────────────────────────
            Box {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = iconColor)
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                ) {
                    ToolbarDropdownItem(
                        label = if (isLocked) "Remove Lock" else "Lock Note",
                        icon = Icons.Filled.Lock,
                    ) {
                        showMoreMenu = false
                        if (isLocked) onRequestLockRemove() else onRequestLockSetup()
                    }
                    ToolbarDropdownItem("Share Note", Icons.Filled.Share) {
                        showMoreMenu = false; onShare()
                    }
                    ToolbarDropdownItem(
                        label = "Delete Note",
                        icon = Icons.Filled.Delete,
                        tint = MaterialTheme.colorScheme.error,
                    ) {
                        showMoreMenu = false; showDeleteConfirm = true
                    }
                }
            }
        }
    }
}

// ─── Small helper composables ─────────────────────────────────────────────────

@Composable
private fun ToolbarDropdownItem(
    label: String,
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = tint) },
        onClick = onClick,
    )
}

@Composable
private fun FormatToggleItem(
    contentDescription: String,
    icon: ImageVector?,
    isActive: Boolean,
    label: String = contentDescription,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                color = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
        },
        leadingIcon = {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        trailingIcon = {
            if (isActive) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun ColorCircle(
    color: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (color == 0L) MaterialTheme.colorScheme.surface else Color(color)
    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(32.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}

// ─── Previews ────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun AddEditNoteEmptyPreview() {
    NotesAppTheme {
        AddEditContent(
            title = "", content = "", isEditMode = false, isPinned = false,
            isBold = false, isItalic = false, isUnderline = false,
            headingLevel = 0, listType = ListType.NONE, fontSize = FontSize.MEDIUM,
            backgroundColor = 0L, textColor = 0L, mediaItems = emptyList(),
            snackbarHostState = remember { SnackbarHostState() },
            onTitleChange = {}, onContentChange = {}, onBack = {},
            onPinToggle = {}, onArchive = {}, onMicClick = {},
            onBoldToggle = {}, onItalicToggle = {},
            onUnderlineToggle = {}, onHeadingLevelChange = {}, onListTypeChange = {},
            onFontSizeChange = {}, onBackgroundColorChange = {}, onTextColorChange = {},
            onRequestGallery = {}, onRequestCamera = {}, onRequestFilePicker = {},
            onShare = {}, onSoftDelete = {},
            isLocked = false, onRequestLockSetup = {}, onRequestLockRemove = {},
            onRemoveMedia = {}, onMediaClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddEditNoteFilledPreview() {
    NotesAppTheme {
        AddEditContent(
            title = "Meeting with Design Team",
            content = "Finalize Material 3 components.\n\nKey points:\n- Dynamic color\n- Typography scales",
            isEditMode = true, isPinned = true,
            isBold = true, isItalic = false, isUnderline = false,
            headingLevel = 0, listType = ListType.NONE, fontSize = FontSize.MEDIUM,
            backgroundColor = 0xFFFFF9C4L, textColor = 0L,
            mediaItems = listOf(
                NoteMedia(id = 1, noteId = 1, uri = "file:///placeholder.jpg", type = "IMAGE"),
                NoteMedia(id = 2, noteId = 1, uri = "content://docs/report.pdf", type = "FILE"),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onTitleChange = {}, onContentChange = {}, onBack = {},
            onPinToggle = {}, onArchive = {}, onMicClick = {},
            onBoldToggle = {}, onItalicToggle = {},
            onUnderlineToggle = {}, onHeadingLevelChange = {}, onListTypeChange = {},
            onFontSizeChange = {}, onBackgroundColorChange = {}, onTextColorChange = {},
            onRequestGallery = {}, onRequestCamera = {}, onRequestFilePicker = {},
            onShare = {}, onSoftDelete = {},
            isLocked = true, onRequestLockSetup = {}, onRequestLockRemove = {},
            onRemoveMedia = {}, onMediaClick = {},
        )
    }
}
