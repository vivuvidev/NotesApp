package com.example.notesapp.ui.components

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.example.notesapp.R

@Composable
fun FloatingActionButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = { onClick() },
    ) {
        Icon(
            painter = painterResource(R.drawable.add),
           contentDescription =  "Floating action button."
        )
    }
}