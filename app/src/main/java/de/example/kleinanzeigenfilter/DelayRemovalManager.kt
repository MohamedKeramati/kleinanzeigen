package de.example.kleinanzeigenfilter

import android.content.Context
import org.json.JSONObject

class DelayRemovalManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("blacklist_prefs", Context.MODE_PRIVATE)

    fun scheduleRemoval(word: String, delaySeconds: Long) {
        val now = System.currentTimeMillis()
        val map = getPendingRemovals().toMutableMap()
        map[word.lowercase()] = now + (delaySeconds * 1000)
        savePending(map)
    }

    fun cancelRemoval(word: String) {
        val map = getPendingRemovals().toMutableMap()
        map.remove(word.lowercase())
        savePending(map)
    }

    fun getPendingRemovals(): Map<String, Long> {
        val raw = prefs.getString(KEY_PENDING, "{}") ?: "{}"
        return try {
            val obj = JSONObject(raw)
            obj.keys().asSequence().associateWith { obj.optLong(it, 0L) }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun popExpiredWords(now: Long = System.currentTimeMillis()): List<String> {
        val map = getPendingRemovals().toMutableMap()
        val expired = map.filterValues { it <= now }.keys.toList()
        expired.forEach { map.remove(it) }
        savePending(map)
        return expired
    }

    private fun savePending(map: Map<String, Long>) {
        val obj = JSONObject()
        map.forEach { (word, ts) -> obj.put(word, ts) }
        prefs.edit().putString(KEY_PENDING, obj.toString()).apply()
    }

    companion object {
        private const val KEY_PENDING = "pending_removals"
    }
}
