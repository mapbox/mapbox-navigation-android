package com.mapbox.navigation.core.sensors

/**
 * Options for the [SensorEventEmitter]. Use this to decide which sensors are
 * enabled and the frequency.
 */
data class SensorOptions(
    val enabledSensorTypes: Set<Int>,
    val signalsPerSecond: Int
) {
    class Builder {
        private val enabledSensors: MutableSet<Int> = mutableSetOf()
        private var signalsPerSecond: Int = 25

        fun enableAllSensors(): Builder {
            this.enabledSensors.addAll(SensorMapper.getSupportedSensorTypes())
            return this
        }

        fun enableSensors(sensorTypes: Set<Int>): Builder {
            this.enabledSensors.addAll(sensorTypes)
            return this
        }

        fun signalsPerSecond(signalsPerSecond: Int): Builder {
            this.signalsPerSecond = signalsPerSecond
            return this
        }

        fun build(): SensorOptions {
            return SensorOptions(
                enabledSensors,
                signalsPerSecond
            )
        }
    }
}
