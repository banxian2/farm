package com.farm.seeker.game

import com.farm.seeker.network.ApiClient
import com.farm.seeker.solana.SolanaManager
import org.json.JSONObject
import java.net.URLEncoder

class TaskManager(private val solanaManager: SolanaManager) {

    suspend fun getTaskList(): String {
        // Reuse login signature to avoid constant signing prompts
        val (signature, message) = solanaManager.getAuthData("Login to Seeker")
        if (signature.startsWith("Error")) return signature

        val walletAddress = solanaManager.getConnectedWallet() ?: return "Error: Wallet not connected"

        // URL Encode params
        val encSignature = URLEncoder.encode(signature, "UTF-8")
        val encMessage = URLEncoder.encode(message, "UTF-8")
        val encWallet = URLEncoder.encode(walletAddress, "UTF-8")

        val endpoint = "/task/list?wallet_address=$encWallet&signature=$encSignature&message=$encMessage"
        
        return ApiClient.get(endpoint)
    }

    suspend fun claimTask(taskId: Int): String {
        // Reuse login signature
        val (signature, message) = solanaManager.getAuthData("Login to Seeker")
        if (signature.startsWith("Error")) return signature
        
        val walletAddress = solanaManager.getConnectedWallet() ?: return "Error: Wallet not connected"

        val params = JSONObject().apply {
            put("wallet_address", walletAddress)
            put("signature", signature)
            put("message", message)
            put("task_id", taskId)
        }

        val response = ApiClient.post("/task/claim", params)
        
        try {
            val json = JSONObject(response)
            if (json.optInt("code") == 200) {
                // Refresh user profile (coins, etc.) after successful claim
                solanaManager.updateUserProfile()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return response
    }
}
