package com.example.smartfridgeassistant

import android.app.DatePickerDialog
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
import com.example.finalproject.Food
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SearchActivity : AppCompatActivity() {

    private lateinit var adapter: SearchResultAdapter
    private var foodList: List<FoodItem> = emptyList()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

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

        // 初始化元件
        val searchInput = findViewById<EditText>(R.id.search_input)
        val datePickerLayout = findViewById<LinearLayout>(R.id.datePickerLayout)
        val dateInput = findViewById<EditText>(R.id.date_input)
        val btnCalendar = findViewById<ImageButton>(R.id.btn_calendar)
        val typeSpinner = findViewById<Spinner>(R.id.type_spinner)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        val searchTypeGroup = findViewById<RadioGroup>(R.id.searchTypeGroup)

        // 设置类型选择器的选项
        val typeOptions = arrayOf("肉類", "蔬菜類","乳品類","水果類", "飲料類", "點心類","熟食","其他")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = spinnerAdapter

        adapter = SearchResultAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            foodList = db.foodDao().getAllFoods()
            adapter.updateData(foodList)  // 預設載入全部資料
        }

        // 设置日期选择器
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

        // 设置搜索类型切换监听器
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

        // 设置名称搜索监听器
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                performSearch()
            }
        })

        // 设置类型选择监听器
        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                performSearch()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        setupBottomNav(this, R.id.nav_search)
    }

    private fun performSearch() {
        val searchInput = findViewById<EditText>(R.id.search_input)
        val dateInput = findViewById<EditText>(R.id.date_input)
        val typeSpinner = findViewById<Spinner>(R.id.type_spinner)
        val searchTypeGroup = findViewById<RadioGroup>(R.id.searchTypeGroup)

        val filteredList = when (searchTypeGroup.checkedRadioButtonId) {
            R.id.radioName -> {
                val query = searchInput.text.toString().trim()
                if (query.isEmpty()) {
                    foodList
                } else {
                    foodList.filter { it.name.contains(query, ignoreCase = true) }
                }
            }
            R.id.radioDate -> {
                val dateStr = dateInput.text.toString().trim()
                if (dateStr.isEmpty()) {
                    foodList
                } else {
                    try {
                        val targetDate = dateFormat.parse(dateStr)
                        foodList.filter { 
                            val foodDate = dateFormat.parse(it.expiryDate)
                            foodDate != null && targetDate != null && foodDate == targetDate
                        }
                    } catch (e: Exception) {
                        foodList
                    }
                }
            }
            R.id.radioType -> {
                val selectedType = typeSpinner.selectedItem.toString()
                foodList.filter { it.type == selectedType }
            }
            else -> foodList
        }

        adapter.updateData(filteredList)
    }
}
