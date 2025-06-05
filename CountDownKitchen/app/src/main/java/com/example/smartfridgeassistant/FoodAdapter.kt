package com.example.smartfridgeassistant

// ➤ 套件匯入
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


// ➤ 食材 RecyclerView 的 Adapter
class FoodAdapter(
    private val itemList: MutableList<FoodItem>,              // 食材清單資料
    private val onItemClick: (FoodItem) -> Unit,              // 編輯功能 callback
    private val onDeleteItem: (FoodItem) -> Unit,             // 刪除功能 callback
    private val onTrashItem: (FoodItem) -> Unit,              // 廚餘功能 callback
    private val onEatItem: (FoodItem) -> Unit,                // 吃掉功能 callback
    private var expandedPosition: Int? = null,
    private val deletedDao: DeletedDao,
    private val refreshCallback: () -> Unit,
    private val foodDao: FoodDao,
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    // ➤ 記錄展開狀態的卡片位置集合（備用）
    private val expandedPositionSet = mutableSetOf<Int>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ➤ 食材卡片的 ViewHolder：對應每一筆項目的畫面元素
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

    // ➤ 建立畫面項目 ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    // ➤ 將資料綁定到畫面項目中
    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val item = itemList[position]

        // 填入文字資料
        holder.tvName.text = item.name
        holder.tvCategory.text = "分類：${item.category}"
        holder.tvType.text = "類型：${item.type}"
        holder.tvDate.text = "到期日：${item.expiryDate}"
        holder.tvNote.text = "備註：${item.note}"

        // ➤ 根據到期日設定卡片背景色（顏色需參考 color 資源）
        val today = Calendar.getInstance()
        val expiryDate = dateFormat.parse(item.expiryDate)
        val daysUntilExpiry = if (expiryDate != null) {
            val diff = expiryDate.time - today.time.time
            diff / (24 * 60 * 60 * 1000)
        } else {
            Long.MAX_VALUE
        }

        val backgroundColor = when {
            daysUntilExpiry < 0 -> R.color.card_light_gray     // 已過期
            daysUntilExpiry < 1 -> R.color.card_light_red     // 即將過期
            daysUntilExpiry <= 7 -> R.color.card_light_blue    // 快到期
            else -> R.color.card_white                         // 安全
        }
        holder.cardView.setCardBackgroundColor(holder.itemView.context.getColor(backgroundColor))

        // ➤ 展開與收合功能（點一下卡片）
        val isExpanded = expandedPosition == position
        val layoutParams = holder.itemView.layoutParams
        layoutParams.width = if (isExpanded) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
        holder.itemView.layoutParams = layoutParams

        holder.itemView.setOnClickListener {
            expandedPosition = if (expandedPosition == position) null else position
            notifyItemChanged(position)
        }

        // ➤ 功能按鈕區（編輯／廚餘／吃掉／刪除）
        holder.btnEdit.setOnClickListener { onItemClick(item) }

        holder.btnTrash.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val item = itemList[position]
                onTrashItem(item) // 呼叫廚餘邏輯
                itemList.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, itemList.size)
            }
        }

        holder.btnEat.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val item = itemList[position]
                onEatItem(item) // 呼叫完食邏輯
                itemList.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, itemList.size)
            }
        }

        holder.btnDelete.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                // ➤ 將資料移入 deleted_table 而不是直接刪除
                CoroutineScope(Dispatchers.IO).launch {
                    val deletedItem = DeletedItem(
                        name = item.name,
                        category = item.category,
                        type = item.type,
                        expiryDate = item.expiryDate,
                        note = item.note
                    )
                    deletedDao.insert(deletedItem)
                    foodDao.delete(item)

                    // 從畫面移除
                    CoroutineScope(Dispatchers.Main).launch {
                        itemList.removeAt(pos)
                        notifyItemRemoved(pos)
                        notifyItemRangeChanged(pos, itemList.size)
                        refreshCallback()
                    }
                }
            }
        }
    }

    // ➤ 傳回目前清單的數量
    override fun getItemCount(): Int = itemList.size
}
