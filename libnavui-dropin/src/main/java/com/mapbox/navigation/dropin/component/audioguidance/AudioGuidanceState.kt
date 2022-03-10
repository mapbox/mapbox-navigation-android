package com.mapbox.navigation.dropin.component.audioguidance

/**
 * @param isMuted user controlled value to mute or un-mute audio guidance
 */
data class AudioGuidanceState internal constructor(
    val isMuted: Boolean = false
)
