package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompletedTokenLearningPolicyTest {
    @Test fun `space still learns the composing token`() {
        assertEquals("hello", CompletedTokenLearningPolicy.completedTokenBeforeCommit(" ", "hello"))
    }

    @Test fun `sentence punctuation learns the composing token`() {
        listOf(".", ",", "!", "?", ";", ":").forEach { punctuation ->
            assertEquals("hello", CompletedTokenLearningPolicy.completedTokenBeforeCommit(punctuation, "hello"))
        }
    }

    @Test fun `letters apostrophe and hyphen commits do not finish the word`() {
        assertNull(CompletedTokenLearningPolicy.completedTokenBeforeCommit("a", "hello"))
        assertNull(CompletedTokenLearningPolicy.completedTokenBeforeCommit("'", "can"))
        assertNull(CompletedTokenLearningPolicy.completedTokenBeforeCommit("-", "mother"))
    }

    @Test fun `dialect apostrophes and internal hyphens remain learnable`() {
        assertEquals("g'hörig", CompletedTokenLearningPolicy.completedTokenBeforeCommit(".", "g'hörig"))
        assertEquals("mother-in-law", CompletedTokenLearningPolicy.completedTokenBeforeCommit(",", "mother-in-law"))
    }

    @Test fun `blank context does not learn`() {
        assertNull(CompletedTokenLearningPolicy.completedTokenBeforeCommit(".", "   "))
    }

    @Test fun `non-separating punctuation and multi-character commits do not learn`() {
        assertNull(CompletedTokenLearningPolicy.completedTokenBeforeCommit("-", "hello"))
        assertNull(CompletedTokenLearningPolicy.completedTokenBeforeCommit("..", "hello"))
    }
}
