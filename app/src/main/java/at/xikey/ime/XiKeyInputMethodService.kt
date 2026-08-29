package at.xikey.ime

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Space
import org.json.JSONArray

internal object KeyboardSurfaceMetrics {
    const val keyHeightDp = 48
    const val keyVisualGapDp = 2
    const val navigationSpacerDp = 44
    const val includeFontPadding = true
    const val singleLineLabels = true
}

/**
 * Local-only Android IME. VoraLex suggestions are read from an APK asset; no text ever leaves the device.
 *
 * Rendering strategy: the keyboard view tree is built once in [onCreateInputView]; subsequent
 * keystrokes call [updateKeyboard] which reconfigures existing Button labels, visibility, and
 * click listeners in place — no Views are created or destroyed after the initial layout.
 */
class XiKeyInputMethodService : InputMethodService() {
    private lateinit var languages: KeyboardLanguageController
    private lateinit var suggestions: LocalPredictionModel
    private lateinit var learningControls: SharedPreferencesLearningControls
    private lateinit var preferences: android.content.SharedPreferences
    private val learningGuard = CompletedTokenLearningGuard()
    private val pages = KeyboardPageController()
    private val shift = KeyboardShiftController()
    private val backspaceRepeater = BackspaceRepeatController()
    private val backspaceHandler = Handler(Looper.getMainLooper())
    private var keyboard: LinearLayout? = null
    private var currentSuggestions: List<String> = emptyList()
    private var suggestionsAllowed = false
    private var isBackspaceHeld = false
    private var audioManager: AudioManager? = null
    private var currentImeOptions: Int = 0
    private var lastSuggestionContext: SuggestionRefreshContext? = null
    private var variantPopup: PopupWindow? = null

    // ── Colors from resources ─────────────────────────────────────
    private var colKeyboardBg: Int = 0
    private var colKeyBg: Int = 0
    private var colKeyAccent: Int = 0
    private var colSpaceBg: Int = 0
    private var colSuggestionBg: Int = 0
    private var colKeyText: Int = 0


    // ── Cached views ──────────────────────────────────────────────
    private var suggestionBar: LinearLayout? = null
    private val suggestionButtons = mutableListOf<Button>()
    private var contentArea: LinearLayout? = null
    private var bottomBar: LinearLayout? = null

    // Reusable key-row pools: each row is a LinearLayout holding Buttons
    private val alphabetRows = mutableListOf<LinearLayout>()
    private val symbolRows = mutableListOf<LinearLayout>()
    private val maxRows = 4 // max rows for alphabet page (3 letter rows + bottom area handled separately)
    private val maxSymbolRows = 5
    private val keysPerRowPool = 12 // max keys per row (shift + 8 letters + backspace = 11)

    // Bottom bar buttons (persistent)
    private lateinit var bottomLeftBtn: Button   // ?123 / ABC
    private lateinit var bottomLangBtn: Button    // VBG / EN
    private lateinit var bottomCommaBtn: Button
    private lateinit var bottomSpaceBtn: Button
    private lateinit var bottomPeriodBtn: Button
    private lateinit var bottomEnterBtn: Button

    // Shift button (persistent across renders)
    private var shiftBtn: Button? = null
    private var deleteBtn: Button? = null

    private val repeatBackspace = object : Runnable {
        override fun run() {
            if (!isBackspaceHeld) return
            repeat(backspaceRepeater.deletionsDue(System.currentTimeMillis())) { deleteOneCharacter() }
            backspaceHandler.postDelayed(this, BackspaceRepeatController.POLL_INTERVAL_MILLIS)
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────
    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
        colKeyboardBg = getColor(R.color.keyboard_background)
        colKeyBg = getColor(R.color.key_background)
        colKeyAccent = getColor(R.color.key_accent)
        colSpaceBg = getColor(R.color.space_background)
        colSuggestionBg = getColor(R.color.suggestion_background)
        colKeyText = getColor(R.color.key_text)
        val storedTag = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).getString(PREFERENCE_LANGUAGE_TAG, null)
        languages = KeyboardLanguageController(KeyboardLanguagePreference.restore(storedTag))
        preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        val learningStore = SharedPreferencesLearningStore(preferences)
        learningControls = SharedPreferencesLearningControls(preferences, learningStore)
        suggestions = LocalPredictionModel(
            dialectWords = loadBundledWords("voralex_words.json"),
            germanWords = loadBundledWords("german_words.json"),
            englishWords = loadBundledWords("english_words.json"),
            store = learningStore,
            learningEnabled = { learningControls.isEnabled() },
            resetGeneration = { learningControls.resetGeneration() },
        )
    }

