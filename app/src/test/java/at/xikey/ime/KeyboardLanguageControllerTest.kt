package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardLanguageControllerTest {
    @Test
    fun `switching cycles between Vorarlberg German and English`() {
        val controller = KeyboardLanguageController()

        assertEquals(PredictionLanguage.VORARLBERG_GERMAN, controller.current)
        assertEquals(PredictionLanguage.ENGLISH, controller.switchToNext())
        assertEquals(PredictionLanguage.VORARLBERG_GERMAN, controller.switchToNext())
    }
}
