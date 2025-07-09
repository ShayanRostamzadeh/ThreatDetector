package ShayanRostamzadeh.UniPassau.threatdetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ShayanRostamzadeh.UniPassau.threatdetector.ui.theme.ThreatDetectorTheme
import android.R
import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.fragment.app.FragmentActivity

data class BottomNavBarItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unSelectedIcon: ImageVector,
    val hasNews: Boolean
)

val APP_NAME = "THREAT DETECTOR"

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            Surface (
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
                ){
                Main()
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main(){
    //todo: implement app-bar + Frame layout + bottom nav-bar

    val bottomNavBarItems = listOf<BottomNavBarItem>(
        BottomNavBarItem(
            title = "MONITOR",
            selectedIcon = Icons.Filled.Lock,
            unSelectedIcon = Icons.Outlined.Lock,
            hasNews = false
        ),
        BottomNavBarItem(
            title = "LOGS",
            selectedIcon = Icons.Filled.Menu,
            unSelectedIcon = Icons.Outlined.Menu,
            hasNews = false
        )
    )

    var selectedNavBarItemIndex by rememberSaveable {
        mutableStateOf(0)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(APP_NAME)
                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.surface,
//                    titleContentColor = MaterialTheme.colorScheme.onSurface
//                ),

                modifier = Modifier.shadow(elevation = 4.dp)
//                modifier = Modifier.background(color = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar {
                bottomNavBarItems.forEachIndexed{ index, item ->
                    NavigationBarItem(
                        selected = selectedNavBarItemIndex == index,
                        onClick = {
                            selectedNavBarItemIndex = index
//                            navController.navigate(item.title)
                        },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (item.hasNews) {
                                        Badge()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selectedNavBarItemIndex == index) item.selectedIcon else item.unSelectedIcon,
                                    contentDescription = item.title
                                )
                            }
                        },
                        label = {
                            Text(item.title)
                        }
                    )
                }
            }
        },
        content = { innerPadding ->
            // Provide content composable here, e.g.

            // TODO: add the fragment here

            Column(modifier = Modifier.padding(innerPadding)) {
                Text("Hello from ${bottomNavBarItems[selectedNavBarItemIndex].title}")
            }
        }
    )

    HomeFragment()
}



@Composable
fun ActivateVPN(){
    val context = LocalContext.current
    Button(onClick = {
        Toast.makeText(context, "Button has been clicked", Toast.LENGTH_LONG).show()
    }) {
        Text("Activate VPN")
    }
}
