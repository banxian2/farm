package com.farm.seeker.me

import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.farm.seeker.network.ApiClient
import com.farm.seeker.solana.SolanaManager
import org.json.JSONObject

class MeManager(private val activity: ComponentActivity, private val solanaManager: SolanaManager) {

    private val prefs = activity.getSharedPreferences("seeker_prefs", Context.MODE_PRIVATE)

    suspend fun updateNickname(newNickname: String): String {
        // 1. Check Login
        if (!solanaManager.isLoggedIn()) return "Error: Not logged in"
        val walletAddress = solanaManager.getConnectedWallet() ?: return "Error: Wallet not found"

        // 2. Get Auth Data (Reuse Session or Sign New)
        val message = "Update Nickname to $newNickname"
        val (signature, messageToSend) = solanaManager.getAuthData(message)
        
        if (signature.startsWith("Error")) return signature

        // 3. Call API
        activity.runOnUiThread { Toast.makeText(activity, "Updating...", Toast.LENGTH_SHORT).show() }
        val params = JSONObject().apply {
            put("wallet_address", walletAddress)
            put("nickname", newNickname)
            put("message", messageToSend)
            put("signature", signature)
        }
        val apiResponse = ApiClient.post("/user_profile/update_nickname", params)
        
        if (apiResponse.startsWith("Error")) {
            return apiResponse
        }

        try {
            val jsonResponse = JSONObject(apiResponse)
            val code = jsonResponse.optInt("code")
            if (code == 0) {
                 // 5. Update Local Storage
                prefs.edit().putString("user_nickname", newNickname).apply()
                return "Success"
            } else {
                 return "Error: ${jsonResponse.optString("msg")}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error: Failed to parse server response"
        }
    }
}
