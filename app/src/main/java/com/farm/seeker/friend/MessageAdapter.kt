package com.farm.seeker.friend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.farm.seeker.R

import com.farm.seeker.utils.AvatarUtils

class MessageAdapter(
    private val messages: List<Message>,
    private val friendAvatarIndex: Int,
    private val myAvatarIndex: Int,
    private val onAvatarClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ME = 1
        private const val TYPE_OTHER = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isMe) TYPE_ME else TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ME) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_right, parent, false)
            MessageViewHolder(view, null)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_left, parent, false)
            MessageViewHolder(view, onAvatarClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        (holder as MessageViewHolder).bind(message, friendAvatarIndex, myAvatarIndex)
    }

    override fun getItemCount() = messages.size

    class MessageViewHolder(view: View, private val onAvatarClick: (() -> Unit)?) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tv_message)
        val layoutSpecial: LinearLayout = view.findViewById(R.id.layout_special)
        val imgSpecialIcon: ImageView = view.findViewById(R.id.img_special_icon)
        val tvSpecialText: TextView = view.findViewById(R.id.tv_special_text)
        val imgAvatar: ImageView? = view.findViewById(R.id.img_avatar)

        fun bind(message: Message, friendAvatarIndex: Int, myAvatarIndex: Int) {
            if (!message.isMe) {
                imgAvatar?.setImageResource(AvatarUtils.getAvatarResId(friendAvatarIndex))
            } else {
                imgAvatar?.setImageResource(AvatarUtils.getAvatarResId(myAvatarIndex))
            }
            imgAvatar?.setOnClickListener {
                onAvatarClick?.invoke()
            }
            when (message.type) {
                MessageType.TEXT -> {
                    tvMessage.visibility = View.VISIBLE
                    layoutSpecial.visibility = View.GONE
                    tvMessage.text = message.content
                }
                MessageType.SEED -> {
                    tvMessage.visibility = View.GONE
                    layoutSpecial.visibility = View.VISIBLE
                    imgSpecialIcon.setImageResource(R.drawable.ic_seed)
                    tvSpecialText.text = "Sent Seed: ${message.content}"
                }
                MessageType.COIN -> {
                    tvMessage.visibility = View.GONE
                    layoutSpecial.visibility = View.VISIBLE
                    imgSpecialIcon.setImageResource(R.drawable.ic_coin)
                    tvSpecialText.text = "Sent Coin: ${message.content}"
                }
            }
        }
    }
}