    override fun onDestroy() {
        variantPopup?.dismiss()
        variantPopup = null
        stopBackspaceRepeat()
        super.onDestroy()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        variantPopup?.dismiss()
        variantPopup = null
        super.onFinishInputView(finishingInput)
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        suggestionsAllowed = allowsPrediction(attribute)
        currentSuggestions = emptyList()
        lastSuggestionContext = null
        learningGuard.reset()
        shift.reset()
        currentImeOptions = attribute?.imeOptions ?: 0
        currentInputConnection
            ?.getTextBeforeCursor(MAX_CURSOR_LOOKBACK, 0)
            ?.toString()
            ?.let(shift::autoEnableForContext)
    }

    override fun onStartInputView(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(attribute, restarting)
        if (keyboard != null) updateKeyboard()
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

    // ── View creation (once) ──────────────────────────────────────
    override fun onCreateInputView(): View = LinearLayout(this).also { root ->
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
        root.setBackgroundColor(colKeyboardBg)
        root.setPadding(dp(4), dp(4), dp(4), dp(6))
        keyboard = root

        // Suggestion bar (reused)
        suggestionBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            for (i in 0 until 3) {
                val btn = makeButton("", "", keyKindSuggestion)
                suggestionButtons.add(btn)
                addView(btn, LinearLayout.LayoutParams(0, dp(SUGGESTION_HEIGHT_DP), 1f).apply {
                    setMargins(dp(KEY_GAP_DP), 0, dp(KEY_GAP_DP), dp(KEY_GAP_DP))
                })
            }
        }
        root.addView(suggestionBar)

        // Content area (rows added/hidden here)
        contentArea = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(contentArea)

        // Bottom bar (persistent)
        bottomLeftBtn = makeBottomButton("", "", keyKindAccent)
        bottomLangBtn = makeBottomButton("", "", keyKindAccent)
        bottomCommaBtn = makeBottomButton(",", "Komma", keyKindNormal)
        bottomSpaceBtn = makeBottomButton("␣", "Leertaste", keyKindSpace)
        bottomPeriodBtn = makeBottomButton(".", "Punkt", keyKindNormal)
        bottomEnterBtn = makeBottomButton("↵", "Eingabe", keyKindAccent)
        bottomBar = keyRow(
            listOf(bottomLeftBtn, bottomLangBtn, bottomCommaBtn, bottomSpaceBtn, bottomPeriodBtn, bottomEnterBtn),
            listOf(1.25f, 1.25f, 0.9f, 3.2f, 0.9f, 1.25f),
        )
        root.addView(bottomBar)

        // Navigation spacer
        root.addView(Space(this), LinearLayout.LayoutParams(0, dp(KeyboardSurfaceMetrics.navigationSpacerDp)))

        // Pre-create row pools for alphabet and symbol pages
        for (i in 0 until maxRows) {
            alphabetRows.add(createKeyRowPool(keysPerRowPool))
            contentArea?.addView(alphabetRows[i])
        }
        for (i in 0 until maxSymbolRows) {
            symbolRows.add(createKeyRowPool(keysPerRowPool))
            contentArea?.addView(symbolRows[i])
        }

        // Set up persistent buttons
        bottomSpaceBtn.setOnClickListener { performKeyFeedback(bottomSpaceBtn); commitAndRefresh(" ") }
        bottomCommaBtn.setOnClickListener { performKeyFeedback(bottomCommaBtn); commitAndRefresh(",") }
        bottomPeriodBtn.setOnClickListener { performKeyFeedback(bottomPeriodBtn); commitAndRefresh(".") }
        bottomEnterBtn.setOnClickListener {
            performKeyFeedback(bottomEnterBtn)
            clearSuggestions()
            val action = ImeActionSpec.from(currentImeOptions)
            if (action.editorAction != null) {
                currentInputConnection?.performEditorAction(action.editorAction)
                if (action.hideKeyboardAfterAction) requestHideSelf(0)
            } else {
                currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            updateKeyboard()
        }
        bottomLeftBtn.setOnClickListener {
            performKeyFeedback(bottomLeftBtn)
            if (pages.current == KeyboardPage.ALPHABETIC) {
                shift.reset(); clearSuggestions(); pages.showPrimarySymbols()
            } else {
                pages.toggle()
            }
            updateKeyboard()
        }
        bottomLangBtn.setOnClickListener {
            performKeyFeedback(bottomLangBtn)
            val language = languages.switchToNext()
            getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).edit().putString(PREFERENCE_LANGUAGE_TAG, KeyboardLanguagePreference.store(language)).apply()
            suggestionsAllowed = allowsPrediction(currentInputEditorInfo)
            clearSuggestions()
            updateKeyboard()
        }

        updateKeyboard()
    }

