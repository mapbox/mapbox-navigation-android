package com.mapbox.navigation.core.adas

import androidx.annotation.IntDef
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.sensor.SensorData

/**
 * Speed limit restriction
 *
 * @param weatherConditionTypes A list of [SensorData.Weather.ConditionType] types where the speed limit is applied. Empty means all
 * @param dateTimeCondition OSM "opening_hours" format, see https://wiki.openstreetmap.org/wiki/Key:opening_hours
 * @param vehicleTypes A list of [VehicleType.Type] types for that the speed limit is included. Empty means all
 * @param lanes Lane numbers where the speed limit is valid. Empty array means all lanes
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasSpeedLimitRestriction private constructor(
    val weatherConditionTypes: List<Int>,
    val dateTimeCondition: String,
    val vehicleTypes: List<Int>,
    val lanes: List<Byte>
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasSpeedLimitRestriction

        if (weatherConditionTypes != other.weatherConditionTypes) return false
        if (dateTimeCondition != other.dateTimeCondition) return false
        if (vehicleTypes != other.vehicleTypes) return false
        return lanes == other.lanes
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = weatherConditionTypes.hashCode()
        result = 31 * result + dateTimeCondition.hashCode()
        result = 31 * result + vehicleTypes.hashCode()
        result = 31 * result + lanes.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "SpeedLimitRestriction(" +
            "weatherConditionTypes=$weatherConditionTypes, " +
            "dateTimeCondition='$dateTimeCondition', " +
            "vehicleTypes=$vehicleTypes, " +
            "lanes=$lanes" +
            ")"
    }

    /**
     * Type of vehicle for which the speed limit is included.
     */
    object VehicleType {

        /**
         * Car vehicle type
         */
        const val CAR = 0

        /**
         * Truck vehicle type
         */
        const val TRUCK = 1

        /**
         * Bus vehicle type
         */
        const val BUS = 2

        /**
         * Trailer vehicle type
         */
        const val TRAILER = 3

        /**
         * Motorcycle vehicle type
         */
        const val MOTORCYCLE = 4

        /**
         * Retention policy for the [VehicleType]
         */
        @Retention(AnnotationRetention.BINARY)
        @IntDef(
            CAR,
            TRUCK,
            BUS,
            TRAILER,
            MOTORCYCLE
        )
        annotation class Type

        @JvmSynthetic
        @Type
        internal fun createVehicleType(nativeObj: com.mapbox.navigator.VehicleType): Int {
            return when (nativeObj) {
                com.mapbox.navigator.VehicleType.CAR -> CAR
                com.mapbox.navigator.VehicleType.TRUCK -> TRUCK
                com.mapbox.navigator.VehicleType.BUS -> BUS
                com.mapbox.navigator.VehicleType.TRAILER -> TRAILER
                com.mapbox.navigator.VehicleType.MOTORCYCLE -> MOTORCYCLE
            }
        }
    }

    internal companion object {

        @JvmSynthetic
        fun createFromNativeObject(nativeObj: com.mapbox.navigator.SpeedLimitRestriction) =
            AdasSpeedLimitRestriction(
                weatherConditionTypes = nativeObj.weather.map {
                    SensorData.Weather.ConditionType.createConditionType(it)
                },
                dateTimeCondition = nativeObj.dateTimeCondition,
                vehicleTypes = nativeObj.vehicleTypes.map { VehicleType.createVehicleType(it) },
                lanes = nativeObj.lanes,
            )
    }
}
