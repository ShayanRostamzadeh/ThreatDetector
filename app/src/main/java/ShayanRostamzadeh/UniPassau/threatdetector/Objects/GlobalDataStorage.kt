package ShayanRostamzadeh.UniPassau.threatdetector.Objects

import android.content.Context

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
    var tcpServerPort = 1234

    // default min value for the score received for an IP address to be
    // considered as malicious and notifies the user
    var abuseIpDbMaliciousScore = 50
}