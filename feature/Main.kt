package com.example.smartfridgeassistant

// 🔹 1. 匯入所需套件（UI、資料庫、掃描器、圖表、權限、日期等）
import android.webkit.WebView
import android.webkit.WebViewClient
import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*

class Main : AppCompatActivity() {
    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 123
    }

    // 🔹 2. 宣告資料庫 DAO、資料清單、Adapter
    private lateinit var dao: FoodDao
    private lateinit var wasteDao: WasteDao
    private lateinit var eatenDao: EatenDao
    private val itemList = mutableListOf<FoodItem>()
    private lateinit var adapter: FoodAdapter
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main)

        // 🔹 3. 小燈泡按鈕點擊 → 開啟網站連結
        val lightbulb: ImageButton = findViewById(R.id.lightbulb)
        lightbulb.setOnClickListener {
            val url = "https://countdownkitchenapp.netlify.app/"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        // 🔹 4. 權限檢查：通知與相機（QR 掃描用）
        checkNotificationPermission()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        }

        // 🔹 5. 初始化資料庫與 DAO
        db = AppDatabase.getDatabase(this)
        val database = AppDatabase.getDatabase(this)
        dao = database.foodDao()
        wasteDao = database.wasteDao()
        eatenDao = database.eatenDao()

        // 🔹 6. 掃描按鈕 → 啟動 ZXing 掃描器
        val scanButton = findViewById<ImageButton>(R.id.scanButton)
        scanButton.setOnClickListener {
            val integrator = IntentIntegrator(this)
            integrator.setCaptureActivity(CustomCaptureActivity::class.java)  // ✅ 指定自訂掃描畫面
            integrator.setPrompt("請對準 QR Code")
            integrator.setCameraId(0)  // 後鏡頭
            integrator.setBeepEnabled(true)
            integrator.setOrientationLocked(true)
            integrator.initiateScan()
        }

        // 🔹 7. 底部導覽列：切換頁面
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_setting -> { startActivity(Intent(this, SettingActivity::class.java)); true }
                R.id.nav_analyze -> { startActivity(Intent(this, AnalyzeActivity::class.java)); true }
                R.id.nav_search -> { startActivity(Intent(this, SearchActivity::class.java)); true }
                else -> false
            }
        }

        // 🔹 8. 設定 RecyclerView 與 Adapter 各種操作（點擊、刪除、廚餘、完食）
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = FoodAdapter(
            itemList = itemList,
            onItemClick = { foodItem -> showEditDialog(foodItem) },
            onDeleteItem = { foodItem -> lifecycleScope.launch { dao.delete(foodItem); refreshItemList() } },
            onTrashItem = { foodItem ->
                lifecycleScope.launch {
                    wasteDao.insert(WasteItem(name = foodItem.name,
                        category = foodItem.category,
                        note = foodItem.note,
                        type = foodItem.type,
                        date = foodItem.expiryDate))
                    dao.delete(foodItem)
                    refreshItemList()
                }
            },
            onEatItem = { foodItem ->
                lifecycleScope.launch {
                    eatenDao.insert(EatenItem(
                        name = foodItem.name,
                        category = foodItem.category,
                        note = foodItem.note,
                        type = foodItem.type,
                        date = foodItem.expiryDate))
                    dao.delete(foodItem)
                    refreshItemList()
                }
            }
        )
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter

        // 🔹 9. 排序 Spinner：預設 / 名稱排序 / 到期日排序
        val sortSpinner = findViewById<Spinner>(R.id.spinner)
        val sortOptions = arrayOf("預設", "A~Z", "Z~A", "到期日近到遠", "到期日遠到近")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner.adapter = spinnerAdapter
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    1 -> itemList.sortBy { it.name.lowercase() }
                    2 -> itemList.sortByDescending { it.name.lowercase() }
                    3 -> itemList.sortBy { it.expiryDate }
                    4 -> itemList.sortByDescending { it.expiryDate }
                }
                adapter.notifyDataSetChanged()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 🔹 10. 新增食材 FloatingButton（彈出新增對話框）
        val fab = findViewById<FloatingActionButton>(R.id.fab_add)
        fab.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.add_item, null)
            val dialog = AlertDialog.Builder(this).setView(dialogView).create()
            val nameEditText = dialogView.findViewById<EditText>(R.id.et_name)
            val categorySpinner = dialogView.findViewById<Spinner>(R.id.spinner_category)
            val dateText = dialogView.findViewById<TextView>(R.id.tv_expiry_date)
            val noteEditText = dialogView.findViewById<EditText>(R.id.et_note)
            val chipGroup = dialogView.findViewById<ChipGroup>(R.id.chip_group)
            val saveButton = dialogView.findViewById<Button>(R.id.btn_save)

            val categoryOptions = arrayOf("冷藏", "冷凍", "常溫")
            categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryOptions)

            var selectedType = ""
            chipGroup.setOnCheckedChangeListener { group, checkedId ->
                val chip = group.findViewById<Chip>(checkedId)
                selectedType = chip?.text?.toString() ?: ""
            }

            dateText.setOnClickListener {
                val datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("選擇到期日").build()
                datePicker.show(supportFragmentManager, "DATE_PICKER")
                datePicker.addOnPositiveButtonClickListener {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    dateText.text = sdf.format(Date(it))
                }
            }

            saveButton.setOnClickListener {
                val name = nameEditText.text.toString()
                val category = categorySpinner.selectedItem.toString()
                val date = dateText.text.toString()
                val note = noteEditText.text.toString()
                val type = selectedType.trim()
                val datePattern = Regex("\\d{4}-\\d{1,2}-\\d{1,2}")
                if (name.isNotBlank() && type.isNotBlank() && date.matches(datePattern)) {
                    val newItem = FoodItem(name=name, category=category, expiryDate=date, note=note, type=type)
                    lifecycleScope.launch { dao.insert(newItem); refreshItemList() }
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "請填寫完整資訊（名稱、種類與到期日）", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.show()
        }

        // 🔹 11. 載入資料
        refreshItemList()
    }

    // 🔹 12. 編輯食材對話框（點擊卡片觸發）
    private fun showEditDialog(item: FoodItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.edit_item, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        val etName = dialogView.findViewById<EditText>(R.id.et_name)
        val etNote = dialogView.findViewById<EditText>(R.id.et_note)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinner_category)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinner_type)
        val tvDate = dialogView.findViewById<TextView>(R.id.tv_expiry_date)
        val btnDone = dialogView.findViewById<Button>(R.id.btn_done)

        etName.setText(item.name)
        etNote.setText(item.note)
        tvDate.text = item.expiryDate

        val categoryOptions = arrayOf("冷藏", "冷凍", "常溫")
        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryOptions)
        spinnerCategory.setSelection(categoryOptions.indexOf(item.category))

        val typeOptions = arrayOf("肉類", "海鮮類","蔬菜類","乳品類","水果類", "飲料類", "點心類","熟食","其他")
        spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, typeOptions)
        spinnerType.setSelection(typeOptions.indexOf(item.type))

        tvDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("選擇到期日").build()
            datePicker.show(this@Main.supportFragmentManager, "EDIT_DATE_PICKER")
            datePicker.addOnPositiveButtonClickListener {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                tvDate.text = sdf.format(Date(it))
            }
        }

        btnDone.setOnClickListener {
            val newItem = item.copy(
                name = etName.text.toString(),
                note = etNote.text.toString(),
                category = spinnerCategory.selectedItem.toString(),
                type = spinnerType.selectedItem.toString(),
                expiryDate = tvDate.text.toString()
            )
            lifecycleScope.launch {
                dao.update(newItem)
                refreshItemList()
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    // 🔹 13. 刷新畫面資料
    private fun refreshItemList() {
        lifecycleScope.launch {
            val data = dao.getAll()
            itemList.clear()
            itemList.addAll(data)
            adapter.notifyDataSetChanged()
        }
    }

    // 🔹 14. 檢查通知權限（Android 13+）
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    // 🔹 15. 處理權限請求回傳結果
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            Toast.makeText(
                this,
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    "通知權限已授予" else "需要通知權限才能接收提醒",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 🔹 16. 處理掃描 QRCode 結果並寫入資料庫
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            try {
                val json = JSONObject(result.contents)
                val foodItem = FoodItem(
                    name = json.getString("name"),
                    category = json.optString("category", ""),
                    expiryDate = json.getString("expiryDate"),
                    note = json.optString("note", ""),
                    type = json.optString("type", "")
                )
                val db = AppDatabase.getDatabase(this)
                lifecycleScope.launch {
                    db.foodDao().insert(foodItem)
                    Toast.makeText(this@Main, "✅ 已新增：${foodItem.name}", Toast.LENGTH_SHORT).show()
                    refreshItemList()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "❌ QR 格式錯誤", Toast.LENGTH_SHORT).show()
                Log.e("QR_ERROR", e.toString())
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
