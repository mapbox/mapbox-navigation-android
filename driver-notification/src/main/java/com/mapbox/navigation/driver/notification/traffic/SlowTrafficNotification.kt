package com.mapbox.navigation.driver.notification.traffic

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.driver.notification.DriverNotification
import kotlin.time.Duration

/**
 * Represents a notification for slow traffic conditions on a specific route leg.
 *
 * This notification is generated when slow traffic is detected along a portion of the route,
 * providing details about the affected geometry range, delay, and distance.
 *
 * @param legIndex the index of the route leg where slow traffic is detected
 * @param slowTrafficGeometryRange the range of geometry indices affected by slow traffic
 * @param freeFlowRangeDuration the duration it would take to traverse the affected geometry range under free-flow conditions
 * @param slowTrafficRangeDuration the duration it takes to traverse the affected geometry range under current slow traffic conditions
 * @param slowTrafficRangeDistance the distance of the affected geometry range in meters
 *
 * @see [DriverNotification] for the base class of all driver notifications
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class SlowTrafficNotification internal constructor(
    val legIndex: Int,
    val slowTrafficGeometryRange: IntRange,
    val freeFlowRangeDuration: Duration,
    val slowTrafficRangeDuration: Duration,
    val slowTrafficRangeDistance: Double,
) : DriverNotification() {

    /**
     * Calculates the delay caused by slow traffic.
     * The delay is determined by subtracting the free-flow duration from the slow traffic duration.
     */
    val slowTrafficDelay: Duration
        get() = slowTrafficRangeDuration - freeFlowRangeDuration

    /**
     * Compares this `SlowTrafficNotification` instance with another object for equality.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SlowTrafficNotification

        if (legIndex != other.legIndex) return false
        if (slowTrafficRangeDistance != other.slowTrafficRangeDistance) return false
        if (slowTrafficGeometryRange != other.slowTrafficGeometryRange) return false
        if (freeFlowRangeDuration != other.freeFlowRangeDuration) return false
        if (slowTrafficRangeDuration != other.slowTrafficRangeDuration) return false

        return true
    }

    /**
     * Generates a hash code for the `SlowTrafficNotification` instance.
     */
    override fun hashCode(): Int {
        var result = legIndex
        result = 31 * result + slowTrafficRangeDistance.hashCode()
        result = 31 * result + slowTrafficGeometryRange.hashCode()
        result = 31 * result + freeFlowRangeDuration.hashCode()
        result = 31 * result + slowTrafficRangeDuration.hashCode()
        return result
    }

    /**
     * Returns a string representation of the `SlowTrafficNotification` instance.
     */
    override fun toString(): String {
        return "SlowTrafficNotification(" +
            "legIndex=$legIndex, " +
            "slowTrafficGeometryRange=$slowTrafficGeometryRange, " +
            "freeFlowRangeDuration=$freeFlowRangeDuration, " +
            "slowTrafficRangeDuration=$slowTrafficRangeDuration, " +
            "slowTrafficRangeDistance=$slowTrafficRangeDistance" +
            ")"
    }
}
