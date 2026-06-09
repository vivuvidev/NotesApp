package com.vivuvi.lightnotes.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteMediaDao {

    @Query("SELECT * FROM note_media WHERE noteId = :noteId")
    fun getByNoteId(noteId: Int): Flow<List<NoteMedia>>

    @Query("SELECT * FROM note_media WHERE noteId = :noteId")
    suspend fun getByNoteIdOnce(noteId: Int): List<NoteMedia>

    @Insert
    suspend fun insert(media: NoteMedia): Long

    @Query("DELETE FROM note_media WHERE id = :id")
    suspend fun deleteById(id: Int)
}
