package com.example.smartfridgeassistant

// 🔹 1. 匯入所需套件
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
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import com.github.mikephil.charting.formatter.ValueFormatter

class AnalyzeActivity : AppCompatActivity() {

    // 🔹 2. 宣告 DAO 與資料與畫面元件變數
    private lateinit var wasteDao: WasteDao
    private lateinit var eatenDao: EatenDao
    private lateinit var foodDao: FoodDao
    private val outList = mutableListOf<OutItem>()
    private lateinit var outAdapter: OutItemAdapter
    private lateinit var pieChart: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_analyze)

        // 🔹 3. 設定狀態列邊距調整（避免被擋住）
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 🔹 4. 初始化資料庫 DAO
        val database = AppDatabase.getDatabase(this)
        wasteDao = database.wasteDao()
        eatenDao = database.eatenDao()
        foodDao = database.foodDao()

        // 🔹 5. 初始化圓餅圖 PieChart
        pieChart = findViewById(R.id.pie_chart)

        // 🔹 6. 初始化 RecyclerView 與 Adapter
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        outAdapter = OutItemAdapter(outList) { item ->

            // ✅ 點擊「返回主列表」按鈕處理邏輯（將資料從廚餘或完食移回主表）
            lifecycleScope.launch {
                when (item.state) {
                    "廚餘" -> {
                        val wasteItems = wasteDao.getAll()
                        val wasteItem = wasteItems.find { it.name == item.name }
                        wasteItem?.let { wasteDao.delete(it) }
                    }
                    "完食" -> {
                        val eatenItems = eatenDao.getAll()
                        val eatenItem = eatenItems.find { it.name == item.name }
                        eatenItem?.let { eatenDao.delete(it) }
                    }
                }

                // ✅ 新增回 Food 表（預設分類為冷藏）
                foodDao.insert(FoodItem(
                    name = item.name,
                    category = "冷藏",
                    expiryDate = item.date,
                    note = item.note,
                    type = item.type
                ))

                // ✅ 重新整理列表與圖表
                refreshList()
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = outAdapter

        // 🔹 7. 初次載入資料與更新畫面
        refreshList()

        // 🔹 8. 啟用底部導覽列（分析頁高亮）
        setupBottomNav(this, R.id.nav_analyze)
    }

    // 🔹 9. 重新整理廚餘與完食紀錄，同步更新 RecyclerView 與 PieChart
    private fun refreshList() {
        lifecycleScope.launch {
            try {
                val wasteList = wasteDao.getAll()
                val eatenList = eatenDao.getAll()

                // ✅ 整合資料進 outList，依日期排序
                outList.clear()
                wasteList.forEach { waste ->
                    outList.add(OutItem(waste.name, "廚餘", waste.date, waste.category, waste.type, waste.note))
                }
                eatenList.forEach { eaten ->
                    outList.add(OutItem(eaten.name, "完食", eaten.date, eaten.category, eaten.type, eaten.note))
                }
                outList.sortByDescending { it.date }
                outAdapter.notifyDataSetChanged()

                // ✅ 更新圓餅圖數據
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

    // 🔹 10. 設定並顯示圓餅圖 PieChart
    private fun setupPieChart(dataMap: Map<String, Int>) {
        val entries = ArrayList<PieEntry>()
        dataMap.forEach { (label, value) ->
            if (value > 0) entries.add(PieEntry(value.toFloat(), label))
        }

        // ✅ 自訂顯示百分比格式
        class IntPercentFormatter : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}%"
            }
        }

        val dataSet = PieDataSet(entries, "浪費概況")
        dataSet.colors = listOf(Color.parseColor("#86BFFF"), Color.parseColor("#FFF59D"))
        dataSet.valueTextSize = 16f
        dataSet.valueTextColor = Color.DKGRAY
        val data = PieData(dataSet)

        // ✅ 設定 PieChart 顯示樣式
        pieChart.setUsePercentValues(true)
        data.setValueFormatter(IntPercentFormatter())
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.centerText = "浪費比例"
        pieChart.setCenterTextSize(18f)
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.animateY(1000)
        pieChart.invalidate()

        // ✅ 設定圖例樣式
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
