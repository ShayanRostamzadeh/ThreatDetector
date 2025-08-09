package ShayanRostamzadeh.UniPassau.threatdetector

import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.tcpServerPort
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State


open class ServerManager() {
    private var pcapReceiver: PcapReceiver? = null

    private val _isRunning = mutableStateOf(false)
    val isRunning: State<Boolean> get() = _isRunning

    fun startServer() {
        if (_isRunning.value) return  // Already running

        pcapReceiver = PcapReceiver(tcpServerPort).apply {
            startServer()
        }
        _isRunning.value = true
    }

    fun stopServer() {
        pcapReceiver?.stopServer()
        pcapReceiver = null
        _isRunning.value = false
    }
}

object ServerStatusTracker : ServerManager()
