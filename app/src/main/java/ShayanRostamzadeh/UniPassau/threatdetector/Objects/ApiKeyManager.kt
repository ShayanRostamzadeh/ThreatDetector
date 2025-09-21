package ShayanRostamzadeh.UniPassau.threatdetector

import android.content.Context
import android.content.SharedPreferences

object ApiKeyManager {
    private const val PREFS_NAME = "abuseipdb_prefs"
    private const val KEY_API = "abuseipdb_api_key"

    fun saveApiKey(context: Context, apiKey: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_API, apiKey.trim()).apply()
    }

    fun getApiKey(context: Context): String? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_API, null)
    }
}
