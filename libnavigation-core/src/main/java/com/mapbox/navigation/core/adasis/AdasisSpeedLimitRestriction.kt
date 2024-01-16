package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.sensor.SensorData

/**
 * Speed limit restriction
 *
 * @param weather Weather conditions where the speed limit is applied. Empty means all
 * @param dateTimeCondition OSM "opening_hours" format, see https://wiki.openstreetmap.org/wiki/Key:opening_hours
 * @param vehicleTypes A list of types of vehicles for that the speed limit is included. Empty means all
 * @param lanes Lane numbers where the speed limit is valid. Empty array means all lanes
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisSpeedLimitRestriction private constructor(
    val weather: List<SensorData.Weather.Condition>,
    val dateTimeCondition: String,
    val vehicleTypes: List<VehicleType>,
    val lanes: List<Byte>
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasisSpeedLimitRestriction

        if (weather != other.weather) return false
        if (dateTimeCondition != other.dateTimeCondition) return false
        if (vehicleTypes != other.vehicleTypes) return false
        return lanes == other.lanes
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = weather.hashCode()
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
            "weather=$weather, " +
            "dateTimeCondition='$dateTimeCondition', " +
            "vehicleTypes=$vehicleTypes, " +
            "lanes=$lanes" +
            ")"
    }

    /**
     * Type of vehicle for which the speed limit is included.
     */
    abstract class VehicleType internal constructor() {

        /**
         * Car vehicle type
         */
        object Car : VehicleType()

        /**
         * Truck vehicle type
         */
        object Truck : VehicleType()

        /**
         * Bus vehicle type
         */
        object Bus : VehicleType()

        /**
         * Trailer vehicle type
         */
        object Trailer : VehicleType()

        /**
         * Motorcycle vehicle type
         */
        object Motorcycle : VehicleType()

        internal companion object {

            @JvmSynthetic
            fun createFromNativeObject(nativeObj: com.mapbox.navigator.VehicleType): VehicleType {
                return when (nativeObj) {
                    com.mapbox.navigator.VehicleType.CAR -> Car
                    com.mapbox.navigator.VehicleType.TRUCK -> Truck
                    com.mapbox.navigator.VehicleType.BUS -> Bus
                    com.mapbox.navigator.VehicleType.TRAILER -> Trailer
                    com.mapbox.navigator.VehicleType.MOTORCYCLE -> Motorcycle
                }
            }
        }
    }

    internal companion object {

        @JvmSynthetic
        fun createFromNativeObject(nativeObj: com.mapbox.navigator.SpeedLimitRestriction) =
            AdasisSpeedLimitRestriction(
                weather = nativeObj.weather.map {
                    SensorData.Weather.Condition.createFromNativeObject(it)
                },
                dateTimeCondition = nativeObj.dateTimeCondition,
                vehicleTypes = nativeObj.vehicleTypes.map {
                    VehicleType.createFromNativeObject(it)
                },
                lanes = nativeObj.lanes,
            )
    }
}
