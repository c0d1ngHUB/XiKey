package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardAccessibilityTest {
    @Test
    fun `shift descriptions expose every current state`() {
        assertEquals("Umschalttaste aus; zweimal tippen für Feststelltaste", KeyboardAccessibility.shift(ShiftState.OFF))
        assertEquals("Automatische Großschreibung aktiv; zweimal tippen für Feststelltaste", KeyboardAccessibility.shift(ShiftState.AUTO))
        assertEquals("Einmalige Großschreibung aktiv; zweimal tippen für Feststelltaste", KeyboardAccessibility.shift(ShiftState.ONESHOT))
        assertEquals("Feststelltaste aktiv; tippen zum Ausschalten", KeyboardAccessibility.shift(ShiftState.CAPS_LOCK))
    }

    @Test
    fun `language description names current and target language`() {
        assertEquals(
            "Aktuelle Sprache: Vorarlberger Deutsch; zu Englisch wechseln",
            KeyboardAccessibility.language(PredictionLanguage.VORARLBERG_GERMAN),
        )
        assertEquals(
            "Aktuelle Sprache: Englisch; zu Vorarlberger Deutsch wechseln",
            KeyboardAccessibility.language(PredictionLanguage.ENGLISH),
        )
    }

    @Test
    fun `symbol page description and label expose current page`() {
        assertEquals(SymbolPageAccessibility("1/2", "Sonderzeichenseite 1 von 2; Seite 2 öffnen"), KeyboardAccessibility.symbolPage(KeyboardPage.SYMBOLS))
        assertEquals(SymbolPageAccessibility("2/2", "Sonderzeichenseite 2 von 2; Seite 1 öffnen"), KeyboardAccessibility.symbolPage(KeyboardPage.SYMBOLS_SECONDARY))
    }
}
