package at.xikey.ime

object InputTypeClassifier {
    private const val TYPE_MASK_VARIATION = 0x00000ff0
    private const val TYPE_TEXT_VARIATION_PASSWORD = 0x00000080
    private const val TYPE_TEXT_VARIATION_VISIBLE_PASSWORD = 0x00000090
    private const val TYPE_TEXT_VARIATION_WEB_PASSWORD = 0x000000e0

    fun isSensitiveInputType(inputType: Int): Boolean {
        val variation = inputType and TYPE_MASK_VARIATION
        return variation == TYPE_TEXT_VARIATION_PASSWORD ||
            variation == TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == TYPE_TEXT_VARIATION_WEB_PASSWORD
    }

    /** Suggestions and local learning share the same sensitive-field gate. */
    fun allowsPrediction(inputType: Int): Boolean = !isSensitiveInputType(inputType)
}
