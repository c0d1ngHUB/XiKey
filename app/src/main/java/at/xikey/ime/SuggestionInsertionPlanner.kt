package at.xikey.ime

data class SuggestionInsertPlan(
    val deleteCount: Int,
    val textToCommit: String,
)

object SuggestionInsertionPlanner {
    fun plan(context: CursorContext, suggestion: String): SuggestionInsertPlan {
        val deleteCount = context.composingWord.length
        return SuggestionInsertPlan(deleteCount, "$suggestion ")
    }
}
