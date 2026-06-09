package com.vivuvi.lightnotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.compose.NotesAppTheme
import com.vivuvi.lightnotes.data.local.Note
import com.vivuvi.lightnotes.util.FontSize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NoteCard(note: Note, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)
    val cardBg = if (note.backgroundColor != 0L) Color(note.backgroundColor)
    else MaterialTheme.colorScheme.surface

    val noteTextColor = if (note.textColor != 0L) Color(note.textColor) else null
    val noteFontSize = FontSize.entries.find { it.name == note.fontSize }?.sp ?: FontSize.MEDIUM.sp
    val noteFontWeight = if (note.isBold) FontWeight.Bold else FontWeight.Normal
    val noteFontStyle = if (note.isItalic) FontStyle.Italic else FontStyle.Normal
    val noteTextDecoration = if (note.isUnderline) TextDecoration.Underline else TextDecoration.None

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBg)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = shape)
            .clickable { onClick() }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val displayTitle = note.title.ifBlank { "Untitled Note" }
            val titleColor = when {
                note.title.isBlank() -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                noteTextColor != null -> noteTextColor
                else -> MaterialTheme.colorScheme.onSurface
            }
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = noteFontWeight,
                    fontStyle = noteFontStyle,
                    textDecoration = noteTextDecoration,
                ),
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (note.isLocked) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp),
                )
            }
            if (note.isPinned) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = "Pinned",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (note.isLocked) {
            Text(
                text = "Protected",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            )
        } else {
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = noteFontSize,
                    fontWeight = noteFontWeight,
                    fontStyle = noteFontStyle,
                    textDecoration = noteTextDecoration,
                ),
                color = noteTextColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        val dateLabel = if (note.updatedAt > 0L) {
            formatTimestamp(note.updatedAt)
        } else {
            formatDate(note.createdAt)
        }
        Text(
            text = dateLabel.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
        )
    }
}

private fun formatDate(date: Date): String =
    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)

private fun formatTimestamp(ts: Long): String =
    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(ts))

@Preview(showBackground = true, backgroundColor = 0xFFF6F6F8)
@Composable
private fun NoteCardPreview() {
    NotesAppTheme {
        NoteCard(
            note = Note(
                id = 1,
                title = "Grocery List",
                content = "Buy milk, organic eggs, sourdough bread, and some fresh kale from the farmers market.",
                createdAt = Date(),
                updatedAt = System.currentTimeMillis(),
                isPinned = true,
                backgroundColor = 0xFFFFF9C4L,
            )
        )
    }
}
