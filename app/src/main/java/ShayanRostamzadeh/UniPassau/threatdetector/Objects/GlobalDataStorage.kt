package ShayanRostamzadeh.UniPassau.threatdetector.Objects

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object GlobalDataStorage {

    // TODO: add the following to the saved preferences since this can change during the day if
    //  the app is closed and reopened again - retrieve the date and check whether a day has passed --> reset to 0

    //var to keep track of the number of API requests sent till exhaustion
    var abuseIpDB_Api_Request_No = 0

    // var to keep the app context centralized for use - will be set on app run
    var appContext: Context? = null

    // the channel ID to further use for depicting notifications
    // for API exhaustion or malicious IP found
    val notficationChannelID = "Threat_Detector_ID"

    // delay set for the background thread which checks for
    // the free 1000 API calls a day - this will notify user
    val delayToCheckAPICalls = 10_000L

    // default tcp server port for PCAPdroid TCP packets to receive
    var tcpServerPort by mutableStateOf(1234)

    // default min value for the score received for an IP address to be
    // considered as malicious and notifies the user
    var abuseIpDbMaliciousScore by mutableStateOf(50)


    // codes indicating the reason behind the negative report of an IP address
    // in AbuseIPDB Check response
    val abuseCategories = mapOf(
        1 to "DNS Compromise",
        2 to "DNS Poisoning",
        3 to "Fraud Orders",
        4 to "DDoS Attack",
        5 to "FTP Brute-Force",
        6 to "Ping of Death",
        7 to "Phishing",
        8 to "Fraud VoIP",
        9 to "Open Proxy",
        10 to "Web Spam",
        11 to "Email Spam",
        12 to "Blog Spam",
        13 to "VPN IP",
        14 to "Port Scan",
        15 to "Hacking",
        16 to "SQL Injection",
        17 to "Spoofing",
        18 to "Brute-Force Credential Attack",
        19 to "Bad Web Bot",
        20 to "Exploited Host",
        21 to "Web App Attack",
        22 to "SSH Abuse",
        23 to "IoT Targeted"
    )

}