package com.example.smartfridgeassistant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WasteAdapter(private val wasteList: MutableList<WasteItem>) : RecyclerView.Adapter<WasteAdapter.WasteViewHolder>() {

    inner class WasteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOut: TextView = itemView.findViewById(R.id.tvOut)
        val tvOutState: TextView = itemView.findViewById(R.id.tvOutState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WasteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_out, parent, false)
        return WasteViewHolder(view)
    }

    override fun onBindViewHolder(holder: WasteViewHolder, position: Int) {
        val item = wasteList[position]
        holder.tvOut.text = item.name
        holder.tvOutState.text = "廚餘"
    }

    override fun getItemCount(): Int = wasteList.size

    fun updateData(newList: List<WasteItem>) {
        wasteList.clear()
        wasteList.addAll(newList)
        notifyDataSetChanged()
    }
} 
