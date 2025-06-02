package com.example.smartfridgeassistant

// 🔸 1. 匯入相關套件
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SearchActivity : AppCompatActivity() {

    // 🔸 2. 宣告變數
    private lateinit var adapter: SearchAdapter
    private var foodList: List<FoodItem> = emptyList()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // 🔸 3. 設定狀態列透明與全螢幕模式
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                systemBars.top,   // ✅ 增加上方邊距，避開狀態列
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        // 🔸 4. 取得畫面上的元件
        val searchInput = findViewById<EditText>(R.id.search_input)
        val datePickerLayout = findViewById<LinearLayout>(R.id.datePickerLayout)
        val dateInput = findViewById<EditText>(R.id.date_input)
        val btnCalendar = findViewById<ImageButton>(R.id.btn_calendar)
        val typeSpinner = findViewById<Spinner>(R.id.type_spinner)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        val searchTypeGroup = findViewById<RadioGroup>(R.id.searchTypeGroup)

        // 🔸 5. 設定下拉選單（類型選擇器）
        val typeOptions = arrayOf("肉類","海鮮類", "蔬菜類","乳品類","水果類", "飲料類", "點心類","熟食","其他")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = spinnerAdapter

        // 🔸 6. 當日期欄位改變時觸發搜尋
        dateInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                performSearch()
            }
        })

        // 🔸 7. 設定 RecyclerView 和資料來源
        adapter = SearchAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 🔸 8. 從資料庫抓取所有食材
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            foodList = db.foodDao().getAllFoods()
            adapter.updateData(foodList)  // 預設載入全部資料
        }

        // 🔸 9. 建立日期選擇器並設定點選事件
        btnCalendar.setOnClickListener {
            val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                .setTitleText("選擇日期")
                .build()

            datePicker.show(supportFragmentManager, "SEARCH_DATE_PICKER")

            datePicker.addOnPositiveButtonClickListener { selection ->
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.format(Date(selection))
                dateInput.setText(date)
                performSearch()
            }
        }

        // 🔸 10. 切換搜尋類型（名稱、日期、類型）
        searchTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioName -> {
                    searchInput.visibility = View.VISIBLE
                    datePickerLayout.visibility = View.GONE
                    typeSpinner.visibility = View.GONE
                    searchInput.hint = "請輸入食材名稱"
                }
                R.id.radioDate -> {
                    searchInput.visibility = View.GONE
                    datePickerLayout.visibility = View.VISIBLE
                    typeSpinner.visibility = View.GONE
                }
                R.id.radioType -> {
                    searchInput.visibility = View.GONE
                    datePickerLayout.visibility = View.GONE
                    typeSpinner.visibility = View.VISIBLE
                }
            }
            performSearch()
        }

        // 🔸 11. 當名稱輸入框改變時即時搜尋
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                performSearch()
            }
        })

        // 🔸 12. 類型下拉選單改變時觸發搜尋
        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                performSearch()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 🔸 13. 啟用底部導覽列（導入函式）
        setupBottomNav(this, R.id.nav_search)
    }

    // 🔸 14. 實作搜尋功能（根據搜尋類型過濾 foodList）
    private fun performSearch() {
        val searchInput = findViewById<EditText>(R.id.search_input)
        val dateInput = findViewById<EditText>(R.id.date_input)
        val typeSpinner = findViewById<Spinner>(R.id.type_spinner)
        val searchTypeGroup = findViewById<RadioGroup>(R.id.searchTypeGroup)

        val filteredList = when (searchTypeGroup.checkedRadioButtonId) {

            // 🔹 名稱搜尋
            R.id.radioName -> {
                val query = searchInput.text.toString().trim()
                if (query.isEmpty()) {
                    foodList
                } else {
                    foodList.filter { it.name.contains(query, ignoreCase = true) }
                }
            }

            // 🔹 日期搜尋（支援 yyyy-MM 與 yyyy-MM-dd）
            R.id.radioDate -> {
                val dateStr = dateInput.text.toString().trim()
                if (dateStr.isEmpty()) {
                    foodList
                } else {
                    try {
                        if (dateStr.matches(Regex("\\d{4}-\\d{2}"))) {
                            // 🔸 處理 yyyy-MM → 抓整個月
                            val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                            val fullFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                            val startDate = fullFormat.parse("${dateStr}-01")!!
                            val calendar = Calendar.getInstance().apply { time = startDate }
                            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                            val endDate = calendar.time

                            foodList.filter {
                                val foodDate = fullFormat.parse(it.expiryDate)
                                foodDate != null && (foodDate >= startDate && foodDate <= endDate)
                            }
                        } else {
                            // 🔸 處理 yyyy-MM-dd → 抓特定日期
                            val fullFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val targetDate = fullFormat.parse(dateStr)
                            foodList.filter {
                                val foodDate = fullFormat.parse(it.expiryDate)
                                foodDate != null && targetDate != null && foodDate == targetDate
                            }
                        }
                    } catch (e: Exception) {
                        foodList
                    }
                }
            }

            // 🔹 類型搜尋
            R.id.radioType -> {
                val selectedType = typeSpinner.selectedItem.toString()
                foodList.filter { it.type == selectedType }
            }

            // 🔹 預設回傳全部資料
            else -> foodList
        }

        // 🔹 更新顯示的資料
        adapter.updateData(filteredList)
    }
}
