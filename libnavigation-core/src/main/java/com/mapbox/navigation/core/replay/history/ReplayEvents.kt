package com.mapbox.navigation.core.replay.history

import com.google.gson.annotations.SerializedName
import com.mapbox.navigation.base.internal.utils.safeCompareTo
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationProvider

/**
 * Replay event that mapped from [ReplayHistoryMapper] or created on your own. Override this
 * to support new or custom events. Each event can be replayed by the [MapboxReplayer].
 *
 * @param events Assumes chronological order, index 0 moves to [List.size] over time
 */
class ReplayEvents(
    val events: MutableList<ReplayEventBase>,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReplayEvents

        return events == other.events
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return events.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "ReplayEvents(events=$events)"
    }
}

/**
 * Base interface event for ReplayEvent.
 *
 * @property eventTimestamp timestamp of event seconds
 * @see [ReplayLocationProvider]
 */
interface ReplayEventBase {
    val eventTimestamp: Double
}

/**
 * The getStatus event from history. This may be deprecated soon but can
 * be useful for comparing versions.
 *
 * @param eventTimestamp timestamp of event in seconds
 */
class ReplayEventGetStatus(
    @SerializedName("event_timestamp")
    override val eventTimestamp: Double,
) : ReplayEventBase {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReplayEventGetStatus

        return eventTimestamp.safeCompareTo(other.eventTimestamp)
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return eventTimestamp.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "ReplayEventGetStatus(eventTimestamp=$eventTimestamp)"
    }
}

/**
 * Location event for replaying device GPS and Fused locations providers
 *
 * @param eventTimestamp timestamp of event in seconds
 * @param location [ReplayEventLocation] location coordinates to be replayed
 */
class ReplayEventUpdateLocation(
    @SerializedName("event_timestamp")
    override val eventTimestamp: Double,

    @SerializedName("location")
    val location: ReplayEventLocation,
) : ReplayEventBase {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReplayEventUpdateLocation

        if (!eventTimestamp.safeCompareTo(other.eventTimestamp)) return false
        return location == other.location
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = eventTimestamp.hashCode()
        result = 31 * result + location.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */

    override fun toString(): String {
        return "ReplayEventUpdateLocation(eventTimestamp=$eventTimestamp, location=$location)"
    }
}

/**
 * Location data for replaying position.
 *
 * @param lon longitude coordinate used for positioning
 * @param lat latitude coordinate used for positioning
 * @param provider String? represents the source of the data for analytical purposes
 * @param time Double? the time the location was measured in seconds and can be used to calculate freshness of the data
 * @param altitude Double? estimated altitude in meters
 * @param accuracyHorizontal Double? estimated accuracy of the coordinates as a radius in meters
 * @param bearing Double? estimated direction of movement in degrees, 0 is North and 180 is South
 * @param speed Double? estimated speed of the location update in meters per second
 */
class ReplayEventLocation(
    @SerializedName("lon")
    val lon: Double,

    @SerializedName("lat")
    val lat: Double,

    @SerializedName("provider")
    val provider: String?,

    @SerializedName("time")
    val time: Double?,

    @SerializedName("altitude")
    val altitude: Double?,

    @SerializedName("accuracyHorizontal")
    val accuracyHorizontal: Double?,

    @SerializedName("bearing")
    val bearing: Double?,

    @SerializedName("speed")
    val speed: Double?,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReplayEventLocation

        if (!lon.safeCompareTo(other.lon)) return false
        if (!lat.safeCompareTo(other.lat)) return false
        if (provider != other.provider) return false
        if (!time.safeCompareTo(other.time)) return false
        if (!altitude.safeCompareTo(other.altitude)) return false
        if (!accuracyHorizontal.safeCompareTo(other.accuracyHorizontal)) return false
        if (!bearing.safeCompareTo(other.bearing)) return false
        return speed.safeCompareTo(other.speed)
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = lon.hashCode()
        result = 31 * result + lat.hashCode()
        result = 31 * result + (provider?.hashCode() ?: 0)
        result = 31 * result + (time?.hashCode() ?: 0)
        result = 31 * result + (altitude?.hashCode() ?: 0)
        result = 31 * result + (accuracyHorizontal?.hashCode() ?: 0)
        result = 31 * result + (bearing?.hashCode() ?: 0)
        result = 31 * result + (speed?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "ReplayEventLocation(" +
            "lon=$lon, " +
            "lat=$lat, " +
            "provider=$provider, " +
            "time=$time, " +
            "altitude=$altitude, " +
            "accuracyHorizontal=$accuracyHorizontal, " +
            "bearing=$bearing, " +
            "speed=$speed" +
            ")"
    }
}

/**
 * Route data for replaying when a route is set.
 *
 * @param eventTimestamp timestamp of event in seconds
 * @param route [NavigationRoute] the route that was set, null when it has been cleared
 */
class ReplaySetNavigationRoute private constructor(
    @SerializedName("event_timestamp")
    override val eventTimestamp: Double,

    @SerializedName("route")
    val route: NavigationRoute?,
) : ReplayEventBase {

    /**
     * Use to rebuild [ReplaySetNavigationRoute].
     */
    fun toBuilder() = Builder(eventTimestamp)
        .route(route)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReplaySetNavigationRoute

        if (eventTimestamp != other.eventTimestamp) return false
        if (route != other.route) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = eventTimestamp.hashCode()
        result = 31 * result + (route?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "ReplaySetNavigationRoute(eventTimestamp=$eventTimestamp, route=$route)"
    }

    /**
     * Builds [ReplaySetNavigationRoute].
     */
    class Builder(
        private val eventTimestamp: Double,
    ) {
        private var route: NavigationRoute? = null

        /**
         * Set the route that this event represents.
         */
        fun route(route: NavigationRoute?): Builder = apply {
            this.route = route
        }

        /**
         * Build [ReplaySetNavigationRoute].
         */
        fun build() = ReplaySetNavigationRoute(
            eventTimestamp = eventTimestamp,
            route = route,
        )
    }
}
