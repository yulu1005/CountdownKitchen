package com.example.smartfridgeassistant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 🔸 用來顯示搜尋結果的 RecyclerView Adapter
class SearchAdapter : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    // ✅ 1. 食材資料清單（會由外部更新）
    private var foods = listOf<FoodItem>()

    // ✅ 2. 提供外部更新資料的方法
    fun updateData(newData: List<FoodItem>)  {
        foods = newData
        notifyDataSetChanged() // 通知 RecyclerView 資料已更新
    }

    // ✅ 3. ViewHolder：綁定畫面元件（item_food_result.xml）
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val foodName: TextView = view.findViewById(R.id.food)                // 顯示食材名稱
        val expirationDate: TextView = view.findViewById(R.id.expiration_date)  // 顯示到期日
    }

    // ✅ 4. 建立每一個 item 的 ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food_result, parent, false)
        return ViewHolder(view)
    }

    // ✅ 5. 資料與畫面綁定
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = foods[position]
        holder.foodName.text = item.name
        holder.expirationDate.text = "到期日：${item.expiryDate}"
    }

    // ✅ 6. 傳回總資料筆數
    override fun getItemCount(): Int = foods.size
}
