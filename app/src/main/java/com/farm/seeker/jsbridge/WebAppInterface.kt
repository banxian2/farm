package com.farm.seeker.jsbridge

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.farm.seeker.solana.SolanaManager

import com.farm.seeker.game.FarmManager
import com.farm.seeker.game.InventoryManager
import com.farm.seeker.game.ShopManager
import com.farm.seeker.game.TaskManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import kotlinx.coroutines.runBlocking

/**
 * Interface to allow JavaScript to communicate with Android.
 */
class WebAppInterface(
    private val context: Context,
    private val solanaManager: SolanaManager? = null,
    private val farmManager: FarmManager? = null,
    private val onReturnHome: (() -> Unit)? = null,
    private val onSwipeRefreshEnabled: ((Boolean) -> Unit)? = null,
    private val taskManager: TaskManager? = null,
    private val shopManager: ShopManager? = null,
    private val inventoryManager: InventoryManager? = null
) {

    /**
     * Show a toast from the web page
     */
    @JavascriptInterface
    fun showToast(toast: String) {
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
    }

    /**
     * Login (Connect + Sign + Save)
     */
    @JavascriptInterface
    fun login(): String {
        if (solanaManager == null) return "Error: SolanaManager not initialized"
        
        return runBlocking {
            val result = solanaManager.login()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Login Result: $result", Toast.LENGTH_LONG).show()
            }
            result
        }
    }

    /**
     * Connect Wallet
     */
    @JavascriptInterface
    fun connectWallet(callbackId: String): String {
        if (solanaManager == null) return "Error: SolanaManager not initialized"
        
        return runBlocking {
            val result = solanaManager.connect()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Connect Result: $result", Toast.LENGTH_LONG).show()
            }
            result
        }
    }
    
    /**
     * Sign Message
     */
    @JavascriptInterface
    fun signMessage(messageBase64: String): String {
        if (solanaManager == null) return "Error: SolanaManager not initialized"
        return runBlocking {
            val result = solanaManager.signMessage(messageBase64)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Sign Result: $result", Toast.LENGTH_LONG).show()
            }
            result
        }
    }

    @JavascriptInterface
    fun verifySignature(signatureBase58: String, messageBase64: String): String {
        if (solanaManager == null) return "Error: SolanaManager not initialized"
        return runBlocking {
            val result = solanaManager.getAddressFromSignature(signatureBase58, messageBase64)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Verify Result: $result", Toast.LENGTH_LONG).show()
            }
            result
        }
    }

    @JavascriptInterface
    fun saveBillboard(content: String) {
        if (farmManager == null) return
        CoroutineScope(Dispatchers.Main).launch {
            val result = farmManager.updateBillboard(content)
            Toast.makeText(context, "Billboard Save Result: $result", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Send Transaction
     */
    @JavascriptInterface
    fun sendTransaction(transactionBase64: String): String {
        if (solanaManager == null) return "Error: SolanaManager not initialized"
        return runBlocking {
            val result = solanaManager.sendTransaction(transactionBase64)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Send Result: $result", Toast.LENGTH_LONG).show()
            }
            result
        }
    }

    /**
     * Transfer SOL (using official guide approach)
     */
    @JavascriptInterface
    fun transferSol(destinationBase58: String, lamports: String): String {
        if (solanaManager == null) return "Error: SolanaManager not initialized"
        return runBlocking {
            val amount = lamports.toLongOrNull()
            if (amount == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Invalid lamports", Toast.LENGTH_SHORT).show()
                }
                return@runBlocking "Error: Invalid lamports"
            }
            val result = solanaManager.transferSol(destinationBase58, amount)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Transfer Result: $result", Toast.LENGTH_LONG).show()
            }
            result
        }
    }
 

    @JavascriptInterface
    fun getTokenBalance(mintAddress: String): String {
        if (solanaManager == null) return "Error: SolanaManager not initialized"
        return runBlocking {
            solanaManager.getTokenBalance(mintAddress)
        }
    }

    @JavascriptInterface
    fun getUserInfoJson(): String {
        val prefs = context.getSharedPreferences("seeker_prefs", Context.MODE_PRIVATE)
        val nickname = prefs.getString("user_nickname", "Seeker")
        val avatarIndex = prefs.getInt("user_avatar_index", 0)
        android.util.Log.d("WebAppInterface", "getUserInfoJson: avatarIndex=$avatarIndex")
        val userId = prefs.getString("user_id", "")
        val level = prefs.getInt("user_level", 1)
        val billboard = prefs.getString("user_billboard", "")

        val json = org.json.JSONObject()
        json.put("nickname", nickname)
        json.put("avatarIndex", avatarIndex)
        json.put("userId", userId)
        json.put("level", level)
        json.put("billboard", billboard)
        return json.toString()
    }

    @JavascriptInterface
    fun getUserResourcesJson(): String {
        val prefs = context.getSharedPreferences("seeker_prefs", Context.MODE_PRIVATE)
        val coins = prefs.getLong("user_coins", 0)
        val gems = prefs.getLong("user_gems", 0)

        val json = org.json.JSONObject()
        json.put("coins", coins)
        json.put("gems", gems)
        return json.toString()
    }

    @JavascriptInterface
    fun getLandDataJson(): String {
        val prefs = context.getSharedPreferences("seeker_prefs", Context.MODE_PRIVATE)
        return prefs.getString("user_land_data", "[]") ?: "[]"
    }

    @JavascriptInterface
    fun isLoggedIn(): Boolean {
        return solanaManager?.isLoggedIn() == true
    }

    @JavascriptInterface
    fun returnHome() {
        CoroutineScope(Dispatchers.Main).launch {
            onReturnHome?.invoke()
        }
    }

    @JavascriptInterface
    fun setSwipeRefreshEnabled(enabled: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            onSwipeRefreshEnabled?.invoke(enabled)
        }
    }

    @JavascriptInterface
    fun fetchTasks(): String {
        if (taskManager == null) return "{ \"code\": 500, \"msg\": \"TaskManager not initialized\" }"
        return runBlocking {
            val result = taskManager.getTaskList()
            Log.d("WebAppInterface", "fetchTasks result: $result")
            result
        }
    }

    @JavascriptInterface
    fun fetchShopSeeds(): String {
        if (shopManager == null) return "{ \"code\": 500, \"msg\": \"ShopManager not initialized\" }"
        return runBlocking {
            val result = shopManager.getSeedList()
            Log.d("WebAppInterface", "fetchShopSeeds result: $result")
            if (result.startsWith("Error")) {
                val json = org.json.JSONObject()
                json.put("code", 500)
                json.put("msg", result)
                return@runBlocking json.toString()
            }
            result
        }
    }

    @JavascriptInterface
    fun buySeed(seedId: String, amount: Int): String {
        if (shopManager == null) return "{ \"code\": 500, \"msg\": \"ShopManager not initialized\" }"
        return runBlocking {
            val result = shopManager.buySeed(seedId, amount)
            Log.d("WebAppInterface", "buySeed result: $result")
            result
        }
    }

    @JavascriptInterface
    fun sellCrop(itemId: String, amount: Int): String {
        if (shopManager == null) return "{ \"code\": 500, \"msg\": \"ShopManager not initialized\" }"
        return runBlocking {
            val result = shopManager.sellCrop(itemId, amount)
            Log.d("WebAppInterface", "sellCrop result: $result")
            result
        }
    }

    @JavascriptInterface
    fun fetchInventory(): String {
        if (inventoryManager == null) return "{ \"code\": 500, \"msg\": \"InventoryManager not initialized\" }"
        return runBlocking {
            val result = inventoryManager.getInventoryList()
            Log.d("WebAppInterface", "fetchInventory result: $result")
            if (result.startsWith("Error")) {
                val json = org.json.JSONObject()
                json.put("code", 500)
                json.put("msg", result)
                return@runBlocking json.toString()
            }
            result
        }
    }

    @JavascriptInterface
    fun claimTask(taskId: Int): String {
        if (taskManager == null) return "{ \"code\": 500, \"msg\": \"TaskManager not initialized\" }"
        return runBlocking {
            val result = taskManager.claimTask(taskId)
            Log.d("WebAppInterface", "claimTask result: $result")
            result
        }
    }

    @JavascriptInterface
    fun fetchLandList(): String {
        if (farmManager == null) return "{ \"code\": 500, \"msg\": \"FarmManager not initialized\" }"
        return runBlocking {
            val result = farmManager.getLandList()
            Log.d("WebAppInterface", "fetchLandList result: $result")
            if (result.startsWith("Error")) {
                val json = org.json.JSONObject()
                json.put("code", 500)
                json.put("msg", result)
                return@runBlocking json.toString()
            }
            result
        }
    }

    @JavascriptInterface
    fun plantSeed(plotIndex: Int, seedId: String): String {
        if (farmManager == null) return "{ \"code\": 500, \"msg\": \"FarmManager not initialized\" }"
        return runBlocking {
            val result = farmManager.plant(plotIndex, seedId)
            Log.d("WebAppInterface", "plantSeed result: $result")
            result
        }
    }

    @JavascriptInterface
    fun waterPlant(plotIndex: Int): String {
        if (farmManager == null) return "{ \"code\": 500, \"msg\": \"FarmManager not initialized\" }"
        return runBlocking {
            val result = farmManager.water(plotIndex)
            Log.d("WebAppInterface", "waterPlant result: $result")
            result
        }
    }

    @JavascriptInterface
    fun harvestCrop(plotIndex: Int): String {
        if (farmManager == null) return "{ \"code\": 500, \"msg\": \"FarmManager not initialized\" }"
        return runBlocking {
            val result = farmManager.harvest(plotIndex)
            Log.d("WebAppInterface", "harvestCrop result: $result")
            result
        }
    }

    @JavascriptInterface
    fun shovelPlot(plotIndex: Int): String {
        if (farmManager == null) return "{ \"code\": 500, \"msg\": \"FarmManager not initialized\" }"
        return runBlocking {
            val result = farmManager.shovel(plotIndex)
            Log.d("WebAppInterface", "shovelPlot result: $result")
            result
        }
    }

    @JavascriptInterface
    fun getAuthDataJson(): String {
        val prefs = context.getSharedPreferences("seeker_prefs", Context.MODE_PRIVATE)
        val savedTimestamp = prefs.getLong("login_timestamp", 0)
        val currentTime = System.currentTimeMillis()
        val isValid = (currentTime - savedTimestamp) < 24 * 60 * 60 * 1000
        
        val walletAddress = prefs.getString("user_wallet", "")
        val signature = prefs.getString("login_signature", "")
        val message = prefs.getString("login_message", "")
        
        if (isValid && !walletAddress.isNullOrEmpty() && !signature.isNullOrEmpty() && !message.isNullOrEmpty()) {
             val json = org.json.JSONObject()
             json.put("wallet_address", walletAddress)
             json.put("signature", signature)
             json.put("message", message)
             return json.toString()
        }
        return "{}"
    }
}
