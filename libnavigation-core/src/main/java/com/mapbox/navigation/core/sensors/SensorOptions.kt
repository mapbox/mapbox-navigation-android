package com.mapbox.navigation.core.sensors

/**
 * Options for the [SensorEventEmitter]. Use this to decide which sensors are
 * enabled and the frequency.
 *
 * @param enableSensorTypes set of enabled sensors
 * @param signalsPerSecond signals per second received from sensors
 */
class SensorOptions private constructor(
    val enableSensorTypes: Set<Int>,
    val signalsPerSecond: Int
) {
    /**
     * @return the builder that created the [SensorOptions]
     */
    fun toBuilder() = Builder().apply {
        enableSensorTypes(enableSensorTypes)
        signalsPerSecond(signalsPerSecond)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorOptions

        if (enableSensorTypes != other.enableSensorTypes) return false
        if (signalsPerSecond != other.signalsPerSecond) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = enableSensorTypes.hashCode()
        result = 31 * result + signalsPerSecond
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "SensorOptions(enableSensorTypes=$enableSensorTypes, signalsPerSecond=$signalsPerSecond)"
    }

    /**
     * Builder of [SensorOptions]
     */
    class Builder {
        private var enableSensorTypes: Set<Int> = emptySet()
        private var signalsPerSecond: Int = 25

        /**
         * Set a set of sensors that will be handled
         *
         * @param sensorTypes that will be handled
         * @return Builder
         * @see SensorMapper.getSupportedSensorTypes
         */
        fun enableSensorTypes(sensorTypes: Set<Int>): Builder {
            this.enableSensorTypes = sensorTypes
            return this
        }

        /**
         * Signals per second received from sensors
         *
         * @param signalsPerSecond received from sensors
         * @return Builder
         */
        fun signalsPerSecond(signalsPerSecond: Int): Builder {
            this.signalsPerSecond = signalsPerSecond
            return this
        }

        /**
         * Build a new instance of [SensorOptions]
         *
         * @return SensorOptions
         */
        fun build(): SensorOptions {
            return SensorOptions(
                enableSensorTypes = enableSensorTypes,
                signalsPerSecond = signalsPerSecond
            )
        }
    }
}
