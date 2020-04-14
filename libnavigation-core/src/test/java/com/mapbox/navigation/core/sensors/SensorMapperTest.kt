package com.mapbox.navigation.core.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import com.mapbox.base.common.logger.Logger
import com.mapbox.navigator.SensorType
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorMapperTest {

    @Test
    fun `should have supported SensorTypesr`() {
        val supportedSensors = SensorMapper.getSupportedSensorTypes()
        assertTrue(supportedSensors.isNotEmpty())
    }

    @Test
    fun `should map SensorEvent to NavigatorSensorData`() {
        val sensor: Sensor = mockk(relaxed = true)
        every { sensor.type } returns Sensor.TYPE_ACCELEROMETER
        val sensorEvent = mockSensorEvent(
            sensor,
            23834291518140L,
            floatArrayOf(1.151f, 0.164f, 9.700f)
        )
        val logger: Logger = mockk()

        val navigationSensorData = SensorMapper.toSensorData(sensorEvent, logger)
        assertNotNull(navigationSensorData!!)
        assertEquals(navigationSensorData.sensorType, SensorType.ACCELEROMETER)
        assertEquals(navigationSensorData.elapsedTimeNanos, 23834291518140L)
        assertEquals(navigationSensorData.values[0], 1.151f)
        assertEquals(navigationSensorData.values[1], 0.164f)
        assertEquals(navigationSensorData.values[2], 9.700f)
    }

    @Test
    fun `should map uncalibrated magnetometer to NavigatorSensorData`() {
        val sensor: Sensor = mockk(relaxed = true)
        every { sensor.type } returns Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED
        val sensorEvent = mockSensorEvent(
            sensor,
            23834307804681,
            floatArrayOf(21.968f, -54.005f, -160.413f, 37.604f, -58.706f, -113.839f)
        )
        val logger: Logger = mockk()

        val navigationSensorData = SensorMapper.toSensorData(sensorEvent, logger)
        assertNotNull(navigationSensorData!!)
        assertEquals(navigationSensorData.sensorType, SensorType.MAGNETOMETER)
        assertEquals(navigationSensorData.elapsedTimeNanos, 23834307804681)
        assertEquals(navigationSensorData.values[0], 21.968f)
        assertEquals(navigationSensorData.values[1], -54.005f)
        assertEquals(navigationSensorData.values[2], -160.413f)
        assertEquals(navigationSensorData.values[3], 37.604f)
        assertEquals(navigationSensorData.values[4], -58.706f)
        assertEquals(navigationSensorData.values[5], -113.839f)
    }

    @Test
    fun `should map pressure to NavigatorSensorData`() {
        val sensor: Sensor = mockk(relaxed = true)
        every { sensor.type } returns Sensor.TYPE_PRESSURE
        val sensorEvent = mockSensorEvent(
            sensor,
            23834956241370,
            floatArrayOf(976.028f)
        )
        val logger: Logger = mockk()

        val navigationSensorData = SensorMapper.toSensorData(sensorEvent, logger)
        assertNotNull(navigationSensorData!!)
        assertEquals(navigationSensorData.sensorType, SensorType.PRESSURE)
        assertEquals(navigationSensorData.elapsedTimeNanos, 23834956241370)
        assertEquals(navigationSensorData.values[0], 976.028f)
    }
}

private fun mockSensorEvent(sensor: Sensor, timestamp: Long, values: FloatArray): SensorEvent {
    val sensorEvent: SensorEvent = mockk(relaxed = true)
    try {
        SensorEvent::class.java.getField("sensor")
            .apply { set(sensorEvent, sensor) }
        SensorEvent::class.java.getField("timestamp")
            .apply { set(sensorEvent, timestamp) }
        SensorEvent::class.java.getField("values")
            .apply { set(sensorEvent, values) }
    } catch (t: Throwable) {
        println("Failed to mock sensor")
    }
    return sensorEvent
}
