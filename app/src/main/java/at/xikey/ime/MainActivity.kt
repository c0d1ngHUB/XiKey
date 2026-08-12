package at.xikey.ime

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** Entry point that takes the user to Android's official IME enable/select flow. */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "XiKey"
        setContentView(createContent())
    }

    private fun createContent(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        val padding = dp(24)
        setPadding(padding, padding, padding, padding)

        addView(TextView(this@MainActivity).apply {
            text = "XiKey\nVorarlberger Android-Tastatur"
            textSize = 24f
        })
        addView(TextView(this@MainActivity).apply {
            text = "Lokale Autovervollständigung mit VoraLex. XiKey hat keine Internetberechtigung und verarbeitet Passwortfelder ohne Vorschläge."
            textSize = 16f
            setPadding(0, dp(18), 0, dp(24))
        })
        addView(Button(this@MainActivity).apply {
            text = "Tastatur aktivieren"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        }, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(Button(this@MainActivity).apply {
            text = "XiKey als Tastatur wählen"
            setOnClickListener {
                val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                manager.showInputMethodPicker()
            }
        }, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(TextView(this@MainActivity).apply {
            text = "Zum Testen: Feld antippen, dann z. B. „Gu“ tippen und „Guata Morga“ in der Vorschlagsleiste wählen."
            textSize = 14f
            setPadding(0, dp(24), 0, dp(8))
        })
        addView(EditText(this@MainActivity).apply {
            hint = "Hier mit XiKey testen"
            contentDescription = "XiKey Testfeld"
            textSize = 18f
            minLines = 3
            gravity = Gravity.TOP
        }, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
