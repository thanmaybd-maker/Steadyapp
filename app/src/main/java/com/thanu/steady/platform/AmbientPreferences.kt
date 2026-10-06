package com.thanu.steady.platform

import kotlinx.serialization.Serializable

@Serializable data class AmbientPreferences(val sound: String = "FOREST", val volume: Float = 0.25f,
    val modulation: Double = 10.0, val autoPlay: Boolean = false, val scene: String = "AUTO") {
    fun validate() {
        require(AmbientSoundType.entries.any { it.name == sound })
        require(volume.isFinite() && volume in 0f..1f && modulation.isFinite() && modulation in 1.0..40.0)
        require(scene in setOf("AUTO", "FOREST", "RAIN", "CAMPFIRE"))
    }
}
