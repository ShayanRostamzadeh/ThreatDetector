package ShayanRostamzadeh.UniPassau.threatdetector

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.net.InetAddress

data class CidrBlock(val baseAddress: InetAddress, val prefixLength: Int)

val filoMap = FiloMap<String, Boolean>(100)

class AppVpnService : VpnService() {
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

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
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

            val IPSecStatus = isIpMalicious(destIp)
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


    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun isIpMalicious(IpAddr: String): Boolean{


        // TODO: check whether the following rationale makes sense


        //checking whether the IP address is already cached
        for(ip in filoMap.map){
            if(ip.key == IpAddr && !ip.value)
                return false
            else if (ip.key == IpAddr && ip.value)
                return true
        }

        //checking whether the IP is belongs to FAANG
        if(isFaangIp(IpAddr)){
//            Log.i("AppVpnService", "IP: $IpAddr is FAANG")
            filoMap.put(IpAddr, false)
            return false
        }

        //sending the request to AbuseIPDB for IP check
        val api = createAbuseClient()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.checkIp(IpAddr)
                if (response.isSuccessful) {
                    val score = response.body()?.data?.abuseConfidenceScore
                    if (score != null && score > 50) {
                        Log.d("AppVpnService", "⚠️ Malicious IP detected: $score/100")
                        // Adding to the cache
                        filoMap.put(IpAddr, true)

                        //todo: Alert the user about the malicious IP address


                    }
                    Log.d("AppVpnService", "IP not malicious - score: $score/100")
                }
            } catch (e: Exception) {
                Log.e("AppVpnService", "Failed to check IP: ${e.message}")
            }
        }

        return true
    }//isIpMalicious


    fun parseCidr(cidr: String): CidrBlock {
        val (ip, prefix) = cidr.split("/")
        return CidrBlock(InetAddress.getByName(ip), prefix.toInt())
    }

    fun isIpInRange(ip: String, cidr: String): Boolean {
        val ipAddress = InetAddress.getByName(ip).address
        val cidrBlock = parseCidr(cidr)
        val network = cidrBlock.baseAddress.address

        val prefix = cidrBlock.prefixLength
        val mask = -1 shl (32 - prefix)
        val ipInt = ByteBuffer.wrap(ipAddress).int
        val netInt = ByteBuffer.wrap(network).int

        return (ipInt and mask) == (netInt and mask)
    }

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
