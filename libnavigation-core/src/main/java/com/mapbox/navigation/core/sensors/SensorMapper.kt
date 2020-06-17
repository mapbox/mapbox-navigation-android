package com.mapbox.navigation.core.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.os.Build
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
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

    fun toSensorData(sensorEvent: SensorEvent, logger: Logger): SensorData? {
        val sensorType = toSensorType(sensorEvent.sensor, logger)
            ?: return null
        return SensorData(
            sensorType,
            Date(),
            sensorEvent.timestamp,
            sensorEvent.values.toList()
        )
    }

    fun toSensorType(sensor: Sensor, logger: Logger): SensorType? {
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
                logger.e(
                    msg = Message("This type of sensor event is not supported: ${sensor.name}")
                )
                null
            }
        }
    }
}
