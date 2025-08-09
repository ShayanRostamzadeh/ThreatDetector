package ShayanRostamzadeh.UniPassau.threatdetector

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class PcapParser {

    fun extractDestinationIPs(pcapData: ByteArray): Set<String> {
        val destIPs = mutableSetOf<String>()
        val byteBuffer = ByteBuffer.wrap(pcapData).order(ByteOrder.LITTLE_ENDIAN)

        // Skip global PCAP header (24 bytes)
        if (byteBuffer.remaining() < 24) return destIPs
        byteBuffer.position(24)

        while (byteBuffer.remaining() > 16) {
            // Read PCAP packet header (16 bytes)
            val tsSec = byteBuffer.int
            val tsUsec = byteBuffer.int
            val inclLen = byteBuffer.int
            val origLen = byteBuffer.int

            if (byteBuffer.remaining() < inclLen) break

            val packetData = ByteArray(inclLen)
            byteBuffer.get(packetData)

            // Skip Ethernet header if present
            var ipStartIndex = 0
            if ((packetData[0].toInt() and 0xF0) != 0x40) {
                // Likely Ethernet, skip 14 bytes
                ipStartIndex = 14
            }

            if (packetData.size > ipStartIndex + 16) {
                val ipVersion = (packetData[ipStartIndex].toInt() shr 4) and 0x0F
                if (ipVersion == 4) {
                    // IPv4 destination IP is at bytes 16–19 after IP header start
                    val dstIpBytes = packetData.copyOfRange(ipStartIndex + 16, ipStartIndex + 20)
                    val dstIp = dstIpBytes.joinToString(".") { (it.toInt() and 0xFF).toString() }
                    destIPs.add(dstIp)
                }
            }
        }

        Log.d("PCAP_PARSER", "Extracted IPs: $destIPs")
        return destIPs
    }
}
