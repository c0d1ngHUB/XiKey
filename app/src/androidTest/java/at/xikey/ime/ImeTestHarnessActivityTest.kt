package at.xikey.ime

import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImeTestHarnessActivityTest {
    @Test
    fun exposesEveryRequiredEditorConfiguration() {
        ActivityScenario.launch(ImeTestHarnessActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val expectations = mapOf(
                    R.id.field_done to EditorInfo.IME_ACTION_DONE,
                    R.id.field_search to EditorInfo.IME_ACTION_SEARCH,
                    R.id.field_send to EditorInfo.IME_ACTION_SEND,
                    R.id.field_go to EditorInfo.IME_ACTION_GO,
                    R.id.field_next to EditorInfo.IME_ACTION_NEXT,
                    R.id.field_previous to EditorInfo.IME_ACTION_PREVIOUS,
                )
                expectations.forEach { (id, action) ->
                    assertEquals(action, activity.findViewById<android.widget.EditText>(id).imeOptions and EditorInfo.IME_MASK_ACTION)
                }
                val multiline = activity.findViewById<android.widget.EditText>(R.id.field_multiline)
                assertTrue(multiline.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0)
                val password = activity.findViewById<android.widget.EditText>(R.id.field_password)
                assertEquals(InputType.TYPE_TEXT_VARIATION_PASSWORD, password.inputType and InputType.TYPE_MASK_VARIATION)
                val autoShift = activity.findViewById<android.widget.EditText>(R.id.field_auto_shift)
                assertTrue(autoShift.inputType and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES != 0)
                assertTrue(activity.findViewById<android.widget.EditText>(R.id.field_voralex).hint.toString().contains("Gu"))
                assertTrue(activity.findViewById<android.widget.EditText>(R.id.field_gestures).hint.toString().contains("Long-Press"))
            }
        }
    }

    @Test
    fun recordsEditorActionWithoutChangingText() {
        ActivityScenario.launch(ImeTestHarnessActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val field = activity.findViewById<android.widget.EditText>(R.id.field_done)
                field.setText("Test")
                field.onEditorAction(EditorInfo.IME_ACTION_DONE)
                assertEquals("ACTION:DONE:${EditorInfo.IME_ACTION_DONE}", activity.findViewById<android.widget.TextView>(R.id.action_status).text.toString())
                assertEquals("Test", field.text.toString())
            }
        }
    }
}