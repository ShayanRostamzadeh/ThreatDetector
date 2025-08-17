/*
ServerManager class implements the server-related functions
while afterwards there is an object made of this class ensuring
there is only going to be one instance of this class which
withstands configuration changes that eventually leads to UI and
server status being out of sync - this approach is needed since
the app might have a TCP server running in the background and the
corresponding UI is not following the server status and provides
the user with the opportunity to create another server resulting in
the application crash.
*/


package ShayanRostamzadeh.UniPassau.threatdetector

import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.appContext
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.tcpServerPort
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State


open class ServerManager() {
    private var pcapReceiver: PcapReceiver? = null
    private val _isRunning = mutableStateOf(false)
    val isRunning: State<Boolean> get() = _isRunning

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun startServer() {
        if (_isRunning.value) return  // Already running

        while (true){
            if (appContext != null)
                break
        }

        pcapReceiver = PcapReceiver(appContext!!, tcpServerPort).apply {
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
