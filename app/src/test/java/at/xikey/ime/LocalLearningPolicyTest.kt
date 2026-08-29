package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLearningPolicyTest {
    private class ResettableLearningStore : LearningStore {
        var snapshot: LearningSnapshot = LearningSnapshot()
            private set
        override fun save(snapshot: LearningSnapshot) {
            this.snapshot = snapshot
        }
        override fun load(): LearningSnapshot = snapshot
        fun clear() {
            snapshot = LearningSnapshot()
        }
    }

    @Test fun `normal learning survives persistence and remains suggestible`() {
        var enabled = true
        var resetGeneration = 0L
        val store = InMemoryLearningStore()
        val model = LocalPredictionModel(
            listOf("guata", "morga"),
            listOf("guten", "morgen"),
            listOf("hello", "world"),
            store = store,
            learningEnabled = { enabled },
            resetGeneration = { resetGeneration },
        )

        model.learn(PredictionLanguage.ENGLISH, "hello", "world")

        assertEquals(1, store.snapshot.words.size)
        assertEquals(1, store.snapshot.transitions.size)
        assertTrue(model.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("wo")).contains("world"))
    }

    @Test fun `disabling learning preserves existing data and blocks new learning`() {
        var enabled = true
        val store = InMemoryLearningStore()
        val model = LocalPredictionModel(
            emptyList(),
            emptyList(),
            listOf("hello", "world"),
            store = store,
            learningEnabled = { enabled },
        )

        model.learn(PredictionLanguage.ENGLISH, null, "hello")
        enabled = false

        model.learn(PredictionLanguage.ENGLISH, null, "world")

        assertTrue(store.snapshot.words.any { it.display == "hello" })
        assertFalse(store.snapshot.words.any { it.display == "world" })
        assertTrue(model.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("he")).contains("hello"))
    }

    @Test fun `reset generation clears live learning without reviving deleted data`() {
        var enabled = true
        var resetGeneration = 0L
        val store = ResettableLearningStore()
        val model = LocalPredictionModel(
            emptyList(),
            emptyList(),
            listOf("alpha", "beta"),
            store = store,
            learningEnabled = { enabled },
            resetGeneration = { resetGeneration },
        )

        model.learn(PredictionLanguage.ENGLISH, null, "zebra")
        assertTrue(model.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("ze")).contains("zebra"))

        resetGeneration += 1L
        store.clear()

        assertTrue(model.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("ze")).isEmpty())
        model.learn(PredictionLanguage.ENGLISH, null, "wolf")

        assertEquals(1, store.snapshot.words.size)
        assertTrue(store.snapshot.words.any { it.display == "wolf" })
        assertFalse(store.snapshot.words.any { it.display == "zebra" })
    }

    @Test fun `static suggestions still work while learning is paused`() {
        var enabled = false
        val store = InMemoryLearningStore()
        val model = LocalPredictionModel(
            emptyList(),
            emptyList(),
            listOf("hello", "world"),
            store = store,
            learningEnabled = { enabled },
        )

        assertTrue(model.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("he")).contains("hello"))
    }
}
