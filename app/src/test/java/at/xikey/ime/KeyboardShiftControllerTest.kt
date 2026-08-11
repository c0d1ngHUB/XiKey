package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardShiftControllerTest {
    @Test
    fun `shift uppercases the next letter then resets`() {
        val shift = KeyboardShiftController()

        assertFalse(shift.isShifted)
        shift.toggle()
        assertTrue(shift.isShifted)
        assertEquals("Ä", shift.applyTo("ä"))
        assertFalse(shift.isShifted)
    }
}
