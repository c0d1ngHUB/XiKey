package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionWordListsTest {
    @Test
    fun `Vorarlberg German suggestions combine dialect and German words`() {
        val lists = SuggestionWordLists(
            dialectWords = listOf("g'hörig"),
            germanWords = listOf("Guten", "Gesundheit"),
            englishWords = listOf("good", "garden"),
        )

        assertEquals(
            listOf("g'hörig", "Gesundheit", "Guten"),
            lists.forLanguage(PredictionLanguage.VORARLBERG_GERMAN).suggestionsFor("g", 3),
        )
    }

    @Test
    fun `English suggestions use the English word list`() {
        val lists = SuggestionWordLists(
            dialectWords = listOf("g'hörig"),
            germanWords = listOf("Guten"),
            englishWords = listOf("good", "garden"),
        )

        assertEquals(
            listOf("garden", "good"),
            lists.forLanguage(PredictionLanguage.ENGLISH).suggestionsFor("g", 3),
        )
    }
}
