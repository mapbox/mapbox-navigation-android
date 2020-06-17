package com.mapbox.navigation.base.internal.route

import android.net.Uri
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.geojson.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.URLDecoder

// Test doesn't check if fields are valid! It checks URL, params in URL, where they locate and if has right query keys
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RouteUrlTest {

    @Test
    fun checkBaseUrl() {
        setupRouteUrl()
            .checkContain(
                RouteUrl.BASE_URL +
                    "/${RouteUrl.BASE_URL_API_NAME}" +
                    "/${RouteUrl.BASE_URL_API_VERSION}/"
            )
    }

    @Test
    fun checkCoordinates() {
        val routeUrl = setupRouteUrl(
            origin = Point.fromLngLat(12.2, 43.4),
            waypoints = listOf(Point.fromLngLat(54.0, 90.01), Point.fromLngLat(32.9, 81.23)),
            destination = Point.fromLngLat(42.00210201, 13.123121)
        )

        assertNotNull(routeUrl.path)
        routeUrl.checkContain("/12.2,43.4;54.0,90.01;32.9,81.23;42.00210201,13.123121")
    }

    @Test
    fun checkUserAndProfile() {
        val routeUrl = setupRouteUrl()

        routeUrl.checkContain("/${RouteUrl.PROFILE_DEFAULT_USER}/${RouteUrl.PROFILE_DRIVING}/")
    }

    @Test
    fun checkNonDefaultUserAndProfile() {
        val routeUrl = setupRouteUrl(user = "vitalik", profile = RouteUrl.PROFILE_CYCLING)

        routeUrl.checkContain("/vitalik/${RouteUrl.PROFILE_CYCLING}/")
    }

    @Test
    fun checkQueries() {
        val token = "pk.token1212.dsda"
        val routeUri = setupRouteUrl(
            accessToken = token,
            steps = true,
            geometries = RouteUrl.GEOMETRY_POLYLINE,
            overview = RouteUrl.OVERVIEW_SIMPLIFIED,
            voiceIntruction = false,
            voiceUnit = RouteUrl.METRIC,
            bannerIntruction = true,
            roundaboutExits = true,
            enableRefresh = false,
            continueStraight = false,
            exclude = DirectionsCriteria.EXCLUDE_MOTORWAY,
            language = "en",
            bearings = "20,45;50,90",
            waypointNames = "One;Two",
            waypointIndices = "0;1",
            waypointTargets = "0.1212,2.02;",
            approaches = DirectionsCriteria.APPROACH_CURB,
            radiuses = "0.9;1.2",
            walkingSpeed = 0.2,
            walkwayBias = 9.0,
            alleyBias = 1.12
        )
        val expectedQueries =
            listOf(
                "access_token" to token,
                "steps" to "true",
                "geometries" to RouteUrl.GEOMETRY_POLYLINE,
                "overview" to RouteUrl.OVERVIEW_SIMPLIFIED,
                "voice_instructions" to "false",
                "voice_units" to RouteUrl.METRIC,
                "roundabout_exits" to "true",
                "enable_refresh" to "false",
                "continue_straight" to "false",
                "exclude" to DirectionsCriteria.EXCLUDE_MOTORWAY,
                "language" to "en",
                "bearings" to "20,45;50,90",
                "waypoint_names" to "One;Two",
                "waypoint_targets" to "0.1212,2.02;",
                "waypoints" to "0;1",
                "approaches" to DirectionsCriteria.APPROACH_CURB,
                "radiuses" to "0.9;1.2",
                "walking_speed" to "0.2",
                "walkway_bias" to "9.0",
                "alley_bias" to "1.12"
            )

        expectedQueries.forEach { (key, value) ->
            assertEquals("Check Query param '$key'", value, routeUri.getQueryParameter(key))
        }
    }

    private fun Uri.checkContain(string: String, decode: String? = "UTF-8") =
        assertTrue(
            this.toString()
                .let { url ->
                    decode?.let { decode -> URLDecoder.decode(url, decode) } ?: url
                }
                .contains(string)
        )

    private fun setupRouteUrl(
        accessToken: String = "",
        origin: Point = Point.fromLngLat(1.25, 1.14),
        waypoints: List<Point>? = null,
        destination: Point = Point.fromLngLat(2.22, 3.12),
        user: String = RouteUrl.PROFILE_DEFAULT_USER,
        profile: String = RouteUrl.PROFILE_DRIVING,
        steps: Boolean = true,
        geometries: String = RouteUrl.GEOMETRY_POLYLINE6,
        overview: String = RouteUrl.OVERVIEW_FULL,
        voiceIntruction: Boolean = true,
        voiceUnit: String = RouteUrl.METRIC,
        bannerIntruction: Boolean = true,
        roundaboutExits: Boolean = true,
        enableRefresh: Boolean = true,
        alternatives: Boolean = true,
        continueStraight: Boolean? = null,
        exclude: String? = null,
        language: String? = null,
        bearings: String? = null,
        waypointNames: String? = null,
        waypointTargets: String? = null,
        waypointIndices: String? = null,
        approaches: String? = null,
        radiuses: String? = null,
        walkingSpeed: Double? = null,
        walkwayBias: Double? = null,
        alleyBias: Double? = null
    ): Uri =
        RouteUrl(
            accessToken,
            origin,
            waypoints,
            destination,
            user,
            profile,
            steps,
            geometries,
            overview,
            voiceIntruction,
            voiceUnit,
            bannerIntruction,
            roundaboutExits,
            enableRefresh,
            alternatives,
            continueStraight,
            exclude,
            language,
            bearings,
            waypointNames,
            waypointTargets,
            waypointIndices,
            approaches,
            radiuses,
            walkingSpeed,
            walkwayBias,
            alleyBias
        ).getRequest()
}
