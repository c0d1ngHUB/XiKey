package at.xikey.ime

/** One-shot shift state for alphabetic keyboard keys. */
class KeyboardShiftController {
    var isShifted: Boolean = false
        private set

    fun toggle() {
        isShifted = !isShifted
    }

    fun applyTo(key: String): String {
        val output = if (isShifted) key.uppercase() else key
        isShifted = false
        return output
    }
}
