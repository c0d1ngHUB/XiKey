package at.xikey.ime

import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout

/**
 * Local-only Android IME. This initial keyboard deliberately has no network permission,
 * telemetry, or remote prediction path.
 */
class XiKeyInputMethodService : InputMethodService() {
    private val languages = KeyboardLanguageController()
    private val pages = KeyboardPageController()
    private val shift = KeyboardShiftController()
    private var keyboard: LinearLayout? = null

    override fun onCreateInputView(): View = LinearLayout(this).also { root ->
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.rgb(32, 34, 39))
        keyboard = root
        renderKeyboard()
    }

    private fun renderKeyboard() {
        val root = keyboard ?: return
        root.removeAllViews()
        val layout = when (pages.current) {
            KeyboardPage.ALPHABETIC -> KeyboardLayout.forLanguage(languages.current)
            KeyboardPage.SYMBOLS -> KeyboardLayout.symbols()
        }
        layout.rows.forEach { row ->
            val buttons = row.map { key ->
                if (pages.current == KeyboardPage.ALPHABETIC) keyButton(key) else symbolButton(key)
            }
            root.addView(keyRow(buttons))
        }
        root.addView(
            keyRow(
                listOf(
                    if (pages.current == KeyboardPage.ALPHABETIC) {
                        actionButton("⇧", "Umschalttaste") { shift.toggle(); renderKeyboard() }
                    } else {
                        actionButton("ABC", "Buchstaben anzeigen") { pages.toggle(); renderKeyboard() }
                    },
                    actionButton(pageToggleLabel(), "Zahlen und Sonderzeichen umschalten") { pages.toggle(); renderKeyboard() },
                    actionButton(languageLabel(), "Sprache wechseln") { switchLanguage() },
                    actionButton("Leertaste", "Leertaste") { commit(" ") },
                    actionButton("⌫", "Löschen") { currentInputConnection?.deleteSurroundingText(1, 0) },
                    actionButton("↵", "Eingabe") { currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER)) },
                ),
            ),
        )
    }

    private fun languageLabel(): String = when (languages.current) {
        PredictionLanguage.VORARLBERG_GERMAN -> "VBG"
        PredictionLanguage.ENGLISH -> "EN"
    }

    private fun pageToggleLabel(): String = when (pages.current) {
        KeyboardPage.ALPHABETIC -> "?123"
        KeyboardPage.SYMBOLS -> "ABC"
    }

    private fun switchLanguage() {
        languages.switchToNext()
        renderKeyboard()
    }

    private fun keyRow(keys: List<Button>): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        keys.forEach { addView(it, LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) }) }
    }

    private fun keyButton(key: String): Button {
        val label = if (shift.isShifted) key.uppercase() else key
        return actionButton(label, "Taste $label") {
            commit(shift.applyTo(key))
            renderKeyboard()
        }
    }

    private fun symbolButton(symbol: String): Button = actionButton(symbol, "Zeichen $symbol") {
        commit(symbol)
    }

    private fun actionButton(label: String, description: String, action: (() -> Unit)? = null): Button = Button(this).apply {
        text = label
        contentDescription = description
        isAllCaps = false
        textSize = 16f
        setOnClickListener { action?.invoke() }
    }

    private fun commit(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
