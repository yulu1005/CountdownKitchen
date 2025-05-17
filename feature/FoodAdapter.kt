package com.example.smartfridgeassistant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FoodAdapter(
    private val itemList: MutableList<FoodItem>,
    private val onItemClick: (FoodItem) -> Unit,
    private val onDeleteItem: (FoodItem) -> Unit,
    private val onTrashItem: (FoodItem) -> Unit,
    private val onEatItem: (FoodItem) -> Unit,
    private var expandedPosition: Int? = null
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    // ➤ 記錄哪幾個 item 有展開
    private val expandedPositionSet = mutableSetOf<Int>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvType: TextView = itemView.findViewById(R.id.tvType)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        val actionButtons: View = itemView.findViewById(R.id.action_buttons)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        val btnTrash: ImageButton = itemView.findViewById(R.id.btn_trash)
        val btnEat: ImageButton = itemView.findViewById(R.id.btn_eat)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
        val cardView: CardView = itemView as CardView
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
        holder.tvType.text = "類型：${item.type}"
        holder.tvDate.text = "到期日：${item.expiryDate}"
        holder.tvNote.text = "備註：${item.note}"

        // 设置卡片背景颜色
        val today = Calendar.getInstance()
        val expiryDate = dateFormat.parse(item.expiryDate)
        val daysUntilExpiry = if (expiryDate != null) {
            val diff = expiryDate.time - today.time.time
            diff / (24 * 60 * 60 * 1000)
        } else {
            Long.MAX_VALUE
        }

        val backgroundColor = when {
            daysUntilExpiry < 0 -> R.color.card_light_gray
            daysUntilExpiry <= 1 -> R.color.card_light_red
            daysUntilExpiry <= 7 -> R.color.card_light_blue
            else -> R.color.card_white
        }
        holder.cardView.setCardBackgroundColor(holder.itemView.context.getColor(backgroundColor))

        // 👉 判斷這張卡片是否是展開狀態
        val isExpanded = expandedPosition == position
        val layoutParams = holder.itemView.layoutParams
        layoutParams.width = if (isExpanded) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
        holder.itemView.layoutParams = layoutParams

        // 👉 點一下展開／收回
        holder.itemView.setOnClickListener {
            expandedPosition = if (expandedPosition == position) null else position
            notifyItemChanged(position)
        }

        // 功能按鈕
        holder.btnEdit.setOnClickListener { onItemClick(item) }
        holder.btnTrash.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val item = itemList[position]
                // 调用厨余回调函数
                onTrashItem(item)
                // 从列表中移除项目
                itemList.removeAt(position)
                // 通知适配器更新
                notifyItemRemoved(position)
                // 通知任何可能的观察者数据已更改
                notifyItemRangeChanged(position, itemList.size)
            }
        }
        holder.btnEat.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val item = itemList[position]
                // 调用完食回调函数
                onEatItem(item)
                // 从列表中移除项目
                itemList.removeAt(position)
                // 通知适配器更新
                notifyItemRemoved(position)
                // 通知任何可能的观察者数据已更改
                notifyItemRangeChanged(position, itemList.size)
            }
        }
        holder.btnDelete.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val item = itemList[position]
                // 调用删除回调函数
                onDeleteItem(item)
                // 从列表中移除项目
                itemList.removeAt(position)
                // 通知适配器更新
                notifyItemRemoved(position)
                // 通知任何可能的观察者数据已更改
                notifyItemRangeChanged(position, itemList.size)
            }
        }
    }

    override fun getItemCount(): Int = itemList.size
    
}
