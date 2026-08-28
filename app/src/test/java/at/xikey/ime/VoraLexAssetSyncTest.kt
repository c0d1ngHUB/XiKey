package at.xikey.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.nio.file.Path

class VoraLexAssetSyncTest {
    private val assetPath: Path = Path.of("src", "main", "assets", "voralex_words.json")
    private val quotedString = Regex("\"((?:\\\\.|[^\"])*)\"")

    @Test
    fun `asset stays synchronized with the exported approved VoraLex surface list`() {
        val words = readAssetWords()

        assertEquals(3_867, words.size)
        assertEquals(words.size, words.toSet().size)
        assertFalse(words.any { it.isBlank() })
        assertEquals(listOf("a Bissa", "a bitz", "a bitzli"), words.take(3))
        assertEquals(listOf("öögla", "öömerig", "öörla"), words.takeLast(3))
    }

    private fun readAssetWords(): List<String> = quotedString.findAll(assetPath.toFile().readText())
        .map { match -> match.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\") }
        .toList()
}