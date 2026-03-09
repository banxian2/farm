package com.farm.seeker.friend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.farm.seeker.R

data class InventoryItem(
    val id: String,
    val name: String,
    val iconResId: Int,
    val count: Int
)

class InventoryAdapter(
    private val items: List<InventoryItem>,
    private val onItemClick: (InventoryItem) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {

    class InventoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcon: ImageView = view.findViewById(R.id.img_item_icon)
        val tvName: TextView = view.findViewById(R.id.tv_item_name)
        val tvCount: TextView = view.findViewById(R.id.tv_item_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inventory, parent, false)
        return InventoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvCount.text = "x${item.count}"
        holder.imgIcon.setImageResource(item.iconResId)
        
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = items.size
}
