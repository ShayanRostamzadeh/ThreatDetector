package ShayanRostamzadeh.UniPassau.threatdetector

import ShayanRostamzadeh.UniPassau.threatdetector.ViewModels.MonitorViewModel
import android.Manifest
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
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/*
Todo:
    - save api calls number to preferences
    - check whether a day has passed --> reset the number of api calls - save it to
        preferences
    - save the IP addresses and their status in preferences
    - retrieve the IP addresses and their status on application launch
 */


data class BottomNavBarItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unSelectedIcon: ImageVector,
    val hasNews: Boolean
)

val APP_NAME = "THREAT DETECTOR"
//val PERMISSION_REQUEST_CODE = 101

lateinit var checkAPICalls: CheckAPICalls

class MainActivity : FragmentActivity() {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
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

    val context = LocalContext.current
//    val lifCycleOwner = LocalLifecycleOwner.current
//    val activity = LocalActivity.current

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

    //Creating a thread to check the AbuseIPDB api calls to notify user
    //when 1000 free daily calls are exhausted
    checkAPICalls = CheckAPICalls(context)
    checkAPICalls.startRepeatingTask()

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

                        //todo: later receive the hard coded port below from the user
                        MonitorScreen(context, 1234)
                    }
                    composable (
                        route = Screen.Logs.route
                    ){
                        Log.w("MainActivity", "should be in LogsScreen")
                        LogsScreen()
                    }
                })

            Spacer (modifier = Modifier.height(16.dp))

        },
    )
}

