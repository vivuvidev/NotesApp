package com.vivuvi.lightnotes.ui.screen.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.compose.NotesAppTheme
import com.vivuvi.lightnotes.data.local.Note
import com.vivuvi.lightnotes.ui.components.FloatingActionButton
import com.vivuvi.lightnotes.ui.components.NoteCard
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.util.Date

// ─── Entry point ──────────────────────────────────────────────────────────────

@Composable
fun noteScreen(
    navController: NavController,
    notesViewModel: NotesViewModel,
    onSettingsClick: () -> Unit = { navController.navigate("settings") },
) {
    val notes by notesViewModel.notes.collectAsState()
    NotesScreenContent(
        notes = notes,
        onAddClick = { navController.navigate("addedit") },
        onNoteClick = { note ->
            if (note.isLocked) {
                navController.navigate("pin_lock?noteId=${note.id}&mode=UNLOCK")
            } else {
                navController.navigate("addedit?noteId=${note.id}")
            }
        },
        onSearchClick = { navController.navigate("search") },
        onArchiveClick = { navController.navigate("archive") },
        onTrashClick = { navController.navigate("trash") },
        onSettingsClick = onSettingsClick,
        onDragStart = notesViewModel::onDragStart,
        onMoveNote = notesViewModel::moveNote,
        onDragEnd = notesViewModel::onDragEnd,
    )
}

// ─── Collapsing top bar ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesLargeTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onSearchClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onTrashClick: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    LargeTopAppBar(
        title = {
            Text(
                text = "My Notes",
                style = MaterialTheme.typography.headlineMedium,
            )
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Archived Notes") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Archive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = { menuExpanded = false; onArchiveClick() },
                    )
                    DropdownMenuItem(
                        text = { Text("Trash") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = { menuExpanded = false; onTrashClick() },
                    )
                }
            }
        },
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        scrollBehavior = scrollBehavior,
    )
}

// ─── Bottom navigation ────────────────────────────────────────────────────────

@Composable
private fun NotesBottomBar(modifier: Modifier = Modifier, onSettingsClick: () -> Unit = {}) {
    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.tertiary,
        unselectedTextColor = MaterialTheme.colorScheme.tertiary,
    )
    Column(modifier = modifier) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
        ) {
            NavigationBarItem(
                selected = true,
                onClick = {},
                icon = { Icon(Icons.Filled.Description, contentDescription = "Notes") },
                label = { Text("Notes", style = MaterialTheme.typography.labelSmall) },
                colors = navItemColors,
            )
            NavigationBarItem(
                selected = false,
                onClick = onSettingsClick,
                icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                label = { Text("Settings", style = MaterialTheme.typography.labelSmall) },
                colors = navItemColors,
            )
        }
    }
}

