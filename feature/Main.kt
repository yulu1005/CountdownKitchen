package com.example.smartfridgeassistant

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.util.*

class Main : AppCompatActivity() {

    private lateinit var dao: FoodDao
    private val itemList = mutableListOf<FoodItem>()
    private lateinit var adapter: FoodAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // 初始化 Room DAO
        dao = AppDatabase.getDatabase(this).foodDao()

        // 初始化 RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = FoodAdapter(
            itemList = itemList,
            onItemClick = { foodItem ->
                showEditDialog(foodItem)
            }
        )

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter

        // 一進畫面就先讀取所有資料
        lifecycleScope.launch {
            itemList.clear()
            itemList.addAll(dao.getAll())
            adapter.notifyDataSetChanged()
        }
        val sortSpinner = findViewById<Spinner>(R.id.spinner) // 你的排序 Spinner

        // Spinner 選項內容
        val sortOptions = arrayOf("預設", "A~Z", "Z~A", "到期日近到遠", "到期日遠到近")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner.adapter = spinnerAdapter

        // 設定排序邏輯
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

        val fab = findViewById<FloatingActionButton>(R.id.fab_add)
        fab.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.add_item, null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            val nameEditText = dialogView.findViewById<EditText>(R.id.et_name)
            val categorySpinner = dialogView.findViewById<Spinner>(R.id.spinner_category)
            val dateText = dialogView.findViewById<TextView>(R.id.tv_expiry_date)
            val noteEditText = dialogView.findViewById<EditText>(R.id.et_note)
            val saveButton = dialogView.findViewById<Button>(R.id.btn_save)

            val categoryOptions = arrayOf("冷藏", "冷凍", "常溫")
            categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryOptions)

            dateText.setOnClickListener {
                val calendar = Calendar.getInstance()
                DatePickerDialog(
                    this,
                    { _, year, month, dayOfMonth ->
                        dateText.text = "$year-${month + 1}-$dayOfMonth"
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            saveButton.setOnClickListener {
                val name = nameEditText.text.toString()
                val category = categorySpinner.selectedItem.toString()
                val date = dateText.text.toString()
                val note = noteEditText.text.toString()

                if (name.isNotBlank() && date.isNotBlank()) {
                    val newItem = FoodItem(
                        name = name,
                        category = category,
                        expiryDate = date,
                        note = note
                    )

                    lifecycleScope.launch {
                        dao.insert(newItem)
                        itemList.clear()
                        itemList.addAll(dao.getAll())
                        adapter.notifyDataSetChanged()
                    }

                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "請填寫名稱與到期日", Toast.LENGTH_SHORT).show()
                }
            }

            dialog.show()
        }
    }
    private fun showEditDialog(item: FoodItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.edit_item, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val etName = dialogView.findViewById<EditText>(R.id.et_name)
        val etNote = dialogView.findViewById<EditText>(R.id.et_note)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinner_category)
        val tvDate = dialogView.findViewById<TextView>(R.id.tv_expiry_date)
        val btnDone = dialogView.findViewById<Button>(R.id.btn_done)


        // 填入現有資料
        etName.setText(item.name)
        etNote.setText(item.note)
        tvDate.text = item.expiryDate

        val categoryOptions = arrayOf("冷藏", "冷凍", "常溫")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryOptions)
        spinner.adapter = adapter
        spinner.setSelection(categoryOptions.indexOf(item.category))

        tvDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                tvDate.text = "$year-${month + 1}-$day"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnDone.setOnClickListener {
            val newItem = item.copy(
                name = etName.text.toString(),
                note = etNote.text.toString(),
                category = spinner.selectedItem.toString(),
                expiryDate = tvDate.text.toString()
            )
            lifecycleScope.launch {
                dao.update(newItem)
                itemList.clear()
                itemList.addAll(dao.getAll())
                adapter.notifyDataSetChanged()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

}
