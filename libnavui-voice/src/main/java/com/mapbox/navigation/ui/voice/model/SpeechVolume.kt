package com.mapbox.navigation.ui.voice.model

import androidx.annotation.FloatRange

private const val MINIMUM_VOLUME_LEVEL = 0.0
private const val MAXIMUM_VOLUME_LEVEL = 1.0

/**
 * The state is returned if we change the speech volume.
 * @param level volume level must be a value between [0.0..1.0]
 */
data class SpeechVolume(
    @FloatRange(from = MINIMUM_VOLUME_LEVEL, to = MAXIMUM_VOLUME_LEVEL)
    val level: Float
) {
    init {
        require(level in MINIMUM_VOLUME_LEVEL..MAXIMUM_VOLUME_LEVEL) {
            "Volume level must be between [$MINIMUM_VOLUME_LEVEL..$MAXIMUM_VOLUME_LEVEL]"
        }
    }
}
