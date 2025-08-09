package ShayanRostamzadeh.UniPassau.threatdetector.ViewModels

import ShayanRostamzadeh.UniPassau.threatdetector.ServerStatusTracker
import androidx.compose.runtime.State

import androidx.lifecycle.ViewModel

class MonitorViewModel : ViewModel() {
    val isServerRunning: State<Boolean> = ServerStatusTracker.isRunning

    fun startServer() = ServerStatusTracker.startServer()
    fun stopServer() = ServerStatusTracker.stopServer()
}
