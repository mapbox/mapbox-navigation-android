package com.mapbox.navigation.route.offboard.router

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.model.RouteOptionsNavigation
import com.mapbox.navigation.base.route.model.WalkingOptionsNavigation
import com.mapbox.navigation.utils.extensions.inferDeviceLocale
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import java.util.Locale
import junit.framework.Assert.assertNotNull
import okhttp3.EventListener
import okhttp3.Interceptor
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import retrofit2.Call

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

    @MockK
    private lateinit var context: Context

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { context.inferDeviceLocale() } returns Locale.US
    }

    @Test
    @Throws(Exception::class)
    fun sanityTest() {
        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(
                RouteOptionsNavigation.builder().accessToken(ACESS_TOKEN)
                    .origin(origin)
                    .destination(destination).build()
            )
            .build()
        assertNotNull(navigationRoute)
    }

    @Test
    @Throws(Exception::class)
    fun changingDefaultValueToCustomWorksProperly() {
        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(
                RouteOptionsNavigation.builder().accessToken(ACESS_TOKEN)
                    .origin(origin)
                    .destination(destination)
                    .profile(DirectionsCriteria.PROFILE_CYCLING)
                    .build()
            )
            .build()

        assertThat(
            navigationRoute.call.request().url().toString(),
            containsString("/cycling/")
        )
    }

    @Test
    fun addApproachesIncludedInRequest() {
        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(
                RouteOptionsNavigation.builder()
                    .accessToken(ACESS_TOKEN)
                    .origin(origin)
                    .profile(DirectionsCriteria.PROFILE_CYCLING)
                    .destination(destination)
                    .approaches(
                        DirectionsCriteria.APPROACH_CURB,
                        DirectionsCriteria.APPROACH_UNRESTRICTED
                    )
                    .build()
            )
            .build()

        assertThat(
            navigationRoute.call.request().url().toString(),
            containsString("curb")
        )
    }

    @Test
    fun checksWaypointIndicesIncludedInRequest() {
        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(
                RouteOptionsNavigation.builder()
                    .origin(origin)
                    .destination(destination)
                    .addWaypoint(Point.fromLngLat(1.0, 3.0))
                    .addWaypoint(Point.fromLngLat(1.0, 3.0))
                    .accessToken(ACESS_TOKEN)
                    .waypointIndices(arrayOf(0, 2, 3).joinToString(separator = ";"))
                    .build()
            )
            .build()

        assertThat(
            navigationRoute.call.request().url().toString(),
            containsString("waypoints")
        )
    }

    @Test
    fun addWaypointNamesIncludedInRequest() {
        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(
                RouteOptionsNavigation.builder().accessToken(ACESS_TOKEN)
                    .origin(origin)
                    .profile(DirectionsCriteria.PROFILE_CYCLING)
                    .destination(destination)
                    .waypointNames("Origin; Destination").build()
            )
            .build()
        assertThat(
            navigationRoute.call.request().url().toString(),
            containsString("Destination")
        )
    }

    @Test
    fun addWaypointTargetsIncludedInRequest() {
        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(
                RouteOptionsNavigation.builder().accessToken(ACESS_TOKEN)
                    .origin(origin)
                    .destination(destination)
                    .waypointTargets(
                        arrayOf(
                            Point.fromLngLat(0.99, 4.99),
                            Point.fromLngLat(1.99, 5.99)
                        ).joinToString(separator = ";") { "${it?.latitude()},${it?.longitude()}" }
                    ).build()
            )
            .build()

        assertThat(
            navigationRoute.call.request().url().toString(),
            containsString("waypoint_targets")
        )
    }

    @Test
    fun reverseOriginDestination_bearingsAreFormattedCorrectly() {
        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(
                RouteOptionsNavigation.builder()
                    .destination(Point.fromLngLat(1.0, 5.0), 1.0, 5.0)
                    .origin(Point.fromLngLat(1.0, 2.0), 90.0, 90.0)
                    .accessToken(ACESS_TOKEN).build()
            )
            .build()

        val requestUrl = navigationRoute.call.request().url().toString()

        assertThat(requestUrl, containsString("bearings=90%2C90%3B1%2C5"))
    }

    @Test
    fun addWaypointsThenOriginDestination_bearingsAreFormattedCorrectly() {
        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(
                RouteOptionsNavigation.builder()
                    .accessToken(ACESS_TOKEN)
                    .addWaypoint(Point.fromLngLat(3.0, 4.0), 20.0, 20.0)
                    .addWaypoint(Point.fromLngLat(5.0, 6.0), 30.0, 30.0)
                    .destination(Point.fromLngLat(7.0, 8.0), 40.0, 40.0)
                    .origin(Point.fromLngLat(1.0, 2.0), 10.0, 10.0)
                    .build()
            )
            .build()

        val requestUrl = navigationRoute.call.request().url().toString()

        assertThat(requestUrl, containsString("bearings=10%2C10%3B20%2C20%3B30%2C30%3B40%2C40"))
    }

    @Test
    fun addRouteOptionsIncludedInRequest() {
        val routeOptions = RouteOptionsNavigation.builder()
            .accessToken(ACESS_TOKEN)
            .baseUrl("https://api-directions-traf.com")
            .requestUuid("XYZ_UUID")
            .alternatives(true)
            .language(Locale.US.language)
            .profile(DirectionsCriteria.PROFILE_WALKING)
            .origin(Point.fromLngLat(1.0, 2.0))
            .addWaypoint(Point.fromLngLat(1.0, 3.0))
            .destination(Point.fromLngLat(1.0, 5.0))
            .voiceUnits(DirectionsCriteria.METRIC)
            .user("example_user")
            .geometries("mocked_geometries")
            .approaches("curb;;unrestricted")
            .waypointNames("Origin;Pickup;Destination")
            .waypointTargets(";;0.99,4.99")
            .waypointIndices("0;2")
            .walkingOptions(
                WalkingOptionsNavigation.builder().alleyBias(0.6).walkwayBias(0.7).walkingSpeed(
                    1.0
                ).build()
            )
            .build()

        val navigationRoute = provideNavigationOffboardRouteBuilder()
            .routeOptions(routeOptions)
            .build()

        val request = navigationRoute.call.request().url().toString()
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

    private fun provideNavigationOffboardRouteBuilder() = NavigationOffboardRoute.builder(ACESS_TOKEN, context)

    @Test
    fun cancelCall_cancelsCallNotExecuted() {
        val mapboxDirections = mockk<MapboxDirections>(relaxed = true)
        val routeCall = mockk<Call<DirectionsResponse>>(relaxed = true)
        every { routeCall.isExecuted } returns false
        every { mapboxDirections.cloneCall() } returns routeCall
        val navigationRoute = NavigationOffboardRoute(mapboxDirections)

        navigationRoute.cancelCall()

        verify { routeCall.cancel() }
    }

    @Test
    fun cancelCall_doesNotCancelExecutedCall() {
        val mapboxDirections = mockk<MapboxDirections>()
        val routeCall = mockk<Call<DirectionsResponse>>()
        every { routeCall.isExecuted } returns true
        every { mapboxDirections.cloneCall() } returns routeCall
        val navigationRoute = NavigationOffboardRoute(mapboxDirections)

        navigationRoute.cancelCall()

        verify(exactly = 0) { routeCall.cancel() }
    }

    @Test
    fun builderInterceptor_setsMapboxDirections() {
        val mapboxDirectionsBuilder = mockk<MapboxDirections.Builder>(relaxed = true)
        val builder = NavigationOffboardRoute.Builder(mapboxDirectionsBuilder)
        val eventListener = mockk<EventListener>(relaxed = true)

        builder.eventListener(eventListener)

        verify { mapboxDirectionsBuilder.eventListener(eventListener) }
    }

    @Test
    fun builderEventListener_setsMapboxDirections() {
        val mapboxDirectionsBuilder = mockk<MapboxDirections.Builder>(relaxed = true)
        val builder = NavigationOffboardRoute.Builder(mapboxDirectionsBuilder)
        val interceptor = mockk<Interceptor>(relaxed = true)

        builder.interceptor(interceptor)

        verify { mapboxDirectionsBuilder.interceptor(interceptor) }
    }

    @Test
    fun builderContinueStraight_setsMapboxDirections() {
        val mapboxDirectionsBuilder = mockk<MapboxDirections.Builder>(relaxed = true)
        val builder = NavigationOffboardRoute.Builder(mapboxDirectionsBuilder)
        val continueStraight = false
        val routeOptions =
            RouteOptionsNavigation.builder().accessToken(ACESS_TOKEN)
                .origin(origin)
                .destination(destination)
                .continueStraight(continueStraight)
                .build()
        builder.routeOptions(routeOptions)

        verify { mapboxDirectionsBuilder.continueStraight(continueStraight) }
    }
}