// ─── Stateless content ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesScreenContent(
    notes: List<Note>,
    onAddClick: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onSearchClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onTrashClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onDragStart: () -> Unit = {},
    onMoveNote: (Int, Int) -> Unit = { _, _ -> },
    onDragEnd: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    val dragDropState = rememberDragDropListState(listState)

    // ── Hide-on-scroll state ──────────────────────────────────────────────────
    val barsVisibleState = remember { mutableStateOf(true) }
    val barsVisible by barsVisibleState

    var bottomBarHeightPx by remember { mutableIntStateOf(0) }
    val bottomBarSlide by animateFloatAsState(
        targetValue = if (barsVisible) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "bottomBarSlide",
    )

    val scrollThresholdPx = with(LocalDensity.current) { 36.dp.toPx() }
    val barsScrollConnection = remember(scrollThresholdPx) {
        object : NestedScrollConnection {
            var accumulated = 0f
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.Drag) return Offset.Zero
                val delta = available.y
                if (accumulated != 0f && (delta > 0f) != (accumulated > 0f)) accumulated = 0f
                accumulated += delta
                when {
                    accumulated < -scrollThresholdPx -> {
                        barsVisibleState.value = false
                        accumulated = -scrollThresholdPx
                    }
                    accumulated > scrollThresholdPx -> {
                        barsVisibleState.value = true
                        accumulated = scrollThresholdPx
                    }
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }
            .distinctUntilChanged()
            .filter { it }
            .collect { barsVisibleState.value = true }
    }

    // ── Staggered entrance ────────────────────────────────────────────────────
    // Only plays on the first load after a cold start; subsequent visits skip it.
    var staggerDone by rememberSaveable { mutableStateOf(false) }
    var revealedCount by remember { mutableIntStateOf(if (staggerDone) Int.MAX_VALUE else 0) }

    LaunchedEffect(notes.isNotEmpty()) {
        if (notes.isNotEmpty() && !staggerDone) {
            staggerDone = true
            notes.indices.forEach { i ->
                kotlinx.coroutines.delay(55L)
                revealedCount = i + 1
            }
            revealedCount = Int.MAX_VALUE
        }
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .nestedScroll(barsScrollConnection),
        topBar = {
            NotesLargeTopBar(
                scrollBehavior = scrollBehavior,
                onSearchClick = onSearchClick,
                onArchiveClick = onArchiveClick,
                onTrashClick = onTrashClick,
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = barsVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                FloatingActionButton(onClick = onAddClick)
            }
        },
        bottomBar = {
            NotesBottomBar(
                modifier = Modifier
                    .onSizeChanged { bottomBarHeightPx = it.height }
                    .graphicsLayer { translationY = bottomBarHeightPx * bottomBarSlide },
                onSettingsClick = onSettingsClick,
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
                    text = "No notes yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                onDragStart()
                                dragDropState.onDragStart(offset)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragDropState.onDrag(dragAmount) { from, to ->
                                    onMoveNote(from, to)
                                }
                            },
                            onDragEnd = {
                                dragDropState.onDragEnd()
                                onDragEnd()
                            },
                            onDragCancel = {
                                dragDropState.onDragEnd()
                                onDragEnd()
                            },
                        )
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                itemsIndexed(notes, key = { _, note -> note.id }) { index, note ->
                    val isDraggingThis = index == dragDropState.draggingItemIndex

                    // Per-item reveal alpha for the stagger (animates 0→1 as revealedCount passes)
                    val revealAlpha by animateFloatAsState(
                        targetValue = if (index < revealedCount) 1f else 0f,
                        animationSpec = tween(durationMillis = 220),
                        label = "revealAlpha$index",
                    )

                    NoteCard(
                        note = note,
                        onClick = if (isDraggingThis) ({}) else ({ onNoteClick(note) }),
                        modifier = Modifier
                            .then(
                                if (staggerDone && !isDraggingThis)
                                    Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                                else Modifier,
                            )
                            .graphicsLayer {
                                alpha = revealAlpha
                                translationY = if (isDraggingThis) dragDropState.draggingItemOffset else 0f
                                shadowElevation = if (isDraggingThis) 10.dp.toPx() else 0f
                                scaleX = if (isDraggingThis) 1.03f else 1f
                                scaleY = if (isDraggingThis) 1.03f else 1f
                            },
                    )
                }
            }
        }
    }
}

// ─── Previews ────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun NotesScreenEmptyPreview() {
    NotesAppTheme {
        NotesScreenContent(
            notes = emptyList(),
            onAddClick = {},
            onNoteClick = {},
            onSearchClick = {},
            onArchiveClick = {},
            onTrashClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotesScreenWithNotesPreview() {
    NotesAppTheme {
        NotesScreenContent(
            notes = listOf(
                Note(1, "Grocery List", "Buy milk, organic eggs, sourdough bread.", Date(), System.currentTimeMillis()),
                Note(2, "Project Ideas", "Brainstorming for the new productivity app.", Date(), System.currentTimeMillis()),
                Note(3, "Weekend Trip", "Book the cabin. Pack hiking boots.", Date(), System.currentTimeMillis()),
            ),
            onAddClick = {},
            onNoteClick = {},
            onSearchClick = {},
            onArchiveClick = {},
            onTrashClick = {},
        )
    }
}
