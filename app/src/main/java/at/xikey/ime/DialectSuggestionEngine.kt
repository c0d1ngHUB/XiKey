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

    private val lookupKeys: List<String> = entries.map(Entry::lookup)

    fun suggestionsFor(prefix: String, limit: Int = DEFAULT_LIMIT): List<String> {
        require(limit > 0) { "limit must be positive" }
        val query = normalized(prefix)
        if (query.isEmpty()) return emptyList()

        val startIdx = findFirstPrefixIndex(query)
        if (startIdx < 0) return emptyList()

        return entries.subList(startIdx, entries.size)
            .asSequence()
            .filter { it.lookup.startsWith(query) }
            .map(Entry::word)
            .take(limit)
            .toList()
    }

    /**
     * Binary search for the first entry whose lookup key starts with [query].
     * Returns -1 if no match exists.
     */
    private fun findFirstPrefixIndex(query: String): Int {
        var lo = 0
        var hi = lookupKeys.size - 1
        var result = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val key = lookupKeys[mid]
            if (key.startsWith(query)) {
                result = mid
                hi = mid - 1  // look further left for an earlier match
            } else if (key < query) {
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return result
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
