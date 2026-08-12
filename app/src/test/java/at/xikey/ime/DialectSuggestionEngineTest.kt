package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DialectSuggestionEngineTest {
    private val engine = DialectSuggestionEngine(
        listOf("Guata Morga", "Guata Obed", "g'hörig", "hocka", "Schtoa"),
    )

    @Test
    fun `suggestions match a dialect prefix regardless of case`() {
        assertEquals(listOf("Guata Morga", "Guata Obed"), engine.suggestionsFor("gu", 3))
    }

    @Test
    fun `suggestions preserve apostrophes in dialect forms`() {
        assertEquals(listOf("g'hörig"), engine.suggestionsFor("G'", 3))
    }

    @Test
    fun `suggestions are empty for a blank prefix`() {
        assertTrue(engine.suggestionsFor("   ", 3).isEmpty())
    }

    @Test
    fun `suggestions are capped to requested count`() {
        assertEquals(1, engine.suggestionsFor("g", 1).size)
    }

    @Test
    fun `word before cursor keeps dialect apostrophes but stops at whitespace`() {
        assertEquals("g'hö", ComposingWord.beforeCursor("Guata g'hö"))
        assertEquals("", ComposingWord.beforeCursor("Guata "))
    }

    @Test
    fun `word before cursor strips punctuation boundary`() {
        assertEquals("hock", ComposingWord.beforeCursor("Heile, hock"))
    }
}
