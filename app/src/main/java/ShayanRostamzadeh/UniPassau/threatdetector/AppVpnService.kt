package ShayanRostamzadeh.UniPassau.threatdetector

import ShayanRostamzadeh.UniPassau.threatdetector.InfoBase.FAANG_IP_Addrs
import ShayanRostamzadeh.UniPassau.threatdetector.InfoBase.ipStatusMapCache
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
    }//onStartCommand

    private fun parsePacket(buffer: ByteBuffer) {
        buffer.order(ByteOrder.BIG_ENDIAN)

        val version = (buffer.get(0).toInt() shr 4) and 0xF
        if (version == 4 && buffer.limit() >= 20) {
            val destIp = "${buffer.get(16).toInt() and 0xFF}." +
                    "${buffer.get(17).toInt() and 0xFF}." +
                    "${buffer.get(18).toInt() and 0xFF}." +
                    "${buffer.get(19).toInt() and 0xFF}"

//            val byteArray = ByteArray(buffer.remaining())
//            buffer.get(byteArray)
//            Log.d("AppVpnService", byteArray.joinToString(" ") { String.format("%02X", it) })
            Log.i("AppVpnService", "Intercepted packet to IP: $destIp")
//            Log.d("AppVpnService", String(byteArray))

            isIpMalicious(destIp)
        }
    }//parsePacket

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


    fun isIpMalicious(IpAddr: String): Boolean{
        /*
        todo:
            - check the cache map - if is a FAANG IP
            - if empty, send the request to AbuseIPDB
            - if not empty:
                - if tagged malicious, return true
                - else if tagged non-malicious, return false
         */
        val isMalicious = false

        //going through the FAANG IP addresses
        for (ip in FAANG_IP_Addrs){
            if (IpAddr == ip)
                return false
        }

        if(ipStatusMapCache.isEmpty()){
            // add the IP to the list

            // send the IP to be checked to AbuseIPDB
        }
        else {
            for (ip in ipStatusMapCache.keys){

            }
        }

        return false
    }
}
