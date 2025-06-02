package com.example.smartfridgeassistant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 🔹 定義資料類別：代表一筆「已出庫」的食材資料
data class OutItem(
    val name: String,       // 食材名稱
    val state: String,      // 狀態（如：吃掉、廚餘）
    val date: String,       // 出庫日期
    val category: String,   // 分類（冷藏、冷凍、常溫）
    val type: String,       // 類型（蔬菜類、肉類等）
    val note: String        // 備註說明
)

// 🔹 RecyclerView Adapter：負責顯示 OutItem 清單
class OutItemAdapter(
    private val outList: MutableList<OutItem>,             // 資料清單
    private val onBackClick: (OutItem) -> Unit             // 返回按鈕點擊事件 callback
) : RecyclerView.Adapter<OutItemAdapter.OutViewHolder>() {

    // 🔹 ViewHolder：對應一個 item_out.xml 項目
    inner class OutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOut: TextView = itemView.findViewById(R.id.tvOut)                 // 食材名稱
        val tvOutState: TextView = itemView.findViewById(R.id.tvOutState)       // 狀態（吃掉/廚餘）
        val tvOutCategory: TextView = itemView.findViewById(R.id.tvOutCategory) // 分類
        val tvOutType: TextView = itemView.findViewById(R.id.tvOutType)         // 類型
        val tvOutNote: TextView = itemView.findViewById(R.id.tvOutNote)         // 備註
        val btnBack: ImageButton = itemView.findViewById(R.id.btnBack)          // 返回按鈕
    }

    // 🔹 建立 ViewHolder：產生畫面 layout 並包裝成 ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_out, parent, false)
        return OutViewHolder(view)
    }

    // 🔹 將資料綁定到 UI 上（顯示內容）
    override fun onBindViewHolder(holder: OutViewHolder, position: Int) {
        val item = outList[position]
        holder.tvOut.text = item.name
        holder.tvOutState.text = item.state
        // 以下這幾行若想顯示其他欄位，可取消註解
//        holder.tvOutCategory.text = item.category
//        holder.tvOutType.text = item.type
//        holder.tvOutNote.text = item.note

        // 返回按鈕：點擊後執行 callback（可能用來還原資料）
        holder.btnBack.setOnClickListener {
            onBackClick(item)
        }
    }

    // 🔹 回傳資料筆數（列表項目數量）
    override fun getItemCount(): Int = outList.size

    // 🔹 更新資料清單並重新整理畫面（通常搭配搜尋、篩選使用）
    fun updateData(newList: List<OutItem>) {
        outList.clear()
        outList.addAll(newList)
        notifyDataSetChanged()
    }
}
