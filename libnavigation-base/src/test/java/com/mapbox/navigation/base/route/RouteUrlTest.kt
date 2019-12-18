package com.mapbox.navigation.base.route

import android.net.Uri
import com.mapbox.geojson.Point
import java.net.URLDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RouteUrlTest {

    @Test
    fun checkBaseUrl() {
        setupRouteUrl()
            .checkContain("${RouteUrl.BASE_URL}/${RouteUrl.BASE_URL_P0}/${RouteUrl.BASE_URL_P1}/")
    }

    @Test
    fun checkCoordinates() {
        val routeUrl = setupRouteUrl(
            orgin = Point.fromLngLat(12.2, 43.4),
            waypoints = listOf(Point.fromLngLat(54.0, 90.01), Point.fromLngLat(32.9, 81.23)),
            destination = Point.fromLngLat(42.00210201, 13.123121)
        )

        assertNotNull(routeUrl.path)
        routeUrl.path!!.contains(
            "/12.2,43.4;54.0,90.01;32.9,81.23;42.00210201,13.123121"
        )
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
        val token = "pk_token1212.dsda"
        val routeUri = setupRouteUrl(
            accessToken = token,
            steps = true,
            geometries = RouteUrl.GEOMETRY_POLYLINE,
            overview = RouteUrl.OVERVIEW_SIMPLIFIED,
            voiceIntruction = false,
            bannerIntruction = true,
            roundaboutExits = true,
            enableRefresh = false
        )
        val expectedQueries =
            listOf(
                "access_token" to token,
                "steps" to "true",
                "geometries" to RouteUrl.GEOMETRY_POLYLINE,
                "overview" to RouteUrl.OVERVIEW_SIMPLIFIED,
                "voice_instructions" to "false",
                "roundabout_exits" to "true",
                "enable_refresh" to "false"
            )

        expectedQueries.forEach { (key, value) ->
            assertEquals("Check Query param", value, routeUri.getQueryParameter(key))
        }
    }

    private fun Uri.checkContain(string: String, decode: String? = "UTF-8") =
        assertTrue(this.toString()
            .let { url ->
                decode?.let { decode -> URLDecoder.decode(url, decode) } ?: url
            }
            .contains(string)
        )

    private fun setupRouteUrl(
        accessToken: String = "",
        orgin: Point = Point.fromLngLat(.0, .0),
        waypoints: List<Point>? = null,
        destination: Point = Point.fromLngLat(.0, .0),
        user: String = RouteUrl.PROFILE_DEFAULT_USER,
        profile: String = RouteUrl.PROFILE_DRIVING,
        steps: Boolean = true,
        geometries: String = RouteUrl.GEOMETRY_POLYLINE6,
        overview: String = RouteUrl.OVERVIEW_FULL,
        voiceIntruction: Boolean = true,
        bannerIntruction: Boolean = true,
        roundaboutExits: Boolean = true,
        enableRefresh: Boolean = true
    ): Uri =
        RouteUrl(
            accessToken,
            orgin,
            waypoints,
            destination,
            user,
            profile,
            steps,
            geometries,
            overview,
            voiceIntruction,
            bannerIntruction,
            roundaboutExits,
            enableRefresh
        ).getRequest()
}
