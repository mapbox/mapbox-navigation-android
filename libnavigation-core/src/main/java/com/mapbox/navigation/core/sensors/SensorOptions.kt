package com.mapbox.navigation.core.sensors

/**
 * Options for the [SensorEventEmitter]. Use this to decide which sensors are
 * enabled and the frequency.
 *
 * @param enabledSensorTypes set of enabled sensors
 * @param signalsPerSecond signals per second received from sensors
 */
data class SensorOptions(
    val enabledSensorTypes: Set<Int>,
    val signalsPerSecond: Int
) {
    /**
     * Builder of [SensorOptions]
     */
    class Builder {
        private val enabledSensors: MutableSet<Int> = mutableSetOf()
        private var signalsPerSecond: Int = 25

        /**
         * Enable all available sensors
         *
         * @return Builder
         */
        fun enableAllSensors(): Builder {
            this.enabledSensors.addAll(SensorMapper.getSupportedSensorTypes())
            return this
        }

        /**
         * Set a set of sensors that will be handled
         *
         * @return Builder
         */
        fun enableSensors(sensorTypes: Set<Int>): Builder {
            this.enabledSensors.addAll(sensorTypes)
            return this
        }

        /**
         * Signals per second received from sensors
         *
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
                enabledSensors,
                signalsPerSecond
            )
        }
    }
}
