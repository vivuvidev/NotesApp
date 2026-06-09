package com.vivuvi.lightnotes.ui.screen.archive

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
fun ArchiveScreen(navController: NavController, viewModel: ArchiveViewModel) {
    val notes by viewModel.archivedNotes.observeAsState(emptyList())
    ArchiveContent(
        notes = notes,
        onBack = { navController.popBackStack() },
        onRestore = viewModel::restoreNote,
        onDelete = viewModel::sendToTrash,
    )
}

// ─── Top bar ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text("Archived Notes", style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
private fun ArchiveContent(
    notes: List<Note>,
    onBack: () -> Unit,
    onRestore: (Note) -> Unit,
    onDelete: (Note) -> Unit,
) {
    Scaffold(
        topBar = { ArchiveTopBar(onBack = onBack) },
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
                    text = "No archived notes",
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
                    ArchivedNoteItem(
                        note = note,
                        onRestore = { onRestore(note) },
                        onDelete = { onDelete(note) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchivedNoteItem(
    note: Note,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
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
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Delete")
            }
        }
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun ArchiveEmptyPreview() {
    NotesAppTheme {
        ArchiveContent(
            notes = emptyList(),
            onBack = {},
            onRestore = {},
            onDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArchiveWithNotesPreview() {
    NotesAppTheme {
        ArchiveContent(
            notes = listOf(
                Note(1, "Old Meeting Notes", "Discussed Q2 budget and resource planning.", Date(), System.currentTimeMillis()),
                Note(2, "Travel Plans 2023", "Flights booked. Hotel confirmation pending.", Date(), System.currentTimeMillis()),
            ),
            onBack = {},
            onRestore = {},
            onDelete = {},
        )
    }
}
