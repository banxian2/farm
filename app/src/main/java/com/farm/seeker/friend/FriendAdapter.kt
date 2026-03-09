package com.farm.seeker.friend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.farm.seeker.R
import com.farm.seeker.utils.AvatarUtils

class FriendAdapter(
    private val friends: List<Friend>,
    private val onActionClick: (Friend) -> Unit,
    private val onChatClick: (Friend) -> Unit,
    private val onLongClick: (Friend) -> Unit,
    private val onAvatarClick: (Friend) -> Unit
) : RecyclerView.Adapter<FriendAdapter.FriendViewHolder>() {

    class FriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAvatar: ImageView = view.findViewById(R.id.img_avatar)
        val tvName: TextView = view.findViewById(R.id.tv_name)
        val tvStatus: TextView = view.findViewById(R.id.tv_status)
        val btnVisit: Button = view.findViewById(R.id.btn_visit)
        val btnChat: ImageView = view.findViewById(R.id.btn_chat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        holder.tvName.text = friend.name
        holder.imgAvatar.setImageResource(AvatarUtils.getAvatarResId(friend.avatarIndex))
        
        val statusText = "Level ${friend.level}"
        holder.tvStatus.text = statusText
        
        // Button Logic based on Status
        when (friend.status) {
            FriendStatus.FRIEND -> {
                holder.btnVisit.text = "Visit"
                holder.btnVisit.isEnabled = true
                holder.btnVisit.alpha = 1.0f
            }
            FriendStatus.REQUEST_RECEIVED -> {
                holder.btnVisit.text = "Accept"
                holder.btnVisit.isEnabled = true
                holder.btnVisit.alpha = 1.0f
            }
            FriendStatus.REQUEST_SENT -> {
                holder.btnVisit.text = "Pending"
                holder.btnVisit.isEnabled = false
                holder.btnVisit.alpha = 0.6f
            }
            FriendStatus.STRANGER -> {
                holder.btnVisit.text = "Add"
                holder.btnVisit.isEnabled = true
                holder.btnVisit.alpha = 1.0f
            }
        }
        
        holder.btnVisit.setOnClickListener {
            onActionClick(friend)
        }

        holder.imgAvatar.setOnClickListener {
            onAvatarClick(friend)
        }
        
        holder.itemView.setOnLongClickListener {
            onLongClick(friend)
            true
        }

        if (friend.status == FriendStatus.FRIEND) {
            holder.btnChat.visibility = View.VISIBLE
            holder.btnChat.setOnClickListener {
                onChatClick(friend)
            }
        } else {
            holder.btnChat.visibility = View.GONE
        }
    }

    override fun getItemCount() = friends.size
}
