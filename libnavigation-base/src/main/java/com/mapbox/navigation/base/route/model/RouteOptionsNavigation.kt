package com.mapbox.navigation.base.route.model

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.RouteUrl

class RouteOptionsNavigation(
    val baseUrl: String,
    val user: String,
    val profile: String,
    val origin: RoutePointNavigation,
    val waypoints: List<RoutePointNavigation>,
    val destination: RoutePointNavigation,
    val alternatives: Boolean,
    val language: String?,
    val radiuses: String?,
    val bearings: String?,
    val continueStraight: Boolean,
    val roundaboutExits: Boolean?,
    val geometries: String?,
    val overview: String?,
    val steps: Boolean,
    val annotations: String?,
    val voiceInstructions: Boolean?,
    val bannerInstructions: Boolean?,
    val voiceUnits: String?,
    val accessToken: String?,
    val requestUuid: String?,
    val exclude: String?,
    val approaches: String?,
    val waypointIndices: String?,
    val waypointNames: String?,
    val waypointTargets: String?,
    val walkingOptions: WalkingOptionsNavigation?
) {

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }

    val coordinates: List<Point>
        get() = listOf(origin.point) + waypoints.map { it.point } + destination.point

    class Builder internal constructor() {
        private lateinit var _origin: RoutePointNavigation
        private lateinit var _destination: RoutePointNavigation
        private val _waypoints = mutableListOf<RoutePointNavigation>()

        private var baseUrl: String? = null
        private var user: String? = null
        private var profile: String? = null
        private var alternatives: Boolean = true
        private var language: String? = null
        private var radiuses: String? = null
        private var bearings: String? = null
        private var continueStraight: Boolean = false
        private var roundaboutExits: Boolean? = null
        private var geometries: String? = null
        private var overview: String? = null
        private var steps: Boolean = true
        private var annotations: String? = null
        private var voiceInstructions: Boolean? = null
        private var bannerInstructions: Boolean? = null
        private var voiceUnits: String? = null
        private var accessToken: String? = null
        private var requestUuid: String? = null
        private var exclude: String? = null
        private var approaches: String? = null
        private var waypointIndices: String? = null
        private var waypointNames: String? = null
        private var waypointTargets: String? = null
        private var walkingOptions: WalkingOptionsNavigation? = null

        fun baseUrl(baseUrl: String): Builder = also { this.baseUrl = baseUrl }

        fun user(user: String): Builder = also { this.user = user }

        fun profile(profile: String): Builder = also { this.profile = profile }

        fun origin(origin: Point): Builder =
            also { this._origin = RoutePointNavigation(origin, null, null) }

        fun origin(
            origin: Point,
            angle: Double?,
            tolerance: Double?
        ): Builder = also { this._origin = RoutePointNavigation(origin, angle, tolerance) }

        fun destination(destination: Point): Builder =
            also { this._destination = RoutePointNavigation(destination, null, null) }

        fun destination(
            destination: Point,
            angle: Double?,
            tolerance: Double?
        ): Builder =
            also { this._destination = RoutePointNavigation(destination, angle, tolerance) }

        fun addWaypoint(waypoint: Point): Builder =
            also { this._waypoints.add(RoutePointNavigation(waypoint, null, null)) }

        fun addWaypoint(
            waypoint: Point,
            angle: Double?,
            tolerance: Double?
        ): Builder = also { _waypoints.add(RoutePointNavigation(waypoint, angle, tolerance)) }

        fun alternatives(alternatives: Boolean): Builder = also { this.alternatives = alternatives }

        fun language(language: String): Builder = also { this.language = language }

        fun radiuses(radiuses: String): Builder = also { this.radiuses = radiuses }

        fun bearings(bearings: String): Builder = also { this.bearings = bearings }

        fun continueStraight(continueStraight: Boolean): Builder =
            also { this.continueStraight = continueStraight }

        fun roundaboutExits(roundaboutExits: Boolean): Builder =
            also { this.roundaboutExits = roundaboutExits }

        fun geometries(geometries: String): Builder = also { this.geometries = geometries }

        fun overview(overview: String): Builder = also { this.overview = overview }

        fun steps(steps: Boolean): Builder = also { this.steps = steps }

        fun annotations(annotations: String): Builder = also { this.annotations = annotations }

        fun voiceInstructions(voiceInstructions: Boolean): Builder =
            also { this.voiceInstructions = voiceInstructions }

        fun bannerInstructions(bannerInstructions: Boolean): Builder =
            also { this.bannerInstructions = bannerInstructions }

        fun voiceUnits(voiceUnits: String): Builder = also { this.voiceUnits = voiceUnits }

        fun accessToken(accessToken: String): Builder = also { this.accessToken = accessToken }

        fun requestUuid(requestUuid: String): Builder = also { this.requestUuid = requestUuid }

        fun exclude(exclude: String): Builder = also { this.exclude = exclude }

        fun approaches(vararg approaches: String): Builder =
            also { this.approaches = approaches.joinToString(separator = ";") }

        fun waypointIndices(indices: String): Builder = also { this.waypointIndices = indices }

        fun waypointNames(waypointNames: String): Builder =
            also { this.waypointNames = waypointNames }

        fun waypointTargets(waypointTargets: String?): Builder =
            also { this.waypointTargets = waypointTargets }

        fun walkingOptions(walkingOptions: WalkingOptionsNavigation): Builder =
            also { this.walkingOptions = walkingOptions }

        fun build(): RouteOptionsNavigation {
            checkFields()
            return RouteOptionsNavigation(
                baseUrl ?: RouteUrl.BASE_URL,
                user ?: RouteUrl.PROFILE_DEFAULT_USER,
                profile ?: RouteUrl.PROFILE_DRIVING,
                _origin,
                _waypoints,
                _destination,
                alternatives,
                language,
                radiuses,
                bearings,
                continueStraight,
                roundaboutExits,
                geometries,
                overview,
                steps,
                annotations,
                voiceInstructions,
                bannerInstructions,
                voiceUnits,
                accessToken,
                requestUuid,
                exclude,
                approaches,
                waypointIndices,
                waypointNames,
                waypointTargets,
                walkingOptions
            )
        }

        private fun checkFields() {
            check(::_origin.isInitialized) { "Property origin hasn't been initialized" }

            check(::_destination.isInitialized) { "Property destination hasn't been initialized" }
        }
    }
}
