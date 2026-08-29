package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLearningPolicyTest {
    @Test fun `disabled learning does not persist new words or transitions`() {
        var enabled = false
        val store = InMemoryLearningStore()
        val model = LocalPredictionModel(emptyList(), emptyList(), emptyList(), store) { enabled }

        model.learn(PredictionLanguage.ENGLISH, "hello", "world")

        assertTrue(store.snapshot.words.isEmpty())
        assertTrue(store.snapshot.transitions.isEmpty())
        assertTrue(model.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("wo")).isEmpty())
    }

    @Test fun `clear learning removes persisted aggregates and future suggestions`() {
        val store = InMemoryLearningStore()
        val model = LocalPredictionModel(emptyList(), emptyList(), emptyList(), store)
        model.learn(PredictionLanguage.ENGLISH, null, "hello")
        model.learn(PredictionLanguage.ENGLISH, "hello", "world")

        assertFalse(store.snapshot.words.isEmpty())
        assertFalse(store.snapshot.transitions.isEmpty())

        model.clearLearning()

        assertEquals(0, store.snapshot.words.size)
        assertEquals(0, store.snapshot.transitions.size)
        assertTrue(model.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("he")).isEmpty())
        assertTrue(model.suggestionsFor(PredictionLanguage.ENGLISH, CursorContext.fromText("hello ")).isEmpty())
    }
}
