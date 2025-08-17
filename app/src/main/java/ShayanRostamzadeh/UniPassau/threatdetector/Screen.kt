//this class keeps the route for each of the
//composable fragments (MonitorScreen, LogsScreen)

package ShayanRostamzadeh.UniPassau.threatdetector

sealed class Screen (val route: String){
    object Monitor: Screen(route = "Monitor")
    object Logs: Screen(route = "Logs")
}