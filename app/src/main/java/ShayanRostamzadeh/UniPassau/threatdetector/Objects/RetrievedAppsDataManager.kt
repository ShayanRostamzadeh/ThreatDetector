/*
this object is a standalone data storage to keep track of the info
received from PCAPdroid pcap file.
this object maps the ip to the app icon using the IP address, and
also keeps a filo (first in, last out) map to keep track of the
app/package name with the associated AbuseIPDB score. This map acts
as a cache to prevent look up of same IP addresses multiple times,
leading to exhaustion of the free API calls

the functionality of methods defined, are pretty self-explanatory
*/

package ShayanRostamzadeh.UniPassau.threatdetector.Objects

import ShayanRostamzadeh.UniPassau.threatdetector.FiloMap
import android.graphics.drawable.Drawable

object RetrievedAppsDataManager {

    private val appToIP = mutableMapOf<String, String>()
    private val appToIcon = mutableMapOf<String, Drawable?>()

    val filoMap = FiloMap<String, Int>(100)

    fun put(app: String, ip: String, icon: Drawable?) {
        val existingIp = appToIP[app]
        if (existingIp == null || existingIp != ip) {
            appToIP[app] = ip
            appToIcon[app] = icon
        }
    }


    fun remove(app: String) {
        appToIP.remove(app)
        appToIcon.remove(app)
    }

    fun getIP(app: String): String? = appToIP[app]

    fun getIcon(app: String): Drawable? = appToIcon[app]

    fun containsApp(app: String): Boolean = appToIP.containsKey(app)

    fun getAppIPMap() : Map<String, String> = appToIP

    fun getAppIconMap() : Map<String, Drawable?> = appToIcon

    fun clear() {
        appToIP.clear()
        appToIcon.clear()
    }
}