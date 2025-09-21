/*
this file is responsible for the creation of the AbuseIPDB
client using Retrofit for handling HTTP calls. the information
is received on a json format that matches the data being kept
AbuseIpData data class for further use
*/

package ShayanRostamzadeh.UniPassau.threatdetector

import ShayanRostamzadeh.UniPassau.threatdetector.Objects.GlobalDataStorage.appContext
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface AbuseIpApi {
    @GET("api/v2/check")
    suspend fun checkIp(
        @Query("ipAddress") ip: String,
        @Query("maxAgeInDays") maxAge: Int = 90,
        @Query("verbose") verbose: Boolean = true
    ): Response<AbuseIpResponse>
}


data class AbuseIpResponse(val data: AbuseIpData)

data class AbuseIpData(
    val ipAddress: String,
    val abuseConfidenceScore: Int,
    val countryCode: String,
    val domain: String?,
    val totalReports: Int,
    val isWhitelisted: Boolean,

    val reports: List<Report> = emptyList()

)

data class Report(
    val reportedAt: String,
    val comment: String?,
    val categories: List<Int>,
    val reporterId: Int
)

//data class AbuseIpData(
//    val ipAddress: String,
//    val abuseConfidenceScore: Int,
//    val countryCode: String,
//    val isp: String?,
//    val domain: String?,
//    val usageType: String?,
//    val totalReports: Int,
//    val lastReportedAt: String?,
//    val isWhitelisted: Boolean?
//)

suspend fun createAbuseClient(): AbuseIpApi {

    val apiKey = ApiKeyManager.getApiKey(appContext!!) ?: ""

    withContext(Dispatchers.Main){
        if(apiKey == ""){
            Toast.makeText(appContext,"No API Keys found!!" +
                    "\n Save the API Key and Restart the App."
                , Toast.LENGTH_LONG).show()
        }
    }


    val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Key", apiKey)
                .addHeader("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    return Retrofit.Builder()
        .baseUrl("https://api.abuseipdb.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AbuseIpApi::class.java)
}

