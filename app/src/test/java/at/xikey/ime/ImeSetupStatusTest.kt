package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeSetupStatusTest {
    @Test
    fun `fresh setup shows activation as the next required step`() {
        val status = ImeSetupStatus(enabled = false, selected = false)

        assertEquals("1 · XiKey noch nicht aktiviert", status.activationStatus)
        assertEquals("Tastatur aktivieren", status.activationButtonLabel)
        assertEquals("2 · Nach der Aktivierung auswählen", status.selectionStatus)
        assertFalse(status.selectionEnabled)
    }

    @Test
    fun `enabled setup makes keyboard selection available`() {
        val status = ImeSetupStatus(enabled = true, selected = false)

        assertEquals("✓ XiKey ist aktiviert", status.activationStatus)
        assertEquals("Einstellungen öffnen", status.activationButtonLabel)
        assertEquals("2 · XiKey jetzt auswählen", status.selectionStatus)
        assertTrue(status.selectionEnabled)
    }

    @Test
    fun `completed setup exposes both successful states`() {
        val status = ImeSetupStatus(enabled = true, selected = true)

        assertEquals("✓ XiKey ist aktiviert", status.activationStatus)
        assertEquals("✓ XiKey ist aktuell ausgewählt", status.selectionStatus)
        assertEquals("Tastatur wechseln", status.selectionButtonLabel)
    }
}
