package at.xikey.ime

import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.KeyEvent
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
        when (pages.current) {
            KeyboardPage.ALPHABETIC -> renderAlphabeticPage(root)
            KeyboardPage.SYMBOLS -> renderSymbolsPage(root)
        }
        root.addView(bottomRow())
    }

    private fun renderAlphabeticPage(root: LinearLayout) {
        val rows = KeyboardLayout.forLanguage(languages.current).rows
        rows.forEachIndexed { index, row ->
            val buttons = if (index == rows.lastIndex) {
                listOf(shiftButton()) + row.map(::keyButton) + listOf(deleteButton())
            } else {
                row.map(::keyButton)
            }
            root.addView(keyRow(buttons))
        }
    }

    private fun renderSymbolsPage(root: LinearLayout) {
        val rows = KeyboardLayout.symbols().rows
        rows.forEachIndexed { index, row ->
            val buttons = if (index == rows.lastIndex) {
                listOf(letterPageButton()) + row.map(::symbolButton) + listOf(deleteButton())
            } else {
                row.map(::symbolButton)
            }
            root.addView(keyRow(buttons))
        }
    }

    private fun bottomRow(): LinearLayout = when (pages.current) {
        KeyboardPage.ALPHABETIC -> keyRow(
            listOf(
                actionButton("?123", "Zahlen und Sonderzeichen anzeigen") { pages.toggle(); renderKeyboard() },
                languageButton(),
                actionButton(",", "Komma") { commit(",") },
                actionButton("Leertaste", "Leertaste") { commit(" ") },
                actionButton(".", "Punkt") { commit(".") },
                enterButton(),
            ),
            listOf(1.2f, 1.2f, 1f, 3f, 1f, 1.2f),
        )
        KeyboardPage.SYMBOLS -> keyRow(
            listOf(
                languageButton(),
                actionButton(",", "Komma") { commit(",") },
                actionButton("Leertaste", "Leertaste") { commit(" ") },
                actionButton(".", "Punkt") { commit(".") },
                enterButton(),
            ),
            listOf(1.2f, 1f, 3f, 1f, 1.2f),
        )
    }

    private fun languageButton(): Button = actionButton(languageLabel(), "Sprache wechseln") { switchLanguage() }

    private fun shiftButton(): Button = actionButton("⇧", "Umschalttaste") { shift.toggle(); renderKeyboard() }

    private fun letterPageButton(): Button = actionButton("ABC", "Buchstaben anzeigen") { pages.toggle(); renderKeyboard() }

    private fun deleteButton(): Button = actionButton("⌫", "Löschen") {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }

    private fun enterButton(): Button = actionButton("↵", "Eingabe") {
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
    }

    private fun languageLabel(): String = when (languages.current) {
        PredictionLanguage.VORARLBERG_GERMAN -> "VBG"
        PredictionLanguage.ENGLISH -> "EN"
    }

    private fun switchLanguage() {
        languages.switchToNext()
        renderKeyboard()
    }

    private fun keyRow(keys: List<Button>, weights: List<Float> = List(keys.size) { 1f }): LinearLayout = LinearLayout(this).apply {
        require(keys.size == weights.size)
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        keys.zip(weights).forEach { (key, weight) ->
            addView(key, LinearLayout.LayoutParams(0, dp(48), weight).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            })
        }
    }

    private fun keyButton(key: String): Button {
        val label = if (shift.isShifted && key == "ß") "ẞ" else if (shift.isShifted) key.uppercase() else key
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
