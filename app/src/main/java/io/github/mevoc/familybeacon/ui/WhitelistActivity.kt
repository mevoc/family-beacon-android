package io.github.mevoc.familybeacon.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.github.mevoc.familybeacon.R
import io.github.mevoc.familybeacon.data.EventLogger
import io.github.mevoc.familybeacon.util.FeaturePrefs

class WhitelistActivity : AppCompatActivity() {

    private lateinit var prefs: FeaturePrefs
    private lateinit var edit: EditText
    private lateinit var textList: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whitelist)

        prefs = FeaturePrefs(this)

        edit = findViewById(R.id.editNumber)
        textList = findViewById(R.id.textList)

        findViewById<Button>(R.id.btnAdd).setOnClickListener { addNumber() }
        findViewById<Button>(R.id.btnRemove).setOnClickListener { removeNumber() }
        findViewById<Button>(R.id.btnClear).setOnClickListener { clearAll() }

        refreshList()
    }

    private fun normalize(n: String): String =
        n.trim().replace(" ", "").replace("-", "")

    private fun currentSet(): MutableSet<String> =
        prefs.whitelistCsv.split(",")
            .map { normalize(it) }
            .filter { it.isNotEmpty() }
            .toMutableSet()

    private fun saveSet(set: Set<String>) {
        prefs.whitelistCsv = set.joinToString(",")
    }

    private fun addNumber() {
        val input = normalize(edit.text?.toString().orEmpty())
        if (input.isEmpty()) return

        val set = currentSet()
        val added = set.add(input)
        saveSet(set)
        refreshList()

        if (added) {
            EventLogger.info(this, "WHITELIST", "Added $input")
        } else {
            EventLogger.info(this, "WHITELIST", "Already present $input")
        }
        edit.setText("")
    }

    private fun removeNumber() {
        val input = normalize(edit.text?.toString().orEmpty())
        if (input.isEmpty()) return

        val set = currentSet()
        val removed = set.remove(input)
        saveSet(set)
        refreshList()

        if (removed) {
            EventLogger.info(this, "WHITELIST", "Removed $input")
        } else {
            EventLogger.warn(this, "WHITELIST", "Not found $input")
        }
        edit.setText("")
    }

    private fun clearAll() {
        saveSet(emptySet())
        refreshList()
        EventLogger.warn(this, "WHITELIST", "Cleared all")
    }

    private fun refreshList() {
        val set = currentSet().toList().sorted()
        textList.text = if (set.isEmpty()) {
            getString(R.string.whitelist_empty)
        } else {
            set.joinToString("\n")
        }
    }
}