package com.mapbox.navigation.core.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.os.Build
import android.util.Log
import com.mapbox.navigator.SensorData
import com.mapbox.navigator.SensorType
import java.util.Date

internal object SensorMapper {

    fun getSupportedSensorTypes(): Set<Int> {
        val supportedSensors = mutableSetOf(
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_PRESSURE
        )
        supportedSensors.add(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED)
        supportedSensors.add(Sensor.TYPE_GYROSCOPE_UNCALIBRATED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            supportedSensors.add(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED)
        }
        return supportedSensors
    }

    fun toSensorData(sensorEvent: SensorEvent): SensorData? {
        val sensorType = toSensorType(sensorEvent.sensor)
            ?: return null
        return SensorData(
            sensorType,
            Date(),
            sensorEvent.timestamp,
            sensorEvent.values.toList()
        )
    }

    fun toSensorType(sensor: Sensor): SensorType? {
        return when (sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> SensorType.ACCELEROMETER
            Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> SensorType.ACCELEROMETER
            Sensor.TYPE_MAGNETIC_FIELD -> SensorType.MAGNETOMETER
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> SensorType.MAGNETOMETER
            Sensor.TYPE_GYROSCOPE -> SensorType.GYROSCOPE
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> SensorType.GYROSCOPE
            Sensor.TYPE_GRAVITY -> SensorType.GRAVITY
            Sensor.TYPE_PRESSURE -> SensorType.PRESSURE
            else -> {
                Log.e(
                    "UnsupportedSensorEvent",
                    "This type of sensor event is not supported: ${sensor.name}"
                )
                null
            }
        }
    }
}
