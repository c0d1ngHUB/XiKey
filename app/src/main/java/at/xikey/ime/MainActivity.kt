package at.xikey.ime

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

/** Product entry point with live progress for Android's official IME setup flow. */
class MainActivity : Activity() {
    private lateinit var content: LinearLayout
    private val prefs by lazy { getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE) }
    private val learningControls by lazy {
        SharedPreferencesLearningControls(prefs, SharedPreferencesLearningStore(prefs))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "XiKey"
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
            val horizontal = dp(20)
            setPadding(horizontal, dp(24), horizontal, dp(32))
        }
        setContentView(ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(getColor(R.color.launcher_background))
            addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        })
    }

    override fun onResume() {
        super.onResume()
        render(readSetupStatus())
    }

    private fun render(status: ImeSetupStatus) {
        content.removeAllViews()

        content.addView(text("XiKey", 32f, Typeface.BOLD, R.color.launcher_text))
        content.addView(text("Vorarlbergerisch. Englisch. Privat.", 18f, Typeface.NORMAL, R.color.launcher_primary).apply {
            setPadding(0, dp(2), 0, 0)
        })
        content.addView(text("Lokale VoraLex-Vorschläge ohne Cloud, Analytics oder Internetberechtigung.", 15f, Typeface.NORMAL, R.color.launcher_secondary_text).apply {
            setPadding(0, dp(12), 0, dp(18))
        })

        content.addView(card().apply {
            addView(text("XiKey einrichten", 20f, Typeface.BOLD, R.color.launcher_text))
            addView(statusText(status.activationStatus, status.enabled))
            addView(actionButton(status.activationButtonLabel) {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            })
            addView(statusText(status.selectionStatus, status.selected))
            addView(actionButton(status.selectionButtonLabel) {
                val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                manager.showInputMethodPicker()
            }.apply {
                isEnabled = status.selectionEnabled
                alpha = if (isEnabled) 1f else 0.55f
            })
        }, spacedParams(top = 0, bottom = 16))

        content.addView(card().apply {
            addView(text("VoraLex ausprobieren", 20f, Typeface.BOLD, R.color.launcher_text))
            addView(text("VBG wählen, „Gu“ tippen und „Guata Morga“ antippen.", 15f, Typeface.NORMAL, R.color.launcher_secondary_text).apply {
                setPadding(0, dp(6), 0, dp(10))
            })
            addView(EditText(this@MainActivity).apply {
                hint = "Hier mit XiKey testen"
                contentDescription = "XiKey Testfeld; Aktion Fertig"
                textSize = 18f
                setSingleLine(true)
                imeOptions = EditorInfo.IME_ACTION_DONE
                minHeight = dp(56)
                setPadding(dp(12), 0, dp(12), 0)
            }, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }, spacedParams(top = 0, bottom = 12))

        content.addView(card().apply {
            addView(text("Lokales Lernen", 20f, Typeface.BOLD, R.color.launcher_text))
            addView(text("Nur persönliche Wort- und Übergangsdaten lokal auf diesem Gerät.", 15f, Typeface.NORMAL, R.color.launcher_secondary_text).apply {
                setPadding(0, dp(6), 0, dp(12))
            })
            addView(Switch(this@MainActivity).apply {
                isChecked = learningControls.isEnabled()
                text = if (isChecked) "Lernen aktiv" else "Lernen pausiert"
                contentDescription = "Lokales Lernen ein- oder ausschalten"
                minHeight = dp(48)
                setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
                    learningControls.setEnabled(checked)
                    text = if (checked) "Lernen aktiv" else "Lernen pausiert"
                }
            }, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(actionButton("Alle gelernten Wörter löschen") {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Lernen löschen?")
                    .setMessage("Dabei werden alle gelernten Wörter und Übergänge dauerhaft vom Gerät entfernt.")
                    .setNegativeButton("Abbrechen", null)
                    .setPositiveButton("Löschen") { _, _ ->
                        learningControls.clearAll()
                    }
                    .show()
            }.apply {
                contentDescription = "Alle gelernten Wörter und Übergänge löschen"
                backgroundTintList = ColorStateList.valueOf(getColor(R.color.launcher_secondary_text))
            })
        }, spacedParams(top = 0, bottom = 12))

        content.addView(text("Keine Eingaben verlassen dieses Gerät.", 13f, Typeface.NORMAL, R.color.launcher_secondary_text).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(4), 0, 0)
        }, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun readSetupStatus(): ImeSetupStatus {
        val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val xiKeyService = XiKeyInputMethodService::class.java.name
        val enabled = manager.enabledInputMethodList.any { info ->
            info.packageName == packageName && info.serviceName == xiKeyService
        }
        val selectedComponent = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD,
        )?.let(ComponentName::unflattenFromString)
        val selected = selectedComponent?.packageName == packageName &&
            selectedComponent.className == xiKeyService
        return ImeSetupStatus(enabled = enabled, selected = selected)
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(16), dp(18), dp(18))
        background = roundedBackground(getColor(R.color.launcher_surface), 16)
        elevation = dp(2).toFloat()
    }

    private fun statusText(value: String, completed: Boolean): TextView =
        text(
            value,
            15f,
            if (completed) Typeface.BOLD else Typeface.NORMAL,
            if (completed) R.color.launcher_success else R.color.launcher_secondary_text,
        ).apply { setPadding(0, dp(14), 0, dp(6)) }

    private fun actionButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 15f
        setTextColor(getColor(R.color.key_text))
        backgroundTintList = ColorStateList.valueOf(getColor(R.color.launcher_primary))
        minHeight = dp(48)
        setOnClickListener { action() }
    }

    private fun text(value: String, size: Float, style: Int, color: Int): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTypeface(typeface, style)
        setTextColor(getColor(color))
    }

    private fun spacedParams(top: Int, bottom: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(top)
            bottomMargin = dp(bottom)
        }

    private fun roundedBackground(color: Int, radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val PREFERENCES_NAME = "xikey_preferences"
    }
}
