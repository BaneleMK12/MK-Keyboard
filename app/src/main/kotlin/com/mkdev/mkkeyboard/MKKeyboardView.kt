package com.mkdev.mkkeyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

class MKKeyboardView(
    context: Context,
    private val onKeyAction: (KeyAction) -> Unit
) : View(context) {
    sealed class KeyAction {
        data class Text(val value: String) : KeyAction()
        data object Shift : KeyAction()
        data object Delete : KeyAction()
        data object Space : KeyAction()
        data object Enter : KeyAction()
        data object Gif : KeyAction()
        data object Emoji : KeyAction()
        data object NoOp : KeyAction()
    }

    private data class KeySpec(
        val label: String,
        val action: KeyAction,
        val weight: Float = 1f
    )

    private data class KeyBounds(val spec: KeySpec, val rect: RectF)

    private val density = resources.displayMetrics.density
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val keyBounds = mutableListOf<KeyBounds>()
    private var shifted = false
    private var capsLocked = false
    private var pressedIndex = -1

    private val rows = listOf(
        listOf(
            KeySpec("GIF", KeyAction.Gif),
            KeySpec("😊", KeyAction.Emoji),
            KeySpec("▣", KeyAction.NoOp),
            KeySpec("⌘", KeyAction.NoOp),
            KeySpec("aあ", KeyAction.NoOp),
            KeySpec("●", KeyAction.NoOp),
            KeySpec("•••", KeyAction.NoOp)
        ),
        "1234567890".map { KeySpec(it.toString(), KeyAction.Text(it.toString())) },
        "qwertyuiop".map { letter(it) },
        "asdfghjkl".map { letter(it) },
        listOf(
            KeySpec("⇧", KeyAction.Shift),
            *"zxcvbnm".map { letter(it) }.toTypedArray(),
            KeySpec("⌫", KeyAction.Delete)
        ),
        listOf(
            KeySpec(",", KeyAction.Text(",")),
            KeySpec("space", KeyAction.Space),
            KeySpec(".", KeyAction.Text(".")),
            KeySpec("↵", KeyAction.Enter)
        )
    )

    init {
        isFocusable = true
        setBackgroundColor(Color.TRANSPARENT)
    }

    fun setShifted(value: Boolean) {
        shifted = value
        invalidate()
    }

    fun setCapsLocked(value: Boolean) {
        capsLocked = value
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = dp(348)
        val measuredHeight = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), measuredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                Color.rgb(232, 236, 241),
                Color.rgb(214, 222, 232),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), background)

        keyBounds.clear()
        val horizontalPadding = dp(5).toFloat()
        val verticalPadding = dp(7).toFloat()
        val rowGap = dp(5).toFloat()
        val keyGap = dp(4).toFloat()
        val rowHeight = (height - verticalPadding * 2 - rowGap * (rows.size - 1)) / rows.size

        rows.forEachIndexed { rowIndex, row ->
            val totalWeight = row.sumOf { it.weight.toDouble() }.toFloat()
            var x = horizontalPadding
            val y = verticalPadding + rowIndex * (rowHeight + rowGap)
            row.forEach { spec ->
                val availableWidth = width - horizontalPadding * 2 - keyGap * (row.size - 1)
                val keyWidth = availableWidth * spec.weight / totalWeight
                val rect = RectF(x, y, x + keyWidth, y + rowHeight)
                keyBounds.add(KeyBounds(spec, rect))
                drawKey(canvas, spec, rect, keyBounds.lastIndex)
                x += keyWidth + keyGap
            }
        }
    }

    private fun drawKey(canvas: Canvas, spec: KeySpec, rect: RectF, index: Int) {
        val isSpecial = spec.action !is KeyAction.Text
        keyPaint.shader = null
        keyPaint.color = when {
            index == pressedIndex -> Color.rgb(174, 207, 235)
            spec.action is KeyAction.Shift && capsLocked -> Color.rgb(170, 205, 238)
            isSpecial -> Color.rgb(207, 216, 226)
            else -> Color.WHITE
        }
        canvas.drawRoundRect(rect, dp(10).toFloat(), dp(10).toFloat(), keyPaint)

        labelPaint.color = Color.rgb(75, 82, 91)
        labelPaint.textSize = when {
            spec.label == "space" -> dp(13).toFloat()
            spec.label == "•••" -> dp(16).toFloat()
            spec.label == "😊" -> dp(17).toFloat()
            else -> dp(18).toFloat()
        }
        val label = if (shifted && spec.action is KeyAction.Text && spec.label.length == 1) {
            spec.label.uppercase()
        } else {
            spec.label
        }
        val baseline = rect.centerY() - (labelPaint.ascent() + labelPaint.descent()) / 2
        canvas.drawText(label, rect.centerX(), baseline, labelPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressedIndex = keyBounds.indexOfFirst { it.rect.contains(event.x, event.y) }
                animate().scaleX(0.995f).scaleY(0.995f).setDuration(70).start()
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                val releasedIndex = keyBounds.indexOfFirst { it.rect.contains(event.x, event.y) }
                if (releasedIndex >= 0 && releasedIndex == pressedIndex) {
                    onKeyAction(keyBounds[releasedIndex].spec.action)
                }
                pressedIndex = -1
                animate().scaleX(1f).scaleY(1f).setDuration(90).start()
                invalidate()
                performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedIndex = -1
                animate().scaleX(1f).scaleY(1f).setDuration(90).start()
                invalidate()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun letter(value: Char) = KeySpec(value.toString(), KeyAction.Text(value.toString()))

    private fun dp(value: Int): Int = max(1, (value * density).toInt())
}