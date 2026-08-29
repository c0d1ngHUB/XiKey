package at.xikey.ime

import android.content.SharedPreferences

internal object LearningPreferenceKeys {
    const val ENABLED = "local_learning_enabled"
    const val RESET_GENERATION = "local_learning_reset_generation"
}

internal class SharedPreferencesLearningControls(
    private val preferences: SharedPreferences,
    private val store: SharedPreferencesLearningStore,
) {
    fun isEnabled(): Boolean = preferences.getBoolean(LearningPreferenceKeys.ENABLED, true)

    fun resetGeneration(): Long = preferences.getLong(LearningPreferenceKeys.RESET_GENERATION, 0L)

    fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(LearningPreferenceKeys.ENABLED, enabled).apply()
    }

    fun clearAll() {
        preferences.edit()
            .putLong(LearningPreferenceKeys.RESET_GENERATION, resetGeneration() + 1L)
            .apply()
        store.clear()
    }
}
