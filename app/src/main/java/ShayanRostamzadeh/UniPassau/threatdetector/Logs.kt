/*
this code depicts a list of the apps and their IP addresses alongside
the score received from AbuseIPDB in real time
*/


package ShayanRostamzadeh.UniPassau.threatdetector

import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.appContext
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.RetrievedAppsDataManager.filoMap
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.RetrievedAppsDataManager.getAppIPMap
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.RetrievedAppsDataManager.getAppIconMap
import android.widget.Space
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap


data class LogsListItems(
    val icon: ImageBitmap?,   // Changed to ImageBitmap for easy display
    val IPAddress: String,
    val IPScore: Int,
    val appName: String
)

@Composable
fun LogsScreen(modifier: Modifier = Modifier) {
//    while (true){
//        if(appContext != null)
//            break
//    }
//    val context = appContext

    val appToIP = getAppIPMap()
    val appToIcon = getAppIconMap()


    val logsListItems = remember(appToIP, appToIcon, filoMap) {
        appToIP.mapNotNull { (appName, ip) ->
            val score = filoMap[ip] ?: 0
            val drawable = appToIcon[appName]

            // converting drawables to image bitmaps which is easier for compose to draw
            val imageBitmap: ImageBitmap? = drawable?.toBitmap()?.asImageBitmap()

            LogsListItems(
                icon = imageBitmap,
                IPAddress = ip,
                IPScore = score,
                appName = appName
            )
        }
    }

    //lazy column used to create an scrollable list of the objects
    //which are card views (incliding icon, ip address, ip score, app name)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(logsListItems) { item ->
            Card(
                modifier = Modifier, // your modifier here, no need to set background manually
                shape = RoundedCornerShape(12.dp),  // set corner radius here
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
            ){
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                    ) {
                        if (item.icon != null) {
                            Image(
                                bitmap = item.icon,
                                contentDescription = "${item.appName} icon",
                                modifier = Modifier
                                    .fillMaxSize()      // fills the box size (40.dp)
                                    .align(Alignment.Center),
                                contentScale = ContentScale.Fit  // or ContentScale.Crop depending on style
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
}