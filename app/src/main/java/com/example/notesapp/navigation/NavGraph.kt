package com.example.notesapp.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import androidx.navigation.compose.rememberNavController
import com.example.notesapp.ui.screen.addedit.addEditNoteScreen
import com.example.notesapp.ui.screen.notes.NotesViewModel
import com.example.notesapp.ui.screen.notes.noteScreen


@Composable
fun navGraph(){

    val navController= rememberNavController()
    val notesViewModel: NotesViewModel = viewModel()

    NavHost (navController=navController,startDestination="notes"){

        composable("notes") {
            noteScreen(
                goToAddEditScreen = { navController.navigate("addedit") },
                viewModel = notesViewModel
            )
        }
        composable("addedit") {
            addEditNoteScreen({navController.popBackStack()},
                viewModel = notesViewModel

            )
        }
    }

}