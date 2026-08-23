package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutTest {
    @Test
    fun `German layout exposes QWERTZ rows including umlauts`() {
        val layout = KeyboardLayout.forLanguage(PredictionLanguage.VORARLBERG_GERMAN)

        assertEquals(listOf("q", "w", "e", "r", "t", "z", "u", "i", "o", "p", "ü"), layout.rows.first())
        assertTrue(layout.rows.flatten().containsAll(listOf("ä", "ö", "ß")))
        assertEquals("ß", layout.rows.last().last())
    }

    @Test
    fun `English layout exposes QWERTY rows`() {
        val layout = KeyboardLayout.forLanguage(PredictionLanguage.ENGLISH)

        assertEquals(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"), layout.rows.first())
    }

    @Test
    fun `navigation spacer keeps the action row clear of Android navigation controls`() {
        assertEquals(44, KeyboardSurfaceMetrics.navigationSpacerDp)
    }

    @Test
    fun `keyboard keys reach the recommended minimum height`() {
        assertEquals(48, KeyboardSurfaceMetrics.keyHeightDp)
    }

    @Test
    fun `visual gaps do not shrink the clickable key cell`() {
        assertEquals(2, KeyboardSurfaceMetrics.keyVisualGapDp)
    }

    @Test
    fun `keyboard keys retain font padding to avoid cropped glyphs on the physical Redmi`() {
        assertTrue(KeyboardSurfaceMetrics.includeFontPadding)
    }

    @Test
    fun `keyboard key labels stay on one line to prevent action-label wrapping`() {
        assertTrue(KeyboardSurfaceMetrics.singleLineLabels)
    }
}
