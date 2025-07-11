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
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.text.Layout
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

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

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main(){
    //todo: implement app-bar + bottom nav-bar

    val navController = rememberNavController()


    val bottomNavBarItems = listOf<BottomNavBarItem>(
        BottomNavBarItem(
            title = "Monitor",
            selectedIcon = Icons.Filled.Lock,
            unSelectedIcon = Icons.Outlined.Lock,
            hasNews = false
        ),
        BottomNavBarItem(
            title = "Logs",
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
                            navController.navigate(item.title)
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

            NavHost(navController = navController,
                startDestination = Screen.Monitor.route,
                builder = {
                    composable (
                        route = Screen.Monitor.route
                    ){
                        Log.w("MainActivity", "should be in MonitorScreen")
                        MonitorScreen()
                    }
                    composable (
                        route = Screen.Logs.route
                    ){
                        Log.w("MainActivity", "should be in LogsScreen")
                        LogsScreen()
                    }
                })

//            Column(
//                verticalArrangement = Arrangement.Center,
//                horizontalAlignment = Alignment.CenterHorizontally,
//                modifier = Modifier
//                    .padding(innerPadding)
//                    .fillMaxSize()
////                    .background(color = Color.Cyan)
//            ) {
////                HomeFragment()
////                Text("Hello from ${bottomNavBarItems[selectedNavBarItemIndex].title}")
//            }
        },
    )
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
