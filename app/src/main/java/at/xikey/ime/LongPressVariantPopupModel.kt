package at.xikey.ime

/** Pure label transformation shared by the long-press popup and tests. */
object LongPressVariantPopupModel {
    fun labels(variants: List<String>, shifted: Boolean): List<String> =
        if (shifted) variants.map(String::uppercase) else variants
}
