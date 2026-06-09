package com.vivuvi.lightnotes.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Date,
    val updatedAt: Long = 0L,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val isPinned: Boolean = false,
    val fontSize: String = "MEDIUM",
    val backgroundColor: Long = 0L,
    val textColor: Long = 0L,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val headingLevel: Int = 0,
    val listType: String = "NONE",
    val orderIndex: Int = 0,
    val isLocked: Boolean = false,
    val pin: String? = null,
)
