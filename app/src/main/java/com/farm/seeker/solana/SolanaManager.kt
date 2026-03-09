package com.farm.seeker.solana

import android.net.Uri
import android.util.Base64
import androidx.activity.ComponentActivity
import com.solana.mobilewalletadapter.clientlib.*
import com.solana.rpc.SolanaRpcClient
import com.solana.networking.KtorNetworkDriver
import com.solana.programs.*
import com.solana.publickey.SolanaPublicKey
import com.solana.publickey.ProgramDerivedAddress
import com.solana.transaction.*
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import java.security.MessageDigest
import java.util.UUID

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.solana.rpccore.JsonRpc20Request
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import com.solana.serializers.SolanaResponseSerializer
import com.farm.seeker.network.ApiClient
import org.json.JSONObject

import android.widget.Toast
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Button
import android.widget.EditText
import android.view.View
import androidx.appcompat.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.farm.seeker.R

class SolanaManager(private val activity: ComponentActivity) {

    private var connectedAccount: ByteArray? = null
    private val sender = ActivityResultSender(activity)
    
    // Last entered invite code during sign message flow
    private var lastInviteCode: String? = null

    // SPL Token Program ID
    private val TOKEN_PROGRAM_ID = SolanaPublicKey(Base58.decode("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"))
    // Associated Token Program ID
    private val ASSOCIATED_TOKEN_PROGRAM_ID = SolanaPublicKey(Base58.decode("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"))

    private val walletAdapter: MobileWalletAdapter by lazy {
        MobileWalletAdapter(
            connectionIdentity = ConnectionIdentity(
                identityUri = Uri.parse("https://www.skr-farm.live"),
                iconUri = Uri.parse("favicon.ico"),
                identityName = "Seeker Game"
            )
        )
    }

    private val prefs = activity.getSharedPreferences("seeker_prefs", android.content.Context.MODE_PRIVATE)

    fun isLoggedIn(): Boolean {
        return !prefs.getString("user_wallet", null).isNullOrEmpty() && isSessionValid()
    }

    fun getConnectedWallet(): String? {
        return prefs.getString("user_wallet", null)
    }

