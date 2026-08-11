package at.xikey.ime

/** Keeps the active keyboard page independent from the Android view lifecycle. */
enum class KeyboardPage {
    ALPHABETIC,
    SYMBOLS,
    SYMBOLS_SECONDARY,
}

class KeyboardPageController(
    initial: KeyboardPage = KeyboardPage.ALPHABETIC,
) {
    var current: KeyboardPage = initial
        private set

    fun toggle(): KeyboardPage {
        current = when (current) {
            KeyboardPage.ALPHABETIC -> KeyboardPage.SYMBOLS
            KeyboardPage.SYMBOLS, KeyboardPage.SYMBOLS_SECONDARY -> KeyboardPage.ALPHABETIC
        }
        return current
    }

    fun showPrimarySymbols(): KeyboardPage {
        current = KeyboardPage.SYMBOLS
        return current
    }

    fun showSecondarySymbols(): KeyboardPage {
        current = KeyboardPage.SYMBOLS_SECONDARY
        return current
    }
}
