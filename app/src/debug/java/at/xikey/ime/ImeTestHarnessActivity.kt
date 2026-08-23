package at.xikey.ime

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Debug-only host for deterministic IME integration tests. */
class ImeTestHarnessActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "XiKey IME Test Harness"
        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(LinearLayout(this@ImeTestHarnessActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(32))
                addView(label("XiKey IME Test Harness", 24f, Typeface.BOLD))
                addView(label("Each field exposes one Android EditorInfo contract.", 14f, Typeface.NORMAL))
                status = label("ACTION:NONE:0", 16f, Typeface.BOLD).apply {
                    id = R.id.action_status
                    contentDescription = "Editor action status"
                    setPadding(0, dp(12), 0, dp(12))
                }
                addView(status)
                addActionField(R.id.field_done, "DONE", EditorInfo.IME_ACTION_DONE)
                addActionField(R.id.field_search, "SEARCH", EditorInfo.IME_ACTION_SEARCH)
                addActionField(R.id.field_send, "SEND", EditorInfo.IME_ACTION_SEND)
                addActionField(R.id.field_go, "GO", EditorInfo.IME_ACTION_GO)
                addActionField(R.id.field_next, "NEXT", EditorInfo.IME_ACTION_NEXT)
                addActionField(R.id.field_previous, "PREVIOUS", EditorInfo.IME_ACTION_PREVIOUS)
                addView(editor(
                    R.id.field_multiline,
                    "MULTILINE",
                    "Mehrzeilentext / Enter",
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
                    EditorInfo.IME_ACTION_NONE,
                    singleLine = false,
                ))
                addView(editor(
                    R.id.field_password,
                    "PASSWORD",
                    "Passwort (keine Vorschläge)",
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    EditorInfo.IME_ACTION_DONE,
                ))
                addView(editor(
                    R.id.field_auto_shift,
                    "AUTO SHIFT / CAPS LOCK",
                    "Satzanfang, Shift und Doppel-Shift testen",
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
                    EditorInfo.IME_ACTION_DONE,
                ))
                addView(editor(
                    R.id.field_voralex,
                    "VORALEX",
                    "Gu tippen, Vorschlag übernehmen",
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
                    EditorInfo.IME_ACTION_DONE,
                ))
                addView(editor(
                    R.id.field_gestures,
                    "LONG-PRESS / BACKSPACE",
                    "Long-Press und Backspace-Wiederholung testen",
                    InputType.TYPE_CLASS_TEXT,
                    EditorInfo.IME_ACTION_DONE,
                ))
            }, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        })
    }

    private fun LinearLayout.addActionField(id: Int, label: String, action: Int) {
        addView(editor(id, label, "$label input", InputType.TYPE_CLASS_TEXT, action))
    }

    private fun editor(
        viewId: Int,
        actionLabel: String,
        fieldHint: String,
        fieldInputType: Int,
        action: Int,
        singleLine: Boolean = true,
    ): EditText = EditText(this).apply {
        id = viewId
        hint = fieldHint
        contentDescription = "$actionLabel input"
        inputType = fieldInputType
        imeOptions = action
        setSingleLine(singleLine)
        if (!singleLine) {
            minLines = 3
            gravity = Gravity.TOP
        }
        setOnEditorActionListener { _, actionId, _ ->
            status.text = "ACTION:$actionLabel:$actionId"
            true
        }
        val showIme = {
            post {
                val manager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                manager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        setOnFocusChangeListener { _, focused -> if (focused) showIme() }
        setOnClickListener { showIme() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(8)
        }
    }

    private fun label(value: String, size: Float, style: Int): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTypeface(typeface, style)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}