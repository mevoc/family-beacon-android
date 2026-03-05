package io.github.mevoc.familybeacon.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class LocationFgService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf()
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
}