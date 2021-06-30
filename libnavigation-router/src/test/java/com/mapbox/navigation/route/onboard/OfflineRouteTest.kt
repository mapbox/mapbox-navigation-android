package com.mapbox.navigation.route.onboard

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.route.internal.util.httpUrl
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.UnsupportedEncodingException
import java.net.URL
import java.net.URLDecoder
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
internal class OfflineRouteTest : BuilderTest<OfflineRoute, OfflineRoute.Builder>() {

    override fun getImplementationClass(): KClass<OfflineRoute> = OfflineRoute::class

    override fun getFilledUpBuilder(): OfflineRoute.Builder {
        return OfflineRoute.Builder(mockk(relaxed = true))
            .bicycleType(mockk(relaxed = true))
            .cyclingSpeed(123f)
            .cyclewayBias(456f)
            .hillBias(789f)
            .ferryBias(101112f)
            .roughSurfaceBias(131415f)
            .waypointTypes(mockk(relaxed = true))
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }

    @Test
    fun addBicycleTypeIncludedInRequest() {
        val routeUrl = provideOnlineRouteBuilder()
        val offlineRoute = OfflineRoute.Builder(routeUrl)
            .bicycleType(OfflineCriteria.BicycleType.ROAD).build()

        val offlineUrl = offlineRoute.buildUrl()

        assertTrue(offlineUrl.contains("bicycle_type=Road"))
    }

    @Test
    fun addCyclingSpeedIncludedInRequest() {
        val routeUrl = provideOnlineRouteBuilder()
        val offlineRoute = OfflineRoute.Builder(routeUrl)
            .cyclingSpeed(10.0f).build()

        val offlineUrl = offlineRoute.buildUrl()

        assertTrue(offlineUrl.contains("cycling_speed=10.0"))
    }

    @Test
    fun addCyclewayBiasIncludedInRequest() {
        val routeUrl = provideOnlineRouteBuilder()
        val offlineRoute = OfflineRoute.Builder(routeUrl)
            .cyclewayBias(0.0f).build()

        val offlineUrl = offlineRoute.buildUrl()

        assertTrue(offlineUrl.contains("cycleway_bias=0.0"))
    }

    @Test
    fun addHillBiasIncludedInRequest() {
        val routeUrl = provideOnlineRouteBuilder()
        val offlineRoute = OfflineRoute.Builder(routeUrl)
            .hillBias(0.0f).build()

        val offlineUrl = offlineRoute.buildUrl()

        assertTrue(offlineUrl.contains("hill_bias=0.0"))
    }

    @Test
    fun addFerryBiasIncludedInRequest() {
        val routeUrl = provideOnlineRouteBuilder()
        val offlineRoute = OfflineRoute.Builder(routeUrl)
            .ferryBias(0.0f).build()

        val offlineUrl = offlineRoute.buildUrl()

        assertTrue(offlineUrl.contains("ferry_bias=0.0"))
    }

    @Test
    fun addRoughSurfaceBiasIncludedInRequest() {
        val routeUrl = provideOnlineRouteBuilder()
        val offlineRoute = OfflineRoute.Builder(routeUrl)
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
        val offlineRoute = OfflineRoute.Builder(routeUrl)
            .waypointTypes(waypointTypes).build()
        val offlineUrl = offlineRoute.buildUrl()

        val offlineUrlDecoded = URLDecoder.decode(offlineUrl, "UTF-8")

        assertTrue(offlineUrlDecoded.contains("break;through;;break"))
    }

    private fun provideOnlineRouteBuilder(): URL =
        MapboxDirections.builder()
            .routeOptions(
                RouteOptions.builder()
                    .accessToken("pk.XXX")
                    .profile(DirectionsCriteria.PROFILE_CYCLING)
                    .coordinates(
                        origin = Point.fromLngLat(1.0, 2.0),
                        waypoints = listOf(Point.fromLngLat(3.0, 2.0)),
                        destination = Point.fromLngLat(1.0, 5.0)
                    )
                    .build()
            )
            .build()
            .httpUrl()
            .toUrl()
}
