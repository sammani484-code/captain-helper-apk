package com.smartclicker.captainhelper

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var tvStatus: TextView
    private lateinit var switchAutoAccept: Switch
    private lateinit var etMinFare: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        switchAutoAccept = findViewById(R.id.switchAutoAccept)
        etMinFare = findViewById(R.id.etMinFare)
        val btnSaveSettings = findViewById<Button>(R.id.btnSaveSettings)
        val btnOpenAccessibility = findViewById<Button>(R.id.btnOpenAccessibility)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        switchAutoAccept.isChecked = prefs.getBoolean(KEY_AUTO_ACCEPT, true)
        etMinFare.setText(prefs.getInt(KEY_MIN_FARE, DEFAULT_MIN_FARE).toString())

        switchAutoAccept.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_AUTO_ACCEPT, isChecked).apply()
        }

        btnSaveSettings.setOnClickListener {
            val minFare = etMinFare.text.toString().toIntOrNull() ?: DEFAULT_MIN_FARE
            prefs.edit().putInt(KEY_MIN_FARE, minFare).apply()
            etMinFare.setText(minFare.toString())
            Toast.makeText(this, "Minimum fare saved: ₹$minFare", Toast.LENGTH_SHORT).show()
        }

        btnOpenAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        val enabled = isAccessibilityServiceEnabled()
        tvStatus.text = if (enabled) {
            "Accessibility Service: ENABLED"
        } else {
            "Accessibility Service: DISABLED (Tap button below to enable)"
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponent = ComponentName(this, OrderAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            val componentName = ComponentName.unflattenFromString(splitter.next())
            if (componentName == expectedComponent) {
                return true
            }
        }
        return false
    }

    companion object {
        const val PREFS_NAME = "captain_helper_prefs"
        const val KEY_AUTO_ACCEPT = "auto_accept_enabled"
        const val KEY_MIN_FARE = "min_fare"
        const val DEFAULT_MIN_FARE = 100
    }
}
