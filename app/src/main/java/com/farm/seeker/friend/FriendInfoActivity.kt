package com.farm.seeker.friend

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.farm.seeker.R
import com.farm.seeker.solana.SolanaManager
import com.farm.seeker.utils.AvatarUtils
import com.farm.seeker.utils.SwipeBackGestureListener
import kotlinx.coroutines.launch

class FriendInfoActivity : AppCompatActivity() {

    private lateinit var solanaManager: SolanaManager
    private lateinit var friendManager: FriendManager
    private var friendName: String = ""
    private var friendWalletAddress: String = ""
    private var friendLevel: Int = 0
    private var friendAvatarIndex: Int = 1
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_info)

        gestureDetector = GestureDetector(this, SwipeBackGestureListener(this))

        solanaManager = SolanaManager(this)
        friendManager = FriendManager(this, solanaManager)

        friendName = intent.getStringExtra("friend_name") ?: "Unknown"
        friendWalletAddress = intent.getStringExtra("friend_wallet_address") ?: ""
        friendLevel = intent.getIntExtra("friend_level", 0)
        friendAvatarIndex = intent.getIntExtra("friend_avatar_index", 1)

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
        findViewById<TextView>(R.id.tv_name).text = friendName
        
        val tvAddress = findViewById<TextView>(R.id.tv_address)
        tvAddress.text = friendWalletAddress
        tvAddress.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Wallet Address", friendWalletAddress)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Address copied", Toast.LENGTH_SHORT).show()
        }
        
        val levelText = if (friendLevel > 0) String.format("%02d", friendLevel) else "01"
        findViewById<TextView>(R.id.tv_level).text = levelText

        findViewById<ImageView>(R.id.img_avatar).setImageResource(AvatarUtils.getAvatarResId(friendAvatarIndex))

        val btnDelete = findViewById<Button>(R.id.btn_delete_friend)
        btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        val dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_confirm_delete, null)
        val dialog = AlertDialog.Builder(this, R.style.Theme_Seeker_Dialog_Transparent)
            .setView(dialogView)
            .create()

        val tvMessage = dialogView.findViewById<TextView>(R.id.tv_message)
        tvMessage.text = "Are you sure you want to remove $friendName from your friends?"

        dialogView.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()
            deleteFriend()
        }

        dialog.show()
    }

    private fun deleteFriend() {
        val progressBar = findViewById<View>(R.id.progressBar)
        val btnDelete = findViewById<View>(R.id.btn_delete_friend)
        
        progressBar.visibility = View.VISIBLE
        btnDelete.isEnabled = false
        
        lifecycleScope.launch {
            val result = friendManager.deleteFriend(friendWalletAddress)
            
            progressBar.visibility = View.GONE
            btnDelete.isEnabled = true
            
            if (result == "Success") {
                Toast.makeText(this@FriendInfoActivity, "Friend deleted", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@FriendInfoActivity, result, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
