package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardLanguagePreferenceTest {
    @Test
    fun `stored English tag restores English`() {
        assertEquals(
            PredictionLanguage.ENGLISH,
            KeyboardLanguagePreference.restore("en"),
        )
    }

    @Test
    fun `missing or unsupported tag restores Vorarlberg German`() {
        assertEquals(PredictionLanguage.VORARLBERG_GERMAN, KeyboardLanguagePreference.restore(null))
        assertEquals(PredictionLanguage.VORARLBERG_GERMAN, KeyboardLanguagePreference.restore("fr"))
    }

    @Test
    fun `language is stored by its stable prediction tag`() {
        assertEquals("de-AT-vorarlberg", KeyboardLanguagePreference.store(PredictionLanguage.VORARLBERG_GERMAN))
        assertEquals("en", KeyboardLanguagePreference.store(PredictionLanguage.ENGLISH))
    }
}
