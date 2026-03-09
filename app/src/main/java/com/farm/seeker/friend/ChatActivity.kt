package com.farm.seeker.friend

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.farm.seeker.R
import com.farm.seeker.solana.SolanaManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import android.view.GestureDetector
import android.view.MotionEvent
import com.farm.seeker.utils.SwipeBackGestureListener
import kotlin.math.abs

class ChatActivity : AppCompatActivity() {

    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private lateinit var rvChat: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var layoutLoading: View
    private lateinit var solanaManager: SolanaManager
    private lateinit var chatManager: ChatManager
    private var friendWalletAddress: String = ""
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        gestureDetector = GestureDetector(this, SwipeBackGestureListener(this))

        solanaManager = SolanaManager(this)
        chatManager = ChatManager(this, solanaManager)

        val friendName = intent.getStringExtra("friend_name") ?: "Friend"
        friendWalletAddress = intent.getStringExtra("friend_wallet_address") ?: ""
        val friendLevel = intent.getIntExtra("friend_level", 0)
        val friendAvatarIndex = intent.getIntExtra("friend_avatar_index", 1)
        
        findViewById<TextView>(R.id.tv_title).text = friendName
        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }

        // Setup RecyclerView first
        rvChat = findViewById(R.id.rv_chat)
        etInput = findViewById(R.id.et_input)
        layoutLoading = findViewById(R.id.layout_loading)
        
        setupRecyclerView(friendLevel, friendAvatarIndex)
        setupInput()
        
        if (friendWalletAddress.isNotEmpty()) {
            loadChatHistory()
        } else {
            Toast.makeText(this, "Error: Invalid Friend Address", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView(friendLevel: Int, friendAvatarIndex: Int) {
        val prefs = getSharedPreferences("seeker_prefs", MODE_PRIVATE)
        val myAvatarIndex = prefs.getInt("user_avatar_index", 1)

        // Pass click listener to adapter
        adapter = MessageAdapter(messages, friendAvatarIndex, myAvatarIndex) {
            val intent = android.content.Intent(this, FriendInfoActivity::class.java)
            intent.putExtra("friend_name", intent.getStringExtra("friend_name") ?: "Friend")
            intent.putExtra("friend_wallet_address", friendWalletAddress)
            intent.putExtra("friend_level", friendLevel)
            intent.putExtra("friend_avatar_index", friendAvatarIndex)
            startActivity(intent)
        }
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter
    }

    private fun loadChatHistory() {
        lifecycleScope.launch {
            layoutLoading.visibility = View.VISIBLE
            val (history, error) = chatManager.getChatHistory(friendWalletAddress)
            layoutLoading.visibility = View.GONE
            if (error != null) {
                Toast.makeText(this@ChatActivity, error, Toast.LENGTH_SHORT).show()
            } else {
                messages.clear()
                messages.addAll(history)
                adapter.notifyDataSetChanged()
                if (messages.isNotEmpty()) {
                    rvChat.scrollToPosition(messages.size - 1)
                }
            }
        }
    }

    private fun setupInput() {
        findViewById<ImageView>(R.id.btn_send).setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text, MessageType.TEXT)
                etInput.text.clear()
            }
        }

        findViewById<ImageView>(R.id.btn_more).setOnClickListener {
            showActionDialog()
        }
    }

    private fun connectWallet() {
        CoroutineScope(Dispatchers.Main).launch {
            val result = solanaManager.connect()
            Toast.makeText(this@ChatActivity, "Wallet: $result", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showActionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_chat_actions, null)
        val dialog = AlertDialog.Builder(this, R.style.Theme_Seeker_Dialog_Transparent)
            .setView(dialogView)
            .create()

        dialogView.findViewById<View>(R.id.btn_action_seed).setOnClickListener {
            dialog.dismiss()
            showSendSeedDialog()
        }

        // Coin action removed

        dialogView.findViewById<View>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showSendSeedDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_inventory, null)
        val dialog = AlertDialog.Builder(this, R.style.Theme_Seeker_Dialog_Transparent)
            .setView(dialogView)
            .create()

        val rvInventory = dialogView.findViewById<RecyclerView>(R.id.rv_inventory)
        val btnClose = dialogView.findViewById<View>(R.id.btn_close)
        
        // Dummy Inventory Data
        val inventoryItems = listOf(
            InventoryItem("1", "Wheat Seed", R.drawable.ic_seed, 10),
            InventoryItem("2", "Corn Seed", R.drawable.ic_seed, 5),
            InventoryItem("3", "Carrot Seed", R.drawable.ic_seed, 8),
            InventoryItem("4", "Potato Seed", R.drawable.ic_seed, 12),
            InventoryItem("5", "Tomato Seed", R.drawable.ic_seed, 3)
        )

        rvInventory.layoutManager = GridLayoutManager(this, 3)
        rvInventory.adapter = InventoryAdapter(inventoryItems) { item ->
            dialog.dismiss()
            showQuantityDialog(item, MessageType.SEED)
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Coin dialog removed

    private fun showQuantityDialog(item: InventoryItem, type: MessageType) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quantity, null)
        val dialog = AlertDialog.Builder(this, R.style.Theme_Seeker_Dialog_Transparent)
            .setView(dialogView)
            .create()

        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_title)
        val imgIcon = dialogView.findViewById<ImageView>(R.id.img_item_icon)
        val tvName = dialogView.findViewById<TextView>(R.id.tv_item_name)
        val tvMax = dialogView.findViewById<TextView>(R.id.tv_max_hint)
        val etQuantity = dialogView.findViewById<EditText>(R.id.et_quantity)
        val btnMinus = dialogView.findViewById<View>(R.id.btn_minus)
        val btnPlus = dialogView.findViewById<View>(R.id.btn_plus)
        val btnConfirm = dialogView.findViewById<View>(R.id.btn_confirm_send)
        val btnClose = dialogView.findViewById<View>(R.id.btn_close)
        val btnBack = dialogView.findViewById<View>(R.id.btn_back)

        tvTitle.text = if (type == MessageType.COIN) "Send Coins" else "Send Seed"
        imgIcon.setImageResource(item.iconResId)
        tvName.text = item.name
        tvMax.text = "Max: ${item.count}"
        
        // Initial Quantity
        var currentQuantity = 1
        etQuantity.setText(currentQuantity.toString())

        btnMinus.setOnClickListener {
            if (currentQuantity > 1) {
                currentQuantity--
                etQuantity.setText(currentQuantity.toString())
            }
        }

        btnPlus.setOnClickListener {
            if (currentQuantity < item.count) {
                currentQuantity++
                etQuantity.setText(currentQuantity.toString())
            }
        }

        btnConfirm.setOnClickListener {
            val inputQty = etQuantity.text.toString().toIntOrNull() ?: 1
            val finalQty = inputQty.coerceIn(1, item.count)
            
            val content = if (type == MessageType.COIN) "$finalQty" else "${item.name} x$finalQty"
            sendMessage(content, type)
            dialog.dismiss()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        btnBack.setOnClickListener {
            dialog.dismiss()
            if (type == MessageType.SEED) {
                showSendSeedDialog()
            }
        }

        dialog.show()
    }

    private fun sendMessage(content: String, type: MessageType) {
        if (friendWalletAddress.isEmpty()) {
            Toast.makeText(this, "Error: No recipient", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val result = chatManager.sendMessage(friendWalletAddress, content, type)
            if (result == "Success") {
                // Optimistic Update
                val msg = Message(
                    id = System.currentTimeMillis().toString(), // Temp ID
                    senderId = solanaManager.getConnectedWallet() ?: "me",
                    content = content,
                    type = type,
                    isMe = true,
                    timestamp = System.currentTimeMillis()
                )
                messages.add(msg)
                adapter.notifyItemInserted(messages.size - 1)
                rvChat.scrollToPosition(messages.size - 1)
            } else {
                Toast.makeText(this@ChatActivity, result, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Delete message dialog removed

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null && gestureDetector.onTouchEvent(ev)) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }
}
