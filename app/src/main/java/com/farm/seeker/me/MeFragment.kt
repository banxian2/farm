package com.farm.seeker.me

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.content.Intent
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import com.farm.seeker.MainActivity
import com.farm.seeker.R
import com.farm.seeker.solana.SolanaManager
import com.farm.seeker.utils.AvatarUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class MeFragment : Fragment() {

    private lateinit var solanaManager: SolanaManager
    private lateinit var meManager: MeManager
    private lateinit var prefs: SharedPreferences
    
    private lateinit var tvNickname: TextView
    private lateinit var tvLevel: TextView
    private lateinit var tvExp: TextView
    private lateinit var imgAvatar: ImageView
    private lateinit var tvId: TextView
    private lateinit var tvWallet: TextView
    
    private var currentAvatarResId = R.drawable.ic_me
    private var shouldSkipFetch = false

    private val avatarSelectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Data already updated in AvatarSelectionActivity
            loadUserData()
            shouldSkipFetch = true
            Toast.makeText(context, "Avatar Updated", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_me, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Init SharedPreferences
        prefs = requireContext().getSharedPreferences("seeker_prefs", Context.MODE_PRIVATE)
        
        // Init SolanaManager
        if (requireActivity() is MainActivity) {
            val mainActivity = requireActivity() as MainActivity
            solanaManager = mainActivity.solanaManager
            meManager = mainActivity.meManager
        }
        
        // Bind Views
        tvNickname = view.findViewById(R.id.text_nickname)
        tvLevel = view.findViewById(R.id.text_level)
        tvExp = view.findViewById(R.id.text_exp)
        imgAvatar = view.findViewById(R.id.image_avatar)
        tvId = view.findViewById(R.id.text_id)
        tvWallet = view.findViewById(R.id.text_wallet)
        
        // Load Data
        loadUserData()
        
        // Listeners
        view.findViewById<View>(R.id.btn_edit_nickname).setOnClickListener { showEditNicknameDialog() }
        view.findViewById<View>(R.id.layout_nickname).setOnClickListener { showEditNicknameDialog() }
        
        view.findViewById<View>(R.id.btn_edit_avatar).setOnClickListener { changeAvatar() }
        imgAvatar.setOnClickListener { changeAvatar() }
        
        view.findViewById<View>(R.id.btn_copy_id).setOnClickListener { copyUserId() }
        tvWallet.setOnClickListener { copyWalletAddress() }
        
        // Invite Friends
        view.findViewById<View>(R.id.btn_invite_friends).setOnClickListener {
            val intent = Intent(activity, InviteFriendsActivity::class.java)
            startActivity(intent)
        }
        
        // Settings
        view.findViewById<View>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        setupLoginButton(view)
    }
    
    override fun onResume() {
        super.onResume()
        checkLoginState()
        if (shouldSkipFetch) {
            shouldSkipFetch = false
            return
        }
        fetchUserInfo()
    }

    private fun fetchUserInfo() {
        if (!::solanaManager.isInitialized || !solanaManager.isLoggedIn()) return

        lifecycleScope.launch {
            val result = solanaManager.fetchUserProfile()
            if (result == "Success") {
                loadUserData()
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            checkLoginState()
            loadUserData()
        }
    }
    
    fun refreshData() {
        checkLoginState()
        loadUserData()
    }
    
    private fun checkLoginState() {
        if (!::solanaManager.isInitialized) return
        val isLoggedIn = solanaManager.isLoggedIn()
        
        val cardProfile = view?.findViewById<View>(R.id.card_profile)
        val layoutLogin = view?.findViewById<View>(R.id.layout_login_required)
        val btnSettings = view?.findViewById<View>(R.id.btn_settings)
        val btnInviteFriends = view?.findViewById<View>(R.id.btn_invite_friends)
        
        if (isLoggedIn) {
            cardProfile?.visibility = View.VISIBLE
            layoutLogin?.visibility = View.GONE
            btnSettings?.visibility = View.VISIBLE
            btnInviteFriends?.visibility = View.VISIBLE
            
            // Update wallet text
            tvWallet.text = solanaManager.getConnectedWallet() ?: "Connected"
        } else {
            cardProfile?.visibility = View.GONE
            layoutLogin?.visibility = View.VISIBLE
            btnSettings?.visibility = View.GONE
            btnInviteFriends?.visibility = View.GONE
        }
    }
    
    private fun setupLoginButton(view: View) {
        val btnLogin = view.findViewById<Button>(R.id.btn_login_me)
        btnLogin.setOnClickListener {
            connectWallet()
        }
    }
    
    private fun loadUserData() {
        // Nickname
        var nickname = prefs.getString("user_nickname", "Seeker")
        if (nickname.isNullOrEmpty()) {
            nickname = "Seeker"
        }
        tvNickname.text = nickname
        
        // Level & Exp
        val level = prefs.getInt("user_level", 1)
        val exp = prefs.getLong("user_exp", 0)
        
        tvLevel.text = "Lv.$level"
        tvExp.text = "XP: $exp"
        
        // Avatar
        val avatarIndex = prefs.getInt("user_avatar_index", 0)
        currentAvatarResId = AvatarUtils.getAvatarResId(avatarIndex)
        imgAvatar.setImageResource(currentAvatarResId)
        
        // ID
        var userId = prefs.getString("user_id", null)
        if (userId == null) {
            userId = UUID.randomUUID().toString().substring(0, 8).uppercase()
            prefs.edit().putString("user_id", userId).apply()
        }
        tvId.text = userId
        
        // Wallet (Load cached if available)
        val cachedWallet = prefs.getString("user_wallet", null)
        if (cachedWallet != null) {
            tvWallet.text = cachedWallet
        }
    }
    
    private fun showEditNicknameDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_nickname, null)
        val etNickname = dialogView.findViewById<EditText>(R.id.et_nickname)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_save)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)

        etNickname.setText(tvNickname.text)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        btnSave.setOnClickListener {
            val newName = etNickname.text.toString().trim()
            if (newName.isEmpty()) {
                Toast.makeText(context, "Nickname cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!solanaManager.isLoggedIn()) {
                Toast.makeText(context, "Please Login First", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val result = meManager.updateNickname(newName)
                if (result == "Success") {
                    tvNickname.text = newName
                    Toast.makeText(context, "Nickname Updated", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    
    private fun changeAvatar() {
        val intent = Intent(requireContext(), AvatarSelectionActivity::class.java)
        intent.putExtra("current_avatar_res_id", currentAvatarResId)
        avatarSelectionLauncher.launch(intent)
    }
    
    private fun connectWallet() {
        if (!::solanaManager.isInitialized) return

        val progressBar = view?.findViewById<View>(R.id.progressBar)
        val btnLogin = view?.findViewById<View>(R.id.btn_login_me)
        
        progressBar?.visibility = View.VISIBLE
        btnLogin?.visibility = View.GONE
        
        lifecycleScope.launch {
            val result = solanaManager.login()

            progressBar?.visibility = View.GONE

            if (result == "Success") {
                val address = solanaManager.getConnectedWallet()
                tvWallet.text = address
                Toast.makeText(context, "Login Success", Toast.LENGTH_SHORT).show()
                
                // Refresh data and UI
                loadUserData()
                checkLoginState()
                
                if (!solanaManager.isLoggedIn()) {
                    Toast.makeText(context, "Error: Login verification failed", Toast.LENGTH_SHORT).show()
                }
            } else {
                btnLogin?.visibility = View.VISIBLE
                Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun copyUserId() {
        val userId = tvId.text.toString()
        if (userId == "Loading..." || userId.isEmpty()) {
            Toast.makeText(context, "No ID to copy", Toast.LENGTH_SHORT).show()
            return
        }
        
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("User ID", userId)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "ID copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun copyWalletAddress() {
        val address = tvWallet.text.toString()
        if (address == "Not Connected" || address.isEmpty()) {
            return
        }

        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Wallet Address", address)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Address copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}
