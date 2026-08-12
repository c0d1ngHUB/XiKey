package at.xikey.ime

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Space
import org.json.JSONArray

internal object KeyboardSurfaceMetrics {
    const val keyHeightDp = 46
    const val navigationSpacerDp = 44
    const val includeFontPadding = true
    const val singleLineLabels = true
}

/**
 * Local-only Android IME. VoraLex suggestions are read from an APK asset; no text ever leaves the device.
 */
class XiKeyInputMethodService : InputMethodService() {
    private lateinit var languages: KeyboardLanguageController
    private lateinit var suggestions: SuggestionWordLists
    private val pages = KeyboardPageController()
    private val shift = KeyboardShiftController()
    private val backspaceRepeater = BackspaceRepeatController()
    private val backspaceHandler = Handler(Looper.getMainLooper())
    private var keyboard: LinearLayout? = null
    private var currentSuggestions: List<String> = emptyList()
    private var suggestionsAllowed = false
    private var isBackspaceHeld = false
    private val repeatBackspace = object : Runnable {
        override fun run() {
            if (!isBackspaceHeld) return
            repeat(backspaceRepeater.deletionsDue(System.currentTimeMillis())) { deleteOneCharacter() }
            backspaceHandler.postDelayed(this, BackspaceRepeatController.REPEAT_INTERVAL_MILLIS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val storedTag = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).getString(PREFERENCE_LANGUAGE_TAG, null)
        languages = KeyboardLanguageController(KeyboardLanguagePreference.restore(storedTag))
        suggestions = SuggestionWordLists(
            dialectWords = loadBundledWords("voralex_words.json"),
            germanWords = loadBundledWords("german_words.json"),
            englishWords = loadBundledWords("english_words.json"),
        )
    }

    override fun onDestroy() {
        stopBackspaceRepeat()
        super.onDestroy()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        suggestionsAllowed = !isSensitiveInput(attribute)
        currentSuggestions = emptyList()
        shift.reset()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (oldSelStart != newSelStart || oldSelEnd != newSelEnd) refreshSuggestions()
    }

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
        root.addView(suggestionRow())
        when (pages.current) {
            KeyboardPage.ALPHABETIC -> renderAlphabeticPage(root)
            KeyboardPage.SYMBOLS -> renderSymbolsPage(root, KeyboardLayout.symbols(), secondary = false)
            KeyboardPage.SYMBOLS_SECONDARY -> renderSymbolsPage(root, KeyboardLayout.secondarySymbols(), secondary = true)
        }
        root.addView(bottomRow())
        root.addView(Space(this), LinearLayout.LayoutParams(0, dp(KeyboardSurfaceMetrics.navigationSpacerDp)))
    }

    private fun suggestionRow(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        visibility = if (currentSuggestions.isEmpty()) View.GONE else View.VISIBLE
        currentSuggestions.forEach { suggestion ->
            addView(actionButton(suggestion, "Vorschlag $suggestion", KeyKind.SUGGESTION) {
                acceptSuggestion(suggestion)
            }, LinearLayout.LayoutParams(0, dp(SUGGESTION_HEIGHT_DP), 1f).apply {
                setMargins(dp(KEY_GAP_DP), 0, dp(KEY_GAP_DP), dp(KEY_GAP_DP))
            })
        }
    }

    private fun renderAlphabeticPage(root: LinearLayout) {
        val rows = KeyboardLayout.forLanguage(languages.current).rows
        rows.forEachIndexed { index, row ->
            val buttons = if (index == rows.lastIndex) listOf(shiftButton()) + row.map(::keyButton) + listOf(deleteButton()) else row.map(::keyButton)
            root.addView(keyRow(buttons))
        }
    }

    private fun renderSymbolsPage(root: LinearLayout, layout: KeyboardLayout, secondary: Boolean) {
        layout.rows.forEachIndexed { index, row ->
            val buttons = if (index == layout.rows.lastIndex) listOf(symbolPageButton(secondary)) + row.map(::symbolButton) + listOf(deleteButton()) else row.map(::symbolButton)
            root.addView(keyRow(buttons))
        }
    }

