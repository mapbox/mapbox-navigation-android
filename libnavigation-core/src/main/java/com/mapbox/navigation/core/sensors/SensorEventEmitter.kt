package com.mapbox.navigation.core.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * This class makes it simple to register listeners to the [SensorManager]. Use [start] to
 * register and pass the [SensorEvent]s to [MapboxNavigation.updateSensorEvent].
 *
 * @param sensorManager [SensorManager]
 */
class SensorEventEmitter(
    private val sensorManager: SensorManager
) : SensorEventListener {

    private var eventEmitter: (SensorEvent) -> Unit = { }

    /**
     * Register to the SensorManager and emit the SensorEvents.
     *
     * @param sensorOptions determines which sensors and at what rate
     * @param eventEmitter callback for every event emitted
     */
    fun start(sensorOptions: SensorOptions, eventEmitter: (SensorEvent) -> Unit) {
        this.eventEmitter = eventEmitter

        val enabledSensors = enabledSensors(sensorOptions)
        val samplingPeriodUs = toSamplingPeriodUs(sensorOptions.signalsPerSecond)
        enabledSensors.forEach { sensorType ->
            sensorManager.registerListener(
                this,
                sensorType,
                samplingPeriodUs
            )
        }
    }

    /**
     * Unregister from the SensorManager and stop emitting SensorEvents
     */
    fun stop() {
        eventEmitter = { }
        sensorManager.unregisterListener(this)
    }

    /**
     * Called when there is a new sensor event.  Note that "on changed"
     * is somewhat of a misnomer, as this will also be called if we have a
     * new reading from a sensor with the exact same sensor values (but a
     * newer timestamp).
     */
    override fun onSensorChanged(event: SensorEvent) {
        eventEmitter(event)
    }

    /**
     * Called when the accuracy of the registered sensor has changed.  Unlike
     * onSensorChanged(), this is only called when this accuracy value changes.
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Haven't found a need for this
    }

    private fun enabledSensors(sensorOptions: SensorOptions): List<Sensor> {
        return sensorManager.getSensorList(Sensor.TYPE_ALL)
            .filter { sensor ->
                sensorOptions.enableSensorTypes.contains(sensor.type)
            }
            .filterNotNull()
    }

    /**
     * Helper function to turn signalsPerSecond into what Android expects, samplingPeriodUs
     *
     * Example: 25 [signalsPerSecond] has a 40000 samplingPeriodUs.
     */
    private fun toSamplingPeriodUs(signalsPerSecond: Int): Int {
        return 1000000 / signalsPerSecond
    }
}
