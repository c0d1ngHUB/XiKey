package at.xikey.ime

import org.junit.Assert.assertEquals
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
    fun `symbols page has common number and punctuation rows`() {
        val layout = KeyboardLayout.symbols()

        assertEquals(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), layout.rows.first())
        assertEquals(listOf("-", "/", ":", ";", "(", ")", "€", "&", "@", "\""), layout.rows[1])
        assertEquals(listOf("#", "+", "=", "_", "!", "?", "'", "%"), layout.rows[2])
    }
}
