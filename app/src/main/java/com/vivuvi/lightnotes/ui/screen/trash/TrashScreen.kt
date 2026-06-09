package com.vivuvi.lightnotes.ui.screen.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.compose.NotesAppTheme
import com.vivuvi.lightnotes.data.local.Note
import com.vivuvi.lightnotes.ui.components.NoteCard
import java.util.Date

// ─── Entry point ──────────────────────────────────────────────────────────────

@Composable
fun TrashScreen(navController: NavController, viewModel: TrashViewModel) {
    val notes by viewModel.deletedNotes.observeAsState(emptyList())
    TrashContent(
        notes = notes,
        onBack = { navController.popBackStack() },
        onRestore = viewModel::restoreNote,
        onPermanentlyDelete = viewModel::permanentlyDelete,
        onEmptyTrash = viewModel::emptyTrash,
    )
}

// ─── Top bar ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrashTopBar(
    onBack: () -> Unit,
    hasNotes: Boolean,
    onEmptyTrashClick: () -> Unit,
) {
    TopAppBar(
        title = { Text("Trash", style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            if (hasNotes) {
                TextButton(
                    onClick = onEmptyTrashClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Empty Trash")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

// ─── Stateless content ────────────────────────────────────────────────────────

@Composable
private fun TrashContent(
    notes: List<Note>,
    onBack: () -> Unit,
    onRestore: (Note) -> Unit,
    onPermanentlyDelete: (Note) -> Unit,
    onEmptyTrash: () -> Unit,
) {
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }

    if (showEmptyTrashConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirm = false },
            title = { Text("Empty Trash?") },
            text = {
                Text(
                    if (notes.size == 1) "1 note will be permanently deleted and cannot be recovered."
                    else "${notes.size} notes will be permanently deleted and cannot be recovered."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showEmptyTrashConfirm = false; onEmptyTrash() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete All") }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashConfirm = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TrashTopBar(
                onBack = onBack,
                hasNotes = notes.isNotEmpty(),
                onEmptyTrashClick = { showEmptyTrashConfirm = true },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Trash is empty",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                items(notes, key = { it.id }) { note ->
                    TrashedNoteItem(
                        note = note,
                        onRestore = { onRestore(note) },
                        onPermanentlyDelete = { onPermanentlyDelete(note) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrashedNoteItem(
    note: Note,
    onRestore: () -> Unit,
    onPermanentlyDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete permanently?") },
            text = { Text("This note will be deleted forever and cannot be recovered.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; onPermanentlyDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    Column {
        NoteCard(note = note, onClick = {})
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onRestore) {
                Text("Restore", color = MaterialTheme.colorScheme.primary)
            }
            TextButton(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Delete Forever")
            }
        }
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun TrashEmptyPreview() {
    NotesAppTheme {
        TrashContent(
            notes = emptyList(),
            onBack = {},
            onRestore = {},
            onPermanentlyDelete = {},
            onEmptyTrash = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrashWithNotesPreview() {
    NotesAppTheme {
        TrashContent(
            notes = listOf(
                Note(1, "Draft: January Goals", "Things I wanted to accomplish this year.", Date(), System.currentTimeMillis()),
            ),
            onBack = {},
            onRestore = {},
            onPermanentlyDelete = {},
            onEmptyTrash = {},
        )
    }
}
