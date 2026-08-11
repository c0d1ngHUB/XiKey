package at.xikey.ime

/** Keeps the active keyboard page independent from the Android view lifecycle. */
enum class KeyboardPage {
    ALPHABETIC,
    SYMBOLS,
}

class KeyboardPageController(
    initial: KeyboardPage = KeyboardPage.ALPHABETIC,
) {
    var current: KeyboardPage = initial
        private set

    fun toggle(): KeyboardPage {
        current = when (current) {
            KeyboardPage.ALPHABETIC -> KeyboardPage.SYMBOLS
            KeyboardPage.SYMBOLS -> KeyboardPage.ALPHABETIC
        }
        return current
    }
}
