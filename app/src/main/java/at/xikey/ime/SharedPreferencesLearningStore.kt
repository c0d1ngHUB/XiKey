package at.xikey.ime

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** Stores only aggregate local prediction statistics, never editor text. */
class SharedPreferencesLearningStore(private val preferences: SharedPreferences) : LearningStore {
    private companion object {
        const val KEY = "local_prediction_learning_v1"
        const val SCHEMA_VERSION = 1
    }

    override fun save(snapshot: LearningSnapshot) {
        val words = JSONArray().apply {
            snapshot.words.forEach { put(JSONObject().apply {
                put("language", it.language.tag); put("key", it.key); put("display", it.display)
                put("count", it.count); put("lastSeen", it.lastSeen)
            }) }
        }
        val transitions = JSONArray().apply {
            snapshot.transitions.forEach { put(JSONObject().apply {
                put("language", it.language.tag); put("from", it.from); put("to", it.to)
                put("count", it.count); put("lastSeen", it.lastSeen)
            }) }
        }
        preferences.edit().putString(KEY, JSONObject().apply {
            put("schemaVersion", SCHEMA_VERSION)
            put("sequence", snapshot.sequence); put("words", words); put("transitions", transitions)
        }.toString()).apply()
    }

    override fun load(): LearningSnapshot {
        val raw = preferences.getString(KEY, null) ?: return LearningSnapshot()
        return runCatching {
            val root = JSONObject(raw)
            if (root.optInt("schemaVersion", -1) != SCHEMA_VERSION) return LearningSnapshot()

            // Treat each entry as an independent trust boundary. A single damaged
            // item must not hide otherwise valid local learning data.
            val words = mutableListOf<LearnedWord>()
            root.optJSONArray("words")?.let { array ->
                for (i in 0 until array.length()) {
                    runCatching {
                        val item = array.getJSONObject(i)
                        words += LearnedWord(
                            PredictionLanguage.fromTag(item.getString("language")),
                            item.getString("key"),
                            item.getString("display"),
                            item.getInt("count"),
                            item.getLong("lastSeen"),
                        )
                    }
                }
            }

            val transitions = mutableListOf<LearnedTransition>()
            root.optJSONArray("transitions")?.let { array ->
                for (i in 0 until array.length()) {
                    runCatching {
                        val item = array.getJSONObject(i)
                        transitions += LearnedTransition(
                            PredictionLanguage.fromTag(item.getString("language")),
                            item.getString("from"),
                            item.getString("to"),
                            item.getInt("count"),
                            item.getLong("lastSeen"),
                        )
                    }
                }
            }
            LearningSnapshot(words, transitions, root.optLong("sequence", 0L))
        }.getOrDefault(LearningSnapshot())
    }

}
