package io.github.mevoc.familybeacon.util

data class Contact(
    val name: String,
    val number: String,
    val canRequestPosition: Boolean = true,
    val receiveBattery: Boolean = true,
    val canRequestPanic: Boolean = true,
    val receiveGeofence: Boolean = true
)
