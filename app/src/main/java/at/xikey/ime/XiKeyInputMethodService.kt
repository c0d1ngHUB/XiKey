package at.xikey.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.Button
import android.widget.LinearLayout

/**
 * Minimal on-device IME shell. It deliberately has no network permission and no telemetry.
 */
class XiKeyInputMethodService : InputMethodService() {
    override fun onCreateInputView(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        listOf("a", "e", "i", "o", "u", " ").forEach { key ->
            addView(Button(context).apply {
                text = if (key == " ") "Leertaste" else key
                contentDescription = if (key == " ") "Leertaste" else "Taste $key"
                setOnClickListener { currentInputConnection?.commitText(key, 1) }
            })
        }
    }
}
