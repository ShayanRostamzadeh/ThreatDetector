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



    // gets the score of the IP address from AbuseIPDB
//    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
//    suspend fun getIpScore(IpAddr: String): Int {
//
//        Log.w("PCAP_PARSER", "in getIpScore")
//
//        // 1. Check the cache
//        fiFoMap.map[IpAddr]?.let { return it }
//
//        // 2. Skip known FAANG IPs — treat as safe score = 0
//        if (isFaangIp(IpAddr)) return 0
//
//        // 3. If the IP is neither cached nor FAANG, it sends the
//        // API request for the score
//        return try {
//            val response = createAbuseClient().checkIp(IpAddr)
//            Log.e("PCAP_PARSER", "received is: $response")
//
//            if (response.isSuccessful) {
//                abuseIpDB_Api_Request_No++
//                val score = response.body()?.data?.abuseConfidenceScore ?: 0
//
//                // caching the response
//                fiFoMap.put(IpAddr, score)
//
//                if (score > abuseIpDbMaliciousScore) {
//                    //waiting till the context for the app has been set in the global object
//                    while (true){
//                        if(appContext != null)
//                            break
//                    }
//
//                    //here we use the main thread to show the notifications
//                    //since the main thread is responsible for the UI
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(appContext, "⚠️ Malicious IP: $IpAddr", Toast.LENGTH_LONG).show()
//                    }
//                }
//
//                Log.d("PCAP_PARSER", "Checked IP $IpAddr: score=$score")
//                score
//            } else {
//                // Handle error response in case the number of free api calls have been exhausted
//                /*
//                https://docs.abuseipdb.com/#clear-address-endpoint
//
//                With the request header "Accept: application/json"
//                {
//                  "errors": [
//                      {
//                          "detail": "Daily rate limit of 1000 requests exceeded for this endpoint. See headers for additional details.",
//                          "status": 429
//                      }
//                  ]
//                }
//                 */
//                if (response.code() == 429) {
//                    val errorBody = response.errorBody()?.string()
//                    Log.e("PCAP_PARSER", "Rate limit exceeded: $errorBody")
//
//                    // fixme: show this as a notification not a toast - make a page to depict user
//                    //  he/she can start using the app from tomorrow
//                    // the following code is functioning without a problem
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(appContext, "AbuseIPDB daily limit reached. Further checks paused.",
//                            Toast.LENGTH_LONG).show()
//                        //stopping TCP server to send API calls to AbuseIPDB
//                        ServerStatusTracker.stopServer()
//
//                        //redirection to API exhaustion page
//                        while (true){
//                            if(appContext != null)
//                                break
//                        }
//                        val intent = Intent(appContext, AbuseApiLimitWarningActivity::class.java)
//                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                        appContext!!.startActivity(intent)
//
//                    }
//
//                    return 0
//                    }
//
//                    Log.e("PCAP_PARSER", "AbuseIPDB response not successful: ${response.code()}")
//                    0
//            }
//        } catch (e: Exception) {
//            Log.e("PCAP_PARSER", "AbuseIPDB error: ${e.message}")
//            0
//        }
//    }

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