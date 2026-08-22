package com.mkdev.mkkeyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo

class KeyboardService : InputMethodService() {
    private var keyboardView: MKKeyboardView? = null
    private var shifted = false

    override fun onCreateInputView(): View {
        keyboardView = MKKeyboardView(this) { key ->
            handleKey(key)
        }
        return keyboardView!!
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        shifted = false
        keyboardView?.setShifted(false)
    }

    private fun handleKey(key: MKKeyboardView.KeyAction) {
        val connection = currentInputConnection ?: return
        when (key) {
            MKKeyboardView.KeyAction.SHIFT -> {
                shifted = !shifted
                keyboardView?.setShifted(shifted)
            }
            MKKeyboardView.KeyAction.DELETE -> connection.deleteSurroundingText(1, 0)
            MKKeyboardView.KeyAction.ENTER -> connection.commitText("\n", 1)
            MKKeyboardView.KeyAction.SPACE -> connection.commitText(" ", 1)
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
}