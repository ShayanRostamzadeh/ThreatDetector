/*
here is the class that establishes the TCP server for PCAPdroid
pcap file reception. this class runs a TCP server on a background thread
that is separate from the Main thread (UI thread).
Furthermore, it keeps a live connection to receive the pcap files in
real time. On reception, it updates the info base for the LogsScreen
to show the data
*/


package ShayanRostamzadeh.UniPassau.threatdetector

import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.abuseCategories
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.RetrievedAppsDataManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import java.net.InetAddress

class PcapReceiver(private val context: Context, private val pcapServerPort: Int) {

    private val serverPort = pcapServerPort
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile
    var isRunning = false
    private var serverSocket: ServerSocket? = null

    val abuseIPDBCheckIP = AbuseIPDBCheckIP()

    @OptIn(DelicateCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun startServer() {
        isRunning = true

        executor.execute {
            try {
                serverSocket = ServerSocket(serverPort)
                Log.d("PCAP_SERVER", "Server started on port $serverPort")

                while (isRunning) {
                    val clientSocket = serverSocket!!.accept()
                    Log.d("PCAP_SERVER", "Client connected: ${clientSocket.inetAddress.hostAddress}")
//                    handleClient(clientSocket)
                    GlobalScope.launch(Dispatchers.IO){
                        handleClient(clientSocket)
                    }
                }

            } catch (e: IOException) {
                if (isRunning) {
                    Log.e("PCAP_SERVER", "Server error: ${e.message}", e)
                } else {
                    Log.d("PCAP_SERVER", "Server stopped.")
                }
            } finally {
                try {
                    serverSocket?.close()
                } catch (e: IOException) {
                    Log.e("PCAP_SERVER", "Error closing server socket: ${e.message}", e)
                }
            }
        }
    }

    fun stopServer() {
        isRunning = false

        try {
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e("PCAP_SERVER", "Error closing server socket: ${e.message}", e)
        }
        executor.shutdownNow()
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private suspend fun handleClient(socket: Socket) {
        try {
            socket.use { sock ->
                val inputStream = sock.getInputStream()

                // Read the global PCAP header (24 bytes)
                val globalHeader = ByteArray(24)
                var totalRead = 0
                while (totalRead < 24) {
                    val read = inputStream.read(globalHeader, totalRead, 24 - totalRead)
                    if (read == -1) throw IOException("Stream closed unexpectedly while reading global header")
                    totalRead += read
                }
                Log.d("PCAP_SERVER", "Global header received")

                val packetHeader = ByteArray(16)

                while (true) {
                    // Read packet header (16 bytes)
                    totalRead = 0
                    while (totalRead < 16) {
                        val read = inputStream.read(packetHeader, totalRead, 16 - totalRead)
                        if (read == -1) {
                            Log.d("PCAP_SERVER", "Stream closed by client")
                            return // End of stream, exit
                        }
                        totalRead += read
                    }

                    // Parse inclLen from packet header (bytes 8-11), little endian
                    val inclLen = ByteBuffer.wrap(packetHeader, 8, 4).order(ByteOrder.LITTLE_ENDIAN).int

                    // Sanity check inclLen
                    if (inclLen <= 0 || inclLen > 65535) {
                        Log.e("PCAP_SERVER", "Invalid packet length: $inclLen")
                        return
                    }

                    // Read the packet data
                    val packetData = ByteArray(inclLen)
//                    Log.e("PCAP_SERVER", "Entire Packet Data is: $packetData")
                    totalRead = 0
                    while (totalRead < inclLen) {
                        val read = inputStream.read(packetData, totalRead, inclLen - totalRead)
                        if (read == -1) {
                            Log.e("PCAP_SERVER", "Stream closed unexpectedly while reading packet data")
                            return
                        }
                        totalRead += read
                    }

                    // Parse and log destination IPs from this packet
                    parseAndLogDestinationIP(packetData)
                }
            }
        } catch (e: Exception) {
            Log.e("PCAP_SERVER", "Client handler error: ${e.message}", e)
        }
    }

    // TODO: include the screenshot of the following dumped packet inside the thesis
    //  also remove the PcapParser kotlin file - no use anymore, the file is parsed and analyzed here
//    private fun dumpPacket(data: ByteArray) {
//        val sb = StringBuilder()
//        val bytesPerLine = 16
//        for (i in data.indices step bytesPerLine) {
//            // Offset in hex
//            sb.append(String.format("%04X: ", i))
//
//            // Hex bytes
//            for (j in 0 until bytesPerLine) {
//                if (i + j < data.size) {
//                    sb.append(String.format("%02X ", data[i + j]))
//                } else {
//                    sb.append("   ")
//                }
//            }
//
//            sb.append("  ")
//
//            // ASCII chars or dot for non-printable
//            for (j in 0 until bytesPerLine) {
//                if (i + j < data.size) {
//                    val b = data[i + j]
//                    val c = if (b in 32..126) b.toInt().toChar() else '.'
//                    sb.append(c)
//                }
//            }
//
//            sb.append("\n")
//        }
//        Log.d("PCAP_PARSER", "Full Packet Dump:\n$sb")
//    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private suspend fun parseAndLogDestinationIP(packetData: ByteArray) {

        var retrievedApplicationName = ""
        var retrievedAppIcon: Drawable? = null
        var retrievedIpAddress = ""

        try {
            // Full packet dump for analysis
//            dumpPacket(packetData)

            val trailerMagic = 0x01072021
            val minTrailerLength = 10

            if (packetData.size > minTrailerLength) {
                val scanLimit = minOf(packetData.size, 40)
                var trailerStartIndex = -1

                // Find trailer magic from near end of packet
                for (i in packetData.size - scanLimit until packetData.size - 4) {
                    val magic =
                        ((packetData[i].toInt() and 0xFF) shl 24) or
                                ((packetData[i + 1].toInt() and 0xFF) shl 16) or
                                ((packetData[i + 2].toInt() and 0xFF) shl 8) or
                                (packetData[i + 3].toInt() and 0xFF)
                    if (magic == trailerMagic) {
                        trailerStartIndex = i
                        break
                    }
                }

                if (trailerStartIndex != -1) {
                    val buffer = packetData

                    // Read UID (4 bytes after trailer magic)
                    val uid = ((buffer[trailerStartIndex + 4].toInt() and 0xFF) shl 24) or
                            ((buffer[trailerStartIndex + 5].toInt() and 0xFF) shl 16) or
                            ((buffer[trailerStartIndex + 6].toInt() and 0xFF) shl 8) or
                            (buffer[trailerStartIndex + 7].toInt() and 0xFF)

                    // Function to read null-terminated ASCII string starting at offset
                    fun readNullTerminatedString(start: Int): Pair<String, Int> {
                        val sb = StringBuilder()
                        var pos = start
                        while (pos < buffer.size && buffer[pos] != 0.toByte()) {
                            sb.append(buffer[pos].toChar())
                            pos++
                        }
                        return Pair(sb.toString(), pos + 1) // +1 to skip null byte
                    }

                    // Read package name starting right after UID (offset +8)
                    val (retrievedAppName, afterPackageIndex) = readNullTerminatedString(trailerStartIndex + 8)

                    // Read app name starting right after package name string
                    val (appName, _) = readNullTerminatedString(afterPackageIndex)

                    Log.d("PCAP_PARSER", "Trailer found at index: $trailerStartIndex")
                    Log.d("PCAP_PARSER", "Packet size: ${packetData.size}")
                    Log.d("PCAP_PARSER", "UID: $uid")
                    Log.d("PCAP_PARSER", "Retrieved App Name: $retrievedAppName")

                    val iconDrawable = getAppIconFromUid(context, uid)
                    Log.w("PCAP_PARSER", "app icon: $iconDrawable")

                    retrievedAppIcon = iconDrawable
                    retrievedApplicationName = retrievedAppName

                } else {
                    Log.d("PCAP_PARSER", "No PCAPdroid trailer magic found in packet")
                }
            }

            // --- START IP DESTINATION EXTRACTION ---

            val ethHeaderLen = 14
            if (packetData.size < ethHeaderLen + 20) {
                Log.d("PCAP_PARSER", "Packet too short for Ethernet + IP header")
                return
            }

            // Ethernet type is bytes 12-13
            val ethType = ((packetData[12].toInt() and 0xFF) shl 8) or (packetData[13].toInt() and 0xFF)

            when (ethType) {
                0x0800 -> { // IPv4
                    val ipHeaderStart = ethHeaderLen

                    if (packetData.size < ipHeaderStart + 20) {
                        Log.d("PCAP_PARSER", "Packet too short for IPv4 header")
                        return
                    }

                    val destIpBytes = packetData.copyOfRange(ipHeaderStart + 16, ipHeaderStart + 20)
                    val destIp = destIpBytes.joinToString(separator = ".") { (it.toInt() and 0xFF).toString() }

                    if(isPrivateIpV4(destIp)) return

                    Log.d("PCAP_PARSER", "IPv4 Destination IP: $destIp")

                    /*
                    Populating the RetrievedAppsDataManager object with the
                    retrieved information from pcap file and the icon which matches
                    the application UID
                     */
                    retrievedIpAddress = destIp

                    if(!abuseIPDBCheckIP.isFaangIp(destIp)){
                        RetrievedAppsDataManager.put(retrievedApplicationName,
                            retrievedIpAddress, retrievedAppIcon)
                    }

//                    abuseIPDBCheckIP.getIpScore(destIp)
                    val ipData = abuseIPDBCheckIP.getIpData(destIp)
                    ipData?.let {
                        Log.d("PCAP_PARSER", "IP: ${it.ipAddress}")
                        Log.d("PCAP_PARSER", "Score: ${it.abuseConfidenceScore}")
                        Log.d("PCAP_PARSER", "Domain: ${it.domain}")
                        Log.d("PCAP_PARSER", "Total reports: ${it.totalReports}")
//                        Log.d("PCAP_PARSER", "Reports: ${it.reports}")
                        Log.d("PCAP_PARSER", "Country Code: ${it.countryCode}")
                        ipData.reports.forEach { report ->
                            val date = report.reportedAt
                            val comment = report.comment ?: "No comment"
                            val categories = report.categories.joinToString { id ->
                                abuseCategories[id] ?: "Unknown($id)"
                            }

                            Log.d("PCAP_PARSER", """
                                Reported at: $date
                                Categories : $categories
                                Comment    : $comment
                            """.trimIndent())
                        }
                    }
                    ipData?.abuseConfidenceScore

                }

                0x86DD -> { // IPv6
                    val ipHeaderStart = ethHeaderLen

                    if (packetData.size < ipHeaderStart + 40) {
                        Log.d("PCAP_PARSER", "Packet too short for IPv6 header")
                        return
                    }

                    val destIpBytes = packetData.copyOfRange(ipHeaderStart + 24, ipHeaderStart + 40)
                    val destIp = destIpBytes.toIPv6String()

                    if(isPrivateIpV6(destIp)) return

                    Log.d("PCAP_PARSER", "IPv6 Destination IP: $destIp")

                    /*
                    Populating the RetrievedAppsDataManager object with the
                    retrieved information from pcap file and the icon which matches
                    the application UID
                     */
                    retrievedIpAddress = destIp

                    if(!abuseIPDBCheckIP.isFaangIp(destIp)){
                        RetrievedAppsDataManager.put(retrievedApplicationName,
                            retrievedIpAddress, retrievedAppIcon)
                    }

//                    abuseIPDBCheckIP.getIpScore(destIp)

                    val ipData = abuseIPDBCheckIP.getIpData(destIp)
                    ipData?.let {
                        Log.d("PCAP_PARSER", "IP: ${it.ipAddress}")
                        Log.d("PCAP_PARSER", "Score: ${it.abuseConfidenceScore}")
                        Log.d("PCAP_PARSER", "Domain: ${it.domain}")
                        Log.d("PCAP_PARSER", "Total reports: ${it.totalReports}")
                    }
                    ipData?.abuseConfidenceScore
                }

                else -> {
                    Log.d("PCAP_PARSER", "Unsupported Ethernet type: 0x${ethType.toString(16)}")
                }

            }

            // --- END IP DESTINATION EXTRACTION ---

        } catch (e: Exception) {
            Log.e("PCAP_PARSER", "Error parsing packet data: ${e.message}", e)
        }
    }

    // Helper extension to convert 16 bytes to IPv6 string
    private fun ByteArray.toIPv6String(): String {
        if (this.size != 16) return ""
        val sb = StringBuilder()
        for (i in 0 until 16 step 2) {
            val segment = ((this[i].toInt() and 0xFF) shl 8) or (this[i + 1].toInt() and 0xFF)
            sb.append(Integer.toHexString(segment))
            if (i < 14) sb.append(":")
        }
        return sb.toString()
    }

    fun getAppIconFromUid(context: Context, uid: Int): Drawable? {
        val pm = context.packageManager
        val packages = pm.getPackagesForUid(uid)
        if (!packages.isNullOrEmpty()) {
            return pm.getApplicationIcon(packages[0])
        }
        return null
    }


    //checking whether the IP address is a local address to skip
    //IPv4
    fun isPrivateIpV4(ip: String): Boolean {
        return try {
            val address = InetAddress.getByName(ip)
            val bytes = address.address

            when {
                // 10.0.0.0 – 10.255.255.255
                (bytes[0].toInt() and 0xFF) == 10 -> true

                // 172.16.0.0 – 172.31.255.255
                (bytes[0].toInt() and 0xFF) == 172 &&
                        (bytes[1].toInt() and 0xFF) in 16..31 -> true

                // 192.168.0.0 – 192.168.255.255
                (bytes[0].toInt() and 0xFF) == 192 &&
                        (bytes[1].toInt() and 0xFF) == 168 -> true

                // 127.0.0.0 – 127.255.255.255 (loopback)
                (bytes[0].toInt() and 0xFF) == 127 -> true

                // 169.254.0.0 – 169.254.255.255 (link-local)
                (bytes[0].toInt() and 0xFF) == 169 &&
                        (bytes[1].toInt() and 0xFF) == 254 -> true

                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    //IPv6
    fun isPrivateIpV6(ip: String): Boolean {
        return try {
            val address = InetAddress.getByName(ip)
            val bytes = address.address

            if (address.isLoopbackAddress || address.isLinkLocalAddress || address.isSiteLocalAddress) {
                return true
            }

            // IPv6 ULA (fc00::/7)
            if (bytes.size == 16) {
                val firstByte = bytes[0].toInt() and 0xFF
                if (firstByte and 0xFE == 0xFC) { // fc00::/7
                    return true
                }
            }

            false
        } catch (e: Exception) {
            false
        }
    }



}
