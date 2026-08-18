package at.xikey.ime

/** One-shot shift and optional caps-lock state for alphabetic keyboard keys. */
class KeyboardShiftController(
    private val capsLockToggleMillis: Long = CAPS_LOCK_TOGGLE_WINDOW_MILLIS,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) {
    var isShifted: Boolean = false
        private set

    var isCapsLocked: Boolean = false
        private set

    private var lastToggleAtMillis: Long = 0L

    fun toggle() {
        val now = nowProvider()
        if (isShifted && !isCapsLocked && now - lastToggleAtMillis <= capsLockToggleMillis) {
            // Double-tap on an already-shifted state → caps-lock
            isCapsLocked = true
            isShifted = true
        } else if (isCapsLocked) {
            // Toggle off caps-lock
            isCapsLocked = false
            isShifted = false
        } else {
            isShifted = !isShifted
        }
        lastToggleAtMillis = now
    }

    fun reset() {
        isShifted = false
        isCapsLocked = false
    }

    fun applyTo(key: String): String {
        val output = if (isShifted && key == "ß") "ẞ" else if (isShifted) key.uppercase() else key
        if (!isCapsLocked) isShifted = false
        return output
    }

    companion object {
        const val CAPS_LOCK_TOGGLE_WINDOW_MILLIS = 300L
    }
}