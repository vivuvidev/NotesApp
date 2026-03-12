package com.example.notesapp.ui.screen.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notesapp.data.local.Note
import com.example.notesapp.ui.components.AppTopBar
import com.example.notesapp.ui.components.FloatingActionButton
import com.example.notesapp.ui.screen.notes.NotesViewModel
import java.util.Date

@Composable
fun addEditNoteScreen(
    goToNotesScreen: () -> Unit,
    viewModel: NotesViewModel = viewModel()
) {

    Scaffold(
        topBar = {
            AppTopBar(

                title = "Edit Note",
                showBackButton=true,
                onBackClick = goToNotesScreen,
                showKeep = true,
                showCheck = true

            )
                 },


    ) { paddingValues ->


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            Text("No notes yet")
        }


    }
}