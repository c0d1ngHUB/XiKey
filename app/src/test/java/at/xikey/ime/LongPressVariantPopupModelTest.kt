package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class LongPressVariantPopupModelTest {
    @Test
    fun `popup keeps lowercase variants when shift is off`() {
        assertEquals(
            listOf("ä", "à", "á"),
            LongPressVariantPopupModel.labels(listOf("ä", "à", "á"), shifted = false),
        )
    }

    @Test
    fun `popup uppercases every variant while shift is active`() {
        assertEquals(
            listOf("Ä", "À", "Á"),
            LongPressVariantPopupModel.labels(listOf("ä", "à", "á"), shifted = true),
        )
    }
}
