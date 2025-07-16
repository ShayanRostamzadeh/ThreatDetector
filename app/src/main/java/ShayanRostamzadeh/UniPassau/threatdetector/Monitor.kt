package ShayanRostamzadeh.UniPassau.threatdetector

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.startActivityForResult


/*
Todo:
    - add a button for connection and disconnection; change its text correspondingly
    - change the background colour of the UI indicating the VPN status
    - FOR-NOW - print the captured packets' infos in logcat
 */

val vpnService = AppVpnService()

@Composable
fun MonitorScreen(context: Context){
//    Box(
//        modifier = Modifier.fillMaxSize()
//            .background(MaterialTheme.colorScheme.primary),
//        contentAlignment = Alignment.Center,
//    ){
//        Text(text = "Monitor screen")
//    }
    val vpnConnected by rememberSaveable {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.inversePrimary),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // text to show the status of the VPN connection
        Text(
            text = if(vpnConnected) "Status: Connected" else "Status: Disconnected"
        )
        Spacer(modifier = Modifier.height(30.dp))

        // Button to take the action of connect/disconnect
        Button(
            onClick = {
                connectDisconnect(vpnConnected, context)
            }) {
            Text(
                text = if(vpnConnected) "Disconnect" else "Connect"
            )
        }
    }
}//MonitorScreen


fun connectDisconnect(connectionStatus: Boolean, context: Context){
    when(connectionStatus){
        true -> {
            //disconnect the vpn
            disconnectVPN()
        }
        false ->{
            //connect the vpn
            connectVPN(context)
        }
    }
}//connectDisconnect


fun connectVPN(context: Context){
//    val intent = VpnService.prepare(context)
//    if (intent != null) {
//        startActivityForResult(intent, 0) // must override onActivityResult
//    } else {
//        context.startService(Intent(context, AppVpnService::class.java))
//    }
}//connectVPN


fun disconnectVPN(){

}//disconnectVPN