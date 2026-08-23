package at.xikey.ime

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

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

    @Test
    fun reusedBackspaceButtonCommitsSharpSAfterLanguageSwitch() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val xiKeyIme = "at.xikey.ime/.XiKeyInputMethodService"
        device.executeShellCommand("ime enable $xiKeyIme")
        device.executeShellCommand("ime set $xiKeyIme")

        ActivityScenario.launch(ImeTestHarnessActivity::class.java).use { scenario ->
            onView(withId(R.id.field_done)).perform(click(), replaceText("X"))
            scenario.onActivity { activity ->
                activity.findViewById<android.widget.EditText>(R.id.field_done).setSelection(1)
            }
            assertTrue(device.wait(Until.hasObject(By.descContains("Aktuelle Sprache:")), 5_000))

            val languageButton = device.findObject(By.descContains("Aktuelle Sprache:"))
            if (languageButton.contentDescription.toString().contains("Vorarlberger Deutsch")) {
                languageButton.click()
                assertTrue(device.wait(Until.hasObject(By.descContains("Aktuelle Sprache: Englisch")), 5_000))
            }
            device.findObject(By.descContains("Aktuelle Sprache: Englisch")).click()
            val sharpSSelector = By.desc(Pattern.compile("Taste [ßẞ]"))
            assertTrue(device.wait(Until.hasObject(sharpSSelector), 5_000))
            val sharpSKey = device.findObject(sharpSSelector)
            val expected = "X${sharpSKey.text}"
            sharpSKey.click()

            scenario.onActivity { activity ->
                assertEquals(expected, activity.findViewById<android.widget.EditText>(R.id.field_done).text.toString())
            }
        }
    }
}
