/*
this class checks whether the IP address has been cached,
whether it is a FAANG IP address and if not it send the API
call to receive the associated score
*/


package ShayanRostamzadeh.UniPassau.threatdetector

import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.abuseCategories
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.abuseIpDB_Api_Request_No
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.abuseIpDbMaliciousScore
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.appContext
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.RetrievedAppsDataManager.fiFoMap
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.RetrievedAppsDataManager.getAppIPMap
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.RetrievedAppsDataManager.getAppIconMap
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.nio.ByteBuffer

data class CidrBlock(val baseAddress: InetAddress, val prefixLength: Int)

class AbuseIPDBCheckIP {

    val notificationManager = AppNotificationManager()

    // function to retrieve the key of a map by providing its value
    fun <K, V> Map<K, V>.getKeyByValue(value: V): K? {
        return this.entries.firstOrNull { it.value == value }?.key
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    suspend fun getIpData(IpAddr: String): AbuseIpData? {

        Log.w("PCAP_PARSER", "in getIpData")

        // 1. Check the cache (store full data instead of just score)
        fiFoMap.map[IpAddr]?.let { return it }

        // 2. Skip known FAANG IPs — treat as safe (score 0)
        if (isFaangIp(IpAddr)) {
            return AbuseIpData(
                ipAddress = IpAddr,
                abuseConfidenceScore = 0,
                countryCode = "US",
                domain = null,
                totalReports = 0,
                isWhitelisted = false,
                reports = emptyList()
            )
        }

        return try {
            val response = createAbuseClient().checkIp(IpAddr)

            if (response.isSuccessful) {
                abuseIpDB_Api_Request_No++

                val data = response.body()?.data

                val report = data?.reports?.firstOrNull()



                data?.reports?.take(5)?.forEach { report ->   // show only first 5
                    val cats = report.categories.joinToString { id ->
                        abuseCategories[id] ?: "Unknown($id)"
                    }
                    Log.w("PCAP_PARSER", """
                        Reported at: ${report.reportedAt}
                        Categories: $cats
                        Comment   : ${report.comment}
                        Reporter  : ${report.reporterId}
                    """.trimIndent())
                }

//                Log.w("PCAP_PARSER", "Report array is: $report")
//                if (report != null) {
//
//                    Log.d("PCAP_PARSER", "Last Reported: ${report.reportedAt}")
//                    Log.d("PCAP_PARSER", "Comment: ${report.comment ?: "No comment"}")
//                    Log.d("PCAP_PARSER", report.categories.joinToString())
//                }


                if (data != null) {
                    // cache the full data
                    fiFoMap.put(IpAddr, data)
                    val appIPMap = getAppIPMap()
                    val appName = appIPMap.getKeyByValue(IpAddr)

                    // notify if malicious
                    if (data.abuseConfidenceScore > abuseIpDbMaliciousScore) {
                        while (appContext == null) { /* waiting for the app context */ }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                appContext,
                                "⚠️ Malicious IP: $IpAddr (score: ${data.abuseConfidenceScore})",
                                Toast.LENGTH_LONG
                            ).show()

                            // using android notifications to inform user of the malicious IP
                            notificationManager.createNotificationChannel(appContext!!)
                            notificationManager.showNotification(appContext!!, appName.toString(),
                                "Malicious IP found: $IpAddr")
                        }
                    }

                    Log.d("PCAP_PARSER", "Checked IP $IpAddr: score=${data.abuseConfidenceScore}")
                }

                data
            } else {
                if (response.code() == 429) {
                    val errorBody = response.errorBody()?.string()
                    Log.e("PCAP_PARSER", "Rate limit exceeded: $errorBody")
                    // handle limit exceeded (show UI / stop server)
                }
                null
            }
        } catch (e: Exception) {
            Log.e("PCAP_PARSER", "AbuseIPDB error: ${e.message}")
            null
        }
    }


    private fun parseCidr(cidr: String): CidrBlock {
        val (ip, prefix) = cidr.split("/")
        return CidrBlock(InetAddress.getByName(ip), prefix.toInt())
    }//parseCidr


    // the following function checks whether the IP is within
    // the range of Cidr block of the FAANG
    private fun isIpInRange(ip: String, cidr: String): Boolean {
        val ipAddress = InetAddress.getByName(ip).address
        val cidrBlock = parseCidr(cidr)
        val network = cidrBlock.baseAddress.address

        val prefix = cidrBlock.prefixLength
        val mask = -1 shl (32 - prefix)
        val ipInt = ByteBuffer.wrap(ipAddress).int
        val netInt = ByteBuffer.wrap(network).int

        return (ipInt and mask) == (netInt and mask)
    }//isIpInRange


    // the following function checks whether the received IP
    // from PCAPdroid belongs to FAANG domains
    private fun isFaangIp(ip: String): Boolean {
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

            // AbuseIPDB
            "104.26.12.38", "172.67.70.74",

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