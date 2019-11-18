package com.mapbox.navigation.base.route

import android.net.Uri
import com.mapbox.geojson.Point

class RouteUrl(
    val accessToken: String,
    val orgin: Point,
    val waypoints: List<Point>? = null,
    val destination: Point,
    val user: String = PROFILE_DEFAULT_USER,
    val profile: String = PROFILE_DRIVING,
    val steps: Boolean = true,
    val geometries: String = GEOMETRY_POLYLINE6,
    val overview: String = OVERVIEW_FULL,
    val voiceIntruction: Boolean = true,
    val bannerIntruction: Boolean = true,
    val roundaboutExits: Boolean = true,
    val enableRefresh: Boolean = true
) {

    companion object {
        const val BASE_URL = "https://api.mapbox.com"

        private const val QUERY_PARAM_ACCESS_TOKEN = "access_token"
        private const val QUERY_PARAM_STEPS = "steps"
        private const val QUERY_PARAM_GEOMERTY = "geometries"
        private const val QUERY_PARAM_OVERVIEW = "overview"
        private const val QUERY_PARAM_VOICE_INSTRUCTIONS = "voice_instructions"
        private const val QUERY_PARAM_BANNER_INSTRUCTIONS = "banner_instructions"
        private const val QUERY_PARAM_ROUNDABOUT_EXITS = "roundabout_exits"
        private const val QUERY_PARAM_ENABLE_REFRESH = "enable_refresh"

        /**
         * Mapbox default username.
         *
         * @since 1.0
         */
        const val PROFILE_DEFAULT_USER = "mapbox"
        /**
         * For car and motorcycle routing. This profile factors in current and historic traffic
         * conditions to avoid slowdowns.
         *
         * @since 1.0
         */
        const val PROFILE_DRIVING_TRAFFIC = "driving-traffic"

        /**
         * For car and motorcycle routing. This profile shows the fastest routes by preferring
         * high-speed roads like highways.
         *
         * @since 1.0
         */
        const val PROFILE_DRIVING = "driving"

        /**
         * For pedestrian and hiking routing. This profile shows the shortest path by using sidewalks
         * and trails.
         *
         * @since 1.0
         */
        const val PROFILE_WALKING = "walking"

        /**
         * For bicycle routing. This profile shows routes that are short and safe for cyclist, avoiding
         * highways and preferring streets with bike lanes.
         *
         * @since 1.0
         */
        const val PROFILE_CYCLING = "cycling"

        /**
         * Format to return route geometry will be an encoded polyline.
         *
         * @since 1.0
         */
        const val GEOMETRY_POLYLINE = "polyline"

        /**
         * Format to return route geometry will be an encoded polyline with precision 6.
         *
         * @since 1.0
         */
        const val GEOMETRY_POLYLINE6 = "polyline6"

        /**
         * A simplified version of the [.OVERVIEW_FULL] geometry. If not specified simplified is
         * the default.
         *
         * @since 1.0
         */
        const val OVERVIEW_SIMPLIFIED = "simplified"

        /**
         * The most detailed geometry available.
         *
         * @since 1.0
         */
        const val OVERVIEW_FULL = "full"

        /**
         * No overview geometry.
         *
         * @since 1.0
         */
        const val OVERVIEW_FALSE = "false"
    }

    fun getRequest(): Uri =
        Uri.Builder()
            .authority(BASE_URL)
            .appendPath("directions")
            .appendPath("v5")
            .appendPath(user)
            .appendPath(profile)
            .appendPath(retrieveCoordinates())
            .appendQueryParameter(QUERY_PARAM_ACCESS_TOKEN, accessToken)
            .appendQueryParameter(QUERY_PARAM_STEPS, steps.toString())
            .appendQueryParameter(QUERY_PARAM_GEOMERTY, geometries)
            .appendQueryParameter(QUERY_PARAM_OVERVIEW, overview)
            .appendQueryParameter(QUERY_PARAM_VOICE_INSTRUCTIONS, voiceIntruction.toString())
            .appendQueryParameter(QUERY_PARAM_BANNER_INSTRUCTIONS, bannerIntruction.toString())
            .appendQueryParameter(QUERY_PARAM_ROUNDABOUT_EXITS, roundaboutExits.toString())
            .appendQueryParameter(QUERY_PARAM_ENABLE_REFRESH, enableRefresh.toString())
            .build()

    private fun retrieveCoordinates(): String {
        val route: List<Point> = mutableListOf(orgin).also { mList ->
            waypoints?.let { mList.addAll(it) }
            mList.add(destination)
        }

        return route.joinToString(separator = ";") { "${it.longitude()},${it.latitude()}" }
    }
}
