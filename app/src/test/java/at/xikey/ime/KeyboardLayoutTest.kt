package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutTest {
    @Test
    fun `German layout exposes QWERTZ rows including umlauts`() {
        val layout = KeyboardLayout.forLanguage(PredictionLanguage.VORARLBERG_GERMAN)

        assertEquals(listOf("q", "w", "e", "r", "t", "z", "u", "i", "o", "p", "ü"), layout.rows.first())
        assertTrue(layout.rows.flatten().containsAll(listOf("ä", "ö")))
    }

    @Test
    fun `English layout exposes QWERTY rows`() {
        val layout = KeyboardLayout.forLanguage(PredictionLanguage.ENGLISH)

        assertEquals(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"), layout.rows.first())
    }
}
