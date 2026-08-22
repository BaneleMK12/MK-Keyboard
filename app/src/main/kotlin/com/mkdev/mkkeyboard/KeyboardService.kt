package com.mkdev.mkkeyboard

import android.content.ClipDescription
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class KeyboardService : InputMethodService() {
    private var keyboardView: MKKeyboardView? = null
    private var rootView: FrameLayout? = null
    private var shifted = false

    override fun onCreateInputView(): View {
        keyboardView = MKKeyboardView(this) { key ->
            if (key == MKKeyboardView.KeyAction.Gif) showGifPanel() else handleKey(key)
        }
        return FrameLayout(this).also { root ->
            rootView = root
            root.setBackgroundColor(Color.rgb(7, 21, 47))
            root.addView(keyboardView!!, FrameLayout.LayoutParams(-1, -1))
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        shifted = false
        keyboardView?.setShifted(false)
    }

    private fun handleKey(key: MKKeyboardView.KeyAction) {
        val connection = currentInputConnection ?: return
        when (key) {
            MKKeyboardView.KeyAction.Shift -> {
                shifted = !shifted
                keyboardView?.setShifted(shifted)
            }
            MKKeyboardView.KeyAction.Delete -> connection.deleteSurroundingText(1, 0)
            MKKeyboardView.KeyAction.Enter -> connection.commitText("\n", 1)
            MKKeyboardView.KeyAction.Space -> connection.commitText(" ", 1)
            MKKeyboardView.KeyAction.Gif -> Unit
            is MKKeyboardView.KeyAction.Text -> {
                val text = if (shifted) key.value.uppercase() else key.value
                connection.commitText(text, 1)
                if (shifted) {
                    shifted = false
                    keyboardView?.setShifted(false)
                }
            }
        }
    }

    private fun showGifPanel() {
        val root = rootView ?: return
        keyboardView?.visibility = View.GONE
        val panel = GifPanelView(this, ::sendGif, {
            root.removeAllViews()
            root.addView(keyboardView!!, FrameLayout.LayoutParams(-1, -1))
            keyboardView?.visibility = View.VISIBLE
        })
        root.removeAllViews()
        root.addView(panel, FrameLayout.LayoutParams(-1, -1).apply {
            gravity = Gravity.TOP
        })
    }

    private fun sendGif(url: String) {
        val connection = currentInputConnection ?: return
        thread {
            val file = runCatching { downloadGif(url) }.getOrNull()
            Handler(Looper.getMainLooper()).post {
                if (file == null) {
                    connection.commitText(url, 1)
                    return@post
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    val uri = FileProvider.getUriForFile(
                        this,
                        "${BuildConfig.APPLICATION_ID}.fileprovider",
                        file
                    )
                    val description = ClipDescription("GIF from GIPHY", arrayOf("image/gif"))
                    val info = android.view.inputmethod.InputContentInfo(uri, description, null)
                    connection.commitContent(
                        info,
                        android.view.inputmethod.InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
                        null
                    )
                } else {
                    connection.commitText(url, 1)
                }
                closeGifPanel()
            }
        }
    }

    private fun closeGifPanel() {
        rootView?.let { root ->
            root.removeAllViews()
            root.addView(keyboardView!!, FrameLayout.LayoutParams(-1, -1))
            keyboardView?.visibility = View.VISIBLE
        }
    }

    private fun downloadGif(url: String): File {
        val directory = File(cacheDir, "gifs").apply { mkdirs() }
        val file = File(directory, "mk_${System.currentTimeMillis()}.gif")
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        try {
            connection.inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
            return file
        } finally {
            connection.disconnect()
        }
    }
}