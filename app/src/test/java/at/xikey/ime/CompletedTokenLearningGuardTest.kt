package at.xikey.ime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompletedTokenLearningGuardTest {
    @Test fun `repeated callback for same completion is ignored`() {
        val guard = CompletedTokenLearningGuard()
        assertTrue(guard.shouldLearn(PredictionLanguage.ENGLISH, "hello", "world", "hello world"))
        assertFalse(guard.shouldLearn(PredictionLanguage.ENGLISH, "hello", "world", "hello world"))
    }

    @Test fun `language and source context are part of deduplication`() {
        val guard = CompletedTokenLearningGuard()
        assertTrue(guard.shouldLearn(PredictionLanguage.ENGLISH, "hello", "world", "hello world"))
        assertTrue(guard.shouldLearn(PredictionLanguage.VORARLBERG_GERMAN, "hello", "world", "hello world"))
        assertTrue(guard.shouldLearn(PredictionLanguage.VORARLBERG_GERMAN, "other", "world", "other world"))
    }

    @Test fun `blank values are not completions`() {
        val guard = CompletedTokenLearningGuard()
        assertFalse(guard.shouldLearn(PredictionLanguage.ENGLISH, null, " ", ""))
    }

    @Test fun `phrases can be deduplicated as one accepted completion`() {
        val guard = CompletedTokenLearningGuard()
        assertTrue(guard.shouldLearn(PredictionLanguage.ENGLISH, "hello", "two words", "hello two"))
        assertFalse(guard.shouldLearn(PredictionLanguage.ENGLISH, "hello", "two words", "hello two"))
    }
}
