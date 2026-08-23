package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestionWordListsTest {
    @Test
    fun `Vorarlberg German reserves up to two leading slots for dialect words`() {
        val lists = SuggestionWordLists(
            dialectWords = listOf("Guata Obed", "Guata Morga", "Guat-wiil"),
            germanWords = listOf("Guantanamo", "Guantanamos"),
            englishWords = emptyList(),
        )

        assertEquals(
            listOf("Guat-wiil", "Guata Morga", "Guantanamo"),
            lists.suggestionsFor(PredictionLanguage.VORARLBERG_GERMAN, "gu", 3),
        )
    }

    @Test
    fun `Vorarlberg German fills free dialect slots with German words`() {
        val lists = SuggestionWordLists(
            dialectWords = listOf("g'hörig"),
            germanWords = listOf("Guten", "Gesundheit"),
            englishWords = emptyList(),
        )

        assertEquals(
            listOf("g'hörig", "Gesundheit", "Guten"),
            lists.suggestionsFor(PredictionLanguage.VORARLBERG_GERMAN, "g", 3),
        )
    }

    @Test
    fun `Vorarlberg German deduplicates normalized words across sources`() {
        val lists = SuggestionWordLists(
            dialectWords = listOf("Guata Morga"),
            germanWords = listOf("guata morga", "Guatemala", "Gummi"),
            englishWords = emptyList(),
        )

        assertEquals(
            listOf("Guata Morga", "Guatemala", "Gummi"),
            lists.suggestionsFor(PredictionLanguage.VORARLBERG_GERMAN, "gu", 3),
        )
    }

    @Test
    fun `English suggestions use only the English word list`() {
        val lists = SuggestionWordLists(
            dialectWords = listOf("garden dialect"),
            germanWords = listOf("Garten"),
            englishWords = listOf("good", "garden"),
        )

        assertEquals(
            listOf("garden", "good"),
            lists.suggestionsFor(PredictionLanguage.ENGLISH, "g", 3),
        )
    }
}
