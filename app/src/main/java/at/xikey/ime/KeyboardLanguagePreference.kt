package at.xikey.ime

/** Stable serialization for the active layout preference. */
object KeyboardLanguagePreference {
    fun restore(storedTag: String?): PredictionLanguage = storedTag
        ?.let { tag -> PredictionLanguage.entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } }
        ?: PredictionLanguage.VORARLBERG_GERMAN

    fun store(language: PredictionLanguage): String = language.tag
}
