package com.vivuvi.lightnotes

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.compose.NotesAppTheme
import com.vivuvi.lightnotes.navigation.noteScreenNav
import com.vivuvi.lightnotes.ui.screen.notes.NotesViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val notesViewModel: NotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Synchronous read of the persisted preference so the very first frame — including
        // the splash-to-app transition — uses the correct theme without any flash.
        val initialDarkMode = runBlocking { MainApplication.themePreferences.isDarkMode.first() }

        // Apply window-level state for the initial frame before Compose runs.
        applyWindowTheme(initialDarkMode)

        setContent {
            val isDarkMode by MainApplication.isDarkModeFlow.collectAsState(initial = initialDarkMode)

            // Single owner of all window-level theme effects.
            // Fires on the first composition and again whenever isDarkMode changes —
            // this covers both the initial render and live theme switches from Settings.
            DisposableEffect(isDarkMode) {
                applyWindowTheme(isDarkMode)
                onDispose { /* nothing to tear down */ }
            }

            NotesAppTheme(darkTheme = isDarkMode) {
                noteScreenNav(notesViewModel)
            }
        }
    }

    /**
     * Applies all three window-level effects that must stay in sync with the current theme:
     * 1. Edge-to-edge bar styles (scrim color + icon tints via [enableEdgeToEdge]).
     * 2. Window background colour (eliminates flash during activity transitions).
     *
     * Called once before [setContent] for the first frame, then reactively from a
     * [DisposableEffect] inside the composition whenever [isDark] changes.
     */
    private fun applyWindowTheme(isDark: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = systemBarStyle(isDark),
            navigationBarStyle = systemBarStyle(isDark),
        )
        window.setBackgroundDrawable(
            ColorDrawable(AndroidColor.parseColor(if (isDark) "#13151F" else "#F6F6F8"))
        )
    }

    private fun systemBarStyle(isDark: Boolean): SystemBarStyle {
        val bg = AndroidColor.parseColor(if (isDark) "#13151F" else "#F6F6F8")
        return if (isDark) SystemBarStyle.dark(bg) else SystemBarStyle.light(bg, bg)
    }
}
