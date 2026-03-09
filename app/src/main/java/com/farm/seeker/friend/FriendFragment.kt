package com.farm.seeker.friend

import com.farm.seeker.R

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.farm.seeker.MainActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class FriendFragment : Fragment() {

    private lateinit var friendListAdapter: FriendAdapter
    
    private val friendList = mutableListOf<Friend>()
    private val notificationList = mutableListOf<Notification>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_friend, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)
        setupAddFriendButton(view)
        setupNotificationButton(view)
        setupLoginButton(view)
        // loadDummyData() // Disable dummy data
    }
    
    override fun onResume() {
        super.onResume()
        checkLoginState()
        // Auto refresh friends if logged in
        val solanaManager = (activity as? MainActivity)?.solanaManager
        if (solanaManager?.isLoggedIn() == true) {
             fetchFriendList()
        }
    }

    private fun fetchFriendList() {
        val friendManager = (activity as? MainActivity)?.friendManager ?: return
        lifecycleScope.launch {
            val result = friendManager.fetchFriendList()
            if (result.startsWith("Error")) {
                Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                updateEmptyState()
            } else {
                try {
                    val jsonArray = JSONArray(result)
                    friendList.clear()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        friendList.add(Friend(
                            id = obj.optString("id"),
                            walletAddress = obj.optString("wallet_address"),
                            name = obj.optString("nickname"),
                            level = obj.optInt("level"),
                            isOnline = false, // Not provided by API yet
                            lastSeen = obj.optString("friendship_created_at"),
                            avatarIndex = obj.optInt("avatar_index", 1),
                            status = FriendStatus.FRIEND
                        ))
                    }
                    friendListAdapter.notifyDataSetChanged()
                    updateEmptyState()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Parse error: ${e.message}", Toast.LENGTH_SHORT).show()
                    updateEmptyState()
                }
            }
        }
    }
    
    private fun updateEmptyState() {
        val rvFriends = view?.findViewById<RecyclerView>(R.id.rv_friend_list)
        val layoutEmpty = view?.findViewById<View>(R.id.layout_empty_state)
        
        if (friendList.isEmpty()) {
            rvFriends?.visibility = View.GONE
            layoutEmpty?.visibility = View.VISIBLE
        } else {
            rvFriends?.visibility = View.VISIBLE
            layoutEmpty?.visibility = View.GONE
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            checkLoginState()
        }
    }
    
    private fun checkLoginState() {
        val solanaManager = (activity as? MainActivity)?.solanaManager ?: return
        val isLoggedIn = solanaManager.isLoggedIn()
        
        val rvFriends = view?.findViewById<RecyclerView>(R.id.rv_friend_list)
        val fab = view?.findViewById<FloatingActionButton>(R.id.fab_add_friend)
        val layoutLogin = view?.findViewById<View>(R.id.layout_login_required)
        val btnNotification = view?.findViewById<View>(R.id.btn_notification)
        val layoutEmpty = view?.findViewById<View>(R.id.layout_empty_state)
        
        if (isLoggedIn) {
            // Visibility handled by updateEmptyState() for list/empty
            // rvFriends?.visibility = View.VISIBLE 
            fab?.visibility = View.VISIBLE
            btnNotification?.visibility = View.VISIBLE
            layoutLogin?.visibility = View.GONE
            
            // Initial check for empty state if list is empty
            if (friendList.isEmpty()) {
                layoutEmpty?.visibility = View.VISIBLE
                rvFriends?.visibility = View.GONE
            } else {
                layoutEmpty?.visibility = View.GONE
                rvFriends?.visibility = View.VISIBLE
            }
        } else {
            rvFriends?.visibility = View.GONE
            fab?.visibility = View.GONE
            btnNotification?.visibility = View.GONE
            layoutEmpty?.visibility = View.GONE
            layoutLogin?.visibility = View.VISIBLE
        }
    }
    
    private fun setupLoginButton(view: View) {
        val btnLogin = view.findViewById<Button>(R.id.btn_login_friend)
        val progressBar = view.findViewById<View>(R.id.progressBar)

        btnLogin.setOnClickListener {
            val solanaManager = (activity as? MainActivity)?.solanaManager ?: return@setOnClickListener
            
            progressBar.visibility = View.VISIBLE
            btnLogin.visibility = View.GONE

            lifecycleScope.launch {
                val result = solanaManager.login()
                
                progressBar.visibility = View.GONE
                
                Toast.makeText(context, "Login Result: $result", Toast.LENGTH_SHORT).show()
                if (result == "Success") {
                    checkLoginState()
                    fetchFriendList()
                } else {
                    btnLogin.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupRecyclerView(view: View) {
        // Friend List (Main)
        val rvFriends = view.findViewById<RecyclerView>(R.id.rv_friend_list)
        rvFriends.layoutManager = LinearLayoutManager(context)
        
        friendListAdapter = FriendAdapter(friendList, 
            onActionClick = { friend ->
                handleFriendAction(friend)
            },
            onChatClick = { friend ->
                val intent = Intent(context, ChatActivity::class.java)
                intent.putExtra("friend_name", friend.name)
                intent.putExtra("friend_id", friend.id)
                intent.putExtra("friend_wallet_address", friend.walletAddress)
                intent.putExtra("friend_level", friend.level)
                intent.putExtra("friend_avatar_index", friend.avatarIndex)
                startActivity(intent)
            },
            onLongClick = { friend ->
                if (friend.status == FriendStatus.FRIEND) {
                    showDeleteFriendDialog(friend)
                }
            },
            onAvatarClick = { friend ->
                val intent = Intent(context, FriendInfoActivity::class.java)
                intent.putExtra("friend_name", friend.name)
                intent.putExtra("friend_wallet_address", friend.walletAddress)
                intent.putExtra("friend_level", friend.level)
                intent.putExtra("friend_avatar_index", friend.avatarIndex)
                startActivity(intent)
            }
        )
        rvFriends.adapter = friendListAdapter
    }

    private fun showDeleteFriendDialog(friend: Friend) {
        AlertDialog.Builder(context)
            .setTitle("Delete Friend")
            .setMessage("Are you sure you want to remove ${friend.name} from your friends?")
            .setPositiveButton("Delete") { _, _ ->
                val friendManager = (activity as? MainActivity)?.friendManager
                if (friendManager != null) {
                    lifecycleScope.launch {
                        val result = friendManager.deleteFriend(friend.walletAddress)
                        if (result == "Success") {
                            Toast.makeText(context, "${friend.name} removed", Toast.LENGTH_SHORT).show()
                            friendList.remove(friend)
                            friendListAdapter.notifyDataSetChanged()
                            updateEmptyState()
                        } else {
                            Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupNotificationButton(view: View) {
        val btnNotif = view.findViewById<View>(R.id.btn_notification)
        btnNotif.setOnClickListener {
            showNotificationDialog()
        }
    }

    private fun setupAddFriendButton(view: View) {
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_add_friend)
        fab.setOnClickListener {
            showAddFriendDialog()
        }
    }

    private fun loadDummyData() {
        friendList.clear()
        
        // Friends
        friendList.addAll(listOf(
        ))

        // Notifications
        notificationList.clear()

        updateUI()
    }

    private fun updateUI() {
        friendListAdapter.notifyDataSetChanged()
    }

    private fun handleFriendAction(friend: Friend) {
        when (friend.status) {
            FriendStatus.FRIEND -> {
                Toast.makeText(context, "Visiting ${friend.name}'s farm...", Toast.LENGTH_SHORT).show()
                (activity as? com.farm.seeker.MainActivity)?.navigateToFarm(
                    friend.id, 
                    friend.name, 
                    friend.level, 
                    friend.avatarIndex
                )
            }
            FriendStatus.REQUEST_RECEIVED -> {
                // Accept Request
                friend.status = FriendStatus.FRIEND
                friendListAdapter.notifyDataSetChanged()
                
                Toast.makeText(context, "You are now friends with ${friend.name}!", Toast.LENGTH_SHORT).show()
            }
            FriendStatus.STRANGER -> {
                // Should not happen in list, but handled for completeness
                friend.status = FriendStatus.REQUEST_SENT
                friendListAdapter.notifyDataSetChanged()
                Toast.makeText(context, "Friend request sent to ${friend.name}", Toast.LENGTH_SHORT).show()
            }
            FriendStatus.REQUEST_SENT -> {
                Toast.makeText(context, "Request already sent", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showNotificationDialog() {
        val activity = activity as? MainActivity
        val solanaManager = activity?.solanaManager
        val friendManager = activity?.friendManager
        
        if (solanaManager == null || !solanaManager.isLoggedIn()) {
             Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
             return
        }
        if (friendManager == null) return

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_notification, null)
        val rvNotifications = dialogView.findViewById<RecyclerView>(R.id.rv_notifications)
        val btnClose = dialogView.findViewById<View>(R.id.btn_close)

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            (resources.displayMetrics.heightPixels * 0.8).toInt()
        )

        val adapter = NotificationAdapter(notificationList) { notification, isAccepted ->
             lifecycleScope.launch {
                 val action = if (isAccepted) "agree" else "reject"
                 val result = friendManager.processNotification(notification.id, action)
                 
                 if (result == "Success") {
                     notification.isProcessed = true
                     rvNotifications.adapter?.notifyDataSetChanged()
                     
                     if (isAccepted) {
                        // Refresh from server to get full details
                        fetchFriendList()
                    }
                     
                     Toast.makeText(context, if(isAccepted) "Accepted" else "Rejected", Toast.LENGTH_SHORT).show()
                 } else {
                     Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                 }
             }
        }
        
        rvNotifications.layoutManager = LinearLayoutManager(context)
        rvNotifications.adapter = adapter
        
        // Fetch Notifications
        lifecycleScope.launch {
            val result = friendManager.fetchNotifications()
            if (result.startsWith("Error") || result.startsWith("[").not()) {
                 Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
            } else {
                 try {
                     val jsonArray = org.json.JSONArray(result)
                     notificationList.clear()
                     for (i in 0 until jsonArray.length()) {
                         val obj = jsonArray.getJSONObject(i)
                         val typeInt = obj.optInt("type", 2)
                         val type = if (typeInt == 1) NotificationType.FRIEND_REQUEST else NotificationType.SYSTEM_ANNOUNCEMENT
                         
                         val senderInfo = obj.optJSONObject("sender_info")
                         val senderNickname = senderInfo?.optString("nickname")
                         val senderAvatarIndex = senderInfo?.optInt("avatar_index", 1) ?: 1
                         val senderUserId = senderInfo?.optString("user_id")
                         
                         notificationList.add(Notification(
                             id = obj.optString("id"),
                             title = obj.optString("title"),
                             content = obj.optString("content"),
                             time = obj.optString("created_at"),
                             type = type,
                             isProcessed = obj.optInt("is_processed") != 0,
                             friendId = obj.optString("related_user_id"),
                             senderNickname = senderNickname,
                             senderAvatarIndex = senderAvatarIndex,
                             senderUserId = senderUserId
                         ))
                     }
                     adapter.notifyDataSetChanged()
                 } catch (e: Exception) {
                     e.printStackTrace()
                     Toast.makeText(context, "Parse error: ${e.message}", Toast.LENGTH_SHORT).show()
                 }
            }
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAddFriendDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_friend, null)
        val input = dialogView.findViewById<EditText>(R.id.et_friend_id)
        val btnAdd = dialogView.findViewById<View>(R.id.btn_add)
        val btnCancel = dialogView.findViewById<View>(R.id.btn_cancel)

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Set background to transparent to show our custom rounded background
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnAdd.setOnClickListener {
            val query = input.text.toString()
            if (query.isNotEmpty()) {
                val friendManager = (activity as? MainActivity)?.friendManager
                if (friendManager != null) {
                    btnAdd.isEnabled = false // Prevent double click
                    lifecycleScope.launch {
                        val result = friendManager.sendFriendRequest(query)
                        btnAdd.isEnabled = true
                        
                        if (result == "Success") {
                            Toast.makeText(context, "Friend request sent to $query", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        } else {
                            Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Error: Manager not found", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
