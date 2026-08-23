package at.xikey.ime

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class XiKeyEditorActionUiTest {
    @Test
    fun runnerSelectsXiKeyAndFocusesDoneField() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val xiKeyIme = "at.xikey.ime/.XiKeyInputMethodService"
        device.executeShellCommand("ime enable $xiKeyIme")
        device.executeShellCommand("ime set $xiKeyIme")
        assertEquals(
            xiKeyIme,
            device.executeShellCommand("settings get secure default_input_method").trim(),
        )
        ActivityScenario.launch(ImeTestHarnessActivity::class.java).use { scenario ->
            onView(withId(R.id.field_done)).perform(click())
            scenario.onActivity { activity ->
                assertTrue(activity.findViewById<android.widget.EditText>(R.id.field_done).hasFocus())
            }
        }
    }
}
