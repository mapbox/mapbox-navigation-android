package com.mapbox.services.android.navigation.v5.navigation

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.WalkingOptions
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.utils.extensions.inferDeviceLocale
import com.mapbox.services.android.navigation.v5.BaseTest
import com.mapbox.services.android.navigation.v5.testsupport.Extensions
import com.mapbox.services.android.navigation.v5.testsupport.mockkStaticSupport
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import java.util.ArrayList
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

class NavigationRouteTest : BaseTest() {

    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            mockkStaticSupport(Extensions.ContextEx)
        }
    }

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
        val navigationRoute = com.mapbox.navigation.route.offboard.NavigationRoute.builder(context)
            .accessToken(BaseTest.ACCESS_TOKEN)
            .origin(Point.fromLngLat(1.0, 2.0))
            .destination(Point.fromLngLat(1.0, 5.0))
            .build()
        assertNotNull(navigationRoute)
    }

    @Test
    @Throws(Exception::class)
    fun changingDefaultValueToCustomWorksProperly() {
        val navigationRoute = com.mapbox.navigation.route.offboard.NavigationRoute.builder(context)
            .accessToken(BaseTest.ACCESS_TOKEN)
            .origin(Point.fromLngLat(1.0, 2.0))
            .destination(Point.fromLngLat(1.0, 5.0))
            .profile(DirectionsCriteria.PROFILE_CYCLING)
            .build()

        assertThat(
            navigationRoute.call.request().url().toString(),
            containsString("/cycling/")
        )
    }

    @Test
    fun addApproachesIncludedInRequest() {
        val navigationRoute = com.mapbox.navigation.route.offboard.NavigationRoute.builder(context)
            .accessToken(BaseTest.ACCESS_TOKEN)
            .origin(Point.fromLngLat(1.0, 2.0))
            .destination(Point.fromLngLat(1.0, 5.0))
            .profile(DirectionsCriteria.PROFILE_CYCLING)
            .addApproaches(
                DirectionsCriteria.APPROACH_CURB,
                DirectionsCriteria.APPROACH_UNRESTRICTED
            )
            .build()

        assertThat(
            navigationRoute.call.request().url().toString(),
            containsString("curb")
        )
    }

    @Test
    fun checksWaypointIndicesIncludedInRequest() {
        val navigationRoute = com.mapbox.navigation.route.offboard.NavigationRoute.builder(context)
            .accessToken(BaseTest.ACCESS_TOKEN)
            .origin(Point.fromLngLat(1.0, 2.0))
            .addWaypoint(Point.fromLngLat(1.0, 3.0))
            .addWaypoint(Point.fromLngLat(1.0, 3.0))
            .destination(Point.fromLngLat(1.0, 5.0))
            .addWaypointIndices(0, 2, 3)
            .build()

        assertThat(
            navigationRoute.call.request().url().toString(),
            containsString("waypoints")
        )
    }

    @Test
    fun addWaypointNamesIncludedInRequest() {
        val navigationRoute = com.mapbox.navigation.route.offboard.NavigationRoute.builder(context)
            .accessToken(BaseTest.ACCESS_TOKEN)
            .origin(Point.fromLngLat(1.0, 2.0))
            .destination(Point.fromLngLat(1.0, 5.0))
            .profile(DirectionsCriteria.PROFILE_CYCLING)
            .addWaypointNames("Origin", "Destination")
            .build()

        assertThat(
            navigationRoute.call.request().url().toString(),
            containsString("Destination")
        )
    }

    @Test
    fun addWaypointTargetsIncludedInRequest() {
        val navigationRoute = com.mapbox.navigation.route.offboard.NavigationRoute.builder(context)
            .accessToken(BaseTest.ACCESS_TOKEN)
            .origin(Point.fromLngLat(1.0, 2.0))
            .destination(Point.fromLngLat(1.0, 5.0))
            .addWaypointTargets(null, Point.fromLngLat(0.99, 4.99))
            .build()

        assertThat(
            navigationRoute.call.request().url().toString(),
            containsString("waypoint_targets")
        )
    }

    @Test
    fun reverseOriginDestination_bearingsAreFormattedCorrectly() {
        val navigationRoute = com.mapbox.navigation.route.offboard.NavigationRoute.builder(context)
            .accessToken(BaseTest.ACCESS_TOKEN)
            .destination(Point.fromLngLat(1.0, 5.0), 1.0, 5.0)
            .origin(Point.fromLngLat(1.0, 2.0), 90.0, 90.0)
            .build()

        val requestUrl = navigationRoute.call.request().url().toString()

        assertThat(requestUrl, containsString("bearings=90%2C90%3B1%2C5"))
    }

    @Test
    fun addWaypointsThenOriginDestination_bearingsAreFormattedCorrectly() {
        val navigationRoute = com.mapbox.navigation.route.offboard.NavigationRoute.builder(context)
            .accessToken(BaseTest.ACCESS_TOKEN)
            .addWaypoint(Point.fromLngLat(3.0, 4.0), 20.0, 20.0)
            .addWaypoint(Point.fromLngLat(5.0, 6.0), 30.0, 30.0)
            .destination(Point.fromLngLat(7.0, 8.0), 40.0, 40.0)
            .origin(Point.fromLngLat(1.0, 2.0), 10.0, 10.0)
            .build()

        val requestUrl = navigationRoute.call.request().url().toString()

        assertThat(requestUrl, containsString("bearings=10%2C10%3B20%2C20%3B30%2C30%3B40%2C40"))
    }

    @Test
    fun addRouteOptionsIncludedInRequest() {
        val coordinates = ArrayList<Point>()
        coordinates.add(Point.fromLngLat(1.0, 2.0))
        coordinates.add(Point.fromLngLat(1.0, 3.0))
        coordinates.add(Point.fromLngLat(1.0, 5.0))

        val routeOptions = RouteOptions.builder()
            .accessToken(BaseTest.ACCESS_TOKEN)
            .baseUrl("https://api-directions-traf.com")
            .requestUuid("XYZ_UUID")
            .alternatives(true)
            .language(Locale.US.language)
            .profile(DirectionsCriteria.PROFILE_WALKING)
            .coordinates(coordinates)
            .voiceUnits(DirectionsCriteria.METRIC)
            .user("example_user")
            .geometries("mocked_geometries")
            .approaches("curb;;unrestricted")!!
            .waypointNames("Origin;Pickup;Destination")!!
            .waypointTargets(";;0.99,4.99")!!
            .waypointIndices("0;2")!!
            .walkingOptions(
                WalkingOptions.builder().alleyBias(0.6).walkwayBias(0.7).walkingSpeed(
                    1.0
                ).build()
            )
            .build()

        val navigationRoute = com.mapbox.navigation.route.offboard.NavigationRoute.builder(context)
            .origin(coordinates[0])
            .addWaypoint(coordinates[1])
            .destination(coordinates[2])
            .routeOptions(routeOptions)
            .build()

        val request = navigationRoute.call.request().url().toString()
        assertThat(request, containsString("https://api-directions-traf.com"))
        assertThat(request, containsString("alternatives=true"))
        assertThat(request, containsString(BaseTest.ACCESS_TOKEN))
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
    fun cancelCall_cancelsCallNotExecuted() {
        val mapboxDirections = mockk<MapboxDirections>(relaxed = true)
        val routeCall = mockk<Call<DirectionsResponse>>(relaxed = true)
        every { routeCall.isExecuted } returns false
        every { mapboxDirections.cloneCall() } returns routeCall
        val navigationRoute = com.mapbox.navigation.route.offboard.NavigationRoute(mapboxDirections)

        navigationRoute.cancelCall()

        verify { routeCall.cancel() }
    }

    @Test
    fun cancelCall_doesNotCancelExecutedCall() {
        val mapboxDirections = mockk<MapboxDirections>()
        val routeCall = mockk<Call<DirectionsResponse>>()
        every { routeCall.isExecuted } returns true
        every { mapboxDirections.cloneCall() } returns routeCall
        val navigationRoute = com.mapbox.navigation.route.offboard.NavigationRoute(mapboxDirections)

        navigationRoute.cancelCall()

        verify(exactly = 0) { routeCall.cancel() }
    }

    @Test
    fun builderInterceptor_setsMapboxDirections() {
        val mapboxDirectionsBuilder = mockk<MapboxDirections.Builder>(relaxed = true)
        val builder = com.mapbox.navigation.route.offboard.NavigationRoute.Builder(mapboxDirectionsBuilder)
        val eventListener = mockk<EventListener>(relaxed = true)

        builder.eventListener(eventListener)

        verify { mapboxDirectionsBuilder.eventListener(eventListener) }
    }

    @Test
    fun builderEventListener_setsMapboxDirections() {
        val mapboxDirectionsBuilder = mockk<MapboxDirections.Builder>(relaxed = true)
        val builder = com.mapbox.navigation.route.offboard.NavigationRoute.Builder(mapboxDirectionsBuilder)
        val interceptor = mockk<Interceptor>(relaxed = true)

        builder.interceptor(interceptor)

        verify { mapboxDirectionsBuilder.interceptor(interceptor) }
    }

    @Test
    fun builderContinueStraight_setsMapboxDirections() {
        val mapboxDirectionsBuilder = mockk<MapboxDirections.Builder>(relaxed = true)
        val builder = com.mapbox.navigation.route.offboard.NavigationRoute.Builder(mapboxDirectionsBuilder)
        val continueStraight = false

        builder.continueStraight(continueStraight)

        verify { mapboxDirectionsBuilder.continueStraight(continueStraight) }
    }
}
