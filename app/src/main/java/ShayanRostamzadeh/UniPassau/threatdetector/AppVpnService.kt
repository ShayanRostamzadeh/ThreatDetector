package ShayanRostamzadeh.UniPassau.threatdetector

import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.abuseIpDB_Api_Request_No
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.filoMap
import android.content.Context
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


//    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
//    private suspend fun parsePacket(buffer: ByteBuffer) {
//        buffer.order(ByteOrder.BIG_ENDIAN)
//
//        val version = (buffer.get(0).toInt() shr 4) and 0xF
//        if (version != 4 || buffer.limit() < 20) return
//
//        // Parse IPv4 header fields
//        val ihl = (buffer.get(0).toInt() and 0x0F) * 4  // IP header length in bytes
//        val protocol = buffer.get(9).toInt() and 0xFF
//
//        // Destination IP address
//        val destIp = "${buffer.get(16).toInt() and 0xFF}." +
//                "${buffer.get(17).toInt() and 0xFF}." +
//                "${buffer.get(18).toInt() and 0xFF}." +
//                "${buffer.get(19).toInt() and 0xFF}"
//
//        // Only handle TCP (protocol 6) or UDP (protocol 17)
//        if (protocol != 6 && protocol != 17) return
//
//        // Read dest port
//        val destPort = ((buffer.get(ihl + 2).toInt() and 0xFF) shl 8) or (buffer.get(ihl + 3).toInt() and 0xFF)
//
//        // Extract payload (after IP + TCP/UDP header)
//        val headerLength = if (protocol == 6) {
//            // TCP header length = 4 bits in offset field (byte 12 in TCP header)
//            val tcpHeaderOffset = ((buffer.get(ihl + 12).toInt() shr 4) and 0xF) * 4
//            ihl + tcpHeaderOffset
//        } else {
//            // UDP header length fixed at 8 bytes
//            ihl + 8
//        }
//
//        val payloadLength = buffer.limit() - headerLength
//        val payloadBytes = ByteArray(payloadLength)
//        buffer.position(headerLength)
//        buffer.get(payloadBytes, 0, payloadLength)
//
//        Log.i("AppVpnService", "Intercepted packet to IP: $destIp:$destPort, protocol: $protocol, payloadLength=$payloadLength")
//
//        val malicious = isIpMalicious(destIp)
//        if (malicious) {
//            Log.w("AppVpnService", "MALICIOUS packet to $destIp")
//        }
//
//        // Forward payload to destination and get response
//        val responseBytes = forwardPacket(destIp, destPort, payloadBytes)
//
//        // Write response back to TUN interface (vpnInterface)
//        responseBytes?.let {
//            vpnInterface?.fileDescriptor?.let { fd ->
//                try {
//                    val output = FileOutputStream(fd)
//                    output.write(it)
//                    output.flush()
//                    // No close here; AutoCloseOutputStream closes on stream close, so keep stream open or manage externally
//                } catch (e: Exception) {
//                    Log.e("AppVpnService", "Error writing back to VPN interface: ${e.message}")
//                }
//            }
//        }
//    }

//    // Synchronous forwarding to dest IP:port, returning response bytes (blocking)
//    private fun forwardPacket(destIp: String, destPort: Int, payload: ByteArray): ByteArray? {
//        return try {
//            Socket(destIp, destPort).use { socket ->
//                val out = socket.getOutputStream()
//                val input = socket.getInputStream()
//
//                // Send the payload
//                out.write(payload)
//                out.flush()
//
//                // Read response (max 32KB)
//                val responseBuffer = ByteArray(32768)
//                val readBytes = input.read(responseBuffer)
//
//                if (readBytes > 0) {
//                    responseBuffer.copyOf(readBytes)
//                } else {
//                    null
//                }
//            }
//        } catch (e: Exception) {
//            Log.e("AppVpnService", "Forwarding error: ${e.message}")
//            null
//        }
//    }


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

            val malicious = isIpMalicious(destIp)
            if (malicious) {
                Log.w("AppVpnService", "MALICIOUS packet to $destIp")
            }

            Log.i("AppVpnService", "IP status check for maliciousness: $destIp")

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


