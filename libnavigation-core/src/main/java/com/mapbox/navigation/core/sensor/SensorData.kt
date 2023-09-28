package com.mapbox.navigation.core.sensor

import android.os.SystemClock
import com.mapbox.bindgen.Value
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigator.SensorType

/**
 * Data obtained from sensors
 */
@ExperimentalPreviewMapboxNavigationAPI
sealed class SensorData {

    /**
     * Weather condition obtained from sensors
     *
     * @param condition weather condition type
     */
    class Weather(val condition: Condition) : SensorData() {

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Weather

            if (condition != other.condition) return false

            return true
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            return condition.hashCode()
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String {
            return "Weather(condition=$condition)"
        }

        /**
         * Weather condition type.
         */
        sealed class Condition {

            /**
             * Rain weather condition
             */
            object Rain : Condition()

            /**
             * Snow weather condition
             */
            object Snow : Condition()

            /**
             * Fog weather condition
             */
            object Fog : Condition()
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
            if (laneCount != other.laneCount) return false

            return true
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
        }
        return com.mapbox.navigator.SensorData(type, elapsedRealtimeNanos, value)
    }

    private fun Weather.toValue(): Value {
        val order: Long = when (condition) {
            is Weather.Condition.Rain -> 0
            is Weather.Condition.Snow -> 1
            is Weather.Condition.Fog -> 2
        }
        return Value.valueOf(order)
    }

    private fun Lane.toValue(): Value {
        val str = "{\"currentLaneIndex\": $currentLaneIndex, \"laneCount\": $laneCount}"
        return Value.valueOf(str)
    }
}
