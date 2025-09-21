/*
this code depicts a list of the apps and their IP addresses alongside
the score received from AbuseIPDB in real time
*/


package ShayanRostamzadeh.UniPassau.threatdetector

import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.abuseCategories
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.abuseIpDbMaliciousScore
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.RetrievedAppsDataManager.fiFoMap
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.RetrievedAppsDataManager.getAppIPMap
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.RetrievedAppsDataManager.getAppIconMap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class LogsListItems(
    val icon: ImageBitmap?,   // Changed to ImageBitmap for easy display
    val IPAddress: String,
    val IPScore: Int = 0,
    val appName: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogsScreen(modifier: Modifier = Modifier) {
    var logsListItems by remember { mutableStateOf(emptyList<LogsListItems>()) }

    // Keep a map of highest scores so far for each of the items shown on the Logs Screen
    var scoreCache by remember { mutableStateOf(mutableMapOf<String, Int>()) }

    // State for popup dialog
    var selectedIpData by remember { mutableStateOf<AbuseIpData?>(null) }
    var showDialog by remember { mutableStateOf(false) }


    // Auto-refresh every 2 seconds
    LaunchedEffect(Unit) {
        while (true) {
            val appToIP = getAppIPMap()
            val appToIcon = getAppIconMap()

            logsListItems = appToIP.mapNotNull { (appName, ip) ->
                val data: AbuseIpData? = fiFoMap[ip]
                val newScore = data?.abuseConfidenceScore ?: 0

                // Compare with previous score - if the previous score is less, then update the UI
                // to depict the new score
                val prevScore = scoreCache[ip] ?: 0
                val finalScore = if (newScore > prevScore) newScore else prevScore

                // Update cache
                scoreCache[ip] = finalScore

                val drawable = appToIcon[appName]
                val imageBitmap: ImageBitmap? = drawable?.toBitmap()?.asImageBitmap()

                LogsListItems(
                    icon = imageBitmap,
                    IPAddress = ip,
                    IPScore = finalScore,
                    appName = appName
                )
            }

            delay(2000) // 2 seconds
        }
    }

    //lazy column used to create a scrollable list
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(logsListItems) { item ->

            val bgColor = if (item.IPScore > abuseIpDbMaliciousScore) {
                Color(0xFFFFCDD2) // light red
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                modifier = Modifier.combinedClickable(
                    onClick = { /* todo: modify if normal click is needed */ },
                    onLongClick = {
                        // Show dialog with IP details
                        selectedIpData = fiFoMap[item.IPAddress]
                        showDialog = true
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(50.dp)
                    ) {
                        item.icon?.let {
                            Image(
                                bitmap = it,
                                contentDescription = "${item.appName} icon",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Column {
                        Text(text = "App: ${item.appName}")
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(text = "IP: ${item.IPAddress}")
                    }

                    Spacer(modifier = Modifier.size(20.dp))

                    Text(
                        modifier = Modifier.padding(15.dp),
                        text = "${item.IPScore}",
                        fontSize = 20.sp,
                        fontWeight = Bold
                    )
                }
            }
        }
    }
    // Popup Dialog for long click
    if (showDialog && selectedIpData != null) {
        val ipData = selectedIpData!!
        val reportItems = ipData.reports.map { report ->
            val date = report.reportedAt
            val comment = report.comment ?: "No comment"
            val categories = report.categories.joinToString { id -> abuseCategories[id] ?: "Unknown($id)" }
            "Reported at: $date\nCountry: ${ipData.countryCode}\nCategories: $categories\nComment: $comment"
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("IP Details: ${ipData.ipAddress}") },
            text = {
                LazyColumn {
                    items(reportItems) { item ->
                        Text(item)
                        Text("\n") // spacing between reports
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Close",
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        )
    }
}
