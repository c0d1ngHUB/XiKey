package at.xikey.ime

import java.text.Normalizer
import java.util.Locale

/** Text state immediately before the editor cursor. */
data class CursorContext(
    val textBeforeCursor: String,
    val composingWord: String,
    val previousWord: String,
    val atWordBoundary: Boolean,
) {
    companion object {
        fun fromText(text: String): CursorContext {
            val composingWord = ComposingWord.beforeCursor(text)
            val withoutComposing = if (composingWord.isEmpty()) text else text.dropLast(composingWord.length)
            val previousWord = WordBoundaries.previousWord(withoutComposing)
            val atWordBoundary = withoutComposing.isNotEmpty() && withoutComposing.last().isWhitespace()
            return CursorContext(text, composingWord, previousWord, atWordBoundary)
        }
    }
}

object WordBoundaries {
    private val boundaryChars = setOf(' ', '\n', '\t', '\u00A0', '.', ',', '!', '?', ';', ':', '(', ')', '[', ']', '{', '}', '"')

    fun previousWord(text: String): String {
        var idx = text.length - 1
        while (idx >= 0 && text[idx] in boundaryChars) idx--
        val end = idx + 1
        while (idx >= 0 && (text[idx].isLetter() || text[idx] == '\'' || text[idx] == '’' || text[idx] == '-')) idx--
        return if (end <= idx + 1) "" else text.substring(idx + 1, end)
    }
}

private fun String.whitespaceTokens(): Sequence<String> = sequence {
    var start = -1
    for (index in indices) {
        if (this@whitespaceTokens[index].isWhitespace()) {
            if (start >= 0) {
                yield(substring(start, index))
                start = -1
            }
        } else if (start < 0) {
            start = index
        }
    }
    if (start >= 0) yield(substring(start))
}

data class LearnedWord(val language: PredictionLanguage, val key: String, val display: String, val count: Int, val lastSeen: Long)
data class LearnedTransition(val language: PredictionLanguage, val from: String, val to: String, val count: Int, val lastSeen: Long)
data class LearningSnapshot(
    val words: List<LearnedWord> = emptyList(),
    val transitions: List<LearnedTransition> = emptyList(),
    val sequence: Long = 0L,
)

/** Small persistence boundary; implementations must never make prediction fail. */
interface LearningStore {
    fun save(snapshot: LearningSnapshot)
    fun load(): LearningSnapshot
}

object NoOpLearningStore : LearningStore {
    override fun save(snapshot: LearningSnapshot) = Unit
    override fun load(): LearningSnapshot = LearningSnapshot()
}

/** In-memory store useful for JVM tests and callers that do not want persistence. */
class InMemoryLearningStore(initial: LearningSnapshot = LearningSnapshot()) : LearningStore {
    var snapshot: LearningSnapshot = initial
        private set
    override fun save(snapshot: LearningSnapshot) { this.snapshot = snapshot }
    override fun load(): LearningSnapshot = snapshot
}

