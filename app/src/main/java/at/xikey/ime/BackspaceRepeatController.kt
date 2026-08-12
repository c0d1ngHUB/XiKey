package at.xikey.ime

/** Pure timing state for repeating Backspace while its key remains pressed. */
class BackspaceRepeatController(
    private val intervalMillis: Long = REPEAT_INTERVAL_MILLIS,
) {
    private var held = false
    private var lastDeletionAtMillis = 0L

    fun onPress(nowMillis: Long): Int {
        held = true
        lastDeletionAtMillis = nowMillis
        return 1
    }

    fun onRelease() {
        held = false
    }

    fun deletionsDue(nowMillis: Long): Int {
        if (!held) return 0
        val count = ((nowMillis - lastDeletionAtMillis) / intervalMillis).toInt().coerceAtLeast(0)
        if (count > 0) lastDeletionAtMillis += count * intervalMillis
        return count
    }

    companion object {
        const val REPEAT_INTERVAL_MILLIS = 300L
    }
}
