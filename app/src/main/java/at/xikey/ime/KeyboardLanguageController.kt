package at.xikey.ime

/** Holds the active language independently from the Android view lifecycle. */
class KeyboardLanguageController(
    initial: PredictionLanguage = PredictionLanguage.VORARLBERG_GERMAN,
) {
    var current: PredictionLanguage = initial
        private set

    fun switchToNext(): PredictionLanguage {
        current = when (current) {
            PredictionLanguage.VORARLBERG_GERMAN -> PredictionLanguage.ENGLISH
            PredictionLanguage.ENGLISH -> PredictionLanguage.VORARLBERG_GERMAN
        }
        return current
    }
}
