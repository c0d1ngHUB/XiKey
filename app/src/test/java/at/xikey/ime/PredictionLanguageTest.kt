package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class PredictionLanguageTest {
    @Test
    fun `recognizes only Vorarlberg German and English`() {
        assertEquals(PredictionLanguage.VORARLBERG_GERMAN, PredictionLanguage.fromTag("de-AT-vorarlberg"))
        assertEquals(PredictionLanguage.ENGLISH, PredictionLanguage.fromTag("en"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects unsupported language`() {
        PredictionLanguage.fromTag("fr")
    }
}
