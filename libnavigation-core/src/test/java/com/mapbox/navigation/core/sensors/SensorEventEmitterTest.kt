package com.mapbox.navigation.core.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class SensorEventEmitterTest {

    private val eventEmitter: (SensorEvent) -> Unit = mockk()
    private val sensorManager: SensorManager = mockk {
        every { getSensorList(Sensor.TYPE_ALL) } returns listOf(
            mockSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED),
            mockSensor(Sensor.TYPE_ACCELEROMETER),
            mockSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED),
            mockSensor(Sensor.TYPE_MAGNETIC_FIELD),
            mockSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED),
            mockSensor(Sensor.TYPE_GYROSCOPE),
            mockSensor(Sensor.TYPE_GRAVITY),
            mockSensor(Sensor.TYPE_PRESSURE)
        )
        every { registerListener(any<SensorEventEmitter>(), any(), any()) } returns false
        every { unregisterListener(any<SensorEventEmitter>()) } returns Unit
    }

    private val sensorEventEmitter = SensorEventEmitter(sensorManager)

    @Test
    fun `should convert signals per second to sampling period microseconds`() {
        val sensorOptions: SensorOptions = mockk {
            every { enableSensorTypes } returns setOf(
                Sensor.TYPE_ACCELEROMETER
            )
            every { signalsPerSecond } returns 25
        }

        sensorEventEmitter.start(sensorOptions, eventEmitter)

        verify { sensorManager.registerListener(sensorEventEmitter, any(), 40000) }
    }

    @Test
    fun `should register multiple sensors`() {
        val sensorOptions: SensorOptions = mockk {
            every { enableSensorTypes } returns setOf(
                Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
                Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
                Sensor.TYPE_GYROSCOPE_UNCALIBRATED
            )
            every { signalsPerSecond } returns 25
        }

        sensorEventEmitter.start(sensorOptions, eventEmitter)

        verify(exactly = 3) { sensorManager.registerListener(sensorEventEmitter, any(), any()) }
    }

    @Test
    fun `should only unregister once`() {
        val sensorOptions: SensorOptions = mockk {
            every { enableSensorTypes } returns setOf(
                Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
                Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
                Sensor.TYPE_GYROSCOPE_UNCALIBRATED
            )
            every { signalsPerSecond } returns 25
        }

        sensorEventEmitter.start(sensorOptions, eventEmitter)
        sensorEventEmitter.stop()

        verify(exactly = 1) { sensorManager.unregisterListener(sensorEventEmitter) }
    }

    @Test
    fun `should only register available sensors`() {
        every { sensorManager.getSensorList(any()) } returns listOf(
            mockSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED),
            mockSensor(Sensor.TYPE_ACCELEROMETER)
        )
        val sensorOptions: SensorOptions = mockk {
            every { enableSensorTypes } returns setOf(
                Sensor.TYPE_ACCELEROMETER,
                Sensor.TYPE_GYROSCOPE
            )
            every { signalsPerSecond } returns 25
        }

        sensorEventEmitter.start(sensorOptions, eventEmitter)

        verify(exactly = 1) { sensorManager.registerListener(sensorEventEmitter, any(), any()) }
    }

    @Test
    fun `should emit events`() {
        val sensorOptions: SensorOptions = mockk {
            every { enableSensorTypes } returns setOf(
                Sensor.TYPE_ACCELEROMETER
            )
            every { signalsPerSecond } returns 25
        }
        val sensorEvents = mutableListOf<SensorEvent>()
        val eventEmitter: (SensorEvent) -> Unit = { sensorEvents.add(it) }

        sensorEventEmitter.start(sensorOptions, eventEmitter)
        sensorEventEmitter.onSensorChanged(mockk())
        sensorEventEmitter.onSensorChanged(mockk())
        sensorEventEmitter.onSensorChanged(mockk())

        assertEquals(sensorEvents.size, 3)
    }

    @Test
    fun `should not emit events after unregister`() {
        val sensorOptions: SensorOptions = mockk {
            every { enableSensorTypes } returns setOf(
                Sensor.TYPE_ACCELEROMETER
            )
            every { signalsPerSecond } returns 25
        }
        val sensorEvents = mutableListOf<SensorEvent>()
        val eventEmitter: (SensorEvent) -> Unit = { sensorEvents.add(it) }

        sensorEventEmitter.start(sensorOptions, eventEmitter)
        sensorEventEmitter.onSensorChanged(mockk())
        sensorEventEmitter.onSensorChanged(mockk())
        sensorEventEmitter.stop()
        sensorEventEmitter.onSensorChanged(mockk())

        assertEquals(sensorEvents.size, 2)
    }

    private fun mockSensor(sensorType: Int): Sensor {
        return mockk {
            every { type } returns sensorType
        }
    }
}
