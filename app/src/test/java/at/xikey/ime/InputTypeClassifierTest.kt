package at.xikey.ime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputTypeClassifierTest {
    @Test fun `text password variations are sensitive`() {
        assertTrue(InputTypeClassifier.isSensitiveInputType(0x00000081))
        assertTrue(InputTypeClassifier.isSensitiveInputType(0x00000091))
        assertTrue(InputTypeClassifier.isSensitiveInputType(0x000000e1))
    }

    @Test fun `numeric password variation is sensitive`() {
        assertTrue(InputTypeClassifier.isSensitiveInputType(0x00000012))
        assertFalse(InputTypeClassifier.isSensitiveInputType(0x00000002))
        assertFalse(InputTypeClassifier.isSensitiveInputType(0x00000011))
    }

    @Test fun `regular text is not sensitive`() {
        assertFalse(InputTypeClassifier.isSensitiveInputType(0x00000001))
        assertFalse(InputTypeClassifier.isSensitiveInputType(0x00000021))
    }

    @Test fun `sensitive fields disable both prediction and local learning`() {
        assertFalse(InputTypeClassifier.allowsPrediction(0x00000081))
        assertTrue(InputTypeClassifier.allowsPrediction(0x00000001))
    }
}
