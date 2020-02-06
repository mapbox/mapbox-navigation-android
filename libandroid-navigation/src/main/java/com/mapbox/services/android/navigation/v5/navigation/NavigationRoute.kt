package com.mapbox.services.android.navigation.v5.navigation

import android.content.Context
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.exceptions.ServicesException
import com.mapbox.core.utils.TextUtils
import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.v5.utils.extensions.getUnitTypeForLocale
import com.mapbox.services.android.navigation.v5.utils.extensions.inferDeviceLocale
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
 * 0.5.0
 */

class NavigationRoute
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
                .annotations(
                    DirectionsCriteria.ANNOTATION_CONGESTION,
                    DirectionsCriteria.ANNOTATION_DISTANCE
                )
                .language(context)
                .voiceUnits(context)
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .continueStraight(true)
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
        mapboxDirections.enqueueCall(NavigationRouteCallback(EVENT_LISTENER, callback))
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
        private var origin: NavigationRouteWaypoint? = null
        private var destination: NavigationRouteWaypoint? = null
        private val waypoints = ArrayList<NavigationRouteWaypoint>()
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
            this.origin = NavigationRouteWaypoint(origin, null, null)
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
                NavigationRouteWaypoint(origin, angle, tolerance)
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
                NavigationRouteWaypoint(destination, null, null)
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
                NavigationRouteWaypoint(destination, angle, tolerance)
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
                NavigationRouteWaypoint(
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
                NavigationRouteWaypoint(
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

        /**
         * Whether or not to return additional metadata along the route. Possible values are:
         * [DirectionsCriteria.ANNOTATION_DISTANCE],
         * [DirectionsCriteria.ANNOTATION_DURATION],
         * [DirectionsCriteria.ANNOTATION_DURATION] and
         * [DirectionsCriteria.ANNOTATION_CONGESTION]. Several annotation can be used by
         * separating them with `,`.
         *
         *
         * If left alone, this will automatically set Congestion to enabled
         *
         *
         * @param annotations string referencing one of the annotation direction criteria's. The strings
         * restricted to one or multiple values inside the [AnnotationCriteria]
         * or null which will result in no annotations being used
         * @return this builder for chaining options together
         * @see [RouteLeg object
         * documentation](https://www.mapbox.com/api-documentation/.routeleg-object)
         *
         * @since 0.5.0
         */
        fun annotations(@DirectionsCriteria.AnnotationCriteria vararg annotations: String?): Builder {
            directionsBuilder.annotations(*annotations)
            return this
        }

        /**
         * Optionally, Use to filter the road segment the waypoint will be placed on by direction and
         * dictates the angle of approach. This option should always be used in conjunction with the
         * [.radiuses] parameter.
         *
         *
         * The parameter takes two values per waypoint: the first is an angle clockwise from true north
         * between 0 and 360. The second is the range of degrees the angle can deviate by. We recommend
         * a value of 45 degrees or 90 degrees for the range, as bearing measurements tend to be
         * inaccurate. This is useful for making sure we reroute vehicles on new routes that continue
         * traveling in their current direction. A request that does this would provide bearing and
         * radius values for the first waypoint and leave the remaining values empty. If provided, the
         * list of bearings must be the same length as the list of waypoints, but you can skip a
         * coordinate and show its position by passing in null value for both the angle and tolerance
         * values.
         *
         *
         * Each bearing value gets associated with the same order which coordinates are arranged in this
         * builder. For example, the first bearing added in this builder will be associated with the
         * origin `Point`, the nth bearing being associated with the nth waypoint added (if added)
         * and the last bearing being added will be associated with the destination.
         *
         *
         * If given the chance, you should pass in the bearing information at the same time the point is
         * passed in as a waypoint, this way it is ensured the value is matched up correctly with the
         * coordinate.
         *
         * @param angle double value used for setting the corresponding coordinate's angle of travel
         * when determining the route
         * @param tolerance the deviation the bearing angle can vary while determining the route,
         * recommended to be either 45 or 90 degree tolerance
         * @return this builder for chaining options together
         * @since 0.5.0
         */
        @Deprecated(
            "use the bearing paired with {@link Builder#origin(Point, Double, Double)},\n" +
                "      {@link Builder#destination(Point, Double, Double)},\n" +
                "      or {@link Builder#addWaypoint(Point, Double, Double)} instead."
        )
        fun addBearing(
            @FloatRange(from = 0.0, to = 360.0) angle: Double?,
            @FloatRange(from = 0.0, to = 360.0) tolerance: Double?
        ): Builder {
            directionsBuilder.addBearing(angle, tolerance)
            return this
        }

        /**
         * Optionally, set the maximum distance in meters that each coordinate is allowed to move when
         * snapped to a nearby road segment. There must be as many radiuses as there are coordinates in
         * the request. Values can be any number greater than 0 or they can be unlimited simply by
         * passing [Double.POSITIVE_INFINITY].
         *
         *
         * If no routable road is found within the radius, a `NoSegment` error is returned.
         *
         *
         * @param radiuses double array containing the radiuses defined in unit meters.
         * @return this builder for chaining options together
         * @since 0.5.0
         */
        fun radiuses(@FloatRange(from = 0.0) vararg radiuses: Double): Builder {
            directionsBuilder.radiuses(*radiuses)
            return this
        }

        /**
         * Change the units used for voice announcements, this does not change the units provided in
         * other fields outside of the [com.mapbox.api.directions.v5.models.VoiceInstructions]
         * object.
         *
         * @param voiceUnits one of the values found inside the [VoiceUnitCriteria]
         * @return this builder for chaining options together
         * @since 0.8.0
         */
        fun voiceUnits(@DirectionsCriteria.VoiceUnitCriteria voiceUnits: String): Builder {
            directionsBuilder.voiceUnits(voiceUnits)
            return this
        }

        internal fun voiceUnits(context: Context): Builder {
            directionsBuilder.voiceUnits(context.inferDeviceLocale().getUnitTypeForLocale())
            return this
        }

        /**
         * Exclude specific road classes such as highways, tolls, and more.
         *
         * @param exclude one of the values found inside the [ExcludeCriteria]
         * @return this builder for chaining options together
         * @since 0.8.0
         */
        fun exclude(@DirectionsCriteria.ExcludeCriteria exclude: String?): Builder {
            directionsBuilder.exclude(exclude)
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
         * Required to call when this is being built. If no access token provided,
         * [ServicesException] will be thrown.
         *
         * @param accessToken Mapbox access token, You must have a Mapbox account inorder to use
         * the Optimization API
         * @return this builder for chaining options together
         * @since 0.5.0
         */
        fun accessToken(accessToken: String): Builder {
            directionsBuilder.accessToken(accessToken)
            return this
        }

        /**
         * Optionally change the APIs base URL to something other then the default Mapbox one.
         *
         * @param baseUrl base url used as end point
         * @return this builder for chaining options together
         * @since 0.5.0
         */
        fun baseUrl(baseUrl: String): Builder {
            directionsBuilder.baseUrl(baseUrl)
            return this
        }

        /**
         * Indicates from which side of the road to approach a waypoint.
         * Accepts <tt>unrestricted</tt> (default), <tt>curb</tt> or <tt>null</tt>.
         * If set to <tt>unrestricted</tt>, the route can approach waypoints
         * from either side of the road. If set to <tt>curb</tt>, the route will be returned
         * so that on arrival, the waypoint will be found on the side that corresponds with the
         * <tt>driving_side</tt> of the region in which the returned route is located.
         * If provided, the list of approaches must be the same length as the list of waypoints.
         *
         * @param approaches null if you'd like the default approaches,
         * else one of the options found in
         * [com.mapbox.api.directions.v5.DirectionsCriteria.ApproachesCriteria].
         * @return this builder for chaining options together
         * @since 0.15.0
         */
        fun addApproaches(vararg approaches: String?): Builder {
            directionsBuilder.addApproaches(*approaches)
            return this
        }

        /**
         * Optionally, set which input coordinates should be treated as waypoints / separate legs.
         * Note: coordinate indices not added here act as silent waypoints
         *
         *
         * Most useful in combination with <tt>steps=true</tt> and requests based on traces
         * with high sample rates. Can be an index corresponding to any of the input coordinates,
         * but must contain the first ( 0 ) and last coordinates' index separated by <tt>;</tt>.
         *
         * @param indices integer array of coordinate indices to be used as waypoints
         * @return this builder for chaining options together
         */
        fun addWaypointIndices(@IntRange(from = 0) vararg indices: Int): Builder {
            val result = Array(indices.size) { 0 }
            var index = 0
            for (i in indices) {
                result[index++] = i
            }
            directionsBuilder.addWaypointIndices(*result)
            return this
        }

        /**
         * Custom names for waypoints used for the arrival instruction,
         * each separated by <tt>;</tt>. Values can be any string and total number of all characters cannot
         * exceed 500. If provided, the list of <tt>waypointNames</tt> must be the same length as the list of
         * coordinates, but you can skip a coordinate and show its position with the <tt>;</tt> separator.
         *
         * @param waypointNames Custom names for waypoints used for the arrival instruction.
         * @return this builder for chaining options together
         * @since 0.15.0
         */
        fun addWaypointNames(vararg waypointNames: String): Builder {
            directionsBuilder.addWaypointNames(*waypointNames)
            return this
        }

        /**
         * A list of coordinate pairs used to specify drop-off
         * locations that are distinct from the locations specified in coordinates.
         * If this parameter is provided, the Directions API will compute the side of the street,
         * <tt>left</tt> or <tt>right</tt>, for each target based on the <tt>waypoint_targets</tt>
         * and the driving direction.
         * The <tt>maneuver.modifier</tt>, banner and voice instructions will be updated with the computed
         * side of street. The number of waypoint targets must be the same as the number of coordinates,
         * but you can skip a coordinate pair and show its position in the list adding <tt>null</tt>.
         * Must be used with <tt>steps=true</tt>.
         *
         * @param waypointTargets [Point] coordinates for drop-off locations
         * @return this builder for chaining options together
         * @since 0.26.0
         */
        fun addWaypointTargets(vararg waypointTargets: Point?): Builder {
            directionsBuilder.addWaypointTargets(*waypointTargets)
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
         * Sets allowed direction of travel when departing intermediate waypoints. If true the route
         * will continue in the same direction of travel. If false the route may continue in the
         * opposite direction of travel. API defaults to true for
         * [DirectionsCriteria.PROFILE_DRIVING] and false for
         * [DirectionsCriteria.PROFILE_WALKING] and [DirectionsCriteria.PROFILE_CYCLING].
         *
         * @param continueStraight boolean true if you want to always continue straight, else false.
         * @return this builder for chaining options together
         */
        fun continueStraight(continueStraight: Boolean): Builder {
            directionsBuilder.continueStraight(continueStraight)
            return this
        }

        /**
         * Sets a [NavigationWalkingOptions] object which contains options for use with the
         * walking profile.
         *
         * @param navigationWalkingOptions object holding walking options
         * @return this builder for chaining options together
         */
        fun walkingOptions(navigationWalkingOptions: NavigationWalkingOptions): Builder {
            directionsBuilder.walkingOptions(navigationWalkingOptions.walkingOptions)
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
        fun routeOptions(options: RouteOptions): Builder {

            if (!TextUtils.isEmpty(options.baseUrl())) {
                directionsBuilder.baseUrl(options.baseUrl())
            }

            if (!TextUtils.isEmpty(options.language())) {
                directionsBuilder.language(java.util.Locale(options.language()))
            }

            if (options.alternatives() != null) {
                directionsBuilder.alternatives(options.alternatives())
            }

            if (!TextUtils.isEmpty(options.profile())) {
                directionsBuilder.profile(options.profile())
            }

            if (options.alternatives() != null) {
                directionsBuilder.alternatives(options.alternatives())
            }

            if (!TextUtils.isEmpty(options.voiceUnits())) {
                directionsBuilder.voiceUnits(options.voiceUnits())
            }

            if (!TextUtils.isEmpty(options.user())) {
                directionsBuilder.user(options.user())
            }

            if (!TextUtils.isEmpty(options.accessToken())) {
                directionsBuilder.accessToken(options.accessToken())
            }

            if (!TextUtils.isEmpty(options.annotations())) {
                directionsBuilder.annotations(options.annotations())
            }

            options.approaches()?.let { approaches ->
                if (approaches.isNotEmpty()) {
                    val result =
                        approaches.split(SEMICOLON.toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    directionsBuilder.addApproaches(*result)
                }
            }

            options.waypointIndices()?.let { waypointIndices ->
                if (waypointIndices.isNotEmpty()) {
                    val splitWaypointIndices = parseWaypointIndices(waypointIndices)
                    directionsBuilder.addWaypointIndices(*splitWaypointIndices)
                }
            }

            options.waypointNames()?.let { waypointNames ->
                if (waypointNames.isNotEmpty()) {
                    val names =
                        waypointNames.split(SEMICOLON.toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    directionsBuilder.addWaypointNames(*names)
                }
            }

            options.waypointTargets()?.let { waypointTargets ->
                if (waypointTargets.isNotEmpty()) {
                    val splitWaypointTargets = parseWaypointTargets(waypointTargets)
                    directionsBuilder.addWaypointTargets(*splitWaypointTargets)
                }
            }

            val walkingOptions = options.walkingOptions()
            if (walkingOptions != null) {
                directionsBuilder.walkingOptions(walkingOptions)
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
