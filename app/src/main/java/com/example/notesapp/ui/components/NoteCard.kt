package com.example.notesapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.notesapp.data.local.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun NoteCard(note: Note) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { /* could navigate to edit note later */ },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ){
        Text(note.title)
        Text(note.content)


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = formatDate(note.createdAt),
                style = MaterialTheme.typography.bodySmall
            )
        }

    }

}


// Helper to format Date to time like "2:00 AM"
fun formatDate(date: Date): String {
    val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    return sdf.format(date)
}
