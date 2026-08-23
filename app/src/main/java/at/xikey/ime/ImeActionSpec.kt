package at.xikey.ime

import android.view.inputmethod.EditorInfo

/** One source of truth for an editor action's icon, spoken label and behavior. */
data class ImeActionSpec(
    val icon: String,
    val contentDescription: String,
    val editorAction: Int?,
    val hideKeyboardAfterAction: Boolean = false,
) {
    companion object {
        fun from(imeOptions: Int): ImeActionSpec {
            if (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return newline()

            return when (val action = imeOptions and EditorInfo.IME_MASK_ACTION) {
                EditorInfo.IME_ACTION_GO -> ImeActionSpec("→", "Los", action)
                EditorInfo.IME_ACTION_SEARCH -> ImeActionSpec("🔍", "Suchen", action)
                EditorInfo.IME_ACTION_SEND -> ImeActionSpec("➤", "Senden", action)
                EditorInfo.IME_ACTION_NEXT -> ImeActionSpec("⇥", "Weiter", action)
                EditorInfo.IME_ACTION_PREVIOUS -> ImeActionSpec("⇤", "Zurück", action)
                EditorInfo.IME_ACTION_DONE -> ImeActionSpec("✓", "Fertig", action, hideKeyboardAfterAction = true)
                else -> newline()
            }
        }

        private fun newline() = ImeActionSpec("↵", "Neue Zeile", editorAction = null)
    }
}
