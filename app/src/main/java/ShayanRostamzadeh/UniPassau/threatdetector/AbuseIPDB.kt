/*
this file is responsible for the creation of the AbuseIPDB
client using Retrofit for handling HTTP calls. the information
is received on a json format that matches the data being kept
AbuseIpData data class for further use
*/

package ShayanRostamzadeh.UniPassau.threatdetector

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
        @Query("maxAgeInDays") maxAge: Int = 90
    ): Response<AbuseIpResponse>
}

//change the key after regeneration of the key in the profile
val apiKey = "7257bd8de3bda5a118b2a388acd86486af24b7e1c175d45c2585d27904c9f2cd4e2543fbdef8ecea"

data class AbuseIpResponse(val data: AbuseIpData)

data class AbuseIpData(
    val ipAddress: String,
    val abuseConfidenceScore: Int,
    val countryCode: String,
    val domain: String?,
    val totalReports: Int,
    val isWhitelisted: Boolean
)

fun createAbuseClient(): AbuseIpApi {
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

