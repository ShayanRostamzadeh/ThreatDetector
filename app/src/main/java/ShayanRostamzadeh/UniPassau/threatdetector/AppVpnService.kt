package ShayanRostamzadeh.UniPassau.threatdetector

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AppVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var running = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running) return START_STICKY
        running = true

        val builder = Builder()
        builder.setSession("AppVpnService")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .setBlocking(true)

        vpnInterface = builder.establish()

        Thread {
            vpnInterface?.fileDescriptor?.let { fd ->
                val input = FileInputStream(fd)
                val channel = input.channel
                val buffer = ByteBuffer.allocate(32767)

                while (running) {
                    buffer.clear()
                    val readBytes = channel.read(buffer)
                    if (readBytes > 0) {
                        buffer.flip()
                        parsePacket(buffer)
                    }
                }
            }
        }.start()

        return START_STICKY
    }

    private fun parsePacket(buffer: ByteBuffer) {
        buffer.order(ByteOrder.BIG_ENDIAN)

        val version = (buffer.get(0).toInt() shr 4) and 0xF
        if (version == 4 && buffer.limit() >= 20) {
            // IPv4 packet
            val destIp = "${buffer.get(16).toInt() and 0xFF}.${buffer.get(17).toInt() and 0xFF}." +
                    "${buffer.get(18).toInt() and 0xFF}.${buffer.get(19).toInt() and 0xFF}"

            Log.i("AppVpnService", "Intercepted packet to IP: $destIp")
        }
    }

    override fun onDestroy() {
        running = false
        vpnInterface?.close()
        vpnInterface = null
        super.onDestroy()
    }
}
