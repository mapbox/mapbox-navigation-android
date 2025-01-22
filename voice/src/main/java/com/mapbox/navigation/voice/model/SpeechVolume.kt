package com.mapbox.navigation.voice.model

import androidx.annotation.FloatRange
import com.mapbox.navigation.base.internal.utils.safeCompareTo

private const val MINIMUM_VOLUME_LEVEL = 0.0
private const val MAXIMUM_VOLUME_LEVEL = 1.0

/**
 * The state is returned if we change the speech volume.
 * @param level volume level must be a value between [0.0..1.0]
 */
class SpeechVolume(
    @FloatRange(from = MINIMUM_VOLUME_LEVEL, to = MAXIMUM_VOLUME_LEVEL)
    val level: Float,
) {
    init {
        require(level in MINIMUM_VOLUME_LEVEL..MAXIMUM_VOLUME_LEVEL) {
            "Volume level must be between [$MINIMUM_VOLUME_LEVEL..$MAXIMUM_VOLUME_LEVEL]"
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SpeechVolume

        return level.safeCompareTo(other.level)
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return level.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "SpeechVolume(level=$level)"
    }
}