/** Local-only ranking and learning. No Android or network dependency. */
class LocalPredictionModel(
    dialectWords: Collection<String>,
    germanWords: Collection<String>,
    englishWords: Collection<String>,
    private val store: LearningStore = NoOpLearningStore,
    private val learningEnabled: () -> Boolean = { true },
) {
    companion object {
        const val MAX_WORDS = 2_000
        const val MAX_TRANSITIONS = 5_000
        fun normalized(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFC).lowercase(Locale.ROOT)
        private val GERMAN_DIALECT_PHRASES = listOf("guata morga", "guata obed", "uf wiederluaga", "machan's guat")
        private val STANDARD_GERMAN_PHRASES = listOf("guten morgen", "gute nacht", "vielen dank", "wie gehts")
        private val ENGLISH_PHRASES = listOf("good morning", "good night", "how are", "thank you", "see you")
    }

    private val baseSuggestions = SuggestionWordLists(dialectWords, germanWords, englishWords)
    private val phraseIndex = PhraseIndex(
        dialectWords + GERMAN_DIALECT_PHRASES,
        germanWords + STANDARD_GERMAN_PHRASES,
        englishWords + ENGLISH_PHRASES,
    )
    private val words = linkedMapOf<Pair<PredictionLanguage, String>, LearnedWord>()
    private val transitions = linkedMapOf<Triple<PredictionLanguage, String, String>, LearnedTransition>()
    private var sequence = 0L

    init {
        runCatching { restore(store.load()) }.onFailure { clearLearning() }
    }

    /** Learns one completed token and its optional immediately preceding token. */
    @Synchronized
    fun learn(language: PredictionLanguage, previousWord: String?, completedWord: String) {
        if (!learningEnabled()) return
        val wordKey = normalized(completedWord)
        if (wordKey.isBlank() || wordKey.any { it.isWhitespace() }) return
        sequence++
        val wordId = language to wordKey
        val old = words[wordId]
        words[wordId] = LearnedWord(language, wordKey, completedWord.trim(), (old?.count ?: 0) + 1, sequence)
        val from = normalized(previousWord.orEmpty())
        if (from.isNotBlank() && from.none(Char::isWhitespace)) {
            val key = Triple(language, from, wordKey)
            val oldTransition = transitions[key]
            transitions[key] = LearnedTransition(language, from, wordKey, (oldTransition?.count ?: 0) + 1, sequence)
        }
        evict()
        persistSafely()
    }

    fun learnPhrase(language: PredictionLanguage, previousWord: String?, phrase: String) {
        if (!learningEnabled()) return
        var previous = previousWord
        for (token in phrase.whitespaceTokens()) {
            learn(language, previous, token)
            previous = token
        }
    }

    @Synchronized
    fun clearLearning() {
        words.clear()
        transitions.clear()
        sequence = 0L
        persistSafely()
    }

    @Synchronized
    fun suggestionsFor(language: PredictionLanguage, context: CursorContext, limit: Int = 3): List<String> {
        require(limit > 0) { "limit must be positive" }
        if (context.composingWord.isNotEmpty()) {
            val prefix = normalized(context.composingWord)
            val personal = words.values.asSequence()
                .filter { it.language == language && it.key.startsWith(prefix) }
                .sortedWith(compareByDescending<LearnedWord> { it.count }.thenByDescending { it.lastSeen }.thenBy { it.key })
                .map { it.display }
            return mergeUnique(personal, baseSuggestions.suggestionsFor(language, context.composingWord, limit), limit)
        }
        if (!context.atWordBoundary || context.previousWord.isBlank()) return emptyList()
        val from = normalized(context.previousWord)
        val personal = transitions.values.asSequence()
            .filter { it.language == language && it.from == from }
            .sortedWith(compareByDescending<LearnedTransition> { it.count }.thenByDescending { it.lastSeen }.thenBy { it.to })
            .mapNotNull { words[language to it.to]?.display ?: it.to }
        val fallback = phraseIndex.nextWords(language, context.previousWord, limit)
        return mergeUnique(personal, fallback, limit)
    }

    private fun mergeUnique(first: Sequence<String>, second: List<String>, limit: Int): List<String> {
        val seen = mutableSetOf<String>()
        return (first + second.asSequence()).filter { seen.add(normalized(it)) }.take(limit).toList()
    }

    private fun restore(snapshot: LearningSnapshot) {
        // Stores are an input boundary: tolerate old/corrupt snapshots and never
        // let malformed aggregate data enter the ranking maps.
        sequence = snapshot.sequence.coerceAtLeast(0L)
        snapshot.words.asSequence()
            .filter { it.key == normalized(it.key) && it.key.isNotBlank() && it.key.none(Char::isWhitespace) }
            .filter { it.display.isNotBlank() && !it.display.any(Char::isWhitespace) }
            .filter { it.count > 0 && it.lastSeen >= 0L }
            .forEach { candidate ->
                val id = candidate.language to candidate.key
                val current = words[id]
                words[id] = if (current == null || candidate.lastSeen >= current.lastSeen) candidate else current
                sequence = maxOf(sequence, candidate.lastSeen)
            }
        snapshot.transitions.asSequence()
            .filter { it.from == normalized(it.from) && it.to == normalized(it.to) }
            .filter { it.from.isNotBlank() && it.to.isNotBlank() }
            .filter { it.from.none(Char::isWhitespace) && it.to.none(Char::isWhitespace) }
            .filter { it.count > 0 && it.lastSeen >= 0L }
            .forEach { candidate ->
                val id = Triple(candidate.language, candidate.from, candidate.to)
                val current = transitions[id]
                transitions[id] = if (current == null || candidate.lastSeen >= current.lastSeen) candidate else current
                sequence = maxOf(sequence, candidate.lastSeen)
            }
        evict()
    }

    private fun evict() {
        for (language in PredictionLanguage.entries) {
            while (words.values.count { it.language == language } > MAX_WORDS) {
                val victim = words.values.filter { it.language == language }
                    .minWithOrNull(compareBy<LearnedWord> { it.count }.thenBy { it.lastSeen }.thenBy { it.key }) ?: break
                words.remove(victim.language to victim.key)
            }
        }
        for (language in PredictionLanguage.entries) {
            while (transitions.values.count { it.language == language } > MAX_TRANSITIONS) {
                val victim = transitions.values.filter { it.language == language }
                    .minWithOrNull(compareBy<LearnedTransition> { it.count }.thenBy { it.lastSeen }.thenBy { it.from }.thenBy { it.to }) ?: break
                transitions.remove(Triple(victim.language, victim.from, victim.to))
            }
        }
    }

    private fun persistSafely() {
        runCatching { store.save(LearningSnapshot(words.values.toList(), transitions.values.toList(), sequence)) }
    }


    private class PhraseIndex(dialect: Collection<String>, german: Collection<String>, english: Collection<String>) {
        private val transitionsByLanguage = mapOf(
            PredictionLanguage.VORARLBERG_GERMAN to merge(build(dialect), build(german)),
            PredictionLanguage.ENGLISH to build(english),
        )
        fun nextWords(language: PredictionLanguage, previousWord: String, limit: Int): List<String> =
            transitionsByLanguage[language]?.get(normalized(previousWord)).orEmpty().take(limit)
        private fun build(values: Collection<String>): Map<String, List<String>> {
            val grouped = linkedMapOf<String, MutableSet<String>>()
            values.forEach { value ->
                var previous: String? = null
                for (token in value.whitespaceTokens()) {
                    val prior = previous
                    if (prior != null) grouped.getOrPut(normalized(prior)) { linkedSetOf() }.add(token)
                    previous = token
                }
            }
            return grouped.mapValues { it.value.toList() }
        }
        private fun merge(a: Map<String, List<String>>, b: Map<String, List<String>>): Map<String, List<String>> {
            val result = linkedMapOf<String, MutableSet<String>>()
            (a.entries + b.entries).forEach { result.getOrPut(it.key) { linkedSetOf() }.addAll(it.value) }
            return result.mapValues { it.value.toList() }
        }
    }

}
