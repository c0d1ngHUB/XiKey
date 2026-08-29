package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPredictionModelTest {
    private val model = LocalPredictionModel(
        dialectWords = listOf("Guata Morga", "Guata Obed", "g'hörig"),
        germanWords = listOf("Guten Morgen", "Gute Nacht", "Danke"),
        englishWords = listOf("good morning", "thank you", "see you", "garden"),
    )

    @Test fun `dialect prefix completion is preserved`() {
        assertTrue(model.suggestionsFor(PredictionLanguage.VORARLBERG_GERMAN, CursorContext.fromText("Gu")).take(2) == listOf("Guata Morga", "Guata Obed"))
    }

    @Test fun `english word prediction follows a completed word`() {
        val context = CursorContext.fromText("GOOD ")
        assertEquals("morning", model.suggestionsFor(PredictionLanguage.ENGLISH, context).first())
    }

    @Test fun `dialect phrase prediction follows a completed word`() {
        val context = CursorContext.fromText("GuAtA ")
        assertTrue(model.suggestionsFor(PredictionLanguage.VORARLBERG_GERMAN, context).first().equals("morga", ignoreCase = true))
    }

    @Test fun `unknown word boundary does not fall back to prefix completion`() {
        assertTrue(model.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("unknown ")).isEmpty())
    }

    @Test fun `fallback stays local when no phrase matches`() {
        assertEquals(listOf("garden"), model.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("gar")))
    }

    @Test fun `learned words are ranked by frequency and survive reload`() {
        val store = InMemoryLearningStore()
        val first = LocalPredictionModel(emptyList(), emptyList(), emptyList(), store)
        first.learn(PredictionLanguage.ENGLISH, null, "hello")
        first.learn(PredictionLanguage.ENGLISH, null, "hello")
        first.learn(PredictionLanguage.ENGLISH, null, "help")
        assertEquals(listOf("hello", "help"), first.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("he")))
        val reloaded = LocalPredictionModel(emptyList(), emptyList(), emptyList(), store)
        assertEquals(listOf("hello", "help"), reloaded.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("he")))
    }

    @Test fun `learned statistics remain isolated by language`() {
        val model = LocalPredictionModel(emptyList(), emptyList(), emptyList(), InMemoryLearningStore())
        model.learn(PredictionLanguage.ENGLISH, null, "hello")
        assertTrue(model.suggestionsFor(PredictionLanguage.VORARLBERG_GERMAN, CursorContext.fromText("he")).isEmpty())
    }

    @Test fun `learned bigram precedes static phrase and deduplicates`() {
        val model = LocalPredictionModel(emptyList(), emptyList(), listOf("good morning"), InMemoryLearningStore())
        model.learn(PredictionLanguage.ENGLISH, "good", "Morning")
        model.learn(PredictionLanguage.ENGLISH, "good", "morning")
        val result = model.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("good "))
        assertEquals("morning", result.first().lowercase())
        assertEquals(result.size, result.map(String::lowercase).distinct().size)
    }

    @Test fun `latest display form is retained for normalized word`() {
        val model = LocalPredictionModel(emptyList(), emptyList(), emptyList(), InMemoryLearningStore())
        model.learn(PredictionLanguage.ENGLISH, null, "Café")
        model.learn(PredictionLanguage.ENGLISH, null, "CAFÉ")
        assertEquals(listOf("CAFÉ"), model.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("caf")))
    }

    @Test fun `malformed persisted aggregates are ignored`() {
        val store = InMemoryLearningStore(
            LearningSnapshot(
                words = listOf(
                    LearnedWord(PredictionLanguage.ENGLISH, "ok", "ok", 2, 2),
                    LearnedWord(PredictionLanguage.ENGLISH, "NOT-NORMALIZED", "bad", -1, 3),
                    LearnedWord(PredictionLanguage.ENGLISH, "with space", "with space", 4, 4),
                ),
                transitions = listOf(
                    LearnedTransition(PredictionLanguage.ENGLISH, "ok", "next", 1, 5),
                    LearnedTransition(PredictionLanguage.ENGLISH, "NOT NORMALIZED", "next", 1, 6),
                    LearnedTransition(PredictionLanguage.ENGLISH, "ok", "next", 0, 7),
                ),
            ),
        )
        val model = LocalPredictionModel(emptyList(), emptyList(), emptyList(), store)

        assertEquals(listOf("ok"), model.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("o")))
        assertEquals(listOf("next"), model.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("ok ")))
    }

    @Test fun `word eviction is bounded per language`() {
        val store = InMemoryLearningStore()
        val model = LocalPredictionModel(emptyList(), emptyList(), emptyList(), store)
        repeat(LocalPredictionModel.MAX_WORDS + 1) { index ->
            model.learn(PredictionLanguage.ENGLISH, null, "word$index")
        }

        assertEquals(LocalPredictionModel.MAX_WORDS, store.snapshot.words.count { it.language == PredictionLanguage.ENGLISH })
        assertTrue(store.snapshot.words.none { it.key == "word0" })
        assertEquals("word2000", model.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("word")).first())
    }

    @Test fun `transition eviction is bounded and removes the oldest lowest-frequency entry`() {
        val store = InMemoryLearningStore()
        val model = LocalPredictionModel(emptyList(), emptyList(), emptyList(), store)
        repeat(LocalPredictionModel.MAX_TRANSITIONS + 1) { index ->
            model.learn(PredictionLanguage.ENGLISH, "from$index", "to$index")
        }

        assertEquals(LocalPredictionModel.MAX_TRANSITIONS, store.snapshot.transitions.size)
        assertTrue(store.snapshot.transitions.none { it.from == "from0" })
        assertTrue(store.snapshot.transitions.any { it.from == "from5000" && it.to == "to5000" })
    }

    @Test fun `load failure leaves a usable empty model`() {
        val model = LocalPredictionModel(
            emptyList(), emptyList(), emptyList(),
            object : LearningStore {
                override fun load(): LearningSnapshot = error("corrupt persisted state")
                override fun save(snapshot: LearningSnapshot) = error("store unavailable")
            },
        )

        model.learn(PredictionLanguage.ENGLISH, null, "usable")
        assertEquals(listOf("usable"), model.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("usa")))
    }

    @Test fun `invalid snapshot schema data is ignored without hiding valid entries`() {
        val store = InMemoryLearningStore(
            LearningSnapshot(
                words = listOf(
                    LearnedWord(PredictionLanguage.ENGLISH, "valid", "valid", 1, 1),
                    LearnedWord(PredictionLanguage.ENGLISH, "invalid key", "invalid", 99, 2),
                ),
                transitions = listOf(
                    LearnedTransition(PredictionLanguage.ENGLISH, "valid", "next", 1, 3),
                    LearnedTransition(PredictionLanguage.ENGLISH, "invalid key", "next", 99, 4),
                ),
            ),
        )
        val model = LocalPredictionModel(emptyList(), emptyList(), emptyList(), store)

        assertEquals(listOf("valid"), model.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("val")))
        assertEquals(listOf("next"), model.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("valid ")))
    }

    @Test fun `suggestion refresh context changes across punctuation transitions`() {
        val afterSpace = SuggestionRefreshContext.from(CursorContext.fromText("good "))
        val afterPeriod = SuggestionRefreshContext.from(CursorContext.fromText("good ."))

        assertEquals("good", afterSpace.previousWord)
        assertEquals("good", afterPeriod.previousWord)
        assertTrue(afterSpace.atWordBoundary)
        assertTrue(!afterPeriod.atWordBoundary)
        assertTrue(afterSpace != afterPeriod)
    }

    @Test fun `cursor context keeps completed word after punctuation`() {
        val context = CursorContext.fromText("good .")
        assertEquals("", context.composingWord)
        assertEquals("good", context.previousWord)
        assertTrue(!context.atWordBoundary)
    }

    @Test fun `single-word source entries do not affect phrase indexing`() {
        val manySingleWords = (1..20_000).map { "word$it" }
        val model = LocalPredictionModel(
            dialectWords = manySingleWords + listOf("alpha beta"),
            germanWords = manySingleWords,
            englishWords = manySingleWords + listOf("gamma delta"),
        )

        assertEquals(listOf("beta"), model.suggestionsFor(PredictionLanguage.VORARLBERG_GERMAN, CursorContext.fromText("alpha ")))
        assertTrue(model.suggestionsFor(PredictionLanguage.VORARLBERG_GERMAN, CursorContext.fromText("word1 ")).isEmpty())
        assertEquals(listOf("delta"), model.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("gamma ")))
        assertTrue(model.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("word1 ")).isEmpty())
    }
}
