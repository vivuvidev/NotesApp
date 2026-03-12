package com.example.notesapp.ui.screen.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notesapp.data.local.Note
import com.example.notesapp.ui.components.AppTopBar
import com.example.notesapp.ui.components.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.*
import com.example.notesapp.ui.components.NoteCard

@Composable
fun noteScreen(
    goToAddEditScreen: () -> Unit,
    viewModel: NotesViewModel = viewModel()
) {
    val notes by viewModel.allNotes.observeAsState(listOf())

    Scaffold(
        topBar = { AppTopBar(
            title = "My Notes",
            showMenu = true,
            showSearch = true
        ) },
        floatingActionButton = { FloatingActionButton(onClick = goToAddEditScreen) }

    ) { paddingValues ->


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

//                    Text("No notes yet")


           LazyColumn(
               content = {
                 itemsIndexed(notes){index,note ->
                     NoteCard(note=note)

                 }
               }
           )
                }


    }
}
