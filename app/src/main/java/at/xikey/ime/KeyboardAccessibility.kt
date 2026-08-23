package at.xikey.ime

data class SymbolPageAccessibility(val label: String, val contentDescription: String)

/** Dynamic spoken semantics for stateful keyboard controls. */
object KeyboardAccessibility {
    fun shift(state: ShiftState): String = when (state) {
        ShiftState.OFF -> "Umschalttaste aus; zweimal tippen für Feststelltaste"
        ShiftState.AUTO -> "Automatische Großschreibung aktiv; zweimal tippen für Feststelltaste"
        ShiftState.ONESHOT -> "Einmalige Großschreibung aktiv; zweimal tippen für Feststelltaste"
        ShiftState.CAPS_LOCK -> "Feststelltaste aktiv; tippen zum Ausschalten"
    }

    fun language(language: PredictionLanguage): String = when (language) {
        PredictionLanguage.VORARLBERG_GERMAN -> "Aktuelle Sprache: Vorarlberger Deutsch; zu Englisch wechseln"
        PredictionLanguage.ENGLISH -> "Aktuelle Sprache: Englisch; zu Vorarlberger Deutsch wechseln"
    }

    fun symbolPage(page: KeyboardPage): SymbolPageAccessibility = when (page) {
        KeyboardPage.SYMBOLS -> SymbolPageAccessibility("1/2", "Sonderzeichenseite 1 von 2; Seite 2 öffnen")
        KeyboardPage.SYMBOLS_SECONDARY -> SymbolPageAccessibility("2/2", "Sonderzeichenseite 2 von 2; Seite 1 öffnen")
        KeyboardPage.ALPHABETIC -> error("Alphabetic page has no symbol-page indicator")
    }
}
