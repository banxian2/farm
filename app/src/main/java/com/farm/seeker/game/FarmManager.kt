package com.farm.seeker.game

import android.content.Context
import androidx.activity.ComponentActivity
import com.farm.seeker.network.ApiClient
import com.farm.seeker.solana.SolanaManager
import org.json.JSONObject

class FarmManager(private val activity: ComponentActivity, private val solanaManager: SolanaManager) {
    
    private val prefs = activity.getSharedPreferences("seeker_prefs", Context.MODE_PRIVATE)

    suspend fun updateBillboard(content: String): String {
        // 1. Check Login
        if (!solanaManager.isLoggedIn()) return "Error: Not logged in"
        val walletAddress = solanaManager.getConnectedWallet() ?: return "Error: Wallet not found"

        // 2. Get Auth Data (Reuse Session or Sign New)
        val message = "Update Billboard: $content"
        val (signature, messageToSend) = solanaManager.getAuthData(message)
        
        if (signature.startsWith("Error")) return signature

        activity.runOnUiThread { android.widget.Toast.makeText(activity, "Updating Billboard...", android.widget.Toast.LENGTH_SHORT).show() }
        val params = JSONObject().apply {
            put("wallet_address", walletAddress)
            put("billboard", content)
            put("message", messageToSend)
            put("signature", signature)
        }
        val apiResponse = ApiClient.post("/user_profile/update_billboard", params)
        if (apiResponse.startsWith("Error")) return apiResponse

        try {
            val jsonResponse = JSONObject(apiResponse)
            val code = jsonResponse.optInt("code")
            if (code == 0) {
                prefs.edit().putString("user_billboard", content).apply()
                return "Success"
            } else {
                return "Error: ${jsonResponse.optString("msg")}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error: Failed to parse server response"
        }
    }

    suspend fun getLandList(): String {
        val (signature, message) = solanaManager.getAuthData("Get Land List")
        if (signature.startsWith("Error")) return "{\"code\": 401, \"msg\": \"$signature\"}"

        val walletAddress = solanaManager.getConnectedWallet() ?: return "{\"code\": 401, \"msg\": \"Wallet not connected\"}"

        val params = mapOf(
            "wallet_address" to walletAddress,
            "signature" to signature,
            "message" to message
        )
        return ApiClient.get("/land/list", params)
    }

    suspend fun plant(plotIndex: Int, seedId: String): String {
        val (signature, message) = solanaManager.getAuthData("Plant Seed")
        if (signature.startsWith("Error")) return "{\"code\": 401, \"msg\": \"$signature\"}"

        val walletAddress = solanaManager.getConnectedWallet() ?: return "{\"code\": 401, \"msg\": \"Wallet not connected\"}"

        val params = JSONObject().apply {
            put("wallet_address", walletAddress)
            put("signature", signature)
            put("message", message)
            put("plot_index", plotIndex)
            put("seed_id", seedId)
        }
        return ApiClient.post("/land/plant", params)
    }

    suspend fun water(plotIndex: Int): String {
        val (signature, message) = solanaManager.getAuthData("Water Plant")
        if (signature.startsWith("Error")) return "{\"code\": 401, \"msg\": \"$signature\"}"

        val walletAddress = solanaManager.getConnectedWallet() ?: return "{\"code\": 401, \"msg\": \"Wallet not connected\"}"

        val params = JSONObject().apply {
            put("wallet_address", walletAddress)
            put("signature", signature)
            put("message", message)
            put("plot_index", plotIndex)
        }
        return ApiClient.post("/land/water", params)
    }

    suspend fun harvest(plotIndex: Int): String {
        val (signature, message) = solanaManager.getAuthData("Harvest Crop")
        if (signature.startsWith("Error")) return "{\"code\": 401, \"msg\": \"$signature\"}"

        val walletAddress = solanaManager.getConnectedWallet() ?: return "{\"code\": 401, \"msg\": \"Wallet not connected\"}"

        val params = JSONObject().apply {
            put("wallet_address", walletAddress)
            put("signature", signature)
            put("message", message)
            put("plot_index", plotIndex)
        }
        return ApiClient.post("/land/harvest", params)
    }

    suspend fun shovel(plotIndex: Int): String {
        val (signature, message) = solanaManager.getAuthData("Shovel Plot")
        if (signature.startsWith("Error")) return "{\"code\": 401, \"msg\": \"$signature\"}"

        val walletAddress = solanaManager.getConnectedWallet() ?: return "{\"code\": 401, \"msg\": \"Wallet not connected\"}"

        val params = JSONObject().apply {
            put("wallet_address", walletAddress)
            put("signature", signature)
            put("message", message)
            put("plot_index", plotIndex)
        }
        return ApiClient.post("/land/shovel", params)
    }
}
