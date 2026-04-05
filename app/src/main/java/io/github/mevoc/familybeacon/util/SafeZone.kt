package io.github.mevoc.familybeacon.util

import java.util.UUID

data class SafeZone(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Float = 200f,
    val alertOnEnter: Boolean = true,
    val alertOnExit: Boolean = true
)
