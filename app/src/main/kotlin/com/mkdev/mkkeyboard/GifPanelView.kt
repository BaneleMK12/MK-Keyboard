package com.mkdev.mkkeyboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class GifPanelView(
    context: Context,
    private val onGifSelected: (String) -> Unit,
    private val onClose: () -> Unit,
    private val onSearchKey: (MKKeyboardView.KeyAction) -> Unit,
    private val onSearchActivated: () -> Unit
) : LinearLayout(context) {
    var isSearchActive = false
        private set
    private val grid = LinearLayout(context).apply {
        orientation = VERTICAL
        setPadding(dp(8), 0, dp(8), dp(12))
    }
    private val queryInput = EditText(context).apply {
        hint = "Search GIPHY"
        setSingleLine(true)
        setShowSoftInputOnFocus(false)
        setTextColor(Color.WHITE)
        setHintTextColor(Color.rgb(150, 190, 220))
        setPadding(dp(14), 0, dp(10), 0)
        background = rounded(Color.rgb(19, 65, 111), dp(18).toFloat())
    }
    private val loading = ProgressBar(context).apply { visibility = GONE }

    init {
        orientation = VERTICAL
        isFocusableInTouchMode = true
        setBackgroundColor(Color.rgb(7, 21, 47))

        val header = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(6))
        }
        val close = ImageButton(context).apply {
            contentDescription = "Close GIF picker"
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { onClose() }
        }
        header.addView(close, LayoutParams(dp(42), dp(42)))
        header.addView(queryInput, LayoutParams(0, dp(42), 1f).apply {
            setMargins(dp(6), 0, dp(6), 0)
        })
        queryInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) isSearchActive = true
        }
        queryInput.setOnClickListener {
            isSearchActive = true
            queryInput.requestFocus()
            onSearchActivated()
        }
        val search = TextView(context).apply {
            text = "Search"
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(7, 21, 47))
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = rounded(Color.rgb(102, 228, 255), dp(18).toFloat())
            setOnClickListener { load(queryInput.text.toString().trim()) }
        }
        header.addView(search, LayoutParams(dp(78), dp(42)))
        addView(header)

        val attribution = TextView(context).apply {
            text = "GIFs from GIPHY  •  Powered by GIPHY"
            textSize = 11f
            setTextColor(Color.rgb(150, 190, 220))
            setPadding(dp(16), dp(2), dp(16), dp(8))
        }
        addView(attribution)

        val content = ScrollView(context).apply { isFillViewport = true }
        content.addView(grid, ViewGroup.LayoutParams(-1, -2))
        addView(content, LayoutParams(-1, 0, 1f))
        addView(loading, LayoutParams(-1, dp(34)))

        queryInput.setOnEditorActionListener { _, _, _ ->
            load(queryInput.text.toString().trim())
            true
        }
        requestFocus()
        load("")
    }

    fun handleSearchKey(key: MKKeyboardView.KeyAction) {
        if (!isSearchActive) return
        when (key) {
            is MKKeyboardView.KeyAction.Text -> queryInput.append(key.value)
            MKKeyboardView.KeyAction.Space -> queryInput.append(" ")
            MKKeyboardView.KeyAction.Delete -> {
                val text = queryInput.text
                if (text.isNotEmpty()) text.delete(text.length - 1, text.length)
            }
            MKKeyboardView.KeyAction.Enter -> load(queryInput.text.toString().trim())
            else -> Unit
        }
        onSearchKey(key)
    }

    private fun load(query: String) {
        loading.visibility = VISIBLE
        thread {
            val endpoint = if (query.isBlank()) "trending" else "search"
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.giphy.com/v1/gifs/$endpoint?api_key=${BuildConfig.GIPHY_API_KEY}" +
                if (query.isBlank()) "&limit=24&rating=pg-13" else "&q=$encoded&limit=24&rating=pg-13"
            val results = runCatching { fetchGifUrls(url) }.getOrElse { emptyList() }
            post {
                loading.visibility = GONE
                render(results)
            }
        }
    }

    private fun render(urls: List<String>) {
        grid.removeAllViews()
        if (urls.isEmpty()) {
            grid.addView(TextView(context).apply {
                text = "No GIFs found. Try another search."
                setTextColor(Color.rgb(180, 205, 232))
                gravity = Gravity.CENTER
                setPadding(0, dp(40), 0, dp(40))
            })
            return
        }
        urls.chunked(3).forEach { rowUrls ->
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                setPadding(0, dp(4), 0, dp(4))
            }
            rowUrls.forEach { gifUrl ->
                val image = ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    contentDescription = "Send GIF"
                    background = rounded(Color.rgb(19, 65, 111), dp(8).toFloat())
                    setOnClickListener { onGifSelected(gifUrl) }
                }
                row.addView(image, LayoutParams(0, dp(94), 1f).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                })
                thread {
                    val bitmap = runCatching { downloadBitmap(gifUrl) }.getOrNull()
                    image.post {
                        if (bitmap != null) image.setImageBitmap(bitmap)
                    }
                }
            }
            repeat(3 - rowUrls.size) { row.addView(SpaceView(context), LayoutParams(0, dp(94), 1f)) }
            grid.addView(row)
        }
    }

    private fun fetchGifUrls(apiUrl: String): List<String> {
        val connection = URL(apiUrl).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            val body = connection.inputStream.bufferedReader().use { reader -> reader.readText() }
            Regex("\"fixed_height_downsampled\"\\s*:\\s*\\{[^}]*?\"url\"\\s*:\\s*\"([^\"]+)\"")
                .findAll(body)
                .map { match -> match.groupValues[1].replace("\\/", "/") }
                .toList()
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadBitmap(url: String): Bitmap? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            BitmapFactory.decodeStream(BufferedInputStream(connection.inputStream))
        } finally {
            connection.disconnect()
        }
    }

    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private class SpaceView(context: Context) : View(context)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}