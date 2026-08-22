package com.mkdev.mkkeyboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView

class EmojiPanelView(
    context: Context,
    private val onEmojiSelected: (String) -> Unit,
    private val onClose: () -> Unit
) : LinearLayout(context) {
    private val emojis = listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
        "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰",
        "😘", "😗", "😙", "😚", "😋", "😛", "🤗", "🤩",
        "🤔", "🫡", "😎", "🥳", "😭", "😢", "😡", "🤯",
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
        "👍", "👎", "👏", "🙏", "🔥", "✨", "💯", "🎉"
    )

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.rgb(232, 236, 241))
        isFocusableInTouchMode = true
        requestFocus()

        val header = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(6), dp(10), dp(4))
        }
        val title = TextView(context).apply {
            text = "Emoji"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(55, 63, 73))
        }
        header.addView(title, LayoutParams(0, dp(42), 1f))
        val close = Button(context).apply {
            text = "Done"
            isAllCaps = false
            setTextColor(Color.rgb(42, 92, 145))
            setOnClickListener { onClose() }
        }
        header.addView(close, LayoutParams(dp(76), dp(42)))
        addView(header)

        val grid = GridLayout(context).apply {
            columnCount = 8
            setPadding(dp(5), 0, dp(5), dp(8))
        }
        emojis.forEach { emoji ->
            val button = TextView(context).apply {
                text = emoji
                textSize = 25f
                gravity = Gravity.CENTER
                isClickable = true
                contentDescription = "Insert $emoji"
                setOnClickListener { onEmojiSelected(emoji) }
                background = rounded(Color.WHITE, dp(8).toFloat())
            }
            val size = (resources.displayMetrics.widthPixels - dp(10)) / 8
            grid.addView(button, GridLayout.LayoutParams().apply {
                width = size
                height = dp(48)
                setMargins(dp(2), dp(2), dp(2), dp(2))
            })
        }
        addView(grid, LayoutParams(-1, 0, 1f))
    }

    private fun rounded(color: Int, radius: Float) =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}