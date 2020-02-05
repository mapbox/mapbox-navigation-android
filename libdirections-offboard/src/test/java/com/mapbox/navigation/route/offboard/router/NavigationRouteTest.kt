package com.mapbox.navigation.route.offboard.router

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.WalkingOptions
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.accounts.SkuTokenProvider
import com.mapbox.navigation.base.extensions.bearings
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.base.route.internal.RouteUrl
import com.mapbox.navigation.route.offboard.RouteBuilderProvider
import com.mapbox.navigation.utils.extensions.inferDeviceLocale
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import java.util.Locale
import junit.framework.Assert.assertNotNull
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class NavigationRouteTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            mockkStatic("com.mapbox.navigation.utils.extensions.ContextEx")
        }

        const val ACESS_TOKEN = "pk.XXX"
    }

    val origin: Point = Point.fromLngLat(0.0, 0.0)
    val destination: Point = Point.fromLngLat(1.0, 1.0)
    private val mockSkuTokenProvider = mockk<SkuTokenProvider>(relaxed = true)

    @MockK
    private lateinit var context: Context

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { context.inferDeviceLocale() } returns Locale.US
        every { mockSkuTokenProvider.obtainUrlWithSkuToken("/mock", 1) } returns ("/mock&sku=102jaksdhfj")
    }

    @Test
    @Throws(Exception::class)
    fun sanityTest() {
        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(
                provideDefaultRouteOptionsBuilder()
                    .accessToken(ACESS_TOKEN)
                    .coordinates(listOf(origin, destination))
                    .build()
            )
            .build()
        assertNotNull(navigationRoute)
    }

    @Test
    @Throws(Exception::class)
    fun changingDefaultValueToCustomWorksProperly() {
        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(
                provideDefaultRouteOptionsBuilder()
                    .accessToken(ACESS_TOKEN)
                    .coordinates(listOf(origin, destination))
                    .profile(DirectionsCriteria.PROFILE_CYCLING)
                    .build()
            )
            .build()

        assertThat(
            navigationRoute.cloneCall().request().url().toString(),
            containsString("/cycling/")
        )
    }

    @Test
    fun addApproachesIncludedInRequest() {
        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(
                provideDefaultRouteOptionsBuilder()
                    .accessToken(ACESS_TOKEN)
                    .coordinates(listOf(origin, destination))
                    .profile(DirectionsCriteria.PROFILE_CYCLING)
                    .approaches("${DirectionsCriteria.APPROACH_CURB};${DirectionsCriteria.APPROACH_UNRESTRICTED}")
                    .build()
            )
            .build()

        assertThat(
            navigationRoute.cloneCall().request().url().toString(),
            containsString("curb")
        )
    }

    @Test
    fun checksWaypointIndicesIncludedInRequest() {
        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(
                provideDefaultRouteOptionsBuilder()
                    .coordinates(
                        origin,
                        listOf(Point.fromLngLat(1.0, 3.0), Point.fromLngLat(1.0, 3.0)),
                        destination
                    )
                    .accessToken(ACESS_TOKEN)
                    .waypointIndices(arrayOf(0, 2, 3).joinToString(separator = ";"))
                    .build()
            )

            .build()

        assertThat(
            navigationRoute.cloneCall().request().url().toString(),
            containsString("waypoints")
        )
    }

    @Test
    fun addWaypointNamesIncludedInRequest() {
        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(
                provideDefaultRouteOptionsBuilder()
                    .accessToken(ACESS_TOKEN)
                    .coordinates(listOf(origin, destination))
                    .profile(DirectionsCriteria.PROFILE_CYCLING)
                    .waypointNames("Origin; Destination").build()
            )
            .build()
        assertThat(
            navigationRoute.cloneCall().request().url().toString(),
            containsString("Destination")
        )
    }

    @Test
    fun addWaypointTargetsIncludedInRequest() {
        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(
                provideDefaultRouteOptionsBuilder()
                    .accessToken(ACESS_TOKEN)
                    .coordinates(listOf(origin, destination))
                    .waypointTargets(
                        arrayOf(
                            Point.fromLngLat(0.99, 4.99),
                            Point.fromLngLat(1.99, 5.99)
                        ).joinToString(separator = ";") { "${it?.latitude()},${it?.longitude()}" }
                    ).build()
            )
            .build()

        assertThat(
            navigationRoute.cloneCall().request().url().toString(),
            containsString("waypoint_targets")
        )
    }

    @Test
    fun reverseOriginDestination_bearingsAreFormattedCorrectly() {
        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(
                provideDefaultRouteOptionsBuilder()
                    .accessToken(ACESS_TOKEN)
                    .coordinates(
                        origin = Point.fromLngLat(1.0, 2.0),
                        destination = Point.fromLngLat(1.0, 5.0)
                    )
                    .bearings(Pair(90.0, 90.0), null)
                    .build()
            )
            .build()

        val requestUrl = navigationRoute.cloneCall().request().url().toString()

        assertThat(requestUrl, containsString("bearings=90%2C90"))
    }

    @Test
    fun addWaypointsThenOriginDestination_bearingsAreFormattedCorrectly() {
        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(
                provideDefaultRouteOptionsBuilder()
                    .accessToken(ACESS_TOKEN)
                    .coordinates(
                        Point.fromLngLat(1.0, 2.0),
                        listOf(Point.fromLngLat(3.0, 4.0), Point.fromLngLat(5.0, 6.0)),
                        Point.fromLngLat(7.0, 8.0)
                    )
                    .bearings(Pair(10.0, 10.0), Pair(20.0, 20.0), Pair(30.0, 30.0), Pair(40.0, 40.0))
                    .build()
            )
            .build()

        val requestUrl = navigationRoute.cloneCall().request().url().toString()

        assertThat(requestUrl, containsString("bearings=10%2C10%3B20%2C20%3B30%2C30%3B40%2C40"))
    }

    @Test
    fun addRouteOptionsIncludedInRequest() {
        val routeOptions = provideDefaultRouteOptionsBuilder()
            .accessToken(ACESS_TOKEN)
            .baseUrl("https://api-directions-traf.com")
            .requestUuid("XYZ_UUID")
            .alternatives(true)
            .language(Locale.US.language)
            .profile(DirectionsCriteria.PROFILE_WALKING)
            .voiceUnits(DirectionsCriteria.METRIC)
            .user("example_user")
            .geometries("mocked_geometries")
            .approaches("curb;;unrestricted")
            .waypointNames("Origin;Pickup;Destination")
            .waypointTargets(";;0.99,4.99")
            .waypointIndices("0;2")
            .walkingOptions(
                WalkingOptions.builder().alleyBias(0.6).walkwayBias(0.7).walkingSpeed(
                    1.0
                ).build()
            )
            .coordinates(
                Point.fromLngLat(1.0, 2.0),
                listOf(Point.fromLngLat(1.0, 3.0)),
                Point.fromLngLat(1.0, 5.0)
            )
            .build()

        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(routeOptions)
            .build()

        val request = navigationRoute.cloneCall().request().url().toString()
        assertThat(request, containsString("https://api-directions-traf.com"))
        assertThat(request, containsString("alternatives=true"))
        assertThat(request, containsString(ACESS_TOKEN))
        assertThat(request, containsString("voice_units=metric"))
        assertThat(request, containsString("example_user"))
        assertThat(request, containsString("language=en"))
        assertThat(request, containsString("walking"))
        assertThat(request, containsString("curb"))
        assertThat(request, containsString("Origin"))
        assertThat(request, containsString("waypoint_targets"))
        assertThat(request, containsString("alley_bias"))
        assertThat(request, containsString("walkway_bias"))
        assertThat(request, containsString("walking_speed"))
    }

    private fun provideNavigationOffboardRouteBuilder() =
        RouteBuilderProvider.getBuilder(ACESS_TOKEN, context, mockSkuTokenProvider)

    private fun provideDefaultRouteOptionsBuilder() =
        RouteOptions.builder()
            .baseUrl(RouteUrl.BASE_URL)
            .user(RouteUrl.PROFILE_DEFAULT_USER)
            .profile(RouteUrl.PROFILE_DRIVING)
            .coordinates(emptyList())
            .geometries("")
            .requestUuid("")
}
