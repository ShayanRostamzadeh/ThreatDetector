package ShayanRostamzadeh.UniPassau.threatdetector

import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.net.InetAddress
import java.net.Socket

data class CidrBlock(val baseAddress: InetAddress, val prefixLength: Int)


class AppVpnService (): VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var running = false
    private val vpnScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
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
            .addDnsServer("8.8.8.8")
            .addRoute("0.0.0.0", 0)
            .setBlocking(true)

        try {
            //Exclude your app from VPN
            builder.addDisallowedApplication(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("AppVpnService", "Package name not found", e)
        }

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


    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private suspend fun parsePacket(buffer: ByteBuffer) {
        buffer.order(ByteOrder.BIG_ENDIAN)

        val version = (buffer.get(0).toInt() shr 4) and 0xF
        if (version == 4 && buffer.limit() >= 20) {
            val destIp = "${buffer.get(16).toInt() and 0xFF}." +
                    "${buffer.get(17).toInt() and 0xFF}." +
                    "${buffer.get(18).toInt() and 0xFF}." +
                    "${buffer.get(19).toInt() and 0xFF}"

//            val destPort = ((buffer.get(22).toInt() and 0xFF) shl 8) or (buffer.get(23).toInt() and 0xFF)
//
//
//            //tunneling the internet packets back and forth to/from the internet
//            val socket = Socket(destIp, destPort)
//            val output = socket.getOutputStream()
//            output.write(payloadBytes) // Extract this from the buffer
//            output.flush()


//            val byteArray = ByteArray(buffer.remaining())
//            buffer.get(byteArray)
//            Log.d("AppVpnService", byteArray.joinToString(" ") { String.format("%02X", it) })
            Log.i("AppVpnService", "Intercepted packet to IP: $destIp")
//            Log.d("AppVpnService", String(byteArray))

//            val malicious = isIpMalicious(destIp)
//            if (malicious) {
//                Log.w("AppVpnService", "MALICIOUS packet to $destIp")
//            }

            Log.i("AppVpnService", "IP status check for maliciousness: $destIp")

        }
    }//parsePacket





}
