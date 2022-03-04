package com.mapbox.navigation.dropin.component.sound

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * @param isMuted user controlled value to mute or un-mute audio guidance
 */
@ExperimentalPreviewMapboxNavigationAPI
data class MapboxAudioState internal constructor(
    val isMuted: Boolean = false
)
