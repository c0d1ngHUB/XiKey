package at.xikey.ime

import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeActionSpecTest {
    @Test
    fun `done maps to matching icon description action and keyboard dismissal`() {
        val spec = ImeActionSpec.from(EditorInfo.IME_ACTION_DONE)

        assertEquals("✓", spec.icon)
        assertEquals("Fertig", spec.contentDescription)
        assertEquals(EditorInfo.IME_ACTION_DONE, spec.editorAction)
        assertTrue(spec.hideKeyboardAfterAction)
    }

    @Test
    fun `action variants expose matching semantics`() {
        val cases = listOf(
            EditorInfo.IME_ACTION_GO to Triple("→", "Los", EditorInfo.IME_ACTION_GO),
            EditorInfo.IME_ACTION_SEARCH to Triple("🔍", "Suchen", EditorInfo.IME_ACTION_SEARCH),
            EditorInfo.IME_ACTION_SEND to Triple("➤", "Senden", EditorInfo.IME_ACTION_SEND),
            EditorInfo.IME_ACTION_NEXT to Triple("⇥", "Weiter", EditorInfo.IME_ACTION_NEXT),
            EditorInfo.IME_ACTION_PREVIOUS to Triple("⇤", "Zurück", EditorInfo.IME_ACTION_PREVIOUS),
        )

        cases.forEach { (options, expected) ->
            val spec = ImeActionSpec.from(options)
            assertEquals(expected.first, spec.icon)
            assertEquals(expected.second, spec.contentDescription)
            assertEquals(expected.third, spec.editorAction)
            assertFalse(spec.hideKeyboardAfterAction)
        }
    }

    @Test
    fun `no-enter-action flag forces newline fallback`() {
        val spec = ImeActionSpec.from(EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_ENTER_ACTION)

        assertEquals("↵", spec.icon)
        assertEquals("Neue Zeile", spec.contentDescription)
        assertNull(spec.editorAction)
    }

    @Test
    fun `none and unspecified use newline fallback`() {
        listOf(EditorInfo.IME_ACTION_NONE, EditorInfo.IME_ACTION_UNSPECIFIED).forEach { options ->
            val spec = ImeActionSpec.from(options)
            assertEquals("↵", spec.icon)
            assertEquals("Neue Zeile", spec.contentDescription)
            assertNull(spec.editorAction)
        }
    }
}
