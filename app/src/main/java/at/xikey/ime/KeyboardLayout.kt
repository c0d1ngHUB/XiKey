package at.xikey.ime

/** Immutable keyboard rows for the deliberately supported XiKey languages and symbol pages. */
data class KeyboardLayout(val rows: List<List<String>>) {
    companion object {
        fun symbols(): KeyboardLayout = KeyboardLayout(
            rows = listOf(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                listOf("@", "#", "€", "%", "&", "-", "+", "(", ")", "/"),
                listOf("*", "\"", "'", ":", ";", "!", "?"),
            ),
        )

        fun secondarySymbols(): KeyboardLayout = KeyboardLayout(
            rows = listOf(
                listOf("~", "\\", "|", "•", "√", "π", "÷", "×", "§", "Δ"),
                listOf("£", "¥", "$", "¢", "^", "°", "=", "{", "}", "\\"),
                listOf("©", "®", "™", "✓", "[", "]", "<", ">", "_"),
            ),
        )

        fun forLanguage(language: PredictionLanguage): KeyboardLayout = when (language) {
            PredictionLanguage.VORARLBERG_GERMAN -> KeyboardLayout(
                rows = listOf(
                    listOf("q", "w", "e", "r", "t", "z", "u", "i", "o", "p", "ü"),
                    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "ö", "ä"),
                    listOf("y", "x", "c", "v", "b", "n", "m", "ß"),
                ),
            )
            PredictionLanguage.ENGLISH -> KeyboardLayout(
                rows = listOf(
                    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                    listOf("z", "x", "c", "v", "b", "n", "m"),
                ),
            )
        }
    }
}
