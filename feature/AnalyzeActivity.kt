package com.example.smartfridgeassistant

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始化 Room DAO
        val database = AppDatabase.getDatabase(this)
        wasteDao = database.wasteDao()
        eatenDao = database.eatenDao()
        foodDao = database.foodDao()

        // 初始化 PieChart
        pieChart = findViewById(R.id.pie_chart)

        // 初始化 RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        outAdapter = OutItemAdapter(outList) { item ->
            // 处理返回按钮点击事件
            lifecycleScope.launch {
                when (item.state) {
                    "廚餘" -> {
                        // 从厨余表中删除
                        val wasteItems = wasteDao.getAll()
                        val wasteItem = wasteItems.find { it.name == item.name }
                        wasteItem?.let { wasteDao.delete(it) }
                    }
                    "完食" -> {
                        // 从完食表中删除
                        val eatenItems = eatenDao.getAll()
                        val eatenItem = eatenItems.find { it.name == item.name }
                        eatenItem?.let { eatenDao.delete(it) }
                    }
                }
                // 将食品添加回主列表
                foodDao.insert(FoodItem(
                    name = item.name,
                    category = "冷藏", // 默认分类
                    expiryDate = item.date,
                    note = item.note,
                    type = item.type
                ))
                // 刷新列表
                refreshList()
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = outAdapter

        // 加载数据
        refreshList()

        // 返回按钮
//        val fabBack = findViewById<FloatingActionButton>(R.id.fab_add)
//        fabBack.setOnClickListener {
//            val intent = Intent(this, Main::class.java)
//            startActivity(intent)
//            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
//            finish()
//        }
        setupBottomNav(this, R.id.nav_analyze)

    }

    private fun refreshList() {
        lifecycleScope.launch {
            try {
                val wasteList = wasteDao.getAll()
                val eatenList = eatenDao.getAll()

                outList.clear()
                wasteList.forEach { waste ->
                    outList.add(OutItem(waste.name, "廚餘", waste.date,waste.category,waste.type,waste.note))
                }
                eatenList.forEach { eaten ->
                    outList.add(OutItem(eaten.name, "完食", eaten.date,eaten.category,eaten.type,eaten.note))
                }
                outList.sortByDescending { it.date }
                outAdapter.notifyDataSetChanged()

                // 统计数据
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

    private fun setupPieChart(dataMap: Map<String, Int>) {
        val entries = ArrayList<PieEntry>()
        dataMap.forEach { (label, value) ->
            if (value > 0) entries.add(PieEntry(value.toFloat(), label))
        }
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

// ✅ 顯示百分比
        pieChart.setUsePercentValues(true)
        data.setValueFormatter(IntPercentFormatter())

// ✅ 設定 PieChart 外觀
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.centerText = "浪費比例"
        pieChart.setCenterTextSize(18f)
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.animateY(1000)
        pieChart.invalidate()

// ✅ 圖例設定
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
