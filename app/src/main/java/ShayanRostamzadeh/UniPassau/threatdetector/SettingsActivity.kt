package ShayanRostamzadeh.UniPassau.threatdetector

import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.abuseIpDbMaliciousScore
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.appContext
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.tcpServerPort
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp

class SettingsActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    SettingsScreen()
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val activity = LocalActivity.current
    val context = LocalContext.current

    // Load default values from your GlobalDataStorage object
    var serverPort by remember { mutableStateOf(tcpServerPort.toString()) }
    var minIpScore by remember { mutableStateOf(abuseIpDbMaliciousScore.toString()) }
    var apiKey by remember { mutableStateOf(ApiKeyManager.getApiKey(context) ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = serverPort,
            onValueChange = { serverPort = it },
            label = { Text("PCAPDroid TCP Server Port") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = minIpScore,
            onValueChange = { minIpScore = it },
            label = { Text("Your Min Malicious Score") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("Your API Key") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val portValue = serverPort.toIntOrNull()
                val scoreValue = minIpScore.toIntOrNull()

                if (portValue != null && scoreValue != null) {
                    tcpServerPort = portValue
                    abuseIpDbMaliciousScore = scoreValue

                    // Save to SharedPreferences safely
                    val sharedPref = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putInt("tcpServerPort", tcpServerPort)
                        putInt("abuseIpDbMaliciousScore", abuseIpDbMaliciousScore)
                        apply()
                    }

                    ApiKeyManager.saveApiKey(context, apiKey)

                    Toast.makeText(context, "Settings Saved", Toast.LENGTH_SHORT).show()

                    // Leave the page on successful saving
                    activity?.finish()
                } else {
                    Toast.makeText(context, "Please enter valid numbers", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.wrapContentSize(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
        ) {
            Text("Save Settings")
        }
    }
}
