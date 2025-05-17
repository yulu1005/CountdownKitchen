package com.example.smartfridgeassistant

import android.app.Activity
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView

fun setupBottomNav(activity: Activity, currentItemId: Int) {
    val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
    bottomNav.selectedItemId = currentItemId

    bottomNav.setOnItemSelectedListener { item ->
        if (item.itemId == currentItemId) {
            // 當前頁面，無需跳轉
            true
        } else {
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
