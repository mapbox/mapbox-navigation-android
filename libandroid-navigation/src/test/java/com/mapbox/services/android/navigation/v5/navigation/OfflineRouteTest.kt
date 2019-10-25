package com.mapbox.services.android.navigation.v5.navigation

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.geojson.Point
import com.mapbox.navigation.utils.extensions.inferDeviceLocale
import com.mapbox.services.android.navigation.v5.testsupport.Extensions
import com.mapbox.services.android.navigation.v5.testsupport.mockkStaticSupport
import io.mockk.every
import io.mockk.mockk
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class OfflineRouteTest {

    companion object {

        @BeforeClass
        @JvmStatic
        fun initialize() {
            mockkStaticSupport(Extensions.ContextEx)
        }
    }

    @Test
    fun addBicycleTypeIncludedInRequest() {
        val onlineBuilder = provideOnlineRouteBuilder()
        val offlineRoute = OfflineRoute.builder(onlineBuilder)
            .bicycleType(OfflineCriteria.BicycleType.ROAD).build()

        val offlineUrl = offlineRoute.buildUrl()

        assertTrue(offlineUrl.contains("bicycle_type=Road"))
    }

    @Test
    fun addCyclingSpeedIncludedInRequest() {
        val onlineBuilder = provideOnlineRouteBuilder()
        val offlineRoute = OfflineRoute.builder(onlineBuilder)
            .cyclingSpeed(10.0f).build()

        val offlineUrl = offlineRoute.buildUrl()

        assertTrue(offlineUrl.contains("cycling_speed=10.0"))
    }

    @Test
    fun addCyclewayBiasIncludedInRequest() {
        val onlineBuilder = provideOnlineRouteBuilder()
        val offlineRoute = OfflineRoute.builder(onlineBuilder)
            .cyclewayBias(0.0f).build()

        val offlineUrl = offlineRoute.buildUrl()

        assertTrue(offlineUrl.contains("cycleway_bias=0.0"))
    }

    @Test
    fun addHillBiasIncludedInRequest() {
        val onlineBuilder = provideOnlineRouteBuilder()
        val offlineRoute = OfflineRoute.builder(onlineBuilder)
            .hillBias(0.0f).build()

        val offlineUrl = offlineRoute.buildUrl()

        assertTrue(offlineUrl.contains("hill_bias=0.0"))
    }

    @Test
    fun addFerryBiasIncludedInRequest() {
        val onlineBuilder = provideOnlineRouteBuilder()
        val offlineRoute = OfflineRoute.builder(onlineBuilder)
            .ferryBias(0.0f).build()

        val offlineUrl = offlineRoute.buildUrl()

        assertTrue(offlineUrl.contains("ferry_bias=0.0"))
    }

    @Test
    fun addRoughSurfaceBiasIncludedInRequest() {
        val onlineBuilder = provideOnlineRouteBuilder()
        val offlineRoute = OfflineRoute.builder(onlineBuilder)
            .roughSurfaceBias(0.0f).build()

        val offlineUrl = offlineRoute.buildUrl()

        assertTrue(offlineUrl.contains("rough_surface_bias=0.0"))
    }

    @Test
    @Throws(UnsupportedEncodingException::class)
    fun addWaypointTypesIncludedInRequest() {
        val onlineBuilder = provideOnlineRouteBuilder()
        val waypointTypes = listOf(
            OfflineCriteria.WaypointType.BREAK,
            OfflineCriteria.WaypointType.THROUGH,
            null,
            OfflineCriteria.WaypointType.BREAK
        )
        val offlineRoute = OfflineRoute.builder(onlineBuilder)
            .waypointTypes(waypointTypes).build()
        val offlineUrl = offlineRoute.buildUrl()

        val offlineUrlDecoded = URLDecoder.decode(offlineUrl, "UTF-8")

        assertTrue(offlineUrlDecoded.contains("break;through;;break"))
    }

    private fun provideOnlineRouteBuilder(): com.mapbox.navigation.route.offboard.NavigationRoute.Builder {
        val context = mockk<Context>()
        every { context.inferDeviceLocale() } returns Locale.US
        return com.mapbox.navigation.route.offboard.NavigationRoute.builder(context)
            .accessToken("pk.XXX")
            .origin(Point.fromLngLat(1.0, 2.0))
            .addWaypoint(Point.fromLngLat(3.0, 2.0))
            .destination(Point.fromLngLat(1.0, 5.0))
            .profile(DirectionsCriteria.PROFILE_CYCLING)
    }
}
