package io.github.mevoc.familybeacon.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.github.mevoc.familybeacon.data.EventLogger

class PanicService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO: build notification channel + startForeground
        EventLogger.warn(this, "PANIC", "PanicService started (stub)")
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        // TODO later
        throw NotImplementedError("Stub")
    }
}