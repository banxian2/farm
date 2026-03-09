package com.farm.seeker.me

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.farm.seeker.R
import com.farm.seeker.utils.AvatarUtils
import com.farm.seeker.utils.SwipeBackGestureListener

import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.farm.seeker.solana.SolanaManager
import org.json.JSONObject
import com.farm.seeker.network.ApiClient
import android.content.Context

class AvatarSelectionActivity : AppCompatActivity() {

    private lateinit var gestureDetector: GestureDetector
    private lateinit var solanaManager: SolanaManager
    private var selectedAvatarResId: Int = 0

    private val maleAvatars = AvatarUtils.maleAvatars
    private val femaleAvatars = AvatarUtils.femaleAvatars

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_avatar_selection)
        
        solanaManager = SolanaManager(this)

        gestureDetector = GestureDetector(this, SwipeBackGestureListener(this))

        selectedAvatarResId = intent.getIntExtra("current_avatar_res_id", 0)

        setupUI()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null && gestureDetector.onTouchEvent(ev)) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun setupUI() {
        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }

        val rvMale = findViewById<RecyclerView>(R.id.rv_male_avatars)
        val rvFemale = findViewById<RecyclerView>(R.id.rv_female_avatars)
        val btnSave = findViewById<Button>(R.id.btn_save)

        setupRecyclerView(rvMale, maleAvatars)
        setupRecyclerView(rvFemale, femaleAvatars)
        
        btnSave.setOnClickListener {
            saveAvatar()
        }
    }
    
    private fun saveAvatar() {
        if (selectedAvatarResId == 0) {
             Toast.makeText(this, "Please select an avatar", Toast.LENGTH_SHORT).show()
             return
        }
        
        val avatarIndex = AvatarUtils.getAvatarIndex(selectedAvatarResId)
        
        Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show()
        findViewById<Button>(R.id.btn_save).isEnabled = false
        
        lifecycleScope.launch {
            val result = try {
                 if (!solanaManager.isLoggedIn()) {
                     "Error: Not logged in"
                 } else {
                     val walletAddress = solanaManager.getConnectedWallet()
                     if (walletAddress == null) {
                         "Error: Wallet info missing"
                     } else {
                         // Doc requires message "Update Avatar"
                         val messageContent = "Update Avatar"
                         val (signature, message) = solanaManager.getAuthData(messageContent)
                         
                         if (signature.startsWith("Error")) {
                             signature
                         } else {
                             val params = JSONObject().apply {
                                 put("wallet_address", walletAddress)
                                 put("avatar_index", avatarIndex)
                                 put("message", message)
                                 put("signature", signature)
                             }
                             
                             val apiResponse = ApiClient.post("/user_profile/update_avatar", params)
                             
                             if (apiResponse.startsWith("Error")) {
                                 apiResponse
                             } else {
                                 val jsonResponse = JSONObject(apiResponse)
                                 val code = jsonResponse.optInt("code")
                                 if (code == 0) {
                                     // Update SharedPreferences
                                     val prefs = getSharedPreferences("seeker_prefs", Context.MODE_PRIVATE)
                                     prefs.edit().putInt("user_avatar_index", avatarIndex).apply()
                                     "Success"
                                 } else {
                                     "Error: ${jsonResponse.optString("msg")}"
                                 }
                             }
                         }
                     }
                 }
            } catch (e: Exception) {
                e.printStackTrace()
                "Error: ${e.message}"
            }

            if (result == "Success") {
                Toast.makeText(this@AvatarSelectionActivity, "Avatar Updated!", Toast.LENGTH_SHORT).show()
                val resultIntent = Intent()
                resultIntent.putExtra("selected_avatar_res_id", selectedAvatarResId)
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this@AvatarSelectionActivity, result, Toast.LENGTH_SHORT).show()
                findViewById<Button>(R.id.btn_save).isEnabled = true
            }
        }
    }

    private fun setupRecyclerView(recyclerView: RecyclerView, avatars: List<Int>) {
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = AvatarAdapter(avatars, selectedAvatarResId) { avatarResId ->
            onAvatarSelected(avatarResId)
        }
    }

    private fun updateAdapters() {
        val rvMale = findViewById<RecyclerView>(R.id.rv_male_avatars)
        val rvFemale = findViewById<RecyclerView>(R.id.rv_female_avatars)

        // Re-create adapters to refresh selection state (simple approach)
        rvMale.adapter = AvatarAdapter(maleAvatars, selectedAvatarResId) { resId -> onAvatarSelected(resId) }
        rvFemale.adapter = AvatarAdapter(femaleAvatars, selectedAvatarResId) { resId -> onAvatarSelected(resId) }
    }

    private fun onAvatarSelected(avatarResId: Int) {
        selectedAvatarResId = avatarResId
        updateAdapters()
    }
}
