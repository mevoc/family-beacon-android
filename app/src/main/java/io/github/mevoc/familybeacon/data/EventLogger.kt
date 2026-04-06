package io.github.mevoc.familybeacon.data

import android.content.Context
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object EventLogger {

    fun info(context: Context, type: String, message: String) =
        log(context, "INFO", type, message)

    fun warn(context: Context, type: String, message: String) =
        log(context, "WARN", type, message)

    fun error(context: Context, type: String, message: String) =
        log(context, "ERROR", type, message)

    private fun log(context: Context, level: String, type: String, message: String) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            type = type,
            message = message
        )
        val db = AppDatabase.get(context)
        // If already on a background thread (e.g. goAsync coroutine), write synchronously
        if (Looper.myLooper() != Looper.getMainLooper()) {
            try {
                db.logDao().insert(entry)
            } catch (_: Exception) {}
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                try { db.logDao().insert(entry) } catch (_: Exception) {}
            }
        }
    }
}