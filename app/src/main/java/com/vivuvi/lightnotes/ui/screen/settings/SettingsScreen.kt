package com.vivuvi.lightnotes.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.compose.NotesAppTheme
import com.vivuvi.lightnotes.BuildConfig

// ─── Entry point ──────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(navController: NavController, viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    SettingsContent(
        uiState = uiState,
        onBackClick = { navController.popBackStack() },
        onDarkModeToggle = viewModel::setDarkMode,
        onArchiveClick = { navController.navigate("archive") },
        onTrashClick = { navController.navigate("trash") },
        onShowDialog = viewModel::showDialog,
        onDismissDialog = viewModel::dismissDialog,
        onConfirmClearAll = viewModel::deleteAllNotes,
    )
}

// ─── Stateless content ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onBackClick: () -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onArchiveClick: () -> Unit,
    onTrashClick: () -> Unit,
    onShowDialog: (SettingsDialog) -> Unit,
    onDismissDialog: () -> Unit,
    onConfirmClearAll: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Appearance ────────────────────────────────────────────────────
            item { SectionHeader("Appearance") }
            item {
                DarkModeRow(
                    checked = uiState.isDarkMode,
                    onCheckedChange = onDarkModeToggle,
                )
            }

            // ── Data Management ───────────────────────────────────────────────
            item { Spacer(Modifier.height(8.dp)) }
            item { SectionHeader("Data Management") }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Archive,
                    label = "Manage Archive",
                    onClick = onArchiveClick,
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Delete,
                    label = "Manage Trash",
                    onClick = onTrashClick,
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.AutoDelete,
                    label = "Clear all notes",
                    onClick = { onShowDialog(SettingsDialog.ConfirmClearAll) },
                    labelColor = MaterialTheme.colorScheme.error,
                    iconTint = MaterialTheme.colorScheme.error,
                    showChevron = false,
                )
            }

            // ── About ─────────────────────────────────────────────────────────
            item { Spacer(Modifier.height(8.dp)) }
            item { SectionHeader("About") }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    label = "App Version",
                    onClick = {},
                    showChevron = false,
                    trailing = {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    },
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Code,
                    label = "Developer Info",
                    onClick = { onShowDialog(SettingsDialog.DeveloperInfo) },
                )
            }

            item { Spacer(Modifier.height(48.dp)) }
        }

        // ── Dialogs ───────────────────────────────────────────────────────────
        when (uiState.activeDialog) {
            SettingsDialog.DeveloperInfo -> InfoDialog(
                title = "Developer Info",
                content = "Light Notes is a simple and fast note-taking app designed to help you capture ideas, organize thoughts, and manage daily tasks with ease.\n\n" +
                        "It supports offline storage, auto-save, and media attachments to keep your notes always safe and accessible.\n\n" +
                        "© 2026 vivuvidev",
                onDismiss = onDismissDialog,
            )
            SettingsDialog.ConfirmClearAll -> ConfirmClearAllDialog(
                onDismiss = onDismissDialog,
                onConfirm = onConfirmClearAll,
            )
            null -> {}
        }
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
private fun DarkModeRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.DarkMode,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = "Dark Mode",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = labelColor,
            modifier = Modifier.weight(1f),
        )
        when {
            trailing != null -> trailing()
            showChevron -> Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun InfoDialog(title: String, content: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
    )
}

@Composable
private fun ConfirmClearAllDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Clear all notes?", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(
                text = "This will permanently delete all notes, including archived and " +
                    "deleted ones. This cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(text = "Delete all")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
    )
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    NotesAppTheme {
        SettingsContent(
            uiState = SettingsUiState(isDarkMode = false),
            onBackClick = {},
            onDarkModeToggle = {},
            onArchiveClick = {},
            onTrashClick = {},
            onShowDialog = {},
            onDismissDialog = {},
            onConfirmClearAll = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsDarkPreview() {
    NotesAppTheme(darkTheme = true) {
        SettingsContent(
            uiState = SettingsUiState(isDarkMode = true),
            onBackClick = {},
            onDarkModeToggle = {},
            onArchiveClick = {},
            onTrashClick = {},
            onShowDialog = {},
            onDismissDialog = {},
            onConfirmClearAll = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeveloperInfoDialogPreview() {
    NotesAppTheme {
        SettingsContent(
            uiState = SettingsUiState(activeDialog = SettingsDialog.DeveloperInfo),
            onBackClick = {},
            onDarkModeToggle = {},
            onArchiveClick = {},
            onTrashClick = {},
            onShowDialog = {},
            onDismissDialog = {},
            onConfirmClearAll = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmClearAllDialogPreview() {
    NotesAppTheme {
        SettingsContent(
            uiState = SettingsUiState(activeDialog = SettingsDialog.ConfirmClearAll),
            onBackClick = {},
            onDarkModeToggle = {},
            onArchiveClick = {},
            onTrashClick = {},
            onShowDialog = {},
            onDismissDialog = {},
            onConfirmClearAll = {},
        )
    }
}
