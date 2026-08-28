package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestionInsertionPlannerTest {
    @Test fun `completion replaces composing word and appends a single space`() {
        val plan = SuggestionInsertionPlanner.plan(CursorContext.fromText("Gu"), "Guata Morga")
        assertEquals(2, plan.deleteCount)
        assertEquals("Guata Morga ", plan.textToCommit)
    }

    @Test fun `next word insertion keeps exactly one trailing space`() {
        val plan = SuggestionInsertionPlanner.plan(CursorContext.fromText("good "), "morning")
        assertEquals(0, plan.deleteCount)
        assertEquals("morning ", plan.textToCommit)
    }
}
