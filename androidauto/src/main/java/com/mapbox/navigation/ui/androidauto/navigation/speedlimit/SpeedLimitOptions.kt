package com.mapbox.navigation.ui.androidauto.navigation.speedlimit

import com.mapbox.navigation.base.speed.model.SpeedLimitSign

/**
 * Modify behavior of the speed limit widget.
 */
class SpeedLimitOptions private constructor(
    val forcedSignFormat: SpeedLimitSign?,
    val warningThreshold: Int,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        forcedSignFormat(forcedSignFormat)
        warningThreshold(warningThreshold)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SpeedLimitOptions

        if (forcedSignFormat != other.forcedSignFormat) return false
        if (warningThreshold != other.warningThreshold) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = forcedSignFormat.hashCode()
        result = 31 * result + warningThreshold
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "SpeedLimitOptions(" +
            "forcedSignFormat=$forcedSignFormat, " +
            "warningThreshold=$warningThreshold" +
            ")"
    }

    /**
     * Build a new [SpeedLimitOptions]
     */
    class Builder {

        private var forcedSignFormat: SpeedLimitSign? = null
        private var warningThreshold = 0

        /**
         * Sign format to use when drawing the speed limit.
         * If not specified, [SpeedLimit.speedLimitSign] will be used.
         */
        fun forcedSignFormat(forcedSignFormat: SpeedLimitSign?) = apply {
            this.forcedSignFormat = forcedSignFormat
        }

        /**
         * Maximum allowed difference between the actual speed and the speed limit
         * before the speed limit widget goes into the 'warning' state.
         */
        fun warningThreshold(warningThreshold: Int) = apply {
            this.warningThreshold = warningThreshold
        }

        /**
         * Build the [SpeedLimitOptions].
         */
        fun build(): SpeedLimitOptions {
            return SpeedLimitOptions(forcedSignFormat, warningThreshold)
        }
    }
}
