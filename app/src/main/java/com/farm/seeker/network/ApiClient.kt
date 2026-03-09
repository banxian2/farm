package com.farm.seeker.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    private const val BASE_URL = "https://www.skr-farm.live"
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("seeker_prefs", Context.MODE_PRIVATE)
    }

    suspend fun post(endpoint: String, jsonBody: JSONObject): String {
        return request(endpoint, "POST", jsonBody)
    }

    suspend fun get(endpoint: String, params: Map<String, String>? = null): String {
        val finalEndpoint = if (params != null && params.isNotEmpty()) {
            val queryString = params.entries.joinToString("&") { 
                "${it.key}=${java.net.URLEncoder.encode(it.value, "UTF-8")}" 
            }
            if (endpoint.contains("?")) "$endpoint&$queryString" else "$endpoint?$queryString"
        } else {
            endpoint
        }
        return request(finalEndpoint, "GET", null)
    }

    private suspend fun request(endpoint: String, method: String, jsonBody: JSONObject?): String {
        return withContext(Dispatchers.IO) {
            try {
                val urlStr = if (endpoint.startsWith("http")) endpoint else "$BASE_URL$endpoint"
                val url = URL(urlStr)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = method
                connection.setRequestProperty("Content-Type", "application/json")

//                Log.i("http url",urlStr)
                
                // Add Wallet Address to Header
                val walletAddress = prefs?.getString("user_wallet", "") ?: ""
                connection.setRequestProperty("Wallet-Address", walletAddress)
                
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                if (jsonBody != null && (method == "POST" || method == "PUT")) {
                    connection.doOutput = true
                    connection.outputStream.use { os ->
                        val input = jsonBody.toString().toByteArray(Charsets.UTF_8)
                        os.write(input, 0, input.size)
                    }
                }


                Log.i("http code", connection.responseCode.toString())

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    "Error: Server returned $responseCode $errorResponse"
                }
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }
}