    private fun getOrRestoreConnectedAccount(): ByteArray? {
        if (connectedAccount != null) return connectedAccount
        
        val savedWallet = prefs.getString("user_wallet", null)
        if (!savedWallet.isNullOrEmpty()) {
            return try {
                val bytes = Base58.decode(savedWallet)
                connectedAccount = bytes
                bytes
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    fun setCachedUserCoins(coins: Long) {
        prefs.edit().putLong("user_coins", coins).apply()
    }

    suspend fun updateUserProfile(): String {
        return updateUserProfileWithParams(null)
    }


    private suspend fun updateUserProfileWithParams(extraParams: JSONObject?): String {
        if (!isLoggedIn()) return "Error: Not logged in"

        val walletAddress = getConnectedWallet() ?: return "Error: Wallet info missing"
        
        // Use "Login to Seeker" message to try reuse existing signature
        val (signature, message) = getAuthData("Login to Seeker")
        if (signature.startsWith("Error")) return signature

        val params = JSONObject().apply {
            put("wallet_address", walletAddress)
            put("message", message)
            put("signature", signature)
            
            // Merge extra params
            extraParams?.let {
                val keys = it.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, it.get(key))
                }
            }
        }
        val apiResponse = ApiClient.post("/user_profile", params)
        
        if (apiResponse.startsWith("Error")) {
            return apiResponse
        }

        try {
            val jsonResponse = JSONObject(apiResponse)
            val code = jsonResponse.optInt("code")
            if (code == 0) {
                val data = jsonResponse.getJSONObject("data")
                val userId = data.optInt("user_id").toString()
                val nickname = data.optString("nickname")
                val avatarIndex = data.optInt("avatar_index")
                val level = data.optInt("level")
                val exp = data.optLong("exp", 0)
                val coins = data.optLong("coins", 0)
                val gems = data.optLong("gems", 0)
                val landData = data.optString("land_data", "[]")
                val billboard = data.optString("billboard", "")
                
                prefs.edit()
                    .putString("user_wallet", walletAddress)
                    .putString("user_id", userId)
                    .putString("user_nickname", nickname)
                    .putInt("user_avatar_index", avatarIndex)
                    .putInt("user_level", level)
                    .putLong("user_exp", exp)
                    .putLong("user_coins", coins)
                    .putLong("user_gems", gems)
                    .putString("user_land_data", landData)
                    .putString("user_billboard", billboard)
                    .apply()
                
                return "Success"
            } else {
                 return "Error: ${jsonResponse.optString("msg")}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error: Failed to parse server response"
        }
    }

    private suspend fun showConfirmationDialog(title: String, message: String, showInput: Boolean = false): Pair<Boolean, String> = suspendCancellableCoroutine { cont ->
        activity.runOnUiThread {
            val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_confirmation, null)
            val tvTitle = dialogView.findViewById<TextView>(R.id.tv_title)
            val tvMessage = dialogView.findViewById<TextView>(R.id.tv_message)
            val etInviteCode = dialogView.findViewById<EditText>(R.id.et_invite_code)
            val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm)
            val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)

            tvTitle.text = title
            tvMessage.text = message
            
            if (showInput) {
                etInviteCode.visibility = View.VISIBLE
            } else {
                etInviteCode.visibility = View.GONE
            }

            val dialog = AlertDialog.Builder(activity)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            btnConfirm.setOnClickListener {
                val inputText = etInviteCode.text.toString().trim()
                dialog.dismiss()
                if (cont.isActive) cont.resume(Pair(true, inputText))
            }

            btnCancel.setOnClickListener {
                dialog.dismiss()
                if (cont.isActive) cont.resume(Pair(false, ""))
            }

            dialog.show()
        }
    }

    fun logout() {
        prefs.edit()
            .remove("user_wallet")
            .remove("login_signature")
            .remove("login_message")
            .remove("login_timestamp")
            .remove("user_id")
            .remove("user_nickname")
            .remove("user_avatar_index")
            .remove("user_level")
            .remove("user_coins")
            .remove("user_gems")
            .remove("user_land_data")
            .remove("user_billboard")
            .apply()
        connectedAccount = null
    }

    suspend fun fetchUserProfile(): String {
        // 1. Check Login & Session
        if (!isLoggedIn()) return "Error: Not logged in"
        if (!isSessionValid()) return "Error: Session expired"

        val walletAddress = getConnectedWallet() ?: return "Error: Wallet not found"
        val signature = prefs.getString("login_signature", "") ?: return "Error: No signature"
        val message = prefs.getString("login_message", "") ?: return "Error: No message"

        // 2. Call API
        val params = JSONObject().apply {
            put("wallet_address", walletAddress)
            put("message", message)
            put("signature", signature)
        }
        
        val apiResponse = ApiClient.post("/user_profile/show", params)

        if (apiResponse.startsWith("Error")) {
            return apiResponse
        }

        try {
            val jsonResponse = JSONObject(apiResponse)
            val code = jsonResponse.optInt("code")
            if (code == 0) {
                val data = jsonResponse.getJSONObject("data")
                val userId = data.optInt("user_id").toString()
                val nickname = data.optString("nickname")
                val avatarIndex = data.optInt("avatar_index")
                val level = data.optInt("level")
                val exp = data.optLong("exp", 0)
                val coins = data.optLong("coins", 0)
                val gems = data.optLong("gems", 0)
                val landData = data.optString("land_data", "[]")
                val billboard = data.optString("billboard", "")
                
                // 3. Save to App
                val success = prefs.edit()
                    .putString("user_id", userId)
                    .putString("user_nickname", nickname)
                    .putInt("user_avatar_index", avatarIndex)
                    .putInt("user_level", level)
                    .putLong("user_exp", exp)
                    .putLong("user_coins", coins)
                    .putLong("user_gems", gems)
                    .putString("user_land_data", landData)
                    .putString("user_billboard", billboard)
                    .commit()
                
                return if (success) "Success" else "Error: Failed to save data"
            } else {
                return "Error: ${jsonResponse.optString("msg")}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error: Failed to parse server response"
        }
    }

    // Function moved to FriendManager


    suspend fun login(): String {
        // 1. Connect
        activity.runOnUiThread { Toast.makeText(activity, "Connecting Wallet...", Toast.LENGTH_SHORT).show() }
        val connectResult = connect()
        if (connectResult.startsWith("Error")) return connectResult
        
        val walletAddress = connectResult
        
        // 2. Sign Message
        activity.runOnUiThread { Toast.makeText(activity, "Please Sign Message...", Toast.LENGTH_SHORT).show() }
        val timestamp = System.currentTimeMillis()
        val message = "Login to Seeker"
        val messageBase64 = Base64.encodeToString(message.toByteArray(), Base64.NO_WRAP)
        
        // Pass allowInviteCode=true for login
        val signResult = signMessage(messageBase64, allowInviteCode = true)
        if (signResult.startsWith("Error")) return signResult

        // 3. Call API to Verify and Register/Login
        activity.runOnUiThread { Toast.makeText(activity, "Verifying...", Toast.LENGTH_SHORT).show() }
        val params = JSONObject().apply {
            put("wallet_address", walletAddress)
            put("message", message)
            put("signature", signResult)
            // Add invite code if present
            if (!lastInviteCode.isNullOrEmpty()) {
                put("invite_code", lastInviteCode)
            }
        }
        val apiResponse = ApiClient.post("/user_profile", params)
        
        if (apiResponse.startsWith("Error")) {
            return apiResponse
        }

        try {
            val jsonResponse = JSONObject(apiResponse)
            val code = jsonResponse.optInt("code")
            if (code == 0) {
                val data = jsonResponse.getJSONObject("data")
                val userId = data.optInt("user_id").toString()
                val nickname = data.optString("nickname")
                val avatarIndex = data.optInt("avatar_index")
                val level = data.optInt("level")
                val exp = data.optLong("exp", 0)
                val coins = data.optLong("coins", 0)
                val gems = data.optLong("gems", 0)
                val landData = data.optString("land_data", "[]") // Assume JSON string
                val billboard = data.optString("billboard", "")
                
                // 4. Save to App
                val success = prefs.edit()
                    .putString("user_wallet", walletAddress)
                    .putString("login_signature", signResult)
                    .putString("login_message", message)
                    .putLong("login_timestamp", timestamp)
                    .putString("user_id", userId)
                    .putString("user_nickname", nickname)
                    .putInt("user_avatar_index", avatarIndex)
                    .putInt("user_level", level)
                    .putLong("user_exp", exp)
                    .putLong("user_coins", coins)
                    .putLong("user_gems", gems)
                    .putString("user_land_data", landData)
                    .putString("user_billboard", billboard)
                    .commit()
                
                if (success) {
                    return "Success"
                } else {
                    return "Error: Failed to save login data"
                }
            } else {
                 return "Error: ${jsonResponse.optString("msg")}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error: Failed to parse server response"
        }
    }



    private fun isSessionValid(): Boolean {
        val savedTimestamp = prefs.getLong("login_timestamp", 0)
        val currentTime = System.currentTimeMillis()
        return (currentTime - savedTimestamp) < 24 * 60 * 60 * 1000
    }

    suspend fun getAuthData(actionMessage: String): Pair<String, String> {
        // 1. Check Session Validity (24h)
        val savedSignature = prefs.getString("login_signature", null)
        val savedMessage = prefs.getString("login_message", null)
        
        if (isSessionValid() && savedSignature != null && savedMessage != null) {
            return Pair(savedSignature, savedMessage)
        }

        // 2. Sign New Message
        val messageBase64 = Base64.encodeToString(actionMessage.toByteArray(), Base64.NO_WRAP)
        activity.runOnUiThread { Toast.makeText(activity, "Please Sign to Continue...", Toast.LENGTH_SHORT).show() }
        
        val signResult = signMessage(messageBase64)
        if (signResult.startsWith("Error")) {
            return Pair(signResult, actionMessage) // Propagate error
        }
        
        return Pair(signResult, actionMessage)
    }

    // Function moved to MeManager

    // Function moved to FarmManager

    // Functions moved to FriendManager


    suspend fun connect(): String {
        val (confirmed, _) = showConfirmationDialog("Connect Wallet", "Do you want to connect your wallet to Seeker?")
        if (!confirmed) return "Error: User cancelled connection"

        return when (val result = walletAdapter.connect(sender)) {
            is TransactionResult.Success -> {
                val publicKeyBytes = result.authResult.accounts.firstOrNull()?.publicKey
                if (publicKeyBytes != null) {
                    connectedAccount = publicKeyBytes
                    Base58.encode(publicKeyBytes)
                } else {
                    "Error: No public key returned"
                }
            }
            is TransactionResult.NoWalletFound -> "Error: No wallet found"
            is TransactionResult.Failure -> "Error: ${result.e.message}"
        }
    }

    suspend fun transferSol(destinationBase58: String, lamports: Long): String {
        return when (val result = walletAdapter.transact(sender) { authResult ->
            val userAccountAddress = SolanaPublicKey(authResult.accounts.first().publicKey)
            val rpcClient = SolanaRpcClient("https://api.mainnet-beta.solana.com", KtorNetworkDriver())
            val blockhashResponse = rpcClient.getLatestBlockhash()
            val blockhash = blockhashResponse.result?.blockhash
                ?: throw IllegalStateException("Failed to fetch blockhash")
            
            val transferTx = buildTransferTransaction(
                blockhash,
                userAccountAddress,
                SolanaPublicKey(Base58.decode(destinationBase58)),
                lamports
            )
            val sendResult = signAndSendTransactions(arrayOf(transferTx.serialize()))
            sendResult.signatures.first()
        }) {
            is TransactionResult.Success -> {
                val signatureBytes = result.payload
                if (signatureBytes != null) {
                    Base58.encode(signatureBytes)
                } else {
                    "Error: No signature returned"
                }
            }
            is TransactionResult.NoWalletFound -> "Error: No wallet found"
            is TransactionResult.Failure -> "Error: ${result.e.message}"
        }
    }

    suspend fun signMessage(messageBase64: String, allowInviteCode: Boolean = false): String {
        val currentAccount = getOrRestoreConnectedAccount() ?: return "Error: Wallet not connected"
        val messageBytes = Base64.decode(messageBase64, Base64.NO_WRAP)
        
        // Optimize: If signing the exact same "Login" message and session is valid, reuse signature
        val loginMessage = prefs.getString("login_message", "")
        val loginSignature = prefs.getString("login_signature", "")
        if (!loginMessage.isNullOrEmpty() && !loginSignature.isNullOrEmpty() && isSessionValid()) {
            val loginMessageBase64 = Base64.encodeToString(loginMessage.toByteArray(), Base64.NO_WRAP)
            // Compare normalized Base64 strings (trim newlines)
            if (messageBase64.trim() == loginMessageBase64.trim()) {
                return loginSignature
            }
        }

        if (allowInviteCode) {
            lastInviteCode = null
        }

        val (confirmed, inviteCode) = showConfirmationDialog("Sign Message", "Do you want to sign this message to verify your identity?", showInput = allowInviteCode)
        if (!confirmed) return "Error: User cancelled signing"

        if (allowInviteCode) {
            lastInviteCode = inviteCode
        }
        
        return when (val result = walletAdapter.transact(sender) {
            val signResult = signMessages(arrayOf(messageBytes), arrayOf(currentAccount))
            signResult.signedPayloads.first()
        }) {
            is TransactionResult.Success -> {
                val signatureBytes = result.payload
                if (signatureBytes != null) {
                    Base58.encode(signatureBytes)
                } else {
                    "Error: No signature returned"
                }
            }
            is TransactionResult.NoWalletFound -> "Error: No wallet found"
            is TransactionResult.Failure -> "Error: ${result.e.message}"
        }
    }

    suspend fun sendTransaction(transactionBase64: String): String {
        val transactionBytes = Base64.decode(transactionBase64, Base64.NO_WRAP)

        return when (val result = walletAdapter.transact(sender) {
            val sendResult = signAndSendTransactions(arrayOf(transactionBytes))
            sendResult.signatures.first()
        }) {
            is TransactionResult.Success -> {
                val signatureBytes = result.payload
                if (signatureBytes != null) {
                    Base58.encode(signatureBytes)
                } else {
                    "Error: No signature returned"
                }
            }
            is TransactionResult.NoWalletFound -> "Error: No wallet found"
            is TransactionResult.Failure -> "Error: ${result.e.message}"
        }
    }

    fun getAddressFromSignature(signatureBase58: String, messageBase64: String): String {
        val account = getOrRestoreConnectedAccount() ?: return "Error: Wallet not connected"
        
        return try {
            val signatureBytes = Base58.decode(signatureBase58)
            val messageBytes = Base64.decode(messageBase64, Base64.NO_WRAP)
            
            // Ed25519 Verify
            val spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)
            val pubKey = EdDSAPublicKey(EdDSAPublicKeySpec(account, spec))
            val engine = EdDSAEngine(MessageDigest.getInstance(spec.hashAlgorithm))
            engine.initVerify(pubKey)
            engine.update(messageBytes)
            
            if (engine.verify(signatureBytes)) {
                Base58.encode(account)
            } else {
                "Error: Signature verification failed"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    suspend fun getTokenBalance(mintAddress: String): String {
        val userAccount = getOrRestoreConnectedAccount() ?: return "Error: Wallet not connected"

        return withContext(Dispatchers.IO) {
            try {
                val rpcClient = SolanaRpcClient("https://api.mainnet-beta.solana.com", KtorNetworkDriver())
                val userPublicKey = SolanaPublicKey(userAccount)

                // Handle SOL request
                if (mintAddress.equals("SOL", ignoreCase = true)) {
                    val balanceResponse = rpcClient.getBalance(userPublicKey)
                    val lamports = balanceResponse.result ?: 0L
                    return@withContext (lamports.toDouble() / 1_000_000_000.0).toString()
                }

                val mintPublicKey = try {
                    SolanaPublicKey(Base58.decode(mintAddress))
                } catch (e: Exception) {
                    return@withContext "Error: Invalid mint address"
                }

                // Derive ATA
                val ata = ProgramDerivedAddress.find(
                    listOf(userPublicKey.bytes, TOKEN_PROGRAM_ID.bytes, mintPublicKey.bytes),
                    ASSOCIATED_TOKEN_PROGRAM_ID
                ).getOrThrow()

                android.util.Log.d("SolanaManager", "Derived ATA: ${ata.base58()}")

                val response = rpcClient.makeRequest(
                    TokenAccountBalanceRequest(ata),
                    SolanaResponseSerializer(TokenAmount.serializer())
                )

                response.result?.uiAmountString ?: "0"
            } catch (e: Exception) {
                e.printStackTrace()
                // Often RPC throws if account not found
                "0"
            }
        }
    }

    // Function moved to FriendManager


    private fun buildTransferTransaction(
        blockhash: String,
        fromPublicKey: SolanaPublicKey,
        toPublicKey: SolanaPublicKey,
        lamports: Long
    ): Transaction {
        val transferTxMessage = Message.Builder()
            .addInstruction(
                SystemProgram.transfer(
                    fromPublicKey,
                    toPublicKey,
                    lamports
                )
            )
            .setRecentBlockhash(blockhash)
            .build()

        return Transaction(transferTxMessage)
    }
}

@kotlinx.serialization.Serializable
data class TokenAmount(
    val amount: String,
    val decimals: Int,
    val uiAmount: Double?,
    val uiAmountString: String?
)

class TokenAccountBalanceRequest(
    address: SolanaPublicKey,
    commitment: com.solana.rpc.Commitment = com.solana.rpc.Commitment.CONFIRMED,
    requestId: String? = null
) : JsonRpc20Request(
    method = "getTokenAccountBalance",
    params = buildJsonArray {
        add(address.base58())
        addJsonObject {
            put("commitment", commitment.value)
        }
    },
    id = requestId ?: UUID.randomUUID().toString()
)
