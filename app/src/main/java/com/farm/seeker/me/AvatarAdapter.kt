package com.farm.seeker.me

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.farm.seeker.R

class AvatarAdapter(
    private val avatars: List<Int>,
    private val selectedAvatarResId: Int,
    private val onAvatarSelected: (Int) -> Unit
) : RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder>() {

    private var currentSelected = selectedAvatarResId

    inner class AvatarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: ImageView = itemView.findViewById(R.id.img_avatar)
        val imgSelected: ImageView = itemView.findViewById(R.id.img_selected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_avatar_selection, parent, false)
        return AvatarViewHolder(view)
    }

    override fun onBindViewHolder(holder: AvatarViewHolder, position: Int) {
        val avatarResId = avatars[position]
        holder.imgAvatar.setImageResource(avatarResId)

        if (avatarResId == currentSelected) {
            holder.imgSelected.visibility = View.VISIBLE
        } else {
            holder.imgSelected.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            currentSelected = avatarResId
            notifyDataSetChanged()
            onAvatarSelected(avatarResId)
        }
    }

    override fun getItemCount(): Int = avatars.size
}
