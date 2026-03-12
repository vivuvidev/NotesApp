package com.example.notesapp.ui.components
import android.widget.Button
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.notesapp.R
import androidx.compose.runtime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    showBackButton: Boolean=false,
    onBackClick:(()-> Unit)?=null,
    showMenu: Boolean=false,
    showSearch: Boolean=false,
    showKeep: Boolean=false,
    showCheck: Boolean=false

) {

    var expanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(title) },



        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = {onBackClick?.invoke()}) {
                    Icon(
                        painter = painterResource(R.drawable.chevron_left),
                        contentDescription = "back icon"
                    )
                }
            }
        },
        actions = {

            if (showKeep) {

                IconButton(onClick = { }) {
                    Icon(
                        painter = painterResource(R.drawable.keep),
                        contentDescription = "Menu"
                    )
                }
            }
            if (showCheck) {

                IconButton(onClick = { }) {
                    Icon(
                        painter = painterResource(R.drawable.check_small),
                        contentDescription = "Menu"
                    )
                }
            }

            if (showSearch) {

                IconButton(onClick = { }) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = "Menu"
                    )
                }
            }

if (showMenu){

    IconButton(onClick = { expanded = true }) {
        Icon(
            painter = painterResource( R.drawable.more_vert),
            contentDescription = "Menu"
        )
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {


        DropdownMenuItem(
            text = { Text("About")},
            onClick = {
                expanded = false
            }
        )
    }
}








        }
    )
}