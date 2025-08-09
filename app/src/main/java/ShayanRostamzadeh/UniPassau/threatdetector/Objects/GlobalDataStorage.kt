package ShayanRostamzadeh.UniPassau.threatdetector.Objects

import ShayanRostamzadeh.UniPassau.threatdetector.FiloMap

object GlobalDataStorage {

    // TODO: add the following to the saved preferences since this can change during the day if
    //  the app is closed and reopened again - retrieve the date and check whether a day has passed --> reset to 0
    var abuseIpDB_Api_Request_No = 0

    val filoMap = FiloMap<String, Boolean>(100)

    val notficationChannelID = "Threat_Detector_ID"

    val delayToCheckAPICalls = 10_000L

    var tcpServerPort = 1234
}