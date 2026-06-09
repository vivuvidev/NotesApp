package com.vivuvi.lightnotes.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vivuvi.lightnotes.ui.screen.addedit.AddEditNoteScreen
import com.vivuvi.lightnotes.ui.screen.addedit.AddEditNoteViewModel
import com.vivuvi.lightnotes.ui.screen.pinlock.PinLockScreen
import com.vivuvi.lightnotes.ui.screen.pinlock.PinLockViewModel
import com.vivuvi.lightnotes.ui.screen.archive.ArchiveScreen
import com.vivuvi.lightnotes.ui.screen.archive.ArchiveViewModel
import com.vivuvi.lightnotes.ui.screen.filepreview.FilePreviewScreen
import com.vivuvi.lightnotes.ui.screen.notes.NotesViewModel
import com.vivuvi.lightnotes.ui.screen.notes.noteScreen
import com.vivuvi.lightnotes.ui.screen.search.SearchScreen
import com.vivuvi.lightnotes.ui.screen.search.SearchViewModel
import com.vivuvi.lightnotes.ui.screen.settings.SettingsScreen
import com.vivuvi.lightnotes.ui.screen.settings.SettingsViewModel
import com.vivuvi.lightnotes.ui.screen.trash.TrashScreen
import com.vivuvi.lightnotes.ui.screen.trash.TrashViewModel

@Composable
fun noteScreenNav(notesViewModel: NotesViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "notes",
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {

        composable("notes") {
            noteScreen(
                navController,
                notesViewModel,
                onSettingsClick = { navController.navigate("settings") },
            )
        }

        composable(
            route = "addedit?noteId={noteId}",
            arguments = listOf(navArgument("noteId") {
                type = NavType.IntType
                defaultValue = -1
            }),
        ) {
            val vm: AddEditNoteViewModel = viewModel()
            AddEditNoteScreen(navController, vm)
        }

        composable("search") {
            val vm: SearchViewModel = viewModel()
            SearchScreen(navController, vm)
        }

        composable("archive") {
            val vm: ArchiveViewModel = viewModel()
            ArchiveScreen(navController, vm)
        }

        composable("trash") {
            val vm: TrashViewModel = viewModel()
            TrashScreen(navController, vm)
        }

        composable("settings") {
            val vm: SettingsViewModel = viewModel()
            SettingsScreen(navController, vm)
        }

        composable(
            route = "pin_lock?noteId={noteId}&mode={mode}",
            arguments = listOf(
                navArgument("noteId") { type = NavType.IntType },
                navArgument("mode") { type = NavType.StringType; defaultValue = "UNLOCK" },
            ),
        ) {
            val vm: PinLockViewModel = viewModel()
            PinLockScreen(navController, vm)
        }

        composable(
            route = "file_preview?uri={uri}&name={name}",
            arguments = listOf(
                navArgument("uri") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("uri") ?: ""
            val name = backStackEntry.arguments?.getString("name") ?: ""
            FilePreviewScreen(navController, encoded, name.takeIf { it.isNotBlank() })
        }
    }
}
