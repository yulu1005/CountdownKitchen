package com.example.smartfridgeassistant

// 🔹 套件匯入
import android.app.Activity
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView

// 🔧 通用函式：設定底部導覽列並處理點擊跳轉
fun setupBottomNav(activity: Activity, currentItemId: Int) {

    // ✅ 1. 取得 BottomNavigationView 並標記當前選中項目
    val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
    bottomNav.selectedItemId = currentItemId

    // ✅ 2. 設定點擊導覽列時的行為
    bottomNav.setOnItemSelectedListener { item ->

        // 👉 若點到的項目是目前所在頁面，不執行跳轉
        if (item.itemId == currentItemId) {
            true
        } else {
            // 👉 否則根據選項跳轉到對應的 Activity，並關閉目前頁面
            when (item.itemId) {
                R.id.nav_home -> {
                    activity.startActivity(Intent(activity, Main::class.java))
                    activity.finish()
                    true
                }
                R.id.nav_analyze -> {
                    activity.startActivity(Intent(activity, AnalyzeActivity::class.java))
                    activity.finish()
                    true
                }
                R.id.nav_setting -> {
                    activity.startActivity(Intent(activity, SettingActivity::class.java))
                    activity.finish()
                    true
                }
                R.id.nav_search -> {
                    activity.startActivity(Intent(activity, SearchActivity::class.java))
                    activity.finish()
                    true
                }
                else -> false
            }
        }
    }
}
