package com.mapbox.navigation.route.offboard.router

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.model.RouteOptionsNavigation
import com.mapbox.navigation.base.route.model.RoutePointNavigation
import com.mapbox.navigation.route.offboard.extension.getUnitTypeForLocale
import com.mapbox.navigation.route.offboard.extension.mapToWalkingOptions
import com.mapbox.navigation.utils.extensions.inferDeviceLocale
import java.util.Locale
import okhttp3.EventListener
import okhttp3.Interceptor
import retrofit2.Call
import retrofit2.Callback

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

internal class NavigationOffboardRoute
constructor(
    private val mapboxDirections: MapboxDirections
) {

    companion object {
        private val EVENT_LISTENER = NavigationRouteEventListener()
    }

    /**
     * Wrapper method for Retrofit's [Call.clone] call, useful for getting call information
     * and allowing you to perform additional functions on this [NavigationOffboardRoute] class.
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

    fun toBuilder(): Builder = Builder(mapboxDirections.toBuilder())

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
         * This selects which mode of transportation the user will be using while navigating from the
         * origin to the final destination. The options include driving, driving considering traffic,
         * walking, and cycling. Using each of these profiles will result in different routing biases.
         *
         * @param profile required to be one of the String values found in the [ProfileCriteria]
         * @return this builder for chaining options together
         * @since 0.5.0
         */
        internal fun profile(@DirectionsCriteria.ProfileCriteria profile: String): Builder {
            directionsBuilder.profile(profile)
            return this
        }

        /**
         * Sets allowed direction of travel when departing intermediate waypoints. If true the route
         * will continue in the same direction of travel. If false the route may continue in the
         * opposite direction of travel. API defaults to true for
         * [DirectionsCriteria.PROFILE_DRIVING] and false for
         * [DirectionsCriteria.PROFILE_WALKING] and [DirectionsCriteria.PROFILE_CYCLING].
         *
         * @param continueStraight boolean true if you want to always continue straight, else false.
         * @return this builder for chaining options together
         */
        internal fun continueStraight(continueStraight: Boolean): Builder {
            directionsBuilder.continueStraight(continueStraight)
            return this
        }

        internal fun language(context: Context): Builder {
            directionsBuilder.language(context.inferDeviceLocale())
            return this
        }

        internal fun roundaboutExits(roundaboutExits: Boolean): Builder {
            directionsBuilder.roundaboutExits(roundaboutExits)
            return this
        }

        internal fun geometries(@DirectionsCriteria.GeometriesCriteria geometry: String): Builder {
            directionsBuilder.geometries(geometry)
            return this
        }

        internal fun overview(@DirectionsCriteria.OverviewCriteria overview: String): Builder {
            directionsBuilder.overview(overview)
            return this
        }

        internal fun steps(steps: Boolean): Builder {
            directionsBuilder.steps(steps)
            return this
        }

        internal fun annotations(@DirectionsCriteria.AnnotationCriteria vararg annotations: String?): Builder {
            directionsBuilder.annotations(*annotations)
            return this
        }

        internal fun voiceInstructions(voiceInstructions: Boolean): Builder {
            directionsBuilder.voiceInstructions(voiceInstructions)
            return this
        }

        internal fun bannerInstructions(bannerInstructions: Boolean): Builder {
            directionsBuilder.bannerInstructions(bannerInstructions)
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
         * Adds an optional network interceptor to set in the OkHttp client.
         *
         * @param interceptor to set for OkHttp
         * @return this builder for chaining options together
         */
        fun networkInterceptor(interceptor: Interceptor): Builder {
            directionsBuilder.networkInterceptor(interceptor)
            return this
        }

        /**
         * Adds an optional event listener to set in the OkHttp client.
         *
         * @param eventListener to set for OkHttp
         * @return this builder for chaining options together
         */
        internal fun eventListener(eventListener: EventListener): Builder {
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
        internal fun routeOptions(options: RouteOptionsNavigation): Builder {
            options.baseUrl?.let {
                directionsBuilder.baseUrl(it)
            }

            options.user?.let {
                directionsBuilder.user(it)
            }

            options.profile?.let {
                directionsBuilder.profile(it)
            }

            origin = options.coordinates.first()

            waypoints.clear()
            waypoints.addAll(options.coordinates.drop(1).dropLast(1))

            destination = options.coordinates.last()

            options.alternatives?.let {
                directionsBuilder.alternatives(it)
            }

            options.language?.let {
                directionsBuilder.language(Locale(it))
            }

            options.radiuses?.let { radiuses ->
                if (radiuses.isNotEmpty()) {
                    radiuses.convertToListOfDoubles(SEMICOLON[0])?.toDoubleArray()?.let { result ->
                        directionsBuilder.radiuses(*result)
                    }
                }
            }

            options.bearings?.let { bearings ->
                if (bearings.isNotEmpty()) {
                    bearings.convertToListOfPairsOfDoubles(SEMICOLON[0], COMMA[0])
                        ?.forEach { pair ->
                            directionsBuilder.addBearing(pair.first, pair.second)
                        }
                }
            }

            options.continueStraight?.let {
                directionsBuilder.continueStraight(it)
            }

            options.roundaboutExits?.let {
                directionsBuilder.roundaboutExits(it)
            }

            options.geometries?.let {
                directionsBuilder.geometries(it)
            }

            options.overview?.let {
                directionsBuilder.overview(it)
            }

            options.steps?.let {
                directionsBuilder.steps(it)
            }

            options.annotations?.let {
                directionsBuilder.annotations(it)
            }

            options.voiceInstructions?.let {
                directionsBuilder.voiceInstructions(it)
            }

            options.bannerInstructions?.let {
                directionsBuilder.bannerInstructions(it)
            }

            options.voiceUnits?.let {
                directionsBuilder.voiceUnits(it)
            }

            options.accessToken?.let {
                directionsBuilder.accessToken(it)
            }

            options.requestUuid?.let {
                // TODO Check if needed as it is only set at response time
            }

            options.exclude?.let {
                directionsBuilder.exclude(it)
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
        fun build(): NavigationOffboardRoute {
            // Set the default values which the user cannot alter.
            assembleWaypoints()
            directionsBuilder
                .eventListener(eventListener)
            return NavigationOffboardRoute(directionsBuilder.build())
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

        private fun String.convertToListOfDoubles(separator: Char = ';'): List<Double>? =
            try {
                this.split(separator).map { token ->
                    token.toDouble()
                }
            } catch (e: Exception) {
                null
            }

        private fun String.convertToListOfPairsOfDoubles(
            firstSeparator: Char = ';',
            secondSeparator: Char = ','
        ): List<Pair<Double, Double>>? =
            try {
                val pairs = split(firstSeparator)
                val result = mutableListOf<Pair<Double, Double>>()
                pairs.forEach { pair ->
                    val parts = pair.split(secondSeparator)
                    result.add(Pair(parts[0].toDouble(), parts[1].toDouble()))
                }
                result.toList()
            } catch (e: Exception) {
                null
            }
    }
}
