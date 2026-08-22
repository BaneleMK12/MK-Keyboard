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
    private var pressedIndex = -1

    private val rows = listOf(
        listOf(KeySpec("GIF", KeyAction.Gif, 1f)),
        "qwertyuiop".map { letter(it) },
        "asdfghjkl".map { letter(it) },
        listOf(
            KeySpec("⇧", KeyAction.Shift, 1.35f),
            *"zxcvbnm".map { letter(it) }.toTypedArray(),
            KeySpec("⌫", KeyAction.Delete, 1.35f)
        ),
        listOf(
            KeySpec(",", KeyAction.Text(","), 1.15f),
            KeySpec("space", KeyAction.Space, 4.6f),
            KeySpec(".", KeyAction.Text("."), 1.15f),
            KeySpec("↵", KeyAction.Enter, 1.35f)
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = dp(292)
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
                Color.rgb(7, 21, 47),
                Color.rgb(11, 43, 82),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), background)

        keyBounds.clear()
        val horizontalPadding = dp(9).toFloat()
        val verticalPadding = dp(10).toFloat()
        val rowGap = dp(7).toFloat()
        val keyGap = dp(5).toFloat()
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
            index == pressedIndex -> Color.rgb(76, 183, 231)
            isSpecial -> Color.rgb(28, 111, 180)
            else -> Color.rgb(20, 74, 132)
        }
        canvas.drawRoundRect(rect, dp(10).toFloat(), dp(10).toFloat(), keyPaint)

        labelPaint.color = if (isSpecial) Color.rgb(224, 250, 255) else Color.WHITE
        labelPaint.textSize = if (spec.label == "space") dp(13).toFloat() else dp(19).toFloat()
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
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                val releasedIndex = keyBounds.indexOfFirst { it.rect.contains(event.x, event.y) }
                if (releasedIndex >= 0 && releasedIndex == pressedIndex) {
                    onKeyAction(keyBounds[releasedIndex].spec.action)
                }
                pressedIndex = -1
                invalidate()
                performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedIndex = -1
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

    private fun letter(value: Char) = KeySpec(value.uppercase(), KeyAction.Text(value.toString()))

    private fun dp(value: Int): Int = max(1, (value * density).toInt())
}