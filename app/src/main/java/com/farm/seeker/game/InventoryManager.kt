package com.farm.seeker.game

import android.util.Log
import com.farm.seeker.network.ApiClient
import com.farm.seeker.solana.SolanaManager
import java.net.URLEncoder

class InventoryManager(private val solanaManager: SolanaManager) {

    suspend fun getInventoryList(): String {
        // Reuse login signature to avoid constant signing prompts
        val (signature, message) = solanaManager.getAuthData("Get Inventory List")
        if (signature.startsWith("Error")) return "{\"code\": 401, \"msg\": \"$signature\"}"

        val walletAddress = solanaManager.getConnectedWallet() ?: return "{\"code\": 401, \"msg\": \"Wallet not connected\"}"

        val encSignature = URLEncoder.encode(signature, "UTF-8")
        val encMessage = URLEncoder.encode(message, "UTF-8")
        val encWallet = URLEncoder.encode(walletAddress, "UTF-8")

        val endpoint = "/inventory/list?wallet_address=$encWallet&signature=$encSignature&message=$encMessage"

        val data = ApiClient.get(endpoint)
        Log.i("InventoryManager", "getInventoryList: $data")
        return data
    }
}
