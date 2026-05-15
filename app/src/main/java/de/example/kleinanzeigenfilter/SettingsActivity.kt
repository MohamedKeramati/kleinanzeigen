package de.example.kleinanzeigenfilter

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.max

class SettingsActivity : AppCompatActivity() {
    private lateinit var blacklistManager: BlacklistManager
    private lateinit var delayRemovalManager: DelayRemovalManager

    private lateinit var listWords: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private var fullItems: List<String> = emptyList()
    private val uiHandler = Handler(Looper.getMainLooper())

    private val tickRunnable = object : Runnable {
        override fun run() {
            refreshList()
            uiHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        blacklistManager = BlacklistManager(this)
        delayRemovalManager = DelayRemovalManager(this)

        val etSearch = findViewById<EditText>(R.id.etSearch)
        val etNewWord = findViewById<EditText>(R.id.etNewWord)
        val etCustomSeconds = findViewById<EditText>(R.id.etCustomSeconds)
        val btnAdd = findViewById<Button>(R.id.btnAdd)
        val spinnerDelay = findViewById<Spinner>(R.id.spinnerDelay)
        listWords = findViewById(R.id.listWords)

        val options = listOf("20 Sekunden", "1 Minute", "10 Minuten", "1 Stunde", "6 Stunden", "24 Stunden", "Benutzerdefiniert")
        spinnerDelay.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listWords.adapter = adapter

        btnAdd.setOnClickListener {
            val word = etNewWord.text.toString().trim().lowercase()
            if (word.isNotBlank()) {
                blacklistManager.addWord(word)
                delayRemovalManager.cancelRemoval(word)
                etNewWord.setText("")
                refreshList()
            }
        }

        listWords.setOnItemLongClickListener { _, _, position, _ ->
            val selected = adapter.getItem(position) ?: return@setOnItemLongClickListener true
            val word = selected.substringBefore(" ")
            if (selected.contains("(Entfernung geplant")) {
                delayRemovalManager.cancelRemoval(word)
                Toast.makeText(this, "Entfernen abgebrochen: $word", Toast.LENGTH_SHORT).show()
            } else {
                delayRemovalManager.scheduleRemoval(word, resolveDelaySeconds(spinnerDelay.selectedItemPosition, etCustomSeconds.text.toString()))
                Toast.makeText(this, "Entfernung gestartet: $word", Toast.LENGTH_SHORT).show()
            }
            refreshList()
            true
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                applySearch(s?.toString().orEmpty())
            }
        })
    }

    override fun onResume() {
        super.onResume()
        refreshList()
        uiHandler.post(tickRunnable)
    }

    override fun onPause() {
        super.onPause()
        uiHandler.removeCallbacks(tickRunnable)
    }

    private fun resolveDelaySeconds(index: Int, custom: String): Long {
        return when (index) {
            0 -> 20L
            1 -> 60L
            2 -> 600L
            3 -> 3600L
            4 -> 21600L
            5 -> 86400L
            else -> max(1L, custom.toLongOrNull() ?: 20L)
        }
    }

    private fun refreshList() {
        delayRemovalManager.popExpiredWords().forEach { blacklistManager.removeWordFinal(it) }
        val active = blacklistManager.getActiveWords().sorted()
        val pending = delayRemovalManager.getPendingRemovals()
        val now = System.currentTimeMillis()
        fullItems = active.map { w ->
            val until = pending[w]
            if (until != null && until > now) "$w (Entfernung geplant: ${formatRemaining(until - now)})" else w
        }
        applySearch(findViewById<EditText>(R.id.etSearch).text.toString())
    }

    private fun applySearch(query: String) {
        val q = query.trim().lowercase()
        adapter.clear()
        adapter.addAll(if (q.isBlank()) fullItems else fullItems.filter { it.lowercase().contains(q) })
    }

    private fun formatRemaining(ms: Long): String {
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return String.format("%02dh %02dm %02ds", h, m, s)
    }
}
