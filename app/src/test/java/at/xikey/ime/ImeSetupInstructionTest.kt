package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class ImeSetupInstructionTest {
    @Test
    fun `instruction names the XiKey keyboard`() {
        assertEquals("XiKey in den Android-Tastatureinstellungen aktivieren", ImeSetupInstruction.enableText())
    }
}
