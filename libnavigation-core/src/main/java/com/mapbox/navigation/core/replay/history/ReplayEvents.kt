package com.mapbox.navigation.core.replay.history

import com.google.gson.annotations.SerializedName
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine

/**
 * Replay event that mapped from [ReplayHistoryMapper] or created on your own. Override this
 * to support new or custom events. Each event can be replayed by the [MapboxReplayer].
 *
 * @param events Assumes chronological order, index 0 moves to [List.size] over time
 */
data class ReplayEvents(
    val events: MutableList<ReplayEventBase>
)

/**
 * Base interface event for ReplayEvent.
 *
 * @property eventTimestamp timestamp of event seconds
 * @see [ReplayLocationEngine]
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
data class ReplayEventGetStatus(
    @SerializedName("event_timestamp")
    override val eventTimestamp: Double
) : ReplayEventBase

/**
 * Location event for replaying device GPS and Fused locations providers
 *
 * @param eventTimestamp timestamp of event in seconds
 * @param location [ReplayEventLocation] location coordinates to be replayed
 */
data class ReplayEventUpdateLocation(
    @SerializedName("event_timestamp")
    override val eventTimestamp: Double,

    @SerializedName("location")
    val location: ReplayEventLocation
) : ReplayEventBase

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
data class ReplayEventLocation(
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
    val speed: Double?
)

/**
 * Route data for replaying when a route is set.
 *
 * @param eventTimestamp timestamp of event in seconds
 * @param route [DirectionsRoute] the route that was set, null when it has been cleared
 */
@Deprecated("use ReplaySetNavigationRoute instead")
data class ReplaySetRoute(
    @SerializedName("event_timestamp")
    override val eventTimestamp: Double,

    @SerializedName("route")
    val route: DirectionsRoute?
) : ReplayEventBase

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
    val route: NavigationRoute?
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
        private val eventTimestamp: Double
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
            route = route
        )
    }
}
