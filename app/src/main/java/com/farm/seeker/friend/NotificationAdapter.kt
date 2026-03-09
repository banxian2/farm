package com.farm.seeker.friend

import com.farm.seeker.R

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(
    private val notifications: List<Notification>,
    private val onAction: (Notification, Boolean) -> Unit // Boolean: true for accept, false for reject
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
        val tvTime: TextView = view.findViewById(R.id.tv_time)
        val tvContent: TextView = view.findViewById(R.id.tv_content)
        val layoutActions: LinearLayout = view.findViewById(R.id.layout_actions)
        val btnAccept: Button = view.findViewById(R.id.btn_accept)
        val btnReject: Button = view.findViewById(R.id.btn_reject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.tvTitle.text = notification.title
        holder.tvTime.text = notification.time
        holder.tvContent.text = notification.content

        if (notification.type == NotificationType.FRIEND_REQUEST && !notification.isProcessed) {
            holder.layoutActions.visibility = View.VISIBLE
            holder.btnAccept.setOnClickListener { onAction(notification, true) }
            holder.btnReject.setOnClickListener { onAction(notification, false) }
        } else {
            holder.layoutActions.visibility = View.GONE
        }
    }

    override fun getItemCount() = notifications.size
}