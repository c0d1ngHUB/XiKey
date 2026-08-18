package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardPageControllerTest {
    @Test
    fun `toggle switches from alphabetic to symbols and back`() {
        val controller = KeyboardPageController()

        assertEquals(KeyboardPage.ALPHABETIC, controller.current)
        assertEquals(KeyboardPage.SYMBOLS, controller.toggle())
        assertEquals(KeyboardPage.ALPHABETIC, controller.toggle())
    }

    @Test
    fun `secondary symbols are reachable and return to primary symbols`() {
        val controller = KeyboardPageController(KeyboardPage.SYMBOLS)

        assertEquals(KeyboardPage.SYMBOLS_SECONDARY, controller.showSecondarySymbols())
        assertEquals(KeyboardPage.SYMBOLS, controller.showPrimarySymbols())
    }

    @Test
    fun `symbols page has common number and punctuation rows`() {
        val layout = KeyboardLayout.symbols()

        assertEquals(listOf("1", "2", "3", "4", "5", "6", "7"), layout.rows.first())
        assertEquals(listOf("8", "9", "0", "@", "#", "€", "%"), layout.rows[1])
        assertEquals(listOf("&", "-", "+", "(", ")", "/"), layout.rows[2])
        assertEquals(listOf("*", "\"", "'", ":", ";", "!", "?"), layout.rows[3])
    }

    @Test
    fun `secondary symbols include mathematical and currency keys`() {
        val layout = KeyboardLayout.secondarySymbols()

        assertEquals(listOf("~", "\\", "|", "•", "√", "π", "÷"), layout.rows.first())
        assertEquals(listOf("×", "§", "Δ", "£", "¥", "$", "¢"), layout.rows[1])
    }

    @Test
    fun `symbol rows have at most 7 keys to keep touch targets large`() {
        val symbols = KeyboardLayout.symbols()
        val secondary = KeyboardLayout.secondarySymbols()

        symbols.rows.forEach { row -> assertTrue("Row $row exceeds 7 keys", row.size <= 7) }
        secondary.rows.forEach { row -> assertTrue("Row $row exceeds 7 keys", row.size <= 7) }
    }
}
