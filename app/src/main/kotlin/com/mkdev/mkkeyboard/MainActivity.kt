package com.mkdev.mkkeyboard

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(7, 21, 47)
        window.navigationBarColor = Color.rgb(7, 21, 47)
        setContentView(createContent())
    }

    override fun onResume() {
        super.onResume()
        if (::statusText.isInitialized) {
            updateStatus()
        }
    }

    private fun createContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(48), dp(28), dp(28))
            setBackgroundColor(Color.rgb(7, 21, 47))
        }

        val mark = TextView(this).apply {
            text = "⌨"
            textSize = 46f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(102, 228, 255))
        }
        root.addView(mark, LinearLayout.LayoutParams(-1, dp(70)))

        val title = TextView(this).apply {
            text = getString(com.mkdev.mkkeyboard.R.string.setup_title)
            textSize = 32f
            typeface = Typeface.create("sans", Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))

        val subtitle = TextView(this).apply {
            text = getString(com.mkdev.mkkeyboard.R.string.setup_subtitle)
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(180, 205, 232))
            setPadding(0, dp(12), 0, 0)
        }
        root.addView(subtitle, LinearLayout.LayoutParams(-1, -2))

        statusText = TextView(this).apply {
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(102, 228, 255))
            setPadding(0, dp(26), 0, dp(20))
        }
        root.addView(statusText, LinearLayout.LayoutParams(-1, -2))

        val enableButton = createButton(getString(com.mkdev.mkkeyboard.R.string.enable_keyboard)) {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        root.addView(enableButton, buttonParams())

        val chooseButton = createButton(getString(com.mkdev.mkkeyboard.R.string.choose_keyboard)) {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showInputMethodPicker()
        }
        root.addView(chooseButton, buttonParams())

        val hint = TextView(this).apply {
            text = getString(com.mkdev.mkkeyboard.R.string.setup_hint)
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(135, 166, 202))
            setPadding(0, dp(24), 0, 0)
        }
        root.addView(hint, LinearLayout.LayoutParams(-1, -2))
        updateStatus()
        return root
    }

    private fun createButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 15f
            isAllCaps = false
            setTextColor(Color.rgb(7, 21, 47))
            setBackgroundColor(Color.rgb(102, 228, 255))
            setOnClickListener { action() }
        }
    }

    private fun buttonParams() = LinearLayout.LayoutParams(-1, dp(52)).apply {
        setMargins(0, dp(6), 0, dp(6))
    }

    private fun updateStatus() {
        if (!::statusText.isInitialized) return
        val enabledMethods = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_INPUT_METHODS)
        val isEnabled = enabledMethods?.contains(packageName) == true
        statusText.text = getString(
            if (isEnabled) com.mkdev.mkkeyboard.R.string.keyboard_enabled
            else com.mkdev.mkkeyboard.R.string.keyboard_ready
        )
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}