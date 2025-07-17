package ShayanRostamzadeh.UniPassau.threatdetector

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AppVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var running = false
//    private var vpnThread: Thread? = null

    private val vpnScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent?.action == "STOP_VPN") {
            Log.d("AppVpnService", "Received STOP_VPN action")
            stopSelf() // <-- This will now trigger onDestroy()
            vpnInterface?.close()
            vpnInterface = null
            return START_NOT_STICKY
        }

        if (running) return START_STICKY
        running = true

        val builder = Builder()
        builder.setSession("AppVpnService")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .setBlocking(true)

        vpnInterface = builder.establish()

        vpnScope.launch {
            vpnInterface?.fileDescriptor?.let { fd ->
                val input = FileInputStream(fd)
                val channel = input.channel
                val buffer = ByteBuffer.allocate(32767)

                try {
                    while (running) {
                        buffer.clear()
                        val readBytes = channel.read(buffer)
                        if (readBytes > 0) {
                            buffer.flip()
                            parsePacket(buffer)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AppVpnService", "VPN read error: ${e.message}")
                } finally {
                    input.close()
                }
            }
        }

        return START_STICKY
    }


    override fun onDestroy() {
        Log.d("AppVpnService", "onDestroy() called")

        running = false
        vpnInterface?.close()

        vpnScope.cancel() // Cancels the coroutine used to read VPN packets

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e("AppVpnService", "Error closing VPN interface: ${e.message}")
        }

        vpnInterface = null

        stopForeground(true)
        stopSelf()
        super.onDestroy()
        Log.d("AppVpnService", "onDestroy() completed")
    }


//    override fun onDestroy() {
//        Log.d("AppVpnService", "onDestroy() is called")
//        running = false
//        if(vpnInterface != null){
//            try {
//                vpnInterface?.close()
//            }
//            catch (e: Exception){
//                Log.d("AppVpnService", "Error closing the " +
//                        "vpn connection: ${e.message}")
//            }
//        }
////        vpnThread?.interrupt()
////        vpnThread = null
//        vpnInterface?.close()
//        vpnInterface = null
//        stopForeground(true)
//        stopSelf()
//        super.onDestroy()
//    }

    private fun parsePacket(buffer: ByteBuffer) {
        buffer.order(ByteOrder.BIG_ENDIAN)

        val version = (buffer.get(0).toInt() shr 4) and 0xF
        if (version == 4 && buffer.limit() >= 20) {
            val destIp = "${buffer.get(16).toInt() and 0xFF}." +
                    "${buffer.get(17).toInt() and 0xFF}." +
                    "${buffer.get(18).toInt() and 0xFF}." +
                    "${buffer.get(19).toInt() and 0xFF}"

            Log.i("AppVpnService", "Intercepted packet to IP: $destIp")
        }
    }
}
