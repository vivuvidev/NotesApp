package com.vivuvi.lightnotes.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_media",
    foreignKeys = [ForeignKey(
        entity = Note::class,
        parentColumns = ["id"],
        childColumns = ["noteId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("noteId")],
)
data class NoteMedia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val noteId: Int,
    /** file:// URI pointing to app-private internal storage. */
    val uri: String,
    /** "IMAGE" or "FILE" */
    val type: String,
    /** MIME type captured at attachment time, e.g. "application/pdf". Empty string for legacy rows. */
    val mimeType: String = "",
    /** Original filename as shown to the user, e.g. "report.pdf". Empty string for legacy rows. */
    val fileName: String = "",
) {
    val isImage: Boolean get() = type == "IMAGE"
}