    // ── Update (no View creation) ─────────────────────────────────
    private fun updateKeyboard() {
        updateSuggestionBar()
        when (pages.current) {
            KeyboardPage.ALPHABETIC -> showAlphabeticRows()
            KeyboardPage.SYMBOLS -> showSymbolRows(KeyboardLayout.symbols(), secondary = false)
            KeyboardPage.SYMBOLS_SECONDARY -> showSymbolRows(KeyboardLayout.secondarySymbols(), secondary = true)
        }
        updateBottomBar()
    }

    private fun updateSuggestionBar() {
        val bar = suggestionBar ?: return
        if (currentSuggestions.isEmpty()) {
            bar.visibility = View.GONE
            return
        }
        bar.visibility = View.VISIBLE
        for (i in 0 until suggestionButtons.size) {
            val btn = suggestionButtons[i]
            if (i < currentSuggestions.size) {
                val suggestion = currentSuggestions[i]
                btn.text = suggestion
                btn.visibility = View.VISIBLE
                btn.contentDescription = "Vorschlag $suggestion"
                btn.setOnClickListener { performKeyFeedback(btn); acceptSuggestion(suggestion) }
            } else {
                btn.text = ""
                btn.visibility = View.GONE
                btn.setOnClickListener(null)
            }
        }
    }

    private fun showAlphabeticRows() {
        // Hide all symbol rows
        symbolRows.forEach { it.visibility = View.GONE }

        val layout = KeyboardLayout.forLanguage(languages.current)
        val rows = layout.rows

        for (rowIndex in 0 until maxRows) {
            val row = alphabetRows[rowIndex]
            if (rowIndex >= rows.size) {
                row.visibility = View.GONE
                continue
            }
            row.visibility = View.VISIBLE
            val keys = rows[rowIndex]
            val isLastRow = rowIndex == rows.lastIndex

            val allKeys = if (isLastRow) listOf(null) + keys + listOf(null) else keys.map { it }
            // null marks shift (first) and delete (last) positions

            for (pos in 0 until keysPerRowPool) {
                val btn = row.getChildAt(pos) as? Button
                if (btn == null) continue

                if (pos >= allKeys.size) {
                    btn.visibility = View.GONE
                    continue
                }

                btn.visibility = View.VISIBLE

                if (isLastRow && pos == 0) {
                    // Shift button
                    configureShiftButton(btn)
                } else if (isLastRow && pos == allKeys.lastIndex) {
                    // Delete button
                    configureDeleteButton(btn)
                } else {
                    val key = allKeys[pos] as String
                    val label = if (shift.isShifted && key == "ß") "ẞ" else if (shift.isShifted) key.uppercase() else key
                    configureKeyButton(btn, label, key)
                }
            }
        }
    }

    private fun showSymbolRows(layout: KeyboardLayout, secondary: Boolean) {
        // Hide all alphabet rows
        alphabetRows.forEach { it.visibility = View.GONE }

        val rows = layout.rows
        for (rowIndex in 0 until maxSymbolRows) {
            val row = symbolRows[rowIndex]
            if (rowIndex >= rows.size) {
                row.visibility = View.GONE
                continue
            }
            row.visibility = View.VISIBLE
            val keys = rows[rowIndex]
            val isLastRow = rowIndex == rows.lastIndex
            val allKeys = if (isLastRow) listOf(null) + keys + listOf(null) else keys.map { it }

            for (pos in 0 until keysPerRowPool) {
                val btn = row.getChildAt(pos) as? Button
                if (btn == null) continue

                if (pos >= allKeys.size) {
                    btn.visibility = View.GONE
                    continue
                }

                btn.visibility = View.VISIBLE

                if (isLastRow && pos == 0) {
                    // Symbol page toggle button
                    configureSymbolPageButton(btn, secondary)
                } else if (isLastRow && pos == allKeys.lastIndex) {
                    configureDeleteButton(btn)
                } else {
                    val symbol = allKeys[pos] as String
                    configureSymbolButton(btn, symbol)
                }
            }
        }
    }

