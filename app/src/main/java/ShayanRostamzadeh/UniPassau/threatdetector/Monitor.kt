/*
this fragment is responsible to depict the status of the
TCP server and also provides a button to control the activation
and deactivation of the corresponding server alongside a background
colour change to exhibit the TCP server status
*/


package ShayanRostamzadeh.UniPassau.threatdetector

import ShayanRostamzadeh.UniPassau.threatdetector.ViewModels.MonitorViewModel
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun MonitorScreen(context: Context, pcapServerPort: Int, monitorViewModel: MonitorViewModel = viewModel()) {

    //isRunning keeps track of the TCP server status and is
    //the underlying value to set the UI accordingly
    val isRunning by monitorViewModel.isServerRunning

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isRunning) colorResource(R.color.light_green)
                else MaterialTheme.colorScheme.primaryContainer
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isRunning) "Status: Listening on port $pcapServerPort"
            else "Status: Not listening",
            fontSize = 22.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
            onClick = {
                if (isRunning) {
                    monitorViewModel.stopServer()
                    Log.i("PCAP_SERVER", "Stopped TCP server")
                } else {
                    monitorViewModel.startServer()
                    Log.i("PCAP_SERVER", "Started TCP server")
                }
            }
        ) {
            Text(text = if (isRunning) "Stop Server" else "Start Server")
        }
    }
}



