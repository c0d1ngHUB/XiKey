package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class BackspaceRepeatControllerTest {
    @Test
    fun `press deletes immediately then repeats every 300 milliseconds while held`() {
        val controller = BackspaceRepeatController()

        assertEquals(1, controller.onPress(0))
        assertEquals(0, controller.deletionsDue(299))
        assertEquals(1, controller.deletionsDue(300))
        assertEquals(2, controller.deletionsDue(900))
    }

    @Test
    fun `release stops pending backspace repeats`() {
        val controller = BackspaceRepeatController()

        controller.onPress(0)
        controller.onRelease()

        assertEquals(0, controller.deletionsDue(1_200))
    }
}
