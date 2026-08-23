package at.xikey.ime

/** Keeps local suggestion dictionaries separate and applies language-specific ranking. */
class SuggestionWordLists(
    dialectWords: Collection<String>,
    germanWords: Collection<String>,
    englishWords: Collection<String>,
) {
    private val dialect = DialectSuggestionEngine(dialectWords)
    private val german = DialectSuggestionEngine(germanWords)
    private val english = DialectSuggestionEngine(englishWords)

    fun suggestionsFor(
        language: PredictionLanguage,
        prefix: String,
        limit: Int = DialectSuggestionEngine.DEFAULT_LIMIT,
    ): List<String> {
        require(limit > 0) { "limit must be positive" }
        return when (language) {
            PredictionLanguage.VORARLBERG_GERMAN -> vorarlbergGermanSuggestions(prefix, limit)
            PredictionLanguage.ENGLISH -> english.suggestionsFor(prefix, limit)
        }
    }

    private fun vorarlbergGermanSuggestions(prefix: String, limit: Int): List<String> {
        val dialectMatches = dialect.suggestionsFor(prefix, minOf(limit, DIALECT_PRIORITY_SLOTS))
        val germanMatches = german.suggestionsFor(prefix, limit + dialectMatches.size)
        val seen = mutableSetOf<String>()

        return (dialectMatches + germanMatches)
            .asSequence()
            .filter { seen.add(DialectSuggestionEngine.normalized(it)) }
            .take(limit)
            .toList()
    }

    private companion object {
        const val DIALECT_PRIORITY_SLOTS = 2
    }
}
