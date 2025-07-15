package ShayanRostamzadeh.UniPassau.threatdetector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier


/*
Todo:
    - add a button for connection and disconnection; change its text correspondingly
    - change the background colour of the UI indicating the VPN status
    - FOR-NOW - print the captured packets' infos in logcat
 */


@Composable
fun MonitorScreen(){
    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ){
        Text(text = "Monitor screen")
    }
}