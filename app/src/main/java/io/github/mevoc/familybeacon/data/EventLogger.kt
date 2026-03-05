package io.github.mevoc.familybeacon.data

import android.content.Context
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
        val db = AppDatabase.get(context)
        CoroutineScope(Dispatchers.IO).launch {
            db.logDao().insert(
                LogEntry(
                    timestamp = System.currentTimeMillis(),
                    level = level,
                    type = type,
                    message = message
                )
            )
        }
    }
}