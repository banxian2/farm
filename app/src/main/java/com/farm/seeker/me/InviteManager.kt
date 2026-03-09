package com.farm.seeker.me

import android.util.Log
import androidx.activity.ComponentActivity
import com.farm.seeker.network.ApiClient
import com.farm.seeker.solana.SolanaManager
import java.net.URLEncoder
import org.json.JSONObject

data class InvitedFriend(
    val inviteId: Int,
    val targetId: String,
    val targetWalletAddress: String,
    val name: String,
    val level: Int,
    val rewardClaimed: Boolean,
    val avatarIndex: Int
)

class InviteManager(private val activity: ComponentActivity, private val solanaManager: SolanaManager) {

    suspend fun getInvitedFriends(): List<InvitedFriend> {
        // 1. Check Login
        if (!solanaManager.isLoggedIn()) return emptyList()
        val walletAddress = solanaManager.getConnectedWallet() ?: return emptyList()

        // 2. Get Auth Data
        val message = "List Invites"
        val (signature, messageToSend) = solanaManager.getAuthData(message)
        
        if (signature.startsWith("Error")) {
            // Handle error or return empty
            return emptyList()
        }

        // 3. Call API
        // GET request with query params is a bit tricky with current ApiClient.get which takes endpoint.
        // We need to append query params manually.
        val encodedMessage = URLEncoder.encode(messageToSend, "UTF-8")
        val encodedSignature = URLEncoder.encode(signature, "UTF-8")
        val endpoint = "/invite/list?wallet_address=$walletAddress&message=$encodedMessage&signature=$encodedSignature"
        val apiResponse = ApiClient.get(endpoint)
        
        if (apiResponse.startsWith("Error")) {
            return emptyList()
        }

        try {
            val jsonResponse = JSONObject(apiResponse)
            val code = jsonResponse.optInt("code")
            if (code == 0) {
                val dataArray = jsonResponse.optJSONArray("data") ?: return emptyList()
                val friends = mutableListOf<InvitedFriend>()
                for (i in 0 until dataArray.length()) {
                    val item = dataArray.getJSONObject(i)
                    friends.add(InvitedFriend(
                        inviteId = item.optInt("invite_id"),
                        targetId = item.optString("target_id"),
                        targetWalletAddress = item.optString("target_wallet_address"),
                        name = item.optString("target_nickname"),
                        level = item.optInt("target_level"),
                        rewardClaimed = item.optInt("reward_claimed") == 1,
                        avatarIndex = item.optInt("target_avatar_index", item.optInt("avatar_index", 1))
                    ))
                }
                return friends
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }

    suspend fun claimReward(targetWalletAddress: String): String {
        // 1. Check Login
        if (!solanaManager.isLoggedIn()) return "Error: Not logged in"
        val walletAddress = solanaManager.getConnectedWallet() ?: return "Error: Wallet not found"

        // 2. Get Auth Data
        val message = "Claim Invite Reward"
        val (signature, messageToSend) = solanaManager.getAuthData(message)
        
        if (signature.startsWith("Error")) return signature

        // 3. Call API
        val params = JSONObject().apply {
            put("wallet_address", walletAddress)
            put("target_wallet_address", targetWalletAddress)
            put("message", messageToSend)
            put("signature", signature)
        }
        val apiResponse = ApiClient.post("/invite/claim", params)
        Log.i("http", apiResponse.toString())
        
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

    // Grand reward is now handled automatically by the server on the 10th claim
    // We can keep this method to verify status or just remove it. 
    // For compatibility with Activity, we'll leave it but make it informative.
    suspend fun claimGrandReward(): String {
        return "Grand rewards are automatically claimed when you reach 10 claimed invites!"
    }
}
