package com.mapbox.navigation.ui.voice.model

import androidx.annotation.FloatRange

/**
 * The state is returned if we change the speech volume.
 * @param level
 */
data class SpeechVolume(
    @FloatRange(from = 0.0, to = 1.0)
    val level: Float
)
