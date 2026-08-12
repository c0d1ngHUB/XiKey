package at.xikey.ime

/** Keeps local suggestion dictionaries separate while selecting the right language at runtime. */
class SuggestionWordLists(
    dialectWords: Collection<String>,
    germanWords: Collection<String>,
    englishWords: Collection<String>,
) {
    private val vorarlbergGerman = DialectSuggestionEngine(dialectWords + germanWords)
    private val english = DialectSuggestionEngine(englishWords)

    fun forLanguage(language: PredictionLanguage): DialectSuggestionEngine = when (language) {
        PredictionLanguage.VORARLBERG_GERMAN -> vorarlbergGerman
        PredictionLanguage.ENGLISH -> english
    }
}
