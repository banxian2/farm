package com.farm.seeker.friend

import androidx.activity.ComponentActivity
import android.widget.Toast
import com.farm.seeker.network.ApiClient
import com.farm.seeker.solana.SolanaManager
import org.json.JSONObject
import java.net.URLEncoder

class ChatManager(
    private val activity: ComponentActivity,
    private val solanaManager: SolanaManager
) {

    suspend fun getChatHistory(friendAddress: String, page: Int = 1, limit: Int = 20): Pair<List<Message>, String?> {
        // 1. Check Login
        if (!solanaManager.isLoggedIn()) return Pair(emptyList(), "Error: Not logged in")
        val walletAddress = solanaManager.getConnectedWallet() ?: return Pair(emptyList(), "Error: Wallet not found")

        // 2. Get Auth Data
        val message = "Get Chat History"
        val (signature, messageToSend) = solanaManager.getAuthData(message)
        
        if (signature.startsWith("Error")) return Pair(emptyList(), signature)

        // 3. Call API
        val endpoint = "/chat/history?wallet_address=$walletAddress&target_wallet_address=$friendAddress&page=$page&limit=$limit&message=${URLEncoder.encode(messageToSend, "UTF-8")}&signature=${URLEncoder.encode(signature, "UTF-8")}"
        
        val apiResponse = ApiClient.get(endpoint)
        
        if (apiResponse.startsWith("Error")) {
            return Pair(emptyList(), apiResponse)
        }

        try {
            val jsonResponse = JSONObject(apiResponse)
            val code = jsonResponse.optInt("code")
            if (code == 0) {
                val data = jsonResponse.optJSONObject("data")
                val messagesData = data?.optJSONObject("messages")
                val list = messagesData?.optJSONArray("data") ?: return Pair(emptyList(), null)
                
                val messages = mutableListOf<Message>()
                for (i in 0 until list.length()) {
                    val item = list.getJSONObject(i)
                    val senderId = item.optString("sender_id")
                    val isMe = senderId == walletAddress
                    
                    // Map integer type to Enum
                    val typeInt = item.optInt("message_type", 0)
                    val type = when (typeInt) {
                        1 -> MessageType.SEED
                        else -> MessageType.TEXT
                    }

                    // Parse created_at timestamp string if necessary, or assume it's long if API changed
                    // The doc says created_at is "2023-10-27 10:05:00", so we might need to parse it.
                    // But Message data class expects Long. Let's try to parse or just use 0 for now if string.
                    // Wait, previous code used optLong. If server returns string, optLong might return 0.
                    // Let's check if we can parse the date string to long.
                    // For now, let's just try to get it as string and convert, or keep optLong if server sends timestamp.
                    // Given the doc example shows string "2023-10-27...", I should probably parse it.
                    // However, to be safe and simple, I'll use a helper or just 0L for now, 
                    // or better: check if I can modify Message to take string or parse it here.
                    // Let's assume for a moment the server might return a timestamp or I'll implement a simple parser later.
                    // Actually, let's check Message class definition again. It expects Long.
                    
                    var timestamp = 0L
                    val createdAtStr = item.optString("created_at")
                    if (createdAtStr.isNotEmpty()) {
                         // Simple parsing or use Java Time if minSdk allows (API 26+). MinSdk is 24.
                         // Simple fallback:
                         try {
                             val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                             timestamp = sdf.parse(createdAtStr)?.time ?: 0L
                         } catch (e: Exception) {
                             // Try optLong as fallback
                             timestamp = item.optLong("created_at", 0L)
                         }
                    }

                    messages.add(Message(
                        id = item.optString("id"),
                        senderId = senderId,
                        content = item.optString("content"),
                        type = type,
                        isMe = isMe,
                        timestamp = timestamp
                    ))
                }
                // Sort by timestamp (ascending)
                messages.sortBy { it.timestamp }
                
                return Pair(messages, null)
            } else {
                return Pair(emptyList(), "Error: ${jsonResponse.optString("msg")}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(emptyList(), "Error: Failed to parse server response")
        }
    }

    suspend fun sendMessage(receiverAddress: String, content: String, type: MessageType): String {
        // 1. Check Login
        if (!solanaManager.isLoggedIn()) return "Error: Not logged in"
        val walletAddress = solanaManager.getConnectedWallet() ?: return "Error: Wallet not found"

        // 2. Get Auth Data
        val message = "Send Message"
        val (signature, messageToSend) = solanaManager.getAuthData(message)
        
        if (signature.startsWith("Error")) return signature

        // 3. Call API
        val typeInt = when (type) {
            MessageType.SEED -> 1
            else -> 0
        }

        val params = JSONObject().apply {
            put("wallet_address", walletAddress)
            put("receiver_wallet_address", receiverAddress)
            put("content", content)
            put("message_type", typeInt)
            put("message", messageToSend)
            put("signature", signature)
        }
        
        val apiResponse = ApiClient.post("/chat/send", params)
        
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

    // Delete message functionality removed
}
