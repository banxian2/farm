package com.farm.seeker.game

import android.util.Log
import com.farm.seeker.network.ApiClient
import com.farm.seeker.solana.SolanaManager
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ShopManager(private val solanaManager: SolanaManager) {

    suspend fun getSeedList(): String {
        // Reuse login signature to avoid constant signing prompts
        val (signature, message) = solanaManager.getAuthData("Get Shop Seeds")
        if (signature.startsWith("Error")) return "{\"code\": 401, \"msg\": \"$signature\"}"

        val walletAddress = solanaManager.getConnectedWallet() ?: return "{\"code\": 401, \"msg\": \"Wallet not connected\"}"

        val encSignature = URLEncoder.encode(signature, "UTF-8")
        val encMessage = URLEncoder.encode(message, "UTF-8")
        val encWallet = URLEncoder.encode(walletAddress, "UTF-8")

        val endpoint = "/shop_seeds/list?wallet_address=$encWallet&signature=$encSignature&message=$encMessage"

        val data = ApiClient.get(endpoint)
//        Log.i("XXX",endpoint)
//        Log.i("XXX",data)
        return data
    }

    suspend fun buySeed(seedId: String, amount: Int): String {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Get Auth Data (Session or New Sign)
                val (signature, message) = solanaManager.getAuthData("Buy Seed")
                if (signature.startsWith("Error")) {
                    return@withContext "{\"code\": 401, \"msg\": \"$signature\"}"
                }

                val walletAddress = solanaManager.getConnectedWallet()
                if (walletAddress == null) {
                    return@withContext "{\"code\": 401, \"msg\": \"Wallet not connected\"}"
                }

                // 2. Call API
                val params = JSONObject()
                params.put("wallet_address", walletAddress)
                params.put("signature", signature)
                params.put("message", message)
                params.put("seed_id", seedId)
                params.put("amount", amount)

                val response = ApiClient.post("/shop_seeds/buy", params)
                return@withContext response
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext "{\"code\": 500, \"msg\": \"${e.message}\"}"
            }
        }
    }

    suspend fun sellCrop(itemId: String, amount: Int): String {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Get Auth Data (Session or New Sign)
                val (signature, message) = solanaManager.getAuthData("Sell Crop")
                if (signature.startsWith("Error")) {
                    return@withContext "{\"code\": 401, \"msg\": \"$signature\"}"
                }

                val walletAddress = solanaManager.getConnectedWallet()
                if (walletAddress == null) {
                    return@withContext "{\"code\": 401, \"msg\": \"Wallet not connected\"}"
                }

                // 2. Call API
                val params = JSONObject()
                params.put("wallet_address", walletAddress)
                params.put("signature", signature)
                params.put("message", message)
                params.put("item_id", itemId)
                params.put("amount", amount)

                val response = ApiClient.post("/shop_seeds/sell", params)
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.optInt("code") == 0) {
                        val data = jsonResponse.optJSONObject("data")
                        if (data != null && data.has("new_balance")) {
                            solanaManager.setCachedUserCoins(data.optLong("new_balance", 0))
                        }
                    }
                } catch (_: Exception) {
                }
                return@withContext response
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext "{\"code\": 500, \"msg\": \"${e.message}\"}"
            }
        }
    }
}
