package com.farm.seeker.me

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.farm.seeker.R
import com.farm.seeker.solana.SolanaManager
import com.farm.seeker.utils.AvatarUtils
import com.farm.seeker.utils.SwipeBackGestureListener
import android.view.GestureDetector
import kotlinx.coroutines.launch

class InviteFriendsActivity : AppCompatActivity() {

    private lateinit var gestureDetector: GestureDetector
    private lateinit var solanaManager: SolanaManager
    private lateinit var inviteManager: InviteManager
    private lateinit var adapter: InvitedFriendsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invite_friends)

        // Setup Swipe Back
        gestureDetector = GestureDetector(this, SwipeBackGestureListener(this))

        solanaManager = SolanaManager(this)
        inviteManager = InviteManager(this, solanaManager)

        setupUI()
        loadData()
    }

    private fun setupUI() {
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        val rvFriends = findViewById<RecyclerView>(R.id.rv_invited_friends)
        rvFriends.layoutManager = LinearLayoutManager(this)
        adapter = InvitedFriendsAdapter(emptyList()) { friend ->
            claimReward(friend)
        }
        rvFriends.adapter = adapter
    }

    private fun loadData() {
        lifecycleScope.launch {
            val friends = inviteManager.getInvitedFriends()
            adapter.updateData(friends)
            
            // Toggle Empty State
            val rvFriends = findViewById<RecyclerView>(R.id.rv_invited_friends)
            val tvEmptyState = findViewById<TextView>(R.id.tv_empty_state)
            val tvInviteCount = findViewById<TextView>(R.id.tv_invite_count)
            
            // Update Invite Count
            tvInviteCount.text = "Invited: ${friends.size}/10"
            
            if (friends.isEmpty()) {
                rvFriends.visibility = View.GONE
                tvEmptyState.visibility = View.VISIBLE
            } else {
                rvFriends.visibility = View.VISIBLE
                tvEmptyState.visibility = View.GONE
            }
            
            // Update Grand Reward UI
            val claimedCount = friends.count { it.rewardClaimed }
            val tvRewardStatus = findViewById<TextView>(R.id.tv_reward_status)
            val imgRewardBox = findViewById<ImageView>(R.id.img_reward_box)
            val layoutReward = findViewById<View>(R.id.layout_reward)

            if (claimedCount >= 10) {
                // Assuming the first 10 grand reward is claimed automatically
                // We can't know for sure if it's "claimed" via API flag for the box itself,
                // but we can infer from the count.
                // Or we can just show the count.
                tvRewardStatus.text = "Claimed (10/10)"
                imgRewardBox.alpha = 0.5f
                layoutReward.setOnClickListener {
                     Toast.makeText(this@InviteFriendsActivity, "Grand Reward already claimed!", Toast.LENGTH_SHORT).show()
                }
            } else {
                tvRewardStatus.text = "$claimedCount / 10"
                imgRewardBox.alpha = 1.0f
                layoutReward.setOnClickListener {
                    Toast.makeText(this@InviteFriendsActivity, "Collect 10 rewards to get the Grand Prize!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun claimGrandReward() {
        // Deprecated: Grand reward is automatic
        lifecycleScope.launch {
             val result = inviteManager.claimGrandReward()
             Toast.makeText(this@InviteFriendsActivity, result, Toast.LENGTH_SHORT).show()
        }
    }

    private fun claimReward(friend: InvitedFriend) {
        lifecycleScope.launch {
            val result = inviteManager.claimReward(friend.targetWalletAddress)
            if (result == "Success") {
                Toast.makeText(this@InviteFriendsActivity, "Reward Claimed!", Toast.LENGTH_SHORT).show()
                adapter.markAsClaimed(friend.targetWalletAddress)
                // Refresh data to update counts
                loadData()
            } else {
                Toast.makeText(this@InviteFriendsActivity, result, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev != null && gestureDetector.onTouchEvent(ev)) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }
}

class InvitedFriendsAdapter(
    private var friends: List<InvitedFriend>,
    private val onClaimClick: (InvitedFriend) -> Unit
) : RecyclerView.Adapter<InvitedFriendsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAvatar: ImageView = view.findViewById(R.id.img_avatar)
        val tvName: TextView = view.findViewById(R.id.tv_name)
        val tvLevel: TextView = view.findViewById(R.id.tv_level)
        val btnClaim: Button = view.findViewById(R.id.btn_claim)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invited_friend, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friend = friends[position]
        holder.tvName.text = friend.name
        holder.tvLevel.text = "Level ${friend.level}"
        
        holder.imgAvatar.setImageResource(AvatarUtils.getAvatarResId(friend.avatarIndex))

        if (friend.rewardClaimed) {
            holder.btnClaim.text = "Claimed"
            holder.btnClaim.isEnabled = false
            holder.btnClaim.alpha = 0.5f
        } else {
            if (friend.level >= 6) {
                holder.btnClaim.text = "Claim"
                holder.btnClaim.isEnabled = true
                holder.btnClaim.alpha = 1.0f
                holder.btnClaim.setOnClickListener {
                    onClaimClick(friend)
                }
            } else {
                holder.btnClaim.text = "Unlock Lv.6"
                holder.btnClaim.isEnabled = false
                holder.btnClaim.alpha = 0.5f
            }
        }
    }

    override fun getItemCount() = friends.size

    fun updateData(newFriends: List<InvitedFriend>) {
        friends = newFriends
        notifyDataSetChanged()
    }

    fun markAsClaimed(targetWalletAddress: String) {
        friends = friends.map {
            if (it.targetWalletAddress == targetWalletAddress) it.copy(rewardClaimed = true) else it
        }
        notifyDataSetChanged()
    }
}
