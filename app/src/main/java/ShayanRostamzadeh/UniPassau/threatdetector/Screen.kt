package ShayanRostamzadeh.UniPassau.threatdetector

sealed class Screen (val route: String){
    object Monitor: Screen(route = "Monitor")
    object Logs: Screen(route = "Logs")
}