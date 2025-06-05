package com.example.smartfridgeassistant

// 🔸 匯入必要套件
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

    // 🔹 定義通知權限請求代碼
    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 123
    }

    // 🔹 宣告所需變數
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var dao: FoodDao
    private lateinit var adapter: InlineFoodAdapter
    private var allFoods: List<FoodItem> = emptyList()
    private var selectedFoodName: String? = null
    private var reminderTimeDaysBefore: Int = 7  // 預設提醒時間：到期前 7 天

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setting)

        // 🔸 檢查是否有通知權限（Android 13+）
        checkNotificationPermission()

        // 🔸 避免系統 UI 遮住畫面內容
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 🔸 初始化資料庫、偏好設定
        sharedPreferences = getSharedPreferences("Reminders", MODE_PRIVATE)
        dao = AppDatabase.getDatabase(this).foodDao()
        setupBottomNav(this, R.id.nav_setting) // 設定底部導覽列選中狀態

        // 🔸 取得元件
        val inputField = findViewById<EditText>(R.id.editTextText)
        val recyclerView = findViewById<RecyclerView>(R.id.selected_food_list)
        val radioGroup = findViewById<RadioGroup>(R.id.radio_group_reminder)

        // 🔸 建立 RecyclerView 的自訂 Adapter，點選後自動填入欄位並隱藏建議清單
        adapter = InlineFoodAdapter { selected ->
            inputField.setText(selected)
            selectedFoodName = selected
            recyclerView.visibility = RecyclerView.GONE
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 🔸 使用 Coroutine 非同步載入資料庫中所有食材
        lifecycleScope.launch {
            allFoods = dao.getAllFoods()
        }

        // 🔸 當輸入框文字改變時，自動顯示對應的建議清單
        inputField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    recyclerView.visibility = RecyclerView.GONE
                } else {
                    // 🔸 搜尋包含輸入內容的食材名稱
                    val result = allFoods.map { it.name }
                        .filter { it.contains(query, ignoreCase = true) }
                    adapter.updateData(result)
                    recyclerView.visibility = if (result.isEmpty()) RecyclerView.GONE else RecyclerView.VISIBLE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 🔸 當使用者選擇提醒天數後，儲存到 SharedPreferences
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            reminderTimeDaysBefore = when (checkedId) {
                R.id.radio_week_before -> 7
                R.id.radio_day_before -> 1
                R.id.radio_same_day -> 0
                else -> 7
            }

            // 🔸 若已選擇食材，儲存設定
            if (selectedFoodName != null) {
                sharedPreferences.edit()
                    .putInt("reminder_${selectedFoodName}", reminderTimeDaysBefore)
                    .apply()

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

    // 🔸 檢查是否有通知權限（Android 13+ 需要顯式授權）
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

    // 🔸 接收通知權限回應
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

    // 🔹 自訂 RecyclerView Adapter（內嵌於此 Activity）
    class InlineFoodAdapter(
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<InlineFoodAdapter.ViewHolder>() {

        private var data: List<String> = emptyList()

        // 🔸 更新建議列表資料
        fun updateData(newData: List<String>) {
            data = newData
            notifyDataSetChanged()
        }

        // 🔸 每個項目使用 TextView 顯示
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
