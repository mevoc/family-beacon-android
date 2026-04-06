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
        if (Looper.myLooper() != Looper.getMainLooper()) {
            // Already on a background thread — write synchronously via non-suspend DAO method
            try { db.logDao().insertSync(entry) } catch (_: Exception) {}
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                try { db.logDao().insert(entry) } catch (_: Exception) {}
            }
        }
    }
}