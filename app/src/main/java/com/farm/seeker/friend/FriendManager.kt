package com.farm.seeker.friend

import androidx.activity.ComponentActivity
import android.widget.Toast
import com.farm.seeker.network.ApiClient
import com.farm.seeker.solana.SolanaManager
import org.json.JSONObject
import java.net.URLEncoder

class FriendManager(
    private val activity: ComponentActivity,
    private val solanaManager: SolanaManager
) {

    suspend fun deleteFriend(friendWalletAddress: String): String {
        // 1. Check Login
        if (!solanaManager.isLoggedIn()) return "Error: Not logged in"
        val walletAddress = solanaManager.getConnectedWallet() ?: return "Error: Wallet not found"

        // 2. Get Auth Data
        val message = "Delete Friend"
        val (signature, messageToSend) = solanaManager.getAuthData(message)
        
        if (signature.startsWith("Error")) return signature

        // 3. Call API
        val params = JSONObject().apply {
            put("wallet_address", walletAddress)
            put("friend_wallet_address", friendWalletAddress)
            put("message", messageToSend)
            put("signature", signature)
        }
        val apiResponse = ApiClient.post("/friend/delete", params)
        
        if (apiResponse.startsWith("Error")) {
            return apiResponse
        }

        try {
            val jsonResponse = JSONObject(apiResponse)
            val code = jsonResponse.optInt("code")
            if (code == 0) {
                return "Success"
            } else {
                return "Error: ${jsonResponse.optString("msg")}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error: Failed to parse server response"
        }
    }

    suspend fun sendFriendRequest(target: String): String {
        // 1. Check Login
        if (!solanaManager.isLoggedIn()) return "Error: Not logged in"
        val walletAddress = solanaManager.getConnectedWallet() ?: return "Error: Wallet not found"

        // 2. Get Auth Data
        val message = "Send Friend Request"
        val (signature, messageToSend) = solanaManager.getAuthData(message)
        
        if (signature.startsWith("Error")) return signature

        // 3. Call API
        activity.runOnUiThread { Toast.makeText(activity, "Sending Request...", Toast.LENGTH_SHORT).show() }
        val params = JSONObject().apply {
            put("wallet_address", walletAddress)
            put("target", target)
            put("message", messageToSend)
            put("signature", signature)
        }
        val apiResponse = ApiClient.post("/notification/friend_request", params)
        
        if (apiResponse.startsWith("Error")) {
            return apiResponse
        }

        try {
            val jsonResponse = JSONObject(apiResponse)
            val code = jsonResponse.optInt("code")
            if (code == 0) {
                return "Success"
            } else {
                return "Error: ${jsonResponse.optString("msg")}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error: Failed to parse server response"
        }
    }

    suspend fun fetchNotifications(page: Int = 1, limit: Int = 20): String {
        // 1. Check Login
        if (!solanaManager.isLoggedIn()) return "Error: Not logged in"
        val walletAddress = solanaManager.getConnectedWallet() ?: return "Error: Wallet not found"

        // 2. Get Auth Data
        val message = "List Notifications"
        val (signature, messageToSend) = solanaManager.getAuthData(message)
        
        if (signature.startsWith("Error")) return signature

        // 3. Call API (GET)
        val endpoint = "/notification/list?wallet_address=$walletAddress&page=$page&limit=$limit&message=${URLEncoder.encode(messageToSend, "UTF-8")}&signature=${URLEncoder.encode(signature, "UTF-8")}"
        
        val apiResponse = ApiClient.get(endpoint)
        
        if (apiResponse.startsWith("Error")) {
            return apiResponse
        }

        try {
            val jsonResponse = JSONObject(apiResponse)
            val code = jsonResponse.optInt("code")
            if (code == 0) {
                val data = jsonResponse.optJSONObject("data")
                val list = data?.optJSONArray("data")
                return list?.toString() ?: "[]"
            } else {
                return "Error: ${jsonResponse.optString("msg")}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error: Failed to parse server response"
        }
    }

    suspend fun processNotification(notificationId: String, action: String): String {
        // action: "agree" or "reject"
        // 1. Check Login
        if (!solanaManager.isLoggedIn()) return "Error: Not logged in"
        val walletAddress = solanaManager.getConnectedWallet() ?: return "Error: Wallet not found"

        // 2. Get Auth Data
        val message = "Process Notification"
        val (signature, messageToSend) = solanaManager.getAuthData(message)
        
        if (signature.startsWith("Error")) return signature

        // 3. Call API
        activity.runOnUiThread { Toast.makeText(activity, "Processing...", Toast.LENGTH_SHORT).show() }
        val params = JSONObject().apply {
            put("wallet_address", walletAddress)
            put("notification_id", notificationId.toIntOrNull() ?: 0)
            put("action", action)
            put("message", messageToSend)
            put("signature", signature)
        }
        val apiResponse = ApiClient.post("/notification/process", params)
        
        if (apiResponse.startsWith("Error")) {
            return apiResponse
        }

        try {
            val jsonResponse = JSONObject(apiResponse)
            val code = jsonResponse.optInt("code")
            if (code == 0) {
                return "Success"
            } else {
                return "Error: ${jsonResponse.optString("msg")}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error: Failed to parse server response"
        }
    }

    suspend fun fetchFriendList(): String {
        if (!solanaManager.isLoggedIn()) return "Error: Not logged in"
        val walletAddress = solanaManager.getConnectedWallet() ?: return "Error: Wallet not found"
        val message = "List Friends"
        val (signature, messageToSend) = solanaManager.getAuthData(message)
        if (signature.startsWith("Error")) return signature

        val endpoint = "/friend/list?wallet_address=$walletAddress&message=${URLEncoder.encode(messageToSend, "UTF-8")}&signature=${URLEncoder.encode(signature, "UTF-8")}"
        
        val apiResponse = ApiClient.get(endpoint)
        if (apiResponse.startsWith("Error")) return apiResponse
        
        try {
            val jsonResponse = JSONObject(apiResponse)
            val code = jsonResponse.optInt("code")
            if (code == 0) {
                val data = jsonResponse.optJSONArray("data")
                return data?.toString() ?: "[]"
            } else {
                return "Error: ${jsonResponse.optString("msg")}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error: Failed to parse server response"
        }
    }
}
