package at.xikey.ime

/** Explicit shift states distinguish automatic capitalization from user-selected modes. */
enum class ShiftState {
    OFF,
    AUTO,
    ONESHOT,
    CAPS_LOCK,
}

class KeyboardShiftController(
    private val capsLockToggleMillis: Long = CAPS_LOCK_TOGGLE_WINDOW_MILLIS,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) {
    var state: ShiftState = ShiftState.OFF
        private set

    val isShifted: Boolean get() = state != ShiftState.OFF
    val isCapsLocked: Boolean get() = state == ShiftState.CAPS_LOCK

    private var lastUserTapAtMillis: Long? = null

    fun toggle() {
        val now = nowProvider()
        if (state == ShiftState.CAPS_LOCK) {
            state = ShiftState.OFF
            lastUserTapAtMillis = null
            return
        }

        val previousTap = lastUserTapAtMillis
        if (previousTap != null && now - previousTap <= capsLockToggleMillis) {
            state = ShiftState.CAPS_LOCK
            lastUserTapAtMillis = null
            return
        }

        state = if (state == ShiftState.OFF) ShiftState.ONESHOT else ShiftState.OFF
        lastUserTapAtMillis = now
    }

    fun reset() {
        state = ShiftState.OFF
        lastUserTapAtMillis = null
    }

    fun autoEnableForContext(textBeforeCursor: String): Boolean {
        if (state != ShiftState.OFF) return false
        val trimmed = textBeforeCursor.trimEnd()
        val shouldEnable = trimmed.isEmpty() ||
            trimmed.endsWith(".") ||
            trimmed.endsWith("!") ||
            trimmed.endsWith("?")
        if (!shouldEnable) return false
        state = ShiftState.AUTO
        return true
    }

    fun applyTo(key: String): String {
        val output = if (isShifted && key == "ß") "ẞ" else if (isShifted) key.uppercase() else key
        if (!isCapsLocked) {
            state = ShiftState.OFF
            lastUserTapAtMillis = null
        }
        return output
    }

    companion object {
        const val CAPS_LOCK_TOGGLE_WINDOW_MILLIS = 300L
    }
}
