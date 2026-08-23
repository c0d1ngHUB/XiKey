package at.xikey.ime

/** Immutable setup progress rendered by the launcher screen. */
data class ImeSetupStatus(
    val enabled: Boolean,
    val selected: Boolean,
) {
    val activationStatus: String
        get() = if (enabled) "✓ XiKey ist aktiviert" else "1 · XiKey noch nicht aktiviert"

    val activationButtonLabel: String
        get() = if (enabled) "Einstellungen öffnen" else "Tastatur aktivieren"

    val selectionStatus: String
        get() = when {
            selected -> "✓ XiKey ist aktuell ausgewählt"
            enabled -> "2 · XiKey jetzt auswählen"
            else -> "2 · Nach der Aktivierung auswählen"
        }

    val selectionButtonLabel: String
        get() = if (selected) "Tastatur wechseln" else "XiKey als Tastatur wählen"

    val selectionEnabled: Boolean
        get() = enabled
}