    private fun bottomRow(): LinearLayout = keyRow(
        listOf(
            if (pages.current == KeyboardPage.ALPHABETIC) actionButton("?123", "Zahlen und Sonderzeichen anzeigen", KeyKind.ACCENT) {
                shift.reset(); clearSuggestions(); pages.showPrimarySymbols(); renderKeyboard()
            } else letterPageButton(),
            languageButton(),
            actionButton(",", "Komma") { commitAndRefresh(",") },
            actionButton("Leertaste", "Leertaste", KeyKind.SPACE) { commitAndRefresh(" ") },
            actionButton(".", "Punkt") { commitAndRefresh(".") },
            enterButton(),
        ),
        listOf(1.25f, 1.25f, 0.9f, 3.2f, 0.9f, 1.25f),
    )

    private fun languageButton(): Button = actionButton(languageLabel(), "Sprache wechseln", KeyKind.ACCENT) {
        val language = languages.switchToNext()
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).edit().putString(PREFERENCE_LANGUAGE_TAG, KeyboardLanguagePreference.store(language)).apply()
        suggestionsAllowed = !isSensitiveInput(currentInputEditorInfo)
        clearSuggestions()
        renderKeyboard()
    }

    private fun shiftButton(): Button = actionButton("⇧", "Umschalttaste", KeyKind.ACCENT) { shift.toggle(); renderKeyboard() }

    private fun letterPageButton(): Button = actionButton("ABC", "Buchstaben anzeigen", KeyKind.ACCENT) { pages.toggle(); clearSuggestions(); renderKeyboard() }

    private fun symbolPageButton(secondary: Boolean): Button = actionButton(if (secondary) "1/2" else "=\\<", if (secondary) "Häufige Sonderzeichen anzeigen" else "Weitere Sonderzeichen anzeigen", KeyKind.ACCENT) {
        if (secondary) pages.showPrimarySymbols() else pages.showSecondarySymbols()
        renderKeyboard()
    }

    private fun deleteButton(): Button = actionButton("⌫", "Löschen; gedrückt halten für fortlaufendes Löschen", KeyKind.ACCENT).apply {
        setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> startBackspaceRepeat()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> stopBackspaceRepeat()
            }
            true
        }
    }

    private fun startBackspaceRepeat() {
        isBackspaceHeld = true
        deleteOneCharacter()
        backspaceRepeater.onPress(System.currentTimeMillis())
        backspaceHandler.removeCallbacks(repeatBackspace)
        backspaceHandler.postDelayed(repeatBackspace, BackspaceRepeatController.REPEAT_INTERVAL_MILLIS)
    }

    private fun stopBackspaceRepeat() {
        isBackspaceHeld = false
        backspaceRepeater.onRelease()
        backspaceHandler.removeCallbacks(repeatBackspace)
    }

    private fun deleteOneCharacter() {
        currentInputConnection?.deleteSurroundingText(1, 0)
        refreshSuggestions()
    }

    private fun enterButton(): Button = actionButton("↵", "Eingabe", KeyKind.ACCENT) {
        clearSuggestions()
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        renderKeyboard()
    }

    private fun languageLabel(): String = if (languages.current == PredictionLanguage.VORARLBERG_GERMAN) "VBG" else "EN"

    private fun keyButton(key: String): Button {
        val label = if (shift.isShifted && key == "ß") "ẞ" else if (shift.isShifted) key.uppercase() else key
        return actionButton(label, "Taste $label") { commitAndRefresh(shift.applyTo(key)); renderKeyboard() }
    }

    private fun symbolButton(symbol: String): Button = actionButton(symbol, "Zeichen $symbol") { commitAndRefresh(symbol) }

    private fun acceptSuggestion(suggestion: String) {
        val prefix = currentComposingWord()
        if (prefix.isNotEmpty()) currentInputConnection?.deleteSurroundingText(prefix.length, 0)
        currentInputConnection?.commitText("$suggestion ", 1)
        clearSuggestions()
        renderKeyboard()
    }

    private fun commitAndRefresh(text: String) {
        currentInputConnection?.commitText(text, 1)
        refreshSuggestions()
    }

    private fun refreshSuggestions() {
        currentSuggestions = if (suggestionsAllowed && pages.current == KeyboardPage.ALPHABETIC) {
            suggestions.forLanguage(languages.current).suggestionsFor(currentComposingWord())
        } else {
            emptyList()
        }
        renderKeyboard()
    }

    private fun currentComposingWord(): String = ComposingWord.beforeCursor(currentInputConnection?.getTextBeforeCursor(MAX_CURSOR_LOOKBACK, 0)?.toString().orEmpty())

    private fun clearSuggestions() { currentSuggestions = emptyList() }

    private fun isSensitiveInput(attribute: EditorInfo?): Boolean {
        val variation = (attribute?.inputType ?: 0) and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
    }

    private fun loadBundledWords(assetName: String): List<String> = assets.open(assetName).bufferedReader(Charsets.UTF_8).use { reader ->
        val array = JSONArray(reader.readText())
        List(array.length()) { index -> array.getString(index) }
    }

    private fun actionButton(label: String, description: String, kind: KeyKind = KeyKind.NORMAL, action: (() -> Unit)? = null): Button = Button(this).apply {
        text = label
        contentDescription = description
        isAllCaps = false
        textSize = if (kind == KeyKind.NORMAL) 18f else 14f
        setTextColor(KEY_TEXT)
        minWidth = 0; minHeight = 0; minimumWidth = 0; minimumHeight = 0
        includeFontPadding = KeyboardSurfaceMetrics.includeFontPadding
        isSingleLine = KeyboardSurfaceMetrics.singleLineLabels
        setPadding(0, 0, 0, 0)
        gravity = Gravity.CENTER
        background = roundedBackground(kind.background)
        setOnClickListener { action?.invoke() }
    }

    private fun keyRow(keys: List<Button>, weights: List<Float> = List(keys.size) { 1f }): LinearLayout = LinearLayout(this).apply {
        require(keys.size == weights.size)
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        keys.zip(weights).forEach { (key, weight) -> addView(key, LinearLayout.LayoutParams(0, dp(KeyboardSurfaceMetrics.keyHeightDp), weight).apply { setMargins(dp(KEY_GAP_DP), dp(KEY_GAP_DP), dp(KEY_GAP_DP), dp(KEY_GAP_DP)) }) }
    }

    private fun roundedBackground(color: Int): GradientDrawable = GradientDrawable().apply { setColor(color); cornerRadius = dp(CORNER_RADIUS_DP).toFloat() }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private enum class KeyKind(val background: Int) { NORMAL(KEY_BACKGROUND), ACCENT(KEY_ACCENT), SPACE(SPACE_BACKGROUND), SUGGESTION(SUGGESTION_BACKGROUND) }

    private companion object {
        const val PREFERENCES_NAME = "xikey_preferences"
        const val PREFERENCE_LANGUAGE_TAG = "language_tag"
        const val KEY_GAP_DP = 2
        const val CORNER_RADIUS_DP = 9
        const val SUGGESTION_HEIGHT_DP = 38
        const val MAX_CURSOR_LOOKBACK = 64
        val KEYBOARD_BACKGROUND: Int = Color.rgb(20, 23, 27)
        val KEY_BACKGROUND: Int = Color.rgb(47, 51, 57)
        val KEY_ACCENT: Int = Color.rgb(47, 92, 103)
        val SPACE_BACKGROUND: Int = Color.rgb(57, 62, 68)
        val SUGGESTION_BACKGROUND: Int = Color.rgb(37, 74, 82)
        val KEY_TEXT: Int = Color.rgb(245, 247, 248)
    }
}
