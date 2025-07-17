package ShayanRostamzadeh.UniPassau.threatdetector

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.setValue
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

@Composable
fun MonitorScreen(context: Context){

    var vpnConnected by rememberSaveable {
        mutableStateOf(false)
    }

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            context.startService(Intent(context, AppVpnService::class.java))
            vpnConnected = true
        } else {
            vpnConnected = false
        }
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
        Button(onClick = {
            if (vpnConnected) {
                disconnectVPN(context)
                Log.w("AppVpnService", "In Monitor --- Should be disconnected now")
                vpnConnected = false
            } else {
                val intent = VpnService.prepare(context)
                if (intent != null) {
                    vpnLauncher.launch(intent)
                } else {
                    // Already has permission
                    context.startService(Intent(context, AppVpnService::class.java))
                    vpnConnected = true
                }
            }
        }) {
            Text(text = if (vpnConnected) "Disconnect" else "Connect")
        }
    }
}//MonitorScreen



fun disconnectVPN(context: Context) {
    val intent = Intent(context, AppVpnService::class.java)
    intent.action = "STOP_VPN"
    context.startService(intent)

    val stopIntent = Intent(context, AppVpnService::class.java)
    val status = context.stopService(stopIntent)

    Log.w("AppVpnService", "In Monitor --- In disconnect function")
    Log.w("AppVpnService", "In Monitor --- vpn status is $status")
}//disconnectVPN
