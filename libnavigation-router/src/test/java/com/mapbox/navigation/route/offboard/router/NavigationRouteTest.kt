package com.mapbox.navigation.route.offboard.router

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.WalkingOptions
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.constants.Constants
import com.mapbox.core.exceptions.ServicesException
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.route.offboard.RouteBuilderProvider
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import okhttp3.Request
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.util.Locale

class NavigationRouteTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            mockkStatic("com.mapbox.navigation.base.internal.extensions.ContextEx")
        }

        const val ACESS_TOKEN = "pk.XXX"
    }

    @get:Rule
    val expectedException = ExpectedException.none()

    private val origin: Point = Point.fromLngLat(0.0, 0.0)
    private val destination: Point = Point.fromLngLat(1.0, 1.0)
    private val mockSkuTokenProvider = mockk<UrlSkuTokenProvider>(relaxed = true)

    @MockK
    private lateinit var context: Context

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { context.inferDeviceLocale() } returns Locale.US
        every {
            mockSkuTokenProvider.obtainUrlWithSkuToken(any())
        } returns (mockk())
    }

    @Test
    @Throws(Exception::class)
    fun sanityTest() {
        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(
                provideDefaultRouteOptionsBuilder()
                    .accessToken(ACESS_TOKEN)
                    .coordinates(listOf(origin, destination))
                    .build(),
                refreshEnabled = true
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
                    .build(),
                refreshEnabled = true
            )
            .build()

        assertThat(
            (navigationRoute.cloneCall().request() as Request).url.toString(),
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
                    .approaches(
                        "${DirectionsCriteria.APPROACH_CURB}" +
                            ";${DirectionsCriteria.APPROACH_UNRESTRICTED}"
                    )
                    .build(),
                refreshEnabled = true
            )
            .build()

        assertThat(
            (navigationRoute.cloneCall().request() as Request).url.toString(),
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
                    .build(),
                refreshEnabled = true
            )

            .build()

        assertThat(
            (navigationRoute.cloneCall().request() as Request).url.toString(),
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
                    .waypointNames("Origin; Destination").build(),
                refreshEnabled = true
            )
            .build()
        assertThat(
            (navigationRoute.cloneCall().request() as Request).url.toString(),
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
                    ).build(),
                refreshEnabled = true
            )
            .build()

        assertThat(
            (navigationRoute.cloneCall().request() as Request).url.toString(),
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
                    .bearingsList(listOf(listOf(90.0, 90.0), listOf(null, null)))
                    .build(),
                refreshEnabled = true
            )
            .build()

        val requestUrl = (navigationRoute.cloneCall().request() as Request).url.toString()

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
                    .bearingsList(
                        listOf(
                            listOf(10.0, 10.0),
                            listOf(20.0, 20.0),
                            listOf(30.0, 30.0),
                            listOf(40.0, 40.0)
                        )
                    )
                    .build(),
                refreshEnabled = true
            )
            .build()

        val requestUrl = (navigationRoute.cloneCall().request() as Request).url.toString()

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
            .routeOptions(
                routeOptions,
                refreshEnabled = true
            )
            .build()

        val request = (navigationRoute.cloneCall().request() as Request).url.toString()
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

    @Test
    fun snappingClosuresIncludedInRequest() {
        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(
                provideDefaultRouteOptionsBuilder()
                    .accessToken(ACESS_TOKEN)
                    .coordinates(
                        origin,
                        listOf(Point.fromLngLat(1.0, 3.0), Point.fromLngLat(1.0, 3.0)),
                        destination
                    )
                    .snappingClosures(listOf(true, null, false, true))
                    .build(),
                refreshEnabled = true
            )
            .build()

        val requestUrl = (navigationRoute.cloneCall().request() as Request).url.toString()

        assertThat(requestUrl, containsString("snapping_include_closures=true%3B%3Bfalse%3Btrue"))
    }

    @Test
    fun snappingClosuresMustMatchCoordinatesSize() {
        expectedException.expect(ServicesException::class.java)
        expectedException.expectMessage(
            "Number of snapping closures elements must match number of coordinates provided."
        )

        provideNavigationOffboardRouteBuilder()
            .routeOptions(
                provideDefaultRouteOptionsBuilder()
                    .accessToken(ACESS_TOKEN)
                    .coordinates(origin = origin, destination = destination)
                    .snappingClosures(listOf(true, null, true))
                    .build(),
                refreshEnabled = true
            )
            .build()
    }

    @Test
    fun snappingClosuresEmptyListNotIncludedInRequest() {
        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(
                provideDefaultRouteOptionsBuilder()
                    .accessToken(ACESS_TOKEN)
                    .coordinates(origin = origin, destination = destination)
                    .snappingClosures(emptyList())
                    .build(),
                refreshEnabled = true
            )
            .build()

        val requestUrl = (navigationRoute.cloneCall().request() as Request).url.toString()

        assertFalse(requestUrl.contains("snapping_include_closures"))
    }

    @Test
    fun snappingClosuresEmptyStringNotIncludedInRequest() {
        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(
                provideDefaultRouteOptionsBuilder()
                    .accessToken(ACESS_TOKEN)
                    .coordinates(origin = origin, destination = destination)
                    .snappingClosures("")
                    .build(),
                refreshEnabled = true
            )
            .build()

        val requestUrl = (navigationRoute.cloneCall().request() as Request).url.toString()

        assertFalse(requestUrl.contains("snapping_include_closures"))
    }

    private fun provideNavigationOffboardRouteBuilder() =
        RouteBuilderProvider.getBuilder(context, mockSkuTokenProvider)

    private fun provideDefaultRouteOptionsBuilder() =
        RouteOptions.builder()
            .baseUrl(Constants.BASE_API_URL)
            .user(Constants.MAPBOX_USER)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .coordinates(emptyList())
            .geometries("")
            .requestUuid("")
}
