package com.mapbox.navigation.route.onboard

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.internal.RouteUrl
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class OfflineRouteTest {

    @Test
    fun addBicycleTypeIncludedInRequest() {
        val routeUrl = provideOnlineRouteBuilder()
        val offlineRoute = OfflineRoute.builder(routeUrl)
            .bicycleType(OfflineCriteria.BicycleType.ROAD).build()

        val offlineUrl = offlineRoute.buildUrl()

        assertTrue(offlineUrl.contains("bicycle_type=Road"))
    }

    @Test
    fun addCyclingSpeedIncludedInRequest() {
        val routeUrl = provideOnlineRouteBuilder()
        val offlineRoute = OfflineRoute.builder(routeUrl)
            .cyclingSpeed(10.0f).build()

        val offlineUrl = offlineRoute.buildUrl()

        assertTrue(offlineUrl.contains("cycling_speed=10.0"))
    }

    @Test
    fun addCyclewayBiasIncludedInRequest() {
        val routeUrl = provideOnlineRouteBuilder()
        val offlineRoute = OfflineRoute.builder(routeUrl)
            .cyclewayBias(0.0f).build()

        val offlineUrl = offlineRoute.buildUrl()

        assertTrue(offlineUrl.contains("cycleway_bias=0.0"))
    }

    @Test
    fun addHillBiasIncludedInRequest() {
        val routeUrl = provideOnlineRouteBuilder()
        val offlineRoute = OfflineRoute.builder(routeUrl)
            .hillBias(0.0f).build()

        val offlineUrl = offlineRoute.buildUrl()

        assertTrue(offlineUrl.contains("hill_bias=0.0"))
    }

    @Test
    fun addFerryBiasIncludedInRequest() {
        val routeUrl = provideOnlineRouteBuilder()
        val offlineRoute = OfflineRoute.builder(routeUrl)
            .ferryBias(0.0f).build()

        val offlineUrl = offlineRoute.buildUrl()

        assertTrue(offlineUrl.contains("ferry_bias=0.0"))
    }

    @Test
    fun addRoughSurfaceBiasIncludedInRequest() {
        val routeUrl = provideOnlineRouteBuilder()
        val offlineRoute = OfflineRoute.builder(routeUrl)
            .roughSurfaceBias(0.0f).build()

        val offlineUrl = offlineRoute.buildUrl()

        assertTrue(offlineUrl.contains("rough_surface_bias=0.0"))
    }

    @Test
    @Throws(UnsupportedEncodingException::class)
    fun addWaypointTypesIncludedInRequest() {
        val routeUrl = provideOnlineRouteBuilder()
        val waypointTypes = listOf(
            OfflineCriteria.WaypointType.BREAK,
            OfflineCriteria.WaypointType.THROUGH,
            null,
            OfflineCriteria.WaypointType.BREAK
        )
        val offlineRoute = OfflineRoute.builder(routeUrl)
            .waypointTypes(waypointTypes).build()
        val offlineUrl = offlineRoute.buildUrl()

        val offlineUrlDecoded = URLDecoder.decode(offlineUrl, "UTF-8")

        assertTrue(offlineUrlDecoded.contains("break;through;;break"))
    }

    private fun provideOnlineRouteBuilder(): RouteUrl {
        return RouteUrl(
            accessToken = "pk.XXX",
            profile = RouteUrl.PROFILE_CYCLING,
            origin = Point.fromLngLat(1.0, 2.0),
            waypoints = listOf(Point.fromLngLat(3.0, 2.0)),
            destination = Point.fromLngLat(1.0, 5.0)
        )
    }
}
