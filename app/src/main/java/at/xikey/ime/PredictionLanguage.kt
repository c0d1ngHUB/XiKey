package at.xikey.ime

/** The only prediction languages intentionally supported by XiKey. */
enum class PredictionLanguage(val tag: String) {
    VORARLBERG_GERMAN("de-AT-vorarlberg"),
    ENGLISH("en");

    companion object {
        fun fromTag(tag: String): PredictionLanguage = entries.firstOrNull {
            it.tag.equals(tag, ignoreCase = true)
        } ?: throw IllegalArgumentException("Unsupported prediction language: $tag")
    }
}
