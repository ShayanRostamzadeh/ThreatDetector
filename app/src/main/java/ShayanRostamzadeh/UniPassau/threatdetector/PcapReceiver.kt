package ShayanRostamzadeh.UniPassau.threatdetector

import android.content.Context
import android.util.Log
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors

class PcapReceiver(private val pcapServerPort: Int) {

    private val serverPort = pcapServerPort
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile
    var isRunning = false
    private var serverSocket: ServerSocket? = null

    fun startServer() {
        isRunning = true

        executor.execute {
            try {
                serverSocket = ServerSocket(serverPort)
                Log.d("PCAP_SERVER", "Server started on port $serverPort")

                while (isRunning) {
                    val clientSocket = serverSocket!!.accept()
                    Log.d("PCAP_SERVER", "Client connected: ${clientSocket.inetAddress.hostAddress}")
                    handleClient(clientSocket)
//                    if(ServerStatusTracker.isListening.value != true){
//                        ServerStatusTracker.setListening(true)
//                        Log.w("PCAP_SERVER", "Server Status Tracker - " +
//                                "should be true: ${ServerStatusTracker.isListening.value}")
//                    }
                }

            } catch (e: IOException) {
                if (isRunning) {
                    Log.e("PCAP_SERVER", "Server error: ${e.message}", e)
                } else {
                    Log.d("PCAP_SERVER", "Server stopped.")
//                    ServerStatusTracker.setListening(false)
//                    ServerStatusTracker.setListening(false)
//                    Log.w("PCAP_SERVER", "Server Status Tracker - " +
//                            "should be false: ${ServerStatusTracker.isListening.value}")
                }
            } finally {
                try {
                    serverSocket?.close()
//                    ServerStatusTracker.setListening(false)
//                    ServerStatusTracker.setListening(false)
//                    Log.w("PCAP_SERVER", "Server Status Tracker - " +
//                            "should be false: ${ServerStatusTracker.isListening.value}")
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
//            Log.w("PCAP_SERVER", "Server Status Tracker - " +
//                    "should be false: ${ServerStatusTracker.isListening.value}")
        } catch (e: IOException) {
            Log.e("PCAP_SERVER", "Error closing server socket: ${e.message}", e)
        }
        executor.shutdownNow()
    }

    private fun handleClient(socket: Socket) {
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

                    // Sanity check inclLen (avoid huge or negative sizes)
                    if (inclLen <= 0 || inclLen > 65535) {
                        Log.e("PCAP_SERVER", "Invalid packet length: $inclLen")
                        return
                    }

                    // Read the packet data
                    val packetData = ByteArray(inclLen)
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

    private fun parseAndLogDestinationIP(packetData: ByteArray) {
        try {
            // Assume Ethernet if first byte's first 4 bits != 4 (IPv4)
            var ipStartIndex = 0
            if ((packetData[0].toInt() shr 4 and 0xF) != 4) {
                // Ethernet header length is 14 bytes
                ipStartIndex = 14
                if (packetData.size < ipStartIndex + 20) {
                    // Not enough data for IP header, ignore
                    return
                }
            }

            val ipVersion = (packetData[ipStartIndex].toInt() shr 4) and 0x0F
            if (ipVersion == 4) {
                // Destination IP bytes are at offset 16-19 after IP header start
                if (packetData.size >= ipStartIndex + 20) {
                    val dstIpBytes = packetData.copyOfRange(ipStartIndex + 16, ipStartIndex + 20)
                    val dstIp = dstIpBytes.joinToString(".") { (it.toInt() and 0xFF).toString() }
                    Log.d("PCAP_PARSER", "Found destination IP: $dstIp")
                    // TODO: Add your AbuseIPDB check or other logic here
                }
            }
        } catch (e: Exception) {
            Log.e("PCAP_PARSER", "Error parsing packet data: ${e.message}", e)
        }
    }



    private fun parsePcapData(pcapData: ByteArray) {
        val parser = PcapParser()
        val destIPs = parser.extractDestinationIPs(pcapData)

        for (ip in destIPs) {
            Log.d("PCAP_PROCESSOR", "Found destination IP: $ip")
            // TODO: handle these IPs further, e.g., send to AbuseIPDB
        }
    }

}
