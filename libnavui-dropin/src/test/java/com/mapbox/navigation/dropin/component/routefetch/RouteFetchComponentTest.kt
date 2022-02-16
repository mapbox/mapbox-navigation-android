package com.mapbox.navigation.dropin.component.routefetch

import android.content.Context
import android.content.res.Resources
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class RouteFetchComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()
    private val parentJob = SupervisorJob()
    private val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)

    private val mockResources = mockk<Resources> {
        every { configuration } returns mockk()
    }
    private val context = mockk<Context> {
        every { resources } returns mockResources
    }

    @Before
    fun setUp() {
        mockkStatic("com.mapbox.navigation.base.internal.extensions.ContextEx")
        mockkObject(InternalJobControlFactory)
        every {
            InternalJobControlFactory.createDefaultScopeJobControl()
        } returns JobControl(parentJob, testScope)

        every { context.inferDeviceLocale() } returns Locale.ENGLISH
    }

    @After
    fun cleanUp() {
        unmockkStatic("com.mapbox.navigation.base.internal.extensions.ContextEx")
        unmockkObject(InternalJobControlFactory)
    }

    @Test
    fun `onAttached calls setRoutes on mapbox navigation via setRouteRequests flow`() {
        val mockRoute = mockk<DirectionsRoute>()
        val routes = listOf(mockRoute)
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)

        RouteFetchComponent(context).onAttached(mockMapboxNavigation).also {
            MapboxDropInRouteRequester.setRoutes(routes)
        }

        verify { mockMapboxNavigation.setRoutes(routes) }
    }

    @Test
    fun `onAttached calls requestRoutes on mapbox navigation via routeRequests flow`() {
        val points = listOf(
            Point.fromLngLat(33.0, 44.0),
            Point.fromLngLat(33.1, 44.1)
        )
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)

        RouteFetchComponent(context).onAttached(mockMapboxNavigation).also {
            MapboxDropInRouteRequester.fetchAndSetRoute(points)
        }

        verify { mockMapboxNavigation.requestRoutes(any(), any()) }
    }

    @Test
    fun `onAttached calls requestRoutes on mapbox navigation via routeOptionsRequests flow`() {
        val routeOptions = mockk<RouteOptions>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)

        RouteFetchComponent(context).onAttached(mockMapboxNavigation).also {
            MapboxDropInRouteRequester.fetchAndSetRoute(routeOptions)
        }

        verify { mockMapboxNavigation.requestRoutes(routeOptions, any()) }
    }

    @Test
    fun `onRoutesReady route requests set routes on mapbox navigation`() {
        val route1 = mockk<DirectionsRoute>()
        val route2 = mockk<DirectionsRoute>()
        val routes = listOf(route1, route2)
        val routeOptions = mockk<RouteOptions>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val callbackSlot = slot<RouterCallback>()
        val setRoutesSlot = slot<List<DirectionsRoute>>()
        RouteFetchComponent(context).onAttached(mockMapboxNavigation).also {
            MapboxDropInRouteRequester.fetchAndSetRoute(routeOptions)
        }
        verify { mockMapboxNavigation.requestRoutes(routeOptions, capture(callbackSlot)) }

        callbackSlot.captured.onRoutesReady(routes, mockk())

        verify { mockMapboxNavigation.setRoutes(capture(setRoutesSlot)) }
        assertEquals(route2, setRoutesSlot.captured.first())
        assertEquals(route1, setRoutesSlot.captured[1])
    }

    @Test
    fun `onDetached cancels mapbox navigation route request`() {
        val requestCode = 333L
        val routeOptions = mockk<RouteOptions>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
            every { requestRoutes(any(), any()) } returns requestCode
        }
        val component = RouteFetchComponent(context).also {
            it.onAttached(mockMapboxNavigation)
            MapboxDropInRouteRequester.fetchAndSetRoute(routeOptions)
        }

        component.onDetached(mockMapboxNavigation)

        verify { mockMapboxNavigation.cancelRouteRequest(requestCode) }
    }

    @Test
    fun `onDetached cancels coroutine children`() {
        val mockParentJob = mockk<CompletableJob>(relaxed = true)
        val mockJobControl = mockk<JobControl> {
            every { job } returns mockParentJob
        }
        every { InternalJobControlFactory.createMainScopeJobControl() } returns mockJobControl
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)

        RouteFetchComponent(context).onDetached(mockMapboxNavigation)

        verify { mockParentJob.cancelChildren() }
    }

    @Test
    fun `route fetch with default route options`() {
        val points = listOf(
            Point.fromLngLat(33.0, 44.0),
            Point.fromLngLat(33.1, 44.1)
        )
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
            every { getZLevel() } returns 9
        }
        val optionsSlot = slot<RouteOptions>()

        RouteFetchComponent(context).onAttached(mockMapboxNavigation).also {
            MapboxDropInRouteRequester.fetchAndSetRoute(points)
        }

        verify { mockMapboxNavigation.requestRoutes(capture(optionsSlot), any()) }
        assertEquals(points.first(), optionsSlot.captured.coordinatesList().first())
        assertEquals(points[1], optionsSlot.captured.coordinatesList()[1])
        assertTrue(optionsSlot.captured.alternatives()!!)
        assertEquals(9, optionsSlot.captured.layersList()!!.first())
        assertEquals("en", optionsSlot.captured.language())
        assertEquals(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC, optionsSlot.captured.profile())
        assertEquals(DirectionsCriteria.OVERVIEW_FULL, optionsSlot.captured.overview())
        assertTrue(optionsSlot.captured.steps()!!)
        assertTrue(optionsSlot.captured.roundaboutExits()!!)
        assertTrue(optionsSlot.captured.voiceInstructions()!!)
        assertTrue(optionsSlot.captured.bannerInstructions()!!)
        assertEquals(
            "congestion_numeric,maxspeed,closure,speed,duration,distance",
            optionsSlot.captured.annotations()
        )
    }
}
