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

    @Test
    fun `shift renders eszett as uppercase eszett`() {
        val shift = KeyboardShiftController()

        shift.toggle()
        assertEquals("ẞ", shift.applyTo("ß"))
        assertFalse(shift.isShifted)
    }

    @Test
    fun `reset cancels a pending shift without committing a key`() {
        val shift = KeyboardShiftController()

        shift.toggle()
        shift.reset()

        assertFalse(shift.isShifted)
        assertEquals("a", shift.applyTo("a"))
    }

    @Test
    fun `double-tap within window activates caps-lock`() {
        var clock = 0L
        val shift = KeyboardShiftController(nowProvider = { clock })

        shift.toggle()    // shift on
        clock = 150       // within 300ms window
        shift.toggle()    // second tap → caps-lock

        assertTrue(shift.isCapsLocked)
        assertTrue(shift.isShifted)
    }

    @Test
    fun `caps-lock keeps shift active across multiple keys`() {
        var clock = 0L
        val shift = KeyboardShiftController(nowProvider = { clock })

        shift.toggle()
        clock = 200
        shift.toggle()    // caps-lock on

        assertEquals("A", shift.applyTo("a"))
        assertTrue(shift.isShifted)  // still shifted
        assertEquals("B", shift.applyTo("b"))
        assertTrue(shift.isShifted)  // still shifted
    }

    @Test
    fun `double-tap outside window does not activate caps-lock`() {
        var clock = 0L
        val shift = KeyboardShiftController(nowProvider = { clock })

        shift.toggle()    // shift on
        clock = 500       // outside 300ms window
        shift.toggle()    // just toggles off

        assertFalse(shift.isCapsLocked)
        assertFalse(shift.isShifted)
    }

    @Test
    fun `toggle while caps-locked deactivates caps-lock`() {
        var clock = 0L
        val shift = KeyboardShiftController(nowProvider = { clock })

        shift.toggle()
        clock = 200
        shift.toggle()    // caps-lock on
        clock = 1000
        shift.toggle()    // caps-lock off

        assertFalse(shift.isCapsLocked)
        assertFalse(shift.isShifted)
    }

    @Test
    fun `reset clears caps-lock`() {
        var clock = 0L
        val shift = KeyboardShiftController(nowProvider = { clock })

        shift.toggle()
        clock = 200
        shift.toggle()    // caps-lock on
        shift.reset()

        assertFalse(shift.isCapsLocked)
        assertFalse(shift.isShifted)
    }

    @Test
    fun `two taps from automatic shift activate caps-lock`() {
        var clock = 0L
        val shift = KeyboardShiftController(nowProvider = { clock })
        shift.autoEnableForContext("")

        shift.toggle()
        clock = 150
        shift.toggle()

        assertTrue(shift.isCapsLocked)
        assertEquals(ShiftState.CAPS_LOCK, shift.state)
    }

    @Test
    fun `caps-lock uppercases eszett`() {
        var clock = 0L
        val shift = KeyboardShiftController(nowProvider = { clock })

        shift.toggle()
        clock = 200
        shift.toggle()    // caps-lock on

        assertEquals("ẞ", shift.applyTo("ß"))
        assertTrue(shift.isShifted)  // still locked
    }

    @Test
    fun `text start automatically enables shift`() {
        val shift = KeyboardShiftController()

        shift.autoEnableForContext("")

        assertTrue(shift.isShifted)
    }

    @Test
    fun `sentence-ending punctuation automatically enables shift`() {
        listOf("Hello.", "Hello!", "Hello?", "Hello. ").forEach { context ->
            val shift = KeyboardShiftController()

            shift.autoEnableForContext(context)

            assertTrue("Expected shift for context: $context", shift.isShifted)
        }
    }

    @Test
    fun `ordinary text does not automatically enable shift`() {
        val shift = KeyboardShiftController()

        shift.autoEnableForContext("Hello")

        assertFalse(shift.isShifted)
    }
}