//    fun forwardPacket(packet: ByteArray): ByteArray? {
//        val destIp = extractDestinationIp(packet)
//        val destPort = extractDestinationPort(packet)
//
//        val socket = Socket(destIp, destPort)
//        socket.getOutputStream().write(extractPayload(packet))
//        socket.soTimeout = 5000
//
//        val response = ByteArray(1024)
//        val readBytes = socket.getInputStream().read(response)
//        socket.close()
//
//        return if (readBytes > 0) response.copyOf(readBytes) else null
//    }
//

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    suspend fun isIpMalicious(IpAddr: String): Boolean {
        // 1. Check the cache
        filoMap.map[IpAddr]?.let { return it }

        // 2. Skip known FAANG IPs
        if (isFaangIp(IpAddr)) return false

        return try {
            val response = createAbuseClient().checkIp(IpAddr)
            if (response.isSuccessful) {
                abuseIpDB_Api_Request_No++
                val score = response.body()?.data?.abuseConfidenceScore
                val isMalicious = (score != null && score > 50)

                filoMap.put(IpAddr, isMalicious)

                if (isMalicious) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "⚠️ Malicious IP: $IpAddr", Toast.LENGTH_LONG).show()
                    }
                }

                Log.d("AppVpnService", "Checked IP $IpAddr: score=$score")
                isMalicious
            } else {
                Log.e("AppVpnService", "AbuseIPDB response not successful")
                false
            }
        } catch (e: Exception) {
            Log.e("AppVpnService", "AbuseIPDB error: ${e.message}")
            false
        }
    }//isIpMalicious


    fun parseCidr(cidr: String): CidrBlock {
        val (ip, prefix) = cidr.split("/")
        return CidrBlock(InetAddress.getByName(ip), prefix.toInt())
    }//parseCidr

    fun isIpInRange(ip: String, cidr: String): Boolean {
        val ipAddress = InetAddress.getByName(ip).address
        val cidrBlock = parseCidr(cidr)
        val network = cidrBlock.baseAddress.address

        val prefix = cidrBlock.prefixLength
        val mask = -1 shl (32 - prefix)
        val ipInt = ByteBuffer.wrap(ipAddress).int
        val netInt = ByteBuffer.wrap(network).int

        return (ipInt and mask) == (netInt and mask)
    }//isIpInRange

    fun isFaangIp(ip: String): Boolean {
        val faangCidrs = listOf(
            // Facebook
            "31.13.24.0/21", "66.220.144.0/20", "69.63.176.0/20", "69.171.224.0/19",
            "74.119.76.0/22", "103.4.96.0/22", "129.134.0.0/16", "157.240.0.0/16",
            "173.252.64.0/18", "179.60.192.0/22", "185.60.216.0/22",

            // Apple
            "17.0.0.0/8",

            // Amazon
            "3.0.0.0/8", "13.52.0.0/16", "13.224.0.0/14", "18.0.0.0/8",
            "52.0.0.0/11", "54.0.0.0/10", "205.251.192.0/19",

            // Netflix
            "52.88.0.0/15", "52.26.0.0/16", "34.210.0.0/15", "35.160.0.0/13",

            // Google
            "8.8.8.0/24", "8.34.208.0/20", "8.35.192.0/20", "23.236.48.0/20",
            "34.64.0.0/10", "35.192.0.0/12", "66.102.0.0/20", "72.14.192.0/18",
            "74.125.0.0/16", "108.177.8.0/21", "172.217.0.0/16", "173.194.0.0/16",
            "192.178.0.0/15", "199.36.154.0/23", "216.58.192.0/19"
        )

        return faangCidrs.any { cidr -> isIpInRange(ip, cidr) }
    }//isFaangIp

}
