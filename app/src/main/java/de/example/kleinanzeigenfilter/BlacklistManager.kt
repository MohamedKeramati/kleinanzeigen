package de.example.kleinanzeigenfilter

import android.content.Context
import org.json.JSONArray

class BlacklistManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("blacklist_prefs", Context.MODE_PRIVATE)

    fun loadInitialFromAssetsIfNeeded() {
        if (prefs.contains(KEY_ACTIVE_WORDS)) return
        val words = try {
            context.assets.open("blocked_words.txt").bufferedReader().useLines { lines ->
                lines.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
            }
        } catch (_: Exception) {
            emptySet()
        }
        saveActiveWords(words)
    }

    fun getActiveWords(): MutableSet<String> {
        val raw = prefs.getString(KEY_ACTIVE_WORDS, "[]") ?: "[]"
        return parseSet(raw).toMutableSet()
    }

    fun addWord(word: String) {
        val normalized = word.trim().lowercase()
        if (normalized.isBlank()) return
        val words = getActiveWords()
        words.add(normalized)
        saveActiveWords(words)
    }

    fun removeWordFinal(word: String) {
        val words = getActiveWords()
        words.remove(word.lowercase())
        saveActiveWords(words)
    }

    private fun saveActiveWords(words: Set<String>) {
        prefs.edit().putString(KEY_ACTIVE_WORDS, JSONArray(words.toList()).toString()).apply()
    }

    private fun parseSet(json: String): Set<String> {
        return try {
            val arr = JSONArray(json)
            buildSet {
                for (i in 0 until arr.length()) {
                    add(arr.optString(i).trim().lowercase())
                }
            }.filter { it.isNotBlank() }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    companion object {
        private const val KEY_ACTIVE_WORDS = "active_words"
    }
}
