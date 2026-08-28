package at.xikey.ime

/** Prevents one completion callback from learning the same accepted token twice. */
class CompletedTokenLearningGuard {
    private data class Completion(
        val language: PredictionLanguage,
        val previousWord: String,
        val completedWord: String,
        val sourceTextBeforeCursor: String,
    )

    private var lastCompletion: Completion? = null

    @Synchronized
    fun shouldLearn(
        language: PredictionLanguage,
        previousWord: String?,
        completedWord: String,
        sourceTextBeforeCursor: String,
    ): Boolean {
        val completion = Completion(
            language = language,
            previousWord = LocalPredictionModel.normalized(previousWord.orEmpty()),
            completedWord = LocalPredictionModel.normalized(completedWord),
            sourceTextBeforeCursor = sourceTextBeforeCursor,
        )
        if (completion.completedWord.isBlank()) return false
        if (completion == lastCompletion) return false
        lastCompletion = completion
        return true
    }

    @Synchronized
    fun reset() {
        lastCompletion = null
    }
}
