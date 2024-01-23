package com.mapbox.navigation.core.sensor

import android.os.SystemClock
import androidx.annotation.IntDef
import com.mapbox.bindgen.Value
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigator.SensorType

/**
 * Data obtained from sensors
 */
@ExperimentalPreviewMapboxNavigationAPI
abstract class SensorData internal constructor() {

    /**
     * Weather condition obtained from sensors
     *
     * @param conditionType weather condition type
     */
    class Weather(@ConditionType.Type val conditionType: Int) : SensorData() {

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Weather

            return conditionType == other.conditionType
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            return conditionType.hashCode()
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String {
            return "Weather(condition=$conditionType)"
        }

        /**
         * Weather condition type.
         */
        object ConditionType {

            /**
             * Rain weather condition
             */
            const val RAIN = 0

            /**
             * Snow weather condition
             */
            const val SNOW = 1

            /**
             * Fog weather condition
             */
            const val FOG = 2

            /**
             * Wet road weather condition
             */
            const val WET_ROAD = 3

            /**
             * Retention policy for the [ConditionType]
             */
            @Retention(AnnotationRetention.BINARY)
            @IntDef(
                RAIN,
                SNOW,
                FOG,
                WET_ROAD
            )
            annotation class Type

            @JvmSynthetic
            @Type
            internal fun createConditionType(nativeObject: com.mapbox.navigator.Weather): Int {
                return when (nativeObject) {
                    com.mapbox.navigator.Weather.FOG -> FOG
                    com.mapbox.navigator.Weather.RAIN -> RAIN
                    com.mapbox.navigator.Weather.SNOW -> SNOW
                    com.mapbox.navigator.Weather.WET_ROAD -> WET_ROAD
                }
            }
        }
    }

    /**
     * Lane information obtained from sensors
     *
     * @param currentLaneIndex the index of the current lane. For right-hand traffic 1 is the very right lane and so on.
     * Should be zero if lane information is not available
     *
     * @param laneCount the number of lanes
     */
    class Lane(val currentLaneIndex: Int, val laneCount: Int) : SensorData() {

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Lane

            if (currentLaneIndex != other.currentLaneIndex) return false
            return laneCount == other.laneCount
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            var result = currentLaneIndex
            result = 31 * result + laneCount
            return result
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String {
            return "Lane(currentLaneIndex=$currentLaneIndex, laneCount=$laneCount)"
        }
    }

    @JvmSynthetic
    internal fun toNativeSensorData(): com.mapbox.navigator.SensorData {
        /**
         * NN requires [com.mapbox.navigator.SensorData.monotonicTimestampNanoseconds] to be based
         * on the same source as [com.mapbox.navigator.FixLocation.monotonicTimestampNanoseconds],
         * which is in turn based on [Location#getElapsedRealtimeNanos()](https://developer.android.com/reference/android/location/Location#getElapsedRealtimeNanos()),
         *
         * @see [com.mapbox.navigation.core.navigator.toFixLocation]
         */
        val elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()

        val (type, value) = when (this) {
            is Weather -> {
                SensorType.WEATHER to toValue()
            }
            is Lane -> {
                SensorType.LANE to toValue()
            }
            else -> error("Unsupported type: $javaClass")
        }
        return com.mapbox.navigator.SensorData(type, elapsedRealtimeNanos, value)
    }

    private fun Weather.toValue(): Value {
        val order: Long = when (conditionType) {
            Weather.ConditionType.RAIN -> 0
            Weather.ConditionType.SNOW -> 1
            Weather.ConditionType.FOG -> 2
            Weather.ConditionType.WET_ROAD -> 3
            else -> error("Unsupported weather condition type: $conditionType")
        }
        return Value.valueOf(order)
    }

    private fun Lane.toValue(): Value {
        val str = "{\"currentLaneIndex\": $currentLaneIndex, \"laneCount\": $laneCount}"
        return Value.valueOf(str)
    }
}
