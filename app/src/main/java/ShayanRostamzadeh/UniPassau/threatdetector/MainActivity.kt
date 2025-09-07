/*
the main and the only activity in the app which hosts two fragments
which are composable functions that are navigable using a NavHost provided
as a bottom bar.
the app uses Material 3 for the UI and also follows the Scaffold implementation
which ensures the utilization of various components in their right position in
the page.
*/


package ShayanRostamzadeh.UniPassau.threatdetector

import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.abuseIpDbMaliciousScore
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.appContext
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.tcpServerPort
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/*
Todo:
    - is notification functionality working for IP addresses?
        - as of now only the user gets notified about the maliciousness
        by a toast :|
    - add a check to replace the ip address used by the app only if the score
        is higher than the score saved previously

    - should I even do the following?
        - save the IP addresses and their status in preferences
        - retrieve the IP addresses and their status on application launch
    the number of free API calls is large enough to cover same calls in case the
    app is closed and reopened again. furthermore, what is the criterion to keep
    an ip address saved in shared-preferences or in a DB and mark it as malicious
    for an extended amount of time??
 */


data class BottomNavBarItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unSelectedIcon: ImageVector,
    val hasNews: Boolean
)

val APP_NAME = "THREAT DETECTOR"
//val PERMISSION_REQUEST_CODE = 101


class MainActivity : FragmentActivity() {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //assigning the application context to a globally accessible variable
        //for other parts of the code, since there is only one activity used,
        //therefore one context exists - appContext is accessible through
        //GlobalDataStorage
        appContext = applicationContext

        setContent {
            //using material surface to build th underlying material UI
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

    // Access SharedPreferences if the user already changed the desired default values
    val sharedPref = LocalContext.current
        .getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

    // Load saved values or use null if not set
    val savedPort = sharedPref.getInt("tcpServerPort", -1).takeIf { it != -1 }
    val savedScore = sharedPref.getInt("abuseIpDbMaliciousScore", -1).takeIf { it != -1 }

    if (savedPort != null && savedScore != null){
        tcpServerPort = savedPort
        abuseIpDbMaliciousScore = savedScore
    }

    //todo: check the variable below iw working as expected
    lateinit var checkAPICalls: CheckAPICalls

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

    //saving the state of which appbar is selected
    var selectedNavBarItemIndex by rememberSaveable {
        mutableStateOf(0)
    }

    //Creating a thread to check the AbuseIPDB api calls to notify user
    //when 1000 free daily calls are exhausted
    checkAPICalls = CheckAPICalls()
    checkAPICalls.startRepeatingTask()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
//            TopAppBar(
//                title = {
//                    Text(APP_NAME)
//                },
////                colors = TopAppBarDefaults.topAppBarColors(
////                    containerColor = MaterialTheme.colorScheme.surface,
////                    titleContentColor = MaterialTheme.colorScheme.onSurface
////                ),
//
//                modifier = Modifier.shadow(elevation = 4.dp)
////                modifier = Modifier.background(color = MaterialTheme.colorScheme.surface)
//            )
            val context = LocalContext.current

            TopAppBar(
                title = {
                    Text(APP_NAME)
                },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(context, SettingsActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                modifier = Modifier.shadow(elevation = 4.dp)
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

            //NavHost responsible for enabling the navigation between different fragments
            NavHost(navController = navController,
                startDestination = Screen.Monitor.route,
                builder = {
                    composable (
                        route = Screen.Monitor.route
                    ){
                        Log.w("MainActivity", "should be in MonitorScreen")

                        //todo: later receive the hard coded port below from the user
                        MonitorScreen()
                    }
                    composable (
                        route = Screen.Logs.route
                    ){
                        Log.w("MainActivity", "should be in LogsScreen")
//                        LogsScreen(innerPadding)
                        LogsScreen(modifier = Modifier.padding(innerPadding))
                    }
                })

            Spacer (modifier = Modifier.height(16.dp))

        },
    )
}

