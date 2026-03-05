package io.github.mevoc.familybeacon.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.github.mevoc.familybeacon.R
import io.github.mevoc.familybeacon.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventsActivity : AppCompatActivity() {

    private lateinit var tv: TextView
    private lateinit var btnClear: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)

        tv = findViewById(R.id.tvEvents)
        btnClear = findViewById(R.id.btnClearEvents)

        btnClear.setOnClickListener { clearEvents() }

        loadEvents()
    }

    private fun loadEvents() {
        val db = AppDatabase.get(this)
        CoroutineScope(Dispatchers.IO).launch {
            val events = db.logDao().latest(200)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            val text = if (events.isEmpty()) {
                getString(R.string.events_empty)
            } else {
                events.joinToString("\n") { e ->
                    "${sdf.format(Date(e.timestamp))}  [${e.level}]  ${e.type}: ${e.message}"
                }
            }

            withContext(Dispatchers.Main) {
                tv.text = text
            }
        }
    }

    private fun clearEvents() {
        val db = AppDatabase.get(this)
        CoroutineScope(Dispatchers.IO).launch {
            db.logDao().clearAll()
            withContext(Dispatchers.Main) {
                tv.text = getString(R.string.events_empty)
            }
        }
    }
}