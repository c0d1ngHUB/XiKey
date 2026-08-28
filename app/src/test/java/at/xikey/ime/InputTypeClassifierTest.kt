package at.xikey.ime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputTypeClassifierTest {
    @Test fun `password variations are sensitive`() {
        assertTrue(InputTypeClassifier.isSensitiveInputType(0x00000080))
        assertTrue(InputTypeClassifier.isSensitiveInputType(0x00000090))
        assertTrue(InputTypeClassifier.isSensitiveInputType(0x000000e0))
    }

    @Test fun `regular text is not sensitive`() {
        assertFalse(InputTypeClassifier.isSensitiveInputType(0x00000001))
    }

    @Test fun `sensitive fields disable both prediction and local learning`() {
        assertFalse(InputTypeClassifier.allowsPrediction(0x00000001 or 0x00000080))
        assertTrue(InputTypeClassifier.allowsPrediction(0x00000001))
    }
}
