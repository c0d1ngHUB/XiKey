package at.xikey.ime

import java.text.Normalizer
import java.util.Locale

/** Local prefix lookup over the bundled VoraLex dialect forms. */
class DialectSuggestionEngine(words: Collection<String>) {
    private val entries = words
        .asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinctBy(::normalized)
        .map { Entry(it, normalized(it)) }
        .sortedWith(compareBy<Entry> { it.lookup }.thenBy { it.word })
        .toList()

    fun suggestionsFor(prefix: String, limit: Int = DEFAULT_LIMIT): List<String> {
        require(limit > 0) { "limit must be positive" }
        val query = normalized(prefix)
        if (query.isEmpty()) return emptyList()
        return entries.asSequence()
            .filter { it.lookup.startsWith(query) }
            .map(Entry::word)
            .take(limit)
            .toList()
    }

    private data class Entry(val word: String, val lookup: String)

    companion object {
        const val DEFAULT_LIMIT = 3

        fun normalized(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFC)
            .lowercase(Locale.ROOT)
    }
}

/** Identifies the current token while preserving dialect apostrophes such as g'hörig. */
object ComposingWord {
    fun beforeCursor(text: String): String = text.takeLastWhile { it.isLetter() || it == '\'' || it == '’' }
}
