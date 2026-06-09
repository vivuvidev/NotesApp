package com.vivuvi.lightnotes.util

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

enum class FontSize(val sp: TextUnit, val label: String) {
    SMALL(14.sp, "Small"),
    MEDIUM(18.sp, "Medium"),
    LARGE(22.sp, "Large"),
    EXTRA_LARGE(28.sp, "Extra Large"),
}

enum class ListType(val label: String) {
    NONE("None"),
    BULLET("Bullet List"),
    NUMBERED("Numbered List"),
}

object NoteColor {
    val backgroundOptions: List<Pair<Long, String>> = listOf(
        0L to "Default",
        0xFFFFF9C4L to "Yellow",
        0xFFBBDEFBL to "Blue",
        0xFFC8E6C9L to "Green",
        0xFFF8BBD0L to "Pink",
        0xFFE1BEE7L to "Purple",
        0xFFFFCCBCL to "Orange",
    )
    val textOptions: List<Pair<Long, String>> = listOf(
        0L to "Default",
        0xFFF44336L to "Red",
        0xFF2196F3L to "Blue",
        0xFF4CAF50L to "Green",
        0xFFFF9800L to "Orange",
        0xFF9C27B0L to "Purple",
    )
}

sealed class AddEditEvent {
    data class Share(val title: String, val content: String) : AddEditEvent()
    object NavigateBack : AddEditEvent()
    object LaunchGallery : AddEditEvent()
    object LaunchCamera : AddEditEvent()
    object LaunchFilePicker : AddEditEvent()
    object NoteSaved : AddEditEvent()
    data class ShowPinSetup(val noteId: Int) : AddEditEvent()
    data class ShowPinRemove(val noteId: Int) : AddEditEvent()
}
