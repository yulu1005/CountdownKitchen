package com.example.smartfridgeassistant

// 🔹 匯入套件
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class AnalyzeActivity : AppCompatActivity() {

    // 🔹 資料庫存取物件（DAO）
    private lateinit var wasteDao: WasteDao
    private lateinit var eatenDao: EatenDao
    private lateinit var foodDao: FoodDao
    private lateinit var deletedDao: DeletedDao

    // 🔹 食材紀錄清單與 Adapter
    private val outList = mutableListOf<OutItem>()
    private lateinit var outAdapter: OutItemAdapter

    // 🔹 圓餅圖元件
    private lateinit var pieChart: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 支援全螢幕邊緣顯示
        setContentView(R.layout.activity_analyze)

        // 🔹 避免畫面元素被系統狀態列或導航列擋住
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 🔹 初始化 DAO
        val database = AppDatabase.getDatabase(this)
        wasteDao = database.wasteDao()
        eatenDao = database.eatenDao()
        foodDao = database.foodDao()
        deletedDao = database.deletedDao()

        // 🔹 初始化 PieChart
        pieChart = findViewById(R.id.pie_chart)

        // 🔹 初始化 RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        outAdapter = OutItemAdapter(outList) { item ->
            // 🔁 點擊返回按鈕時，將資料從「廚餘 / 完食 / 刪除」移回主食材資料表
            lifecycleScope.launch {
                when (item.state) {
                    "廚餘" -> {
                        val wasteItem = wasteDao.getAll().find { it.name == item.name }
                        wasteItem?.let { wasteDao.delete(it) }
                    }
                    "完食" -> {
                        val eatenItem = eatenDao.getAll().find { it.name == item.name }
                        eatenItem?.let { eatenDao.delete(it) }
                    }
                    "已刪除" -> {
                        val deletedItem = deletedDao.getAll().find { it.name == item.name }
                        deletedItem?.let { deletedDao.delete(it) }
                    }
                }

                // ➕ 插入回主食材資料表
                foodDao.insert(
                    FoodItem(
                        name = item.name,
                        category = item.category,
                        expiryDate = item.date,
                        note = item.note,
                        type = item.type
                    )
                )

                // 🔄 重新整理畫面
                refreshList()
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = outAdapter

        // 🔃 初次載入資料
        refreshList()

        // 🔻 啟用底部導覽列（目前高亮顯示分析頁）
        setupBottomNav(this, R.id.nav_analyze)
    }

    // 🔁 載入所有「已出庫資料」並更新畫面與圓餅圖
    private fun refreshList() {
        lifecycleScope.launch {
            try {
                // 🔄 取得三個資料表的資料
                val wasteList = wasteDao.getAll()
                val eatenList = eatenDao.getAll()
                val deletedList = deletedDao.getAll()

                outList.clear()

                // ➕ 將廚餘資料加入清單
                wasteList.forEach {
                    outList.add(OutItem(it.name, "廚餘", it.date, it.category, it.type, it.note))
                }

                // ➕ 將完食資料加入清單
                eatenList.forEach {
                    outList.add(OutItem(it.name, "完食", it.date, it.category, it.type, it.note))
                }

                // ➕ 將已刪除資料加入清單
                deletedList.forEach {
                    outList.add(OutItem(it.name, "已刪除", it.expiryDate, it.category, it.type, it.note))
                }

                // 🔃 根據日期由新到舊排序
                outList.sortByDescending { it.date }
                outAdapter.notifyDataSetChanged()

                // 🔢 更新圖表資料
                val dataMap = mapOf(
                    "廚餘" to wasteList.size,
                    "完食" to eatenList.size
                )
                setupPieChart(dataMap)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 🔷 設定 PieChart 圓餅圖顯示樣式
    private fun setupPieChart(dataMap: Map<String, Int>) {
        val entries = ArrayList<PieEntry>()
        dataMap.forEach { (label, value) ->
            if (value > 0) entries.add(PieEntry(value.toFloat(), label))
        }

        // ✅ 顯示整數百分比格式
        class IntPercentFormatter : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}%"
            }
        }

        val dataSet = PieDataSet(entries, "浪費概況")
        dataSet.colors = listOf(
            Color.parseColor("#86BFFF"), // 廚餘藍
            Color.parseColor("#FFF59D")  // 完食黃
        )
        dataSet.valueTextSize = 16f
        dataSet.valueTextColor = Color.DKGRAY

        val data = PieData(dataSet)
        data.setValueFormatter(IntPercentFormatter())

        pieChart.setUsePercentValues(true)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.centerText = "浪費比例"
        pieChart.setCenterTextSize(18f)
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.animateY(1000)
        pieChart.invalidate()

        // 📊 設定圖例樣式
        val legend = pieChart.legend
        legend.textSize = 14f
        legend.formSize = 12f
        legend.xEntrySpace = 12f
        legend.yEntrySpace = 8f
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
        legend.setDrawInside(false)
    }
}
