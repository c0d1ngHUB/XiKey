package at.xikey.ime

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Space

/**
 * Local-only Android IME. It deliberately has no network permission, telemetry, or remote prediction path.
 * The visual language follows the compact, dark, rounded-key treatment familiar from modern mobile keyboards.
 */
class XiKeyInputMethodService : InputMethodService() {
    private val languages = KeyboardLanguageController()
    private val pages = KeyboardPageController()
    private val shift = KeyboardShiftController()
    private var keyboard: LinearLayout? = null

    override fun onCreateInputView(): View = LinearLayout(this).also { root ->
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
        root.setBackgroundColor(KEYBOARD_BACKGROUND)
        root.setPadding(dp(4), dp(4), dp(4), dp(6))
        keyboard = root
        renderKeyboard()
    }

    private fun renderKeyboard() {
        val root = keyboard ?: return
        root.removeAllViews()
        when (pages.current) {
            KeyboardPage.ALPHABETIC -> renderAlphabeticPage(root)
            KeyboardPage.SYMBOLS -> renderSymbolsPage(root, KeyboardLayout.symbols(), secondary = false)
            KeyboardPage.SYMBOLS_SECONDARY -> renderSymbolsPage(root, KeyboardLayout.secondarySymbols(), secondary = true)
        }
        root.addView(bottomRow())
        // Gesture navigation occupies the bottom of the IME surface and intercepts touches there.
        // Reserve that area so the action row stays fully visible and touchable above it.
        root.addView(Space(this), LinearLayout.LayoutParams(0, dp(NAVIGATION_SPACER_DP)))
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

    private fun renderSymbolsPage(root: LinearLayout, layout: KeyboardLayout, secondary: Boolean) {
        layout.rows.forEachIndexed { index, row ->
            val buttons = if (index == layout.rows.lastIndex) {
                listOf(symbolPageButton(secondary)) + row.map(::symbolButton) + listOf(deleteButton())
            } else {
                row.map(::symbolButton)
            }
            root.addView(keyRow(buttons))
        }
    }

    private fun bottomRow(): LinearLayout = when (pages.current) {
        KeyboardPage.ALPHABETIC -> keyRow(
            listOf(
                actionButton("?123", "Zahlen und Sonderzeichen anzeigen", KeyKind.ACCENT) { pages.showPrimarySymbols(); renderKeyboard() },
                languageButton(),
                actionButton(",", "Komma") { commit(",") },
                actionButton("Leertaste", "Leertaste", KeyKind.SPACE) { commit(" ") },
                actionButton(".", "Punkt") { commit(".") },
                enterButton(),
            ),
            listOf(1.25f, 1.25f, 0.9f, 3.2f, 0.9f, 1.25f),
        )
        KeyboardPage.SYMBOLS, KeyboardPage.SYMBOLS_SECONDARY -> keyRow(
            listOf(
                letterPageButton(),
                languageButton(),
                actionButton(",", "Komma") { commit(",") },
                actionButton("Leertaste", "Leertaste", KeyKind.SPACE) { commit(" ") },
                actionButton(".", "Punkt") { commit(".") },
                enterButton(),
            ),
            listOf(1.25f, 1.25f, 0.9f, 3.2f, 0.9f, 1.25f),
        )
    }

    private fun languageButton(): Button = actionButton(languageLabel(), "Sprache wechseln", KeyKind.ACCENT) {
        languages.switchToNext()
        renderKeyboard()
    }

    private fun shiftButton(): Button = actionButton("⇧", "Umschalttaste", KeyKind.ACCENT) {
        shift.toggle()
        renderKeyboard()
    }

    private fun letterPageButton(): Button = actionButton("ABC", "Buchstaben anzeigen", KeyKind.ACCENT) {
        pages.toggle()
        renderKeyboard()
    }

    private fun symbolPageButton(secondary: Boolean): Button = actionButton(
        if (secondary) "1/2" else "=\\<",
        if (secondary) "Häufige Sonderzeichen anzeigen" else "Weitere Sonderzeichen anzeigen",
        KeyKind.ACCENT,
    ) {
        if (secondary) pages.showPrimarySymbols() else pages.showSecondarySymbols()
        renderKeyboard()
    }

    private fun deleteButton(): Button = actionButton("⌫", "Löschen", KeyKind.ACCENT) {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }

    private fun enterButton(): Button = actionButton("↵", "Eingabe", KeyKind.ACCENT) {
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
    }

    private fun languageLabel(): String = when (languages.current) {
        PredictionLanguage.VORARLBERG_GERMAN -> "VBG"
        PredictionLanguage.ENGLISH -> "EN"
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

    private fun actionButton(
        label: String,
        description: String,
        kind: KeyKind = KeyKind.NORMAL,
        action: (() -> Unit)? = null,
    ): Button = Button(this).apply {
        text = label
        contentDescription = description
        isAllCaps = false
        textSize = if (kind == KeyKind.NORMAL) 18f else 14f
        setTextColor(KEY_TEXT)
        minWidth = 0
        minHeight = 0
        minimumWidth = 0
        minimumHeight = 0
        includeFontPadding = false
        gravity = Gravity.CENTER
        background = roundedBackground(kind.background)
        setOnClickListener { action?.invoke() }
    }

    private fun keyRow(keys: List<Button>, weights: List<Float> = List(keys.size) { 1f }): LinearLayout = LinearLayout(this).apply {
        require(keys.size == weights.size)
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        keys.zip(weights).forEach { (key, weight) ->
            addView(key, LinearLayout.LayoutParams(0, dp(KEY_HEIGHT_DP), weight).apply {
                setMargins(dp(KEY_GAP_DP), dp(KEY_GAP_DP), dp(KEY_GAP_DP), dp(KEY_GAP_DP))
            })
        }
    }

    private fun roundedBackground(color: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(CORNER_RADIUS_DP).toFloat()
    }

    private fun commit(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private enum class KeyKind(val background: Int) {
        NORMAL(KEY_BACKGROUND),
        ACCENT(KEY_ACCENT),
        SPACE(SPACE_BACKGROUND),
    }

    private companion object {
        const val KEY_HEIGHT_DP = 38
        const val KEY_GAP_DP = 2
        const val NAVIGATION_SPACER_DP = 18
        const val CORNER_RADIUS_DP = 9
        val KEYBOARD_BACKGROUND: Int = Color.rgb(20, 23, 27)
        val KEY_BACKGROUND: Int = Color.rgb(47, 51, 57)
        val KEY_ACCENT: Int = Color.rgb(47, 92, 103)
        val SPACE_BACKGROUND: Int = Color.rgb(57, 62, 68)
        val KEY_TEXT: Int = Color.rgb(245, 247, 248)
    }
}
