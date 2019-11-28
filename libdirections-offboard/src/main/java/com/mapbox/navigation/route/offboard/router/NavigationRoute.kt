package com.mapbox.navigation.route.offboard.router

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.utils.TextUtils
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.model.RouteOptionsNavigation
import com.mapbox.navigation.base.route.model.RoutePointNavigation
import com.mapbox.navigation.route.offboard.extension.getUnitTypeForLocale
import com.mapbox.navigation.route.offboard.extension.mapToWalkingOptions
import com.mapbox.navigation.utils.extensions.inferDeviceLocale
import okhttp3.EventListener
import okhttp3.Interceptor
import retrofit2.Call
import retrofit2.Callback
import java.util.Locale

/**
 * The NavigationRoute class wraps the [MapboxDirections] class with parameters which
 * <u>must</u> be set in order for a navigation session to successfully begin. While it is possible
 * to pass in any [com.mapbox.api.directions.v5.models.DirectionsRoute] into
 * [MapboxNavigation.startNavigation], using this class will ensure your
 * request includes all the proper information needed for the navigation session to begin.
 *
 * Developer Note: MapboxDirections cannot be directly extended since it is an AutoValue class.
 *
 * 1.0
 */

internal class NavigationRoute
internal constructor(
    private val mapboxDirections: MapboxDirections
) {

    companion object {
        private val EVENT_LISTENER = NavigationRouteEventListener()

        /**
         * Build a new [NavigationRoute] object with the proper navigation parameters already setup.
         *
         * @return a [Builder] object for creating this object
         * @since 0.5.0
         */
        @JvmStatic
        fun builder(context: Context): Builder =
            Builder()
                .language(context)
                .voiceUnits(context)
    }

    /**
     * Wrapper method for Retrofit's [Call.clone] call, useful for getting call information
     * and allowing you to perform additional functions on this [NavigationRoute] class.
     *
     * @return cloned call
     * @since 1.0.0
     */
    val call: Call<DirectionsResponse>
        get() = mapboxDirections.cloneCall()

    /**
     * Call when you have constructed your navigation route with your desired parameters. A
     * [Callback] must be passed into the method to handle both the response and failure.
     *
     * @param callback a RetroFit callback which contains an onResponse and onFailure
     * @since 0.5.0
     */
    fun getRoute(callback: Callback<DirectionsResponse>) {
        mapboxDirections.enqueueCall(callback)
    }

    /**
     * Wrapper method for Retrofit's [Call.cancel] call, important to manually cancel call if
     * the user dismisses the calling activity or no longer needs the returned results.
     */
    fun cancelCall() {
        if (!call.isExecuted) {
            call.cancel()
        }
    }

    /**
     * This builder is used to create a new request to the Mapbox Directions API and removes options
     * which would cause this navigation SDK to not behave properly. At a bare minimum, your request
     * must include an access token, an origin, and a destination. All other fields can be left alone
     * inorder to use the default behaviour of the API.
     *
     *
     * By default, the directions profile is set to driving with traffic but can be changed to
     * reflect your users use-case.
     *
     *
     * @since 0.5.0
     */
    class Builder internal constructor(private val directionsBuilder: MapboxDirections.Builder) {
        private val eventListener: NavigationRouteEventListener
        private var origin: RoutePointNavigation? = null
        private var destination: RoutePointNavigation? = null
        private val waypoints = ArrayList<RoutePointNavigation>()
        private val SEMICOLON = ";"
        private val COMMA = ","

        /**
         * Private constructor for initializing the raw MapboxDirections.Builder
         */
        constructor() : this(MapboxDirections.builder()) {}

        init {
            this.eventListener = EVENT_LISTENER
        }

        /**
         * The username for the account that the directions engine runs on. In most cases, this should
         * always remain the default value of [DirectionsCriteria.PROFILE_DEFAULT_USER].
         *
         * @param user a non-null string which will replace the default user used in the directions
         * request
         * @return this builder for chaining options together
         * @since 0.5.0
         */
        fun user(user: String): Builder {
            directionsBuilder.user(user)
            return this
        }

        /**
         * This selects which mode of transportation the user will be using while navigating from the
         * origin to the final destination. The options include driving, driving considering traffic,
         * walking, and cycling. Using each of these profiles will result in different routing biases.
         *
         * @param profile required to be one of the String values found in the [ProfileCriteria]
         * @return this builder for chaining options together
         * @since 0.5.0
         */
        fun profile(@DirectionsCriteria.ProfileCriteria profile: String): Builder {
            directionsBuilder.profile(profile)
            return this
        }

        /**
         * This sets the starting point on the map where the route will begin. It is one of the
         * required parameters which must be set for a successful directions response.
         *
         * @param origin a GeoJson [Point] object representing the starting location for the route
         * @return this builder for chaining options together
         * @since 0.5.0
         */
        fun origin(origin: Point): Builder {
            this.origin = RoutePointNavigation(
                origin,
                null,
                null
            )
            return this
        }

        /**
         * This sets the starting point on the map where the route will begin. It is one of the
         * required parameters which must be set for a successful directions response.
         *
         * @param origin a GeoJson [Point] object representing the starting location for the
         * route
         * @param angle double value used for setting the corresponding coordinate's angle of travel
         * when determining the route
         * @param tolerance the deviation the bearing angle can vary while determining the route,
         * recommended to be either 45 or 90 degree tolerance
         * @return this builder for chaining options together
         * @since 0.5.0
         */
        fun origin(
            origin: Point,
            angle: Double?,
            tolerance: Double?
        ): Builder {
            this.origin =
                RoutePointNavigation(
                    origin,
                    angle,
                    tolerance
                )
            return this
        }

        /**
         * This sets the ending point on the map where the route will end. It is one of the required
         * parameters which must be set for a successful directions response.
         *
         * @param destination a GeoJson [Point] object representing the starting location for the
         * route
         * @return this builder for chaining options together
         * @since 0.50
         */
        fun destination(destination: Point): Builder {
            this.destination =
                RoutePointNavigation(
                    destination,
                    null,
                    null
                )
            return this
        }

        /**
         * This sets the ending point on the map where the route will end. It is one of the required
         * parameters which must be set for a successful directions response.
         *
         * @param destination a GeoJson [Point] object representing the starting location for the
         * route
         * @param angle double value used for setting the corresponding coordinate's angle of travel
         * when determining the route
         * @param tolerance the deviation the bearing angle can vary while determining the route,
         * recommended to be either 45 or 90 degree tolerance
         * @return this builder for chaining options together
         * @since 0.5.0
         */
        fun destination(
            destination: Point,
            angle: Double?,
            tolerance: Double?
        ): Builder {
            this.destination =
                RoutePointNavigation(
                    destination,
                    angle,
                    tolerance
                )
            return this
        }

        /**
         * This can be used to set up to 23 additional in-between points which will act as pit-stops
         * along the users route. Note that if you are using the
         * [DirectionsCriteria.PROFILE_DRIVING_TRAFFIC] that the max number of waypoints allowed
         * in the request is currently limited to 1.
         *
         * @param waypoint a [Point] which represents the pit-stop or waypoint where you'd like
         * one of the [com.mapbox.api.directions.v5.models.RouteLeg] to
         * navigate the user to
         * @return this builder for chaining options together
         * @since 0.5.0
         */
        fun addWaypoint(waypoint: Point): Builder {
            this.waypoints.add(
                RoutePointNavigation(
                    waypoint,
                    null,
                    null
                )
            )
            return this
        }

        /**
         * This can be used to set up to 23 additional in-between points which will act as pit-stops
         * along the users route.
         *
         *
         * Note that if you are using the
         * [DirectionsCriteria.PROFILE_DRIVING_TRAFFIC] that the max number of waypoints allowed
         * in the request is currently limited to 1.
         *
         *
         * These waypoints are added to the request in the order you add them to the builder with this method.
         *
         * @param waypoint a [Point] which represents the pit-stop or waypoint where you'd like
         * one of the [com.mapbox.api.directions.v5.models.RouteLeg] to
         * navigate the user to
         * @param angle double value used for setting the corresponding coordinate's angle of travel
         * when determining the route
         * @param tolerance the deviation the bearing angle can vary while determining the route,
         * recommended to be either 45 or 90 degree tolerance
         * @return this builder for chaining options together
         * @since 0.5.0
         */
        fun addWaypoint(
            waypoint: Point,
            angle: Double?,
            tolerance: Double?
        ): Builder {
            this.waypoints.add(
                RoutePointNavigation(
                    waypoint,
                    angle,
                    tolerance
                )
            )
            return this
        }

        /**
         * Optionally set whether to try to return alternative routes. An alternative is classified as a
         * route that is significantly different then the fastest route, but also still reasonably fast.
         * Not in all circumstances such a route exists. At the moment at most one alternative can be
         * returned.
         *
         * @param alternatives true if you'd like to receive an alternative route, otherwise false or
         * null to use the APIs default value
         * @return this builder for chaining options together
         * @since 0.5.0
         */
        fun alternatives(alternatives: Boolean?): Builder {
            directionsBuilder.alternatives(alternatives)
            return this
        }

        /**
         * Set the instruction language for the directions request, the default is english. Only a
         * select number of languages are currently supported, reference the table provided in the see
         * link below.
         *
         * @param language a Locale representing the language you'd like the instructions to be
         * written in when returned
         * @return this builder for chaining options together
         * @see [Supported
         * Languages](https://www.mapbox.com/api-documentation/.instructions-languages)
         *
         * @since 0.5.0
         */
        fun language(language: Locale): Builder {
            directionsBuilder.language(language)
            return this
        }

        internal fun language(context: Context): Builder {
            directionsBuilder.language(context.inferDeviceLocale())
            return this
        }

        internal fun voiceUnits(context: Context): Builder {
            directionsBuilder.voiceUnits(context.inferDeviceLocale().getUnitTypeForLocale())
            return this
        }

        /**
         * Base package name or other simple string identifier. Used inside the calls user agent header.
         *
         * @param clientAppName base package name or other simple string identifier
         * @return this builder for chaining options together
         * @since 0.5.0
         */
        fun clientAppName(clientAppName: String): Builder {
            directionsBuilder.clientAppName(clientAppName)
            return this
        }

        /**
         * Adds an optional interceptor to set in the OkHttp client.
         *
         * @param interceptor to set for OkHttp
         * @return this builder for chaining options together
         */
        fun interceptor(interceptor: Interceptor): Builder {
            directionsBuilder.interceptor(interceptor)
            return this
        }

        /**
         * Adds an optional event listener to set in the OkHttp client.
         *
         * @param eventListener to set for OkHttp
         * @return this builder for chaining options together
         */
        fun eventListener(eventListener: EventListener): Builder {
            directionsBuilder.eventListener(eventListener)
            return this
        }

        /**
         * Enables a route to be refreshable
         *
         * @param enableRefresh whether or not to enable refresh
         * @return this builder for chaining options together
         */
        fun enableRefresh(enableRefresh: Boolean): Builder {
            directionsBuilder.enableRefresh(enableRefresh)
            return this
        }

        /**
         * Optionally create a [Builder] based on all variables
         * from given [RouteOptions].
         *
         *
         * Note: [RouteOptions.bearings] are excluded because it's better
         * to recalculate these at the time of the request, as your location bearing
         * is constantly changing.
         *
         * @param options containing all variables for request
         * @return this builder for chaining options together
         * @since 0.9.0
         */
        fun routeOptions(options: RouteOptionsNavigation): Builder {
            origin = options.coordinates.first()
            destination = options.coordinates.last()

            waypoints.addAll(options.coordinates.toMutableList())
            
            if (!TextUtils.isEmpty(options.baseUrl)) {
                directionsBuilder.baseUrl(options.baseUrl)
            }

            if (!TextUtils.isEmpty(options.language)) {
                directionsBuilder.language(Locale(options.language))
            }

            directionsBuilder.geometries(options.geometries)


            options.profile?.let {
                directionsBuilder.profile(it)
            }

            options.alternatives?.let {
                directionsBuilder.alternatives(it)
            }

            options.voiceUnits?.let {
                directionsBuilder.voiceUnits(it)
            }

            options.user?.let {
                directionsBuilder.user(it)
            }

            options.accessToken?.let {
                directionsBuilder.accessToken(it)
            }

            options.annotations?.let {
                directionsBuilder.annotations(it)
            }

            options.approaches?.let { approaches ->
                if (approaches.isNotEmpty()) {
                    val result =
                        approaches.split(SEMICOLON.toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    directionsBuilder.addApproaches(*result)
                }
            }

            options.waypointIndices?.let { waypointIndices ->
                if (waypointIndices.isNotEmpty()) {
                    val splitWaypointIndices = parseWaypointIndices(waypointIndices)
                    directionsBuilder.addWaypointIndices(*splitWaypointIndices)
                }
            }

            options.waypointNames?.let { waypointNames ->
                if (waypointNames.isNotEmpty()) {
                    val names =
                        waypointNames.split(SEMICOLON.toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    directionsBuilder.addWaypointNames(*names)
                }
            }

            options.waypointTargets?.let { waypointTargets ->
                if (waypointTargets.isNotEmpty()) {
                    val splitWaypointTargets = parseWaypointTargets(waypointTargets)
                    directionsBuilder.addWaypointTargets(*splitWaypointTargets)
                }
            }

            options.walkingOptions?.let {
                directionsBuilder.walkingOptions(it.mapToWalkingOptions())
            }

            return this
        }

        /**
         * This uses the provided parameters set using the [Builder] and adds the required
         * settings for navigation to work correctly.
         *
         * @return a new instance of Navigation Route
         * @since 0.5.0
         */
        fun build(): NavigationRoute {
            // Set the default values which the user cannot alter.
            assembleWaypoints()
            directionsBuilder
                .steps(true)
                .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .voiceInstructions(true)
                .bannerInstructions(true)
                .roundaboutExits(true)
                .eventListener(eventListener)
                .enableRefresh(true)
            return NavigationRoute(directionsBuilder.build())
        }

        private fun parseWaypointIndices(waypointIndices: String): Array<Int> {
            val splitWaypointIndices =
                waypointIndices.split(SEMICOLON.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            val indices = Array(splitWaypointIndices.size, { 0 })
            var index = 0
            for (waypointIndex in splitWaypointIndices) {
                val parsedIndex = Integer.valueOf(waypointIndex)
                indices[index++] = parsedIndex
            }
            return indices
        }

        private fun parseWaypointTargets(waypointTargets: String): Array<Point?> {
            val splitWaypointTargets =
                waypointTargets.split(SEMICOLON.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            val waypoints = arrayOfNulls<Point>(splitWaypointTargets.size)
            var index = 0
            for (waypointTarget in splitWaypointTargets) {
                val point = waypointTarget.split(COMMA.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                if (waypointTarget.isEmpty()) {
                    waypoints[index++] = null
                } else {
                    val longitude = java.lang.Double.valueOf(point[0])
                    val latitude = java.lang.Double.valueOf(point[0])
                    waypoints[index++] = Point.fromLngLat(longitude, latitude)
                }
            }
            return waypoints
        }

        private fun assembleWaypoints() {
            origin?.let { origin ->
                directionsBuilder.origin(origin.point)
                directionsBuilder.addBearing(origin.bearingAngle, origin.tolerance)
            }

            for (waypoint in waypoints) {
                directionsBuilder.addWaypoint(waypoint.point)
                directionsBuilder.addBearing(waypoint.bearingAngle, waypoint.tolerance)
            }

            destination?.let { destination ->
                directionsBuilder.destination(destination.point)
                directionsBuilder.addBearing(destination.bearingAngle, destination.tolerance)
            }
        }
    }
}
