package ShayanRostamzadeh.UniPassau.threatdetector

import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.abuseIpDB_Api_Request_No
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.delayToCheckAPICalls
import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.notficationChannelID
import android.content.Context
import kotlinx.coroutines.*
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat


class CheckAPICalls (val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startRepeatingTask() {
        scope.launch {
            while (isActive) {
                try {
//                    println("Running task at: ${System.currentTimeMillis()}")
                    if (abuseIpDB_Api_Request_No >= 1000){
                        //notifying the user that the free API calls are exhausted
                        createNotificationChannel(context)
                        showNotification(context, "Threat Detector",
                            "You Have Exhausted Free API Calls For Today!")
                    }

                    //Wait 30 seconds
                    delay(delayToCheckAPICalls)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun stopRepeatingTask() {
        scope.cancel() // Stops the coroutine
    }//stopRepeatingTask


    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = notficationChannelID
            val channelName = "My Notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Channel for important notifications"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }//createNotificationChannel


    fun showNotification(context: Context, title: String, message: String) {
        val channelId = notficationChannelID
        val notificationId = 1

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }//showNotification


}
