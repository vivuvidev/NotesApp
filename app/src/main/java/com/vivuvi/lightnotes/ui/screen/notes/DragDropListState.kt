package com.vivuvi.lightnotes.ui.screen.notes

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

class DragDropListState(val lazyListState: LazyListState) {

    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    var draggingItemOffset by mutableFloatStateOf(0f)
        private set

    fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.let { item ->
                draggingItemIndex = item.index
                draggingItemOffset = 0f
            }
    }

    fun onDrag(delta: Offset, onSwap: (fromIndex: Int, toIndex: Int) -> Unit) {
        draggingItemOffset += delta.y

        val idx = draggingItemIndex ?: return
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val draggingItem = visibleItems.find { it.index == idx } ?: return

        // Virtual center of the dragged card as the user sees it
        val visualCenter = draggingItem.offset + draggingItem.size / 2f + draggingItemOffset

        when {
            delta.y > 0 -> {
                val next = visibleItems.find { it.index == idx + 1 } ?: return
                if (visualCenter > next.offset + next.size / 2f) {
                    onSwap(idx, idx + 1)
                    draggingItemIndex = idx + 1
                    // Shift offset so the card's visual position is continuous after the swap
                    draggingItemOffset -= next.size.toFloat()
                }
            }
            delta.y < 0 -> {
                val prev = visibleItems.find { it.index == idx - 1 } ?: return
                if (visualCenter < prev.offset + prev.size / 2f) {
                    onSwap(idx, idx - 1)
                    draggingItemIndex = idx - 1
                    draggingItemOffset += prev.size.toFloat()
                }
            }
        }
    }

    fun onDragEnd() {
        draggingItemIndex = null
        draggingItemOffset = 0f
    }
}

@Composable
fun rememberDragDropListState(lazyListState: LazyListState): DragDropListState =
    remember(lazyListState) { DragDropListState(lazyListState) }