    private fun updateBottomBar() {
        if (pages.current == KeyboardPage.ALPHABETIC) {
            bottomLeftBtn.text = "?123"
            bottomLeftBtn.contentDescription = "Zahlen und Sonderzeichen anzeigen"
        } else {
            bottomLeftBtn.text = "ABC"
            bottomLeftBtn.contentDescription = "Buchstaben anzeigen"
        }
        bottomLangBtn.text = languageLabel()
        bottomLangBtn.contentDescription = KeyboardAccessibility.language(languages.current)
        val action = ImeActionSpec.from(currentImeOptions)
        bottomEnterBtn.text = action.icon
        bottomEnterBtn.contentDescription = action.contentDescription
    }

    // ── Button configuration (no allocation) ───────────────────────
    private fun performKeyFeedback(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK)
    }

    private fun configureKeyButton(btn: Button, label: String, key: String) {
        btn.text = label
        btn.contentDescription = "Taste $label"
        btn.isActivated = false
        btn.isSelected = false
        btn.textSize = 18f
        btn.background.setTint(colKeyBg)
        btn.setOnTouchListener(null)
        btn.setOnClickListener { performKeyFeedback(btn); commitAndRefresh(shift.applyTo(key)); updateKeyboard() }
        btn.setOnLongClickListener { handleLongPress(btn, key) }
    }

    private fun configureSymbolButton(btn: Button, symbol: String) {
        btn.text = symbol
        btn.contentDescription = "Zeichen $symbol"
        btn.isActivated = false
        btn.isSelected = false
        btn.textSize = 18f
        btn.background.setTint(colKeyBg)
        btn.setOnClickListener { performKeyFeedback(btn); commitAndRefresh(symbol) }
        btn.setOnLongClickListener(null)
    }

    private fun configureShiftButton(btn: Button) {
        btn.text = shiftLabel()
        btn.contentDescription = KeyboardAccessibility.shift(shift.state)
        btn.isActivated = shift.isShifted
        btn.isSelected = shift.isCapsLocked
        btn.textSize = 14f
        btn.background.setTint(colKeyAccent)
        btn.setOnClickListener { performKeyFeedback(btn); shift.toggle(); updateKeyboard() }
        btn.setOnLongClickListener(null)
        shiftBtn = btn
    }

    private fun configureDeleteButton(btn: Button) {
        btn.text = "⌫"
        btn.contentDescription = "Löschen; gedrückt halten für fortlaufendes Löschen"
        btn.isActivated = false
        btn.isSelected = false
        btn.textSize = 14f
        btn.background.setTint(colKeyAccent)
        btn.setOnClickListener(null)
        btn.setOnLongClickListener(null)
        btn.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    performKeyFeedback(btn)
                    startBackspaceRepeat()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> stopBackspaceRepeat()
            }
            true
        }
        deleteBtn = btn
    }

    private fun configureSymbolPageButton(btn: Button, secondary: Boolean) {
        val semantics = KeyboardAccessibility.symbolPage(
            if (secondary) KeyboardPage.SYMBOLS_SECONDARY else KeyboardPage.SYMBOLS,
        )
        btn.text = semantics.label
        btn.contentDescription = semantics.contentDescription
        btn.isSelected = secondary
        btn.textSize = 14f
        btn.background.setTint(colKeyAccent)
        btn.setOnClickListener { performKeyFeedback(btn); if (secondary) pages.showPrimarySymbols() else pages.showSecondarySymbols(); updateKeyboard() }
        btn.setOnLongClickListener(null)
    }

    // ── Backspace ─────────────────────────────────────────────────
    private fun startBackspaceRepeat() {
        isBackspaceHeld = true
        deleteOneCharacter()
        backspaceRepeater.onPress(System.currentTimeMillis())
        backspaceHandler.removeCallbacks(repeatBackspace)
        backspaceHandler.postDelayed(repeatBackspace, BackspaceRepeatController.POLL_INTERVAL_MILLIS)
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

    // ── Suggestions ──────────────────────────────────────────────
    private fun acceptSuggestion(suggestion: String) {
        val context = CursorContext.fromText(textBeforeCursor())
        val plan = SuggestionInsertionPlanner.plan(context, suggestion)
        if (plan.deleteCount > 0) currentInputConnection?.deleteSurroundingText(plan.deleteCount, 0)
        currentInputConnection?.commitText(plan.textToCommit, 1)
        if (suggestionsAllowed && learningGuard.shouldLearn(
                languages.current,
                context.previousWord,
                suggestion,
                context.textBeforeCursor,
            )
        ) {
            suggestions.learnPhrase(languages.current, context.previousWord, suggestion)
        }
        clearSuggestions()
        updateKeyboard()
    }

    private fun commitAndRefresh(text: String) {
        if (suggestionsAllowed) {
            val context = CursorContext.fromText(textBeforeCursor())
            val completedWord = CompletedTokenLearningPolicy.completedTokenBeforeCommit(text, context.composingWord)
            if (completedWord != null && learningGuard.shouldLearn(
                    languages.current,
                    context.previousWord,
                    completedWord,
                    context.textBeforeCursor,
                )
            ) {
                suggestions.learn(languages.current, context.previousWord, completedWord)
            }
        }
        currentInputConnection?.commitText(text, 1)
        autoEnableShiftAfterSentenceEnd()
        refreshSuggestions()
    }

    /** Auto-enable shift after sentence-ending punctuation (. ! ?) or at text start. */
    private fun autoEnableShiftAfterSentenceEnd() {
        if (shift.autoEnableForContext(textBeforeCursor())) updateKeyboard()
    }

    private fun refreshSuggestions() {
        val context = CursorContext.fromText(textBeforeCursor())
        val refreshContext = SuggestionRefreshContext.from(context)
        if (refreshContext == lastSuggestionContext && currentSuggestions.isNotEmpty()) return
        lastSuggestionContext = refreshContext
        currentSuggestions = if (suggestionsAllowed && pages.current == KeyboardPage.ALPHABETIC) {
            suggestions.suggestionsFor(languages.current, context)
        } else {
            emptyList()
        }
        updateKeyboard()
    }

    private fun textBeforeCursor(): String = currentInputConnection?.getTextBeforeCursor(MAX_CURSOR_LOOKBACK, 0)?.toString().orEmpty()

    private fun clearSuggestions() { currentSuggestions = emptyList(); lastSuggestionContext = SuggestionRefreshContext.from(CursorContext.fromText(textBeforeCursor())) }

    private fun allowsPrediction(attribute: EditorInfo?): Boolean =
        InputTypeClassifier.allowsPrediction(attribute?.inputType ?: 0)

    // ── Helpers ───────────────────────────────────────────────────
    private fun languageLabel(): String = if (languages.current == PredictionLanguage.VORARLBERG_GERMAN) "VBG" else "EN"

    private fun shiftLabel(): String = when (shift.state) {
        ShiftState.CAPS_LOCK -> "⇪"
        ShiftState.AUTO, ShiftState.ONESHOT -> "⇧"
        ShiftState.OFF -> "⇩"
    }


    /** P1-5: Long-press character variants (like Gboard accent popups). */
    private val longPressMap = mapOf(
        "a" to listOf("ä", "à", "á", "â", "æ"),
        "o" to listOf("ö", "ò", "ó", "ô", "œ"),
        "u" to listOf("ü", "ù", "ú", "û"),
        "s" to listOf("ß", "ś", "š"),
        "e" to listOf("é", "è", "ê", "ë"),
        "i" to listOf("ì", "í", "î", "ï"),
        "n" to listOf("ñ"),
        "c" to listOf("ç", "ć", "č"),
        "y" to listOf("ÿ"),
        "z" to listOf("ź", "ż"),
    )

    private fun handleLongPress(btn: View, key: String): Boolean {
        val variants = longPressMap[key] ?: return false
        if (variants.isEmpty()) return false
        val labels = LongPressVariantPopupModel.labels(variants, shift.isShifted)

        if (labels.size == 1) {
            commitLongPressVariant(btn, labels.single())
            return true
        }

        variantPopup?.dismiss()
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(2), dp(2), dp(2), dp(2))
            background = roundedBackgroundRaw(colKeyboardBg)
        }
        val popup = PopupWindow(
            row,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dp(LONG_PRESS_POPUP_HEIGHT_DP),
            false,
        ).apply {
            elevation = dp(8).toFloat()
            isOutsideTouchable = false
            setOnDismissListener { if (variantPopup === this) variantPopup = null }
        }
        labels.forEach { label ->
            val choice = makeButton(label, "Zeichen $label auswählen", keyKindAccent).apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    popup.dismiss()
                    commitLongPressVariant(btn, label)
                }
            }
            row.addView(choice, LinearLayout.LayoutParams(dp(LONG_PRESS_CHOICE_WIDTH_DP), dp(LONG_PRESS_POPUP_HEIGHT_DP)))
        }
        row.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val anchorLocation = IntArray(2)
        btn.getLocationOnScreen(anchorLocation)
        val popupWidth = row.measuredWidth
        val screenWidth = resources.displayMetrics.widthPixels
        val desiredLeft = anchorLocation[0] + (btn.width - popupWidth) / 2
        val clampedLeft = desiredLeft.coerceIn(0, (screenWidth - popupWidth).coerceAtLeast(0))
        val horizontalOffset = clampedLeft - anchorLocation[0]
        popup.showAsDropDown(
            btn,
            horizontalOffset,
            -btn.height - dp(LONG_PRESS_POPUP_HEIGHT_DP),
            Gravity.START,
        )
        variantPopup = popup
        return true
    }

    private fun commitLongPressVariant(source: View, text: String) {
        performKeyFeedback(source)
        currentInputConnection?.commitText(text, 1)
        if (!shift.isCapsLocked) shift.reset()
        clearSuggestions()
        updateKeyboard()
    }

    private fun loadBundledWords(assetName: String): List<String> = assets.open(assetName).bufferedReader(Charsets.UTF_8).use { reader ->
        val array = JSONArray(reader.readText())
        List(array.length()) { index -> array.getString(index) }
    }

    // ── View factory (called once) ───────────────────────────────
    private fun makeButton(label: String, description: String, kind: KeyKind): Button = Button(this).apply {
        text = label
        contentDescription = description
        isAllCaps = false
        textSize = if (kind.isNormal) 18f else 14f
        setTextColor(colKeyText)
        minWidth = 0; minHeight = 0; minimumWidth = 0; minimumHeight = 0
        includeFontPadding = KeyboardSurfaceMetrics.includeFontPadding
        isSingleLine = KeyboardSurfaceMetrics.singleLineLabels
        setPadding(0, 0, 0, 0)
        gravity = Gravity.CENTER
        background = InsetDrawable(
            roundedBackgroundRaw(kind.background),
            dp(KeyboardSurfaceMetrics.keyVisualGapDp),
        )
        visibility = View.GONE
    }

    private fun makeBottomButton(label: String, description: String, kind: KeyKind): Button =
        makeButton(label, description, kind).apply { visibility = View.VISIBLE }

    private fun createKeyRowPool(maxKeys: Int): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        for (i in 0 until maxKeys) {
            val btn = makeButton("", "", keyKindNormal)
            addView(btn, LinearLayout.LayoutParams(0, dp(KeyboardSurfaceMetrics.keyHeightDp), 1f))
        }
        visibility = View.GONE
    }

    private fun keyRow(keys: List<Button>, weights: List<Float> = List(keys.size) { 1f }): LinearLayout = LinearLayout(this).apply {
        require(keys.size == weights.size)
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        keys.zip(weights).forEach { (key, weight) ->
            addView(key, LinearLayout.LayoutParams(0, dp(KeyboardSurfaceMetrics.keyHeightDp), weight))
        }
    }

    private fun roundedBackgroundRaw(color: Int): GradientDrawable = GradientDrawable().apply { setColor(color); cornerRadius = dp(CORNER_RADIUS_DP).toFloat() }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private data class KeyKind(val background: Int, val isNormal: Boolean = false)

    private val keyKindNormal get() = KeyKind(colKeyBg, isNormal = true)
    private val keyKindAccent get() = KeyKind(colKeyAccent)
    private val keyKindSpace get() = KeyKind(colSpaceBg)
    private val keyKindSuggestion get() = KeyKind(colSuggestionBg)

    private companion object {
        const val PREFERENCES_NAME = "xikey_preferences"
        const val PREFERENCE_LANGUAGE_TAG = "language_tag"
        const val PREFERENCE_LOCAL_LEARNING_ENABLED = "local_learning_enabled"
        const val PREFERENCE_LEARNING_STORE = "local_prediction_learning_v1"
        const val KEY_GAP_DP = 4
        const val CORNER_RADIUS_DP = 9
        const val SUGGESTION_HEIGHT_DP = 38
        const val LONG_PRESS_POPUP_HEIGHT_DP = 56
        const val LONG_PRESS_CHOICE_WIDTH_DP = 48
        const val MAX_CURSOR_LOOKBACK = 64
    }
}