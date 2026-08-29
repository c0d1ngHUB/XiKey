package at.xikey.ime

import android.content.SharedPreferences

internal class SharedPreferencesLearningControls(
    private val preferences: SharedPreferences,
    private val store: SharedPreferencesLearningStore,
) {
    private companion object {
        const val KEY_ENABLED = "local_learning_enabled"
    }

    fun isEnabled(): Boolean = preferences.getBoolean(KEY_ENABLED, true)

    fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (!enabled) {
            store.clear()
        }
    }

    fun clearAll() {
        store.clear()
    }
}
