package com.vivuvi.lightnotes.ui.screen.pinlock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.compose.NotesAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinLockScreen(navController: NavController, viewModel: PinLockViewModel) {
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PinLockEvent.Success -> {
                    // Signal the caller (addedit) so it can refresh its lock state.
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("lockResult", "changed")
                    navController.popBackStack()
                }
                is PinLockEvent.UnlockSuccess -> {
                    // Replace pin_lock with addedit so back goes to notes, not pin_lock.
                    navController.popBackStack()
                    navController.navigate("addedit?noteId=${event.noteId}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (viewModel.mode) {
                            PinMode.SETUP -> "Lock Note"
                            PinMode.UNLOCK -> "Unlock Note"
                            PinMode.REMOVE -> "Remove Lock"
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        PinLockContent(
            mode = viewModel.mode,
            pin = viewModel.pin,
            isConfirmStep = viewModel.isConfirmStep,
            errorMessage = viewModel.errorMessage,
            onDigit = viewModel::appendDigit,
            onDelete = viewModel::deleteLastDigit,
            onConfirm = viewModel::confirm,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun PinLockContent(
    mode: PinMode,
    pin: String,
    isConfirmStep: Boolean,
    errorMessage: String?,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(52.dp),
        )

        Spacer(Modifier.height(28.dp))

        Text(
            text = when {
                mode == PinMode.SETUP && !isConfirmStep -> "Set a PIN for this note"
                mode == PinMode.SETUP && isConfirmStep  -> "Confirm your PIN"
                mode == PinMode.UNLOCK                  -> "Enter PIN to unlock"
                else                                    -> "Enter PIN to remove lock"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(36.dp))

        // Six dot indicators (filled per entered digit, max 6).
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(6) { index ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < pin.length) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                        ),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Fixed-height slot for error text so nothing shifts on appearance.
        Box(modifier = Modifier.height(20.dp), contentAlignment = Alignment.Center) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        // Numeric keypad
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            listOf(
                listOf(1, 2, 3),
                listOf(4, 5, 6),
                listOf(7, 8, 9),
            ).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    row.forEach { digit ->
                        PinKeyButton(label = digit.toString(), onClick = { onDigit(digit) })
                    }
                }
            }
            // Bottom row: confirm | 0 | backspace
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                PinKeyButton(
                    label = "✓",
                    isConfirm = true,
                    enabled = pin.length >= 4,
                    onClick = onConfirm,
                )
                PinKeyButton(label = "0", onClick = { onDigit(0) })
                PinKeyButton(label = "⌫", onClick = onDelete)
            }
        }
    }
}

@Composable
private fun PinKeyButton(
    label: String,
    isConfirm: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    // Confirm key switches to primary colour when the PIN length requirement is met,
    // giving users a clear signal that the button is actionable.
    val containerColor = when {
        isConfirm && enabled  -> MaterialTheme.colorScheme.primary
        enabled               -> MaterialTheme.colorScheme.surfaceVariant
        else                  -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
    }
    val contentColor = when {
        isConfirm && enabled  -> MaterialTheme.colorScheme.onPrimary
        enabled               -> MaterialTheme.colorScheme.onSurfaceVariant
        else                  -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }

    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal),
            color = contentColor,
        )
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun PinUnlockPreview() {
    NotesAppTheme {
        PinLockContent(
            mode = PinMode.UNLOCK,
            pin = "12",
            isConfirmStep = false,
            errorMessage = null,
            onDigit = {}, onDelete = {}, onConfirm = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PinSetupConfirmPreview() {
    NotesAppTheme {
        PinLockContent(
            mode = PinMode.SETUP,
            pin = "1234",
            isConfirmStep = true,
            errorMessage = "PINs don't match. Try again.",
            onDigit = {}, onDelete = {}, onConfirm = {},
        )
    }
}
