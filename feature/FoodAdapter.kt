package com.example.smartfridgeassistant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class FoodAdapter(
    private val itemList: List<FoodItem>,
    private val onItemClick: (FoodItem) -> Unit,
    private var expandedPosition: Int? = null

) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    
    private val expandedPositionSet = mutableSetOf<Int>()

    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        val actionButtons: View = itemView.findViewById(R.id.action_buttons)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        val btnTrash: ImageButton = itemView.findViewById(R.id.btn_trash)
        val btnEat: ImageButton = itemView.findViewById(R.id.btn_eat)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val item = itemList[position]

        holder.tvName.text = item.name
        holder.tvCategory.text = "分類：${item.category}"
        holder.tvDate.text = "到期日：${item.expiryDate}"
        holder.tvNote.text = "備註：${item.note}"

        
        val isExpanded = expandedPosition == position
        val layoutParams = holder.itemView.layoutParams
        layoutParams.width = if (isExpanded) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
        holder.itemView.layoutParams = layoutParams

       
        holder.itemView.setOnClickListener {
            expandedPosition = if (expandedPosition == position) null else position
            notifyItemChanged(position)
        }

        // 功能按鈕
        holder.btnEdit.setOnClickListener { onItemClick(item) }
        holder.btnTrash.setOnClickListener {
            Toast.makeText(holder.itemView.context, "廚餘功能尚未實作", Toast.LENGTH_SHORT).show()
        }
        holder.btnEat.setOnClickListener {
            Toast.makeText(holder.itemView.context, "吃掉功能尚未實作", Toast.LENGTH_SHORT).show()
        }
        holder.btnDelete.setOnClickListener {
            Toast.makeText(holder.itemView.context, "刪除功能尚未實作", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = itemList.size
}
