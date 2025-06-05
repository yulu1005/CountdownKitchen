package com.example.smartfridgeassistant

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class SettingActivity : AppCompatActivity() {

    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 123
    }

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var dao: FoodDao
    private lateinit var adapter: InlineFoodAdapter
    private var allFoods: List<FoodItem> = emptyList()
    private var selectedFoodName: String? = null
    private var reminderTimeDaysBefore: Int = 7  // 預設：7天前

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setting)

        checkNotificationPermission()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("Reminders", MODE_PRIVATE)
        dao = AppDatabase.getDatabase(this).foodDao()
        setupBottomNav(this, R.id.nav_setting)

        val inputField = findViewById<EditText>(R.id.editTextText)
        val recyclerView = findViewById<RecyclerView>(R.id.selected_food_list)
        val radioGroup = findViewById<RadioGroup>(R.id.radio_group_reminder)

        // 🔹 建立 RecyclerView 搜尋邏輯
        adapter = InlineFoodAdapter { selected ->
            inputField.setText(selected)
            selectedFoodName = selected
            recyclerView.visibility = RecyclerView.GONE
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            allFoods = dao.getAllFoods()
        }

        inputField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    recyclerView.visibility = RecyclerView.GONE
                } else {
                    val result = allFoods.map { it.name }
                        .filter { it.contains(query, ignoreCase = true) }
                    adapter.updateData(result)
                    recyclerView.visibility = if (result.isEmpty()) RecyclerView.GONE else RecyclerView.VISIBLE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 🔹 使用者選擇提醒時間 → 儲存設定 & 顯示成功提示
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            reminderTimeDaysBefore = when (checkedId) {
                R.id.radio_week_before -> 7  // 提前一週
                R.id.radio_day_before -> 1   // 提前一天
                R.id.radio_same_day -> 0     // 當天提醒
                else -> 7
            }

            if (selectedFoodName != null) {
                val key = "reminder_${selectedFoodName}"

                // 先移除舊的設定（保險做法）
                sharedPreferences.edit().remove(key).apply()

                // 儲存新的提醒設定
                sharedPreferences.edit().putInt(key, reminderTimeDaysBefore).apply()

                // Log 可選：幫助你在 Logcat 裡 debug
                Log.d("ReminderSetting", "已設定 $selectedFoodName ➜ 提前 $reminderTimeDaysBefore 天提醒")

                Toast.makeText(
                    this,
                    "已設定提醒：${selectedFoodName} 的到期日前 $reminderTimeDaysBefore 天",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this, "請先選擇食材名稱", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "通知權限已授予", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "需要通知權限才能接收提醒", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🔹 內建簡易 Adapter（不需額外檔案）
    class InlineFoodAdapter(
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<InlineFoodAdapter.ViewHolder>() {

        private var data: List<String> = emptyList()

        fun updateData(newData: List<String>) {
            data = newData
            notifyDataSetChanged()
        }

        inner class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView) {
            init {
                textView.setOnClickListener {
                    onClick(data[adapterPosition])
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val tv = TextView(parent.context).apply {
                textSize = 18f
                setPadding(24, 24, 24, 24)
            }
            return ViewHolder(tv)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = data[position]
        }

        override fun getItemCount(): Int = data.size
    }
}
