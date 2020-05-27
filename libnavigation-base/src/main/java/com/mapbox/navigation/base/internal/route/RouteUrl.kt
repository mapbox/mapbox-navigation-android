package com.mapbox.navigation.base.internal.route

import android.net.Uri
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.utils.ifNonNull

class RouteUrl(
    val accessToken: String,
    val origin: Point,
    val waypoints: List<Point>? = null,
    val destination: Point,
    val user: String = PROFILE_DEFAULT_USER,
    val profile: String = PROFILE_DRIVING,
    val steps: Boolean = STEPS_DEFAULT_VALUE,
    val geometries: String = GEOMETRY_POLYLINE6,
    val overview: String = OVERVIEW_FULL,
    val voiceInstruction: Boolean = VOICE_INSTRUCTION_DEFAULT_VALUE,
    val voiceUnits: String? = null,
    val bannerInstruction: Boolean = BANNER_INSTRUCTION_DEFAULT_VALUE,
    val roundaboutExits: Boolean = ROUNDABOUT_EXITS_DEFAULT_VALUE,
    val enableRefresh: Boolean = ENABLE_REFRESH_DEFAULT_VALUE,
    val alternatives: Boolean = ALTERNATIVES_DEFAULT_VALUE,
    val continueStraight: Boolean? = null,
    val exclude: String? = null,
    val language: String? = null,
    val bearings: String? = null,
    val waypointNames: String? = null,
    val waypointTargets: String? = null,
    val waypointIndices: String? = null,
    val approaches: String? = null,
    val radiuses: String? = null,
    val walkingSpeed: Double? = null,
    val walkwayBias: Double? = null,
    val alleyBias: Double? = null
) {

    companion object {
        const val BASE_URL = "https://api.mapbox.com"

        const val BASE_URL_API_NAME = "directions"
        const val BASE_URL_API_VERSION = "v5"

        private const val QUERY_PARAM_ACCESS_TOKEN = "access_token"
        private const val QUERY_PARAM_STEPS = "steps"
        private const val QUERY_PARAM_GEOMERTY = "geometries"
        private const val QUERY_PARAM_OVERVIEW = "overview"
        private const val QUERY_PARAM_VOICE_INSTRUCTIONS = "voice_instructions"
        private const val QUERY_PARAM_VOICE_UNITS = "voice_units"
        private const val QUERY_PARAM_BANNER_INSTRUCTIONS = "banner_instructions"
        private const val QUERY_PARAM_ROUNDABOUT_EXITS = "roundabout_exits"
        private const val QUERY_PARAM_ENABLE_REFRESH = "enable_refresh"
        private const val QUERY_PARAM_ALTERNATIVES = "alternatives"
        private const val QUERY_PARAM_CONTINUE_STRAIGHT = "continue_straight"
        private const val QUERY_PARAM_EXCLUDE = "exclude"
        private const val QUERY_PARAM_LANGUAGE = "language"
        private const val QUERY_PARAM_BEARINGS = "bearings"
        private const val QUERY_PARAM_WAYPOINT_NAMES = "waypoint_names"
        private const val QUERY_PARAM_WAYPOINT_TARGETS = "waypoint_targets"
        private const val QUERY_PARAM_APPROACHES = "approaches"
        private const val QUERY_PARAM_RADIUSES = "radiuses"
        private const val QUERY_PARAM_WAYPOINT_INDICES = "waypoints"
        private const val QUERY_PARAM_WALKING_SPEED = "walking_speed"
        private const val QUERY_PARAM_WALKWAY_BIAS = "walkway_bias"
        private const val QUERY_PARAM_ALLEY_BIAS = "alley_bias"

        const val STEPS_DEFAULT_VALUE = true
        const val VOICE_INSTRUCTION_DEFAULT_VALUE = true
        const val BANNER_INSTRUCTION_DEFAULT_VALUE = true
        const val ROUNDABOUT_EXITS_DEFAULT_VALUE = true
        const val ENABLE_REFRESH_DEFAULT_VALUE = true
        const val ALTERNATIVES_DEFAULT_VALUE = true

        /**
         * Mapbox default username.
         */
        const val PROFILE_DEFAULT_USER = "mapbox"

        /**
         * For car and motorcycle routing. This profile factors in current and historic traffic
         * conditions to avoid slowdowns.
         */
        const val PROFILE_DRIVING_TRAFFIC = "driving-traffic"

        /**
         * For car and motorcycle routing. This profile shows the fastest routes by preferring
         * high-speed roads like highways.
         */
        const val PROFILE_DRIVING = "driving"

        /**
         * For pedestrian and hiking routing. This profile shows the shortest path by using sidewalks
         * and trails.
         */
        const val PROFILE_WALKING = "walking"

        /**
         * For bicycle routing. This profile shows routes that are short and safe for cyclist, avoiding
         * highways and preferring streets with bike lanes.
         */
        const val PROFILE_CYCLING = "cycling"

        /**
         * Format to return route geometry will be an encoded polyline.
         */
        const val GEOMETRY_POLYLINE = "polyline"

        /**
         * Format to return route geometry will be an encoded polyline with precision 6.
         */
        const val GEOMETRY_POLYLINE6 = "polyline6"

        /**
         * A simplified version of the [.OVERVIEW_FULL] geometry. If not specified simplified is
         * the default.
         */
        const val OVERVIEW_SIMPLIFIED = "simplified"

        /**
         * The most detailed geometry available.
         */
        const val OVERVIEW_FULL = "full"

        /**
         * No overview geometry.
         */
        const val OVERVIEW_FALSE = "false"

        /**
         * Change the units to imperial for voice and visual information. Note that this won't change
         * other results such as raw distance measurements which will always be returned in meters.
         */
        const val IMPERIAL = "imperial"

        /**
         * Change the units to metric for voice and visual information. Note that this won't change
         * other results such as raw distance measurements which will always be returned in meters.
         */
        const val METRIC = "metric"
    }

    fun getRequest(): Uri =
        Uri.parse(BASE_URL)
            .buildUpon()
            .appendPath(BASE_URL_API_NAME)
            .appendPath(BASE_URL_API_VERSION)
            .appendPath(user)
            .appendPath(profile)
            .appendPath(retrieveCoordinates())
            .appendQueryParameter(QUERY_PARAM_ACCESS_TOKEN, accessToken)
            .appendQueryParameter(QUERY_PARAM_STEPS, steps.toString())
            .appendQueryParameter(QUERY_PARAM_GEOMERTY, geometries)
            .appendQueryParameter(QUERY_PARAM_OVERVIEW, overview)
            .appendQueryParameter(QUERY_PARAM_VOICE_INSTRUCTIONS, voiceInstruction.toString())
            .appendQueryParameterIfNonNull(QUERY_PARAM_VOICE_UNITS, voiceUnits)
            .appendQueryParameter(QUERY_PARAM_BANNER_INSTRUCTIONS, bannerInstruction.toString())
            .appendQueryParameter(QUERY_PARAM_ROUNDABOUT_EXITS, roundaboutExits.toString())
            .appendQueryParameter(QUERY_PARAM_ENABLE_REFRESH, enableRefresh.toString())
            .appendQueryParameter(QUERY_PARAM_ALTERNATIVES, alternatives.toString())
            .appendQueryParameterIfNonNull(QUERY_PARAM_CONTINUE_STRAIGHT, continueStraight)
            .appendQueryParameterIfNonNull(QUERY_PARAM_EXCLUDE, exclude)
            .appendQueryParameterIfNonNull(QUERY_PARAM_LANGUAGE, language)
            .appendQueryParameterIfNonNull(QUERY_PARAM_BEARINGS, bearings)
            .appendQueryParameterIfNonNull(QUERY_PARAM_WAYPOINT_NAMES, waypointNames)
            .appendQueryParameterIfNonNull(QUERY_PARAM_WAYPOINT_TARGETS, waypointTargets)
            .appendQueryParameterIfNonNull(QUERY_PARAM_WAYPOINT_INDICES, waypointIndices)
            .appendQueryParameterIfNonNull(QUERY_PARAM_APPROACHES, approaches)
            .appendQueryParameterIfNonNull(QUERY_PARAM_RADIUSES, radiuses)
            .appendQueryParameterIfNonNull(QUERY_PARAM_WALKING_SPEED, walkingSpeed)
            .appendQueryParameterIfNonNull(QUERY_PARAM_WALKWAY_BIAS, walkwayBias)
            .appendQueryParameterIfNonNull(QUERY_PARAM_ALLEY_BIAS, alleyBias)
            .build()

    private fun Uri.Builder.appendQueryParameterIfNonNull(key: String, value: Any?) = apply {
        ifNonNull(value) {
            appendQueryParameter(key, it.toString())
        }
    }

    private fun retrieveCoordinates(): String {
        val route: List<Point> = listOf(origin) + (waypoints ?: emptyList()) + destination

        return route.joinToString(separator = ";") { "${it.longitude()},${it.latitude()}" }
    }
}
