package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class BackspaceRepeatControllerTest {
    @Test
    fun `press deletes immediately and waits 350 milliseconds before repeating`() {
        val controller = BackspaceRepeatController()

        assertEquals(1, controller.onPress(0))
        assertEquals(0, controller.deletionsDue(349))
        assertEquals(1, controller.deletionsDue(350))
    }

    @Test
    fun `held backspace repeats every 90 milliseconds before acceleration`() {
        val controller = BackspaceRepeatController()
        controller.onPress(0)

        assertEquals(4, controller.deletionsDue(620)) // 350, 440, 530, 620
        assertEquals(1, controller.deletionsDue(710))
    }

    @Test
    fun `held backspace accelerates to 50 milliseconds after 1500 milliseconds`() {
        val controller = BackspaceRepeatController()
        controller.onPress(0)
        controller.deletionsDue(1_430)

        assertEquals(3, controller.deletionsDue(1_600)) // 1500, 1550, 1600
        assertEquals(2, controller.deletionsDue(1_700))
    }

    @Test
    fun `release stops pending backspace repeats`() {
        val controller = BackspaceRepeatController()
        controller.onPress(0)
        controller.onRelease()

        assertEquals(0, controller.deletionsDue(1_200))
    }
}
