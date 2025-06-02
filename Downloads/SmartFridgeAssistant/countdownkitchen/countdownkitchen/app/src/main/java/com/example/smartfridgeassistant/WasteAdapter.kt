package com.example.smartfridgeassistant

// 🔹 套件匯入
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 🔸 用於顯示「廚餘紀錄」的 RecyclerView Adapter
class WasteAdapter(private val wasteList: MutableList<WasteItem>) : RecyclerView.Adapter<WasteAdapter.WasteViewHolder>() {

    // ✅ 1. ViewHolder：對應 item_out.xml 的元件
    inner class WasteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOut: TextView = itemView.findViewById(R.id.tvOut)             // 顯示食材名稱
        val tvOutState: TextView = itemView.findViewById(R.id.tvOutState)   // 顯示狀態（此處固定為 "廚餘"）
    }

    // ✅ 2. 建立 ViewHolder（建立每一個卡片 view）
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WasteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_out, parent, false)
        return WasteViewHolder(view)
    }

    // ✅ 3. 將資料綁定到 ViewHolder 上（每個項目顯示什麼）
    override fun onBindViewHolder(holder: WasteViewHolder, position: Int) {
        val item = wasteList[position]
        holder.tvOut.text = item.name            // 顯示食材名稱
        holder.tvOutState.text = "廚餘"          // 狀態固定顯示為「廚餘」
    }

    // ✅ 4. 資料總筆數（用於建立幾個 item）
    override fun getItemCount(): Int = wasteList.size

    // ✅ 5. 更新資料（外部可呼叫此方法來刷新列表）
    fun updateData(newList: List<WasteItem>) {
        wasteList.clear()
        wasteList.addAll(newList)
        notifyDataSetChanged()
    }
}
