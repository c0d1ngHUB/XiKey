package at.xikey.ime

/** Pure timing state for responsive, progressively accelerated Backspace repetition. */
class BackspaceRepeatController {
    private var held = false
    private var pressedAtMillis = 0L
    private var nextDeletionAtMillis = 0L

    fun onPress(nowMillis: Long): Int {
        held = true
        pressedAtMillis = nowMillis
        nextDeletionAtMillis = nowMillis + INITIAL_REPEAT_DELAY_MILLIS
        return 1
    }

    fun onRelease() {
        held = false
    }

    fun deletionsDue(nowMillis: Long): Int {
        if (!held) return 0
        var count = 0
        val accelerationAt = pressedAtMillis + ACCELERATE_AFTER_MILLIS

        while (nowMillis >= nextDeletionAtMillis) {
            count++
            nextDeletionAtMillis = if (nextDeletionAtMillis < accelerationAt) {
                minOf(nextDeletionAtMillis + REPEAT_INTERVAL_MILLIS, accelerationAt)
            } else {
                nextDeletionAtMillis + FAST_REPEAT_INTERVAL_MILLIS
            }
        }
        return count
    }

    companion object {
        const val INITIAL_REPEAT_DELAY_MILLIS = 350L
        const val REPEAT_INTERVAL_MILLIS = 90L
        const val ACCELERATE_AFTER_MILLIS = 1_500L
        const val FAST_REPEAT_INTERVAL_MILLIS = 50L
        const val POLL_INTERVAL_MILLIS = 40L
    }
}
