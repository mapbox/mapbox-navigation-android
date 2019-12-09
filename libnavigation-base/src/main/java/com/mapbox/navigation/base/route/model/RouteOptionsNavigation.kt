package com.mapbox.navigation.base.route.model

import com.google.gson.annotations.SerializedName
import com.mapbox.geojson.Point

class RouteOptionsNavigation private constructor(
    val baseUrl: String?,
    val user: String?,
    val profile: String?,
    val coordinates: List<RoutePointNavigation>,
    val alternatives: Boolean?,
    val language: String?,
    val radiuses: String?,
    val bearings: String?,
    @SerializedName("continue_straight") val continueStraight: Boolean?,
    @SerializedName("roundabout_exits") val roundaboutExits: Boolean?,
    val geometries: String?,
    val overview: String?,
    val steps: Boolean?,
    val annotations: String?,
    @SerializedName("voice_instructions") val voiceInstructions: Boolean?,
    @SerializedName("banner_instructions") val bannerInstructions: Boolean?,
    @SerializedName("voice_units") val voiceUnits: String?,
    val accessToken: String?,
    @SerializedName("uuid") val requestUuid: String?,
    val exclude: String?,
    val approaches: String?,
    @SerializedName("waypoints") val waypointIndices: String?,
    @SerializedName("waypoint_names") val waypointNames: String?,
    @SerializedName("waypoint_targets") val waypointTargets: String?,
    val walkingOptions: WalkingOptionsNavigation?
) {

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }

    class Builder internal constructor() {
        private var _origin: RoutePointNavigation? = null
        private var _destination: RoutePointNavigation? = null
        private val _waypoints = mutableListOf<RoutePointNavigation>()

        private var baseUrl: String? = null
        private var user: String? = null
        private var profile: String? = null
        private val coordinates = mutableListOf<RoutePointNavigation>()
        private var alternatives: Boolean? = null
        private var language: String? = null
        private var radiuses: String? = null
        private var bearings: String? = null
        private var continueStraight: Boolean? = null
        private var roundaboutExits: Boolean? = null
        private var geometries: String? = null
        private var overview: String? = null
        private var steps: Boolean? = null
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
            assembleCoordinates()
            return RouteOptionsNavigation(
                baseUrl,
                user,
                profile,
                coordinates,
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

        private fun assembleCoordinates() {
            _origin?.let { origin ->
                coordinates.add(origin)
            }

            for (waypoint in _waypoints) {
                coordinates.add(waypoint)
            }

            _destination?.let { destination ->
                coordinates.add(destination)
            }
        }
    }
}
