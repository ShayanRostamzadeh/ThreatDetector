/*
here is the view model of the information being depicted in
MonitorScreen. this class separates the UI from the background
processes modifying the UI and also prevents reinitiation of
the TCP server on configuration changes (e.g. screen orientation change,
renavigation to the MonitorScreen, etc.)
*/


package ShayanRostamzadeh.UniPassau.threatdetector.ViewModels

import ShayanRostamzadeh.UniPassau.threatdetector.ServerStatusTracker
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel

class MonitorViewModel : ViewModel() {
    val isServerRunning: State<Boolean> = ServerStatusTracker.isRunning

    fun startServer() = ServerStatusTracker.startServer()
    fun stopServer() = ServerStatusTracker.stopServer()
}
