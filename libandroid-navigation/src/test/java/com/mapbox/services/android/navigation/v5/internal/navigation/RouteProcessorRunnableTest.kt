package com.mapbox.services.android.navigation.v5.internal.navigation

import android.location.Location
import android.os.Handler
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.geojson.Point
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.RouteState
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions
import com.mapbox.services.android.navigation.v5.navigation.camera.SimpleCamera
import com.mapbox.services.android.navigation.v5.offroute.OffRouteDetector
import com.mapbox.services.android.navigation.v5.route.FasterRouteDetector
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.snap.SnapToRoute
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.ArrayList
import java.util.Date
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RouteProcessorRunnableTest {

    @Test
    fun onRun_buildNewRouteProgressReceivesStatusAndRoute() {
        val processor = mockk<NavigationRouteProcessor>(relaxed = true)
        val navigator = mockk<MapboxNavigator>()
        val status = buildMockStatus()
        val route = mockk<DirectionsRoute>(relaxed = true)
        val runnable = buildRouteProcessorRunnableWith(navigator, processor, status, route)
        runnable.updateRawLocation(mockk<Location>())

        runnable.run()

        verify { processor.buildNewRouteProgress(navigator, status, route) }
    }

    @Test
    fun onRun_previousRouteProgressIsUpdated() {
        val processor = mockk<NavigationRouteProcessor>(relaxed = true)
        val navigator = mockk<MapboxNavigator>()
        val status = buildMockStatus()
        val route = mockk<DirectionsRoute>(relaxed = true)
        val progress = mockk<RouteProgress>()
        every { processor.buildNewRouteProgress(navigator, status, route) } returns progress
        val runnable = buildRouteProcessorRunnableWith(navigator, processor, status, route)
        runnable.updateRawLocation(mockk<Location>())

        runnable.run()

        verify { processor.updatePreviousRouteProgress(progress) }
    }

    @Test
    fun onRun_offRouteDetectorReceivesStatus() {
        val detector = mockk<OffRouteDetector>(relaxed = true)
        val factory = buildMockFactory(detector)
        every { factory.retrieveOffRouteEngine() } returns detector
        val status = buildMockStatus()
        val runnable = buildRouteProcessorRunnableWith(factory, status)
        runnable.updateRawLocation(mockk<Location>())

        runnable.run()

        verify { detector.isUserOffRouteWith(status) }
    }

    @Test
    fun onRun_snapToRouteReceivesStatus() {
        val snapToRoute = mockk<SnapToRoute>(relaxed = true)
        val factory = buildMockFactory(snapToRoute)
        val status = buildMockStatus()
        val runnable = buildRouteProcessorRunnableWith(factory, status)
        val rawLocation = mockk<Location>()
        runnable.updateRawLocation(rawLocation)

        runnable.run()

        verify { snapToRoute.getSnappedLocationWith(status, rawLocation) }
    }

    // @Test
    // fun onRun_legIndexIncrementsOnLegCompletionWithValidDistanceRemaining() {
    //     val snapToRoute = mockk<SnapToRoute>(relaxed = true)
    //     val factory = buildMockFactory(snapToRoute)
    //     val previousStatus = buildMockStatus()
    //     val status = buildMockStatus()
    //     every { previousStatus.routeState } returns RouteState.COMPLETE
    //     every { previousStatus.remainingLegDistance } returns 20f
    //     val navigator = mockk<MapboxNavigator>(relaxed = true)
    //     val route = buildTwoLegRoute()
    //     val autoIncrementEnabled = true
    //     val runnable = buildRouteProcessorRunnableWith(
    //         navigator, factory, previousStatus, status, route, autoIncrementEnabled
    //     )
    //     val rawLocation = mockk<Location>()
    //     runnable.updateRawLocation(rawLocation)
    //
    //     runnable.run()
    //
    //     verify { navigator.updateLegIndex(any()) }
    // }
    //
    // @Test
    // fun onRun_legIndexDoesNotIncrementsOnLegCompletionWithInvalidDistanceRemaining() {
    //     val snapToRoute = mockk<SnapToRoute>(relaxed = true)
    //     val factory = buildMockFactory(snapToRoute)
    //     val previousStatus = buildMockStatus()
    //     val status = buildMockStatus()
    //     every { previousStatus.routeState } returns RouteState.COMPLETE
    //     every { previousStatus.remainingLegDistance } returns 50f
    //     val navigator = mockk<MapboxNavigator>()
    //     val route = buildTwoLegRoute()
    //     val autoIncrementEnabled = true
    //     val runnable = buildRouteProcessorRunnableWith(
    //         navigator, factory, previousStatus, status, route, autoIncrementEnabled
    //     )
    //     val rawLocation = mockk<Location>()
    //     runnable.updateRawLocation(rawLocation)
    //
    //     runnable.run()
    //
    //     verify(exactly = 0) { navigator.updateLegIndex(any()) }
    // }
    //
    // @Test
    // fun onRun_legIndexDoesNotIncrementsOnLegCompletionWithInvalidLegsRemaining() {
    //     val snapToRoute = mockk<SnapToRoute>(relaxed = true)
    //     val factory = buildMockFactory(snapToRoute)
    //     val previousStatus = buildMockStatus()
    //     val status = buildMockStatus()
    //     every { previousStatus.routeState } returns RouteState.COMPLETE
    //     every { previousStatus.remainingLegDistance } returns 20f
    //     every { previousStatus.legIndex } returns 1
    //     val navigator = mockk<MapboxNavigator>()
    //     val route = buildTwoLegRoute()
    //     val autoIncrementEnabled = true
    //     val runnable = buildRouteProcessorRunnableWith(
    //         navigator, factory, previousStatus, status, route, autoIncrementEnabled
    //     )
    //     val rawLocation = mockk<Location>()
    //     runnable.updateRawLocation(rawLocation)
    //
    //     runnable.run()
    //
    //     verify(exactly = 0) { navigator.updateLegIndex(eq(1)) }
    // }
    //
    // @Test
    // fun onRun_legIndexDoesNotIncrementOnLegCompletionWithAutoIncrementDisabled() {
    //     val snapToRoute = mockk<SnapToRoute>(relaxed = true)
    //     val factory = buildMockFactory(snapToRoute)
    //     val previousStatus = buildMockStatus()
    //     val status = buildMockStatus()
    //     every { previousStatus.routeState } returns RouteState.COMPLETE
    //     every { previousStatus.remainingLegDistance } returns 20f
    //     val navigator = mockk<MapboxNavigator>()
    //     val route = buildTwoLegRoute()
    //     val autoIncrementEnabled = false
    //     val runnable = buildRouteProcessorRunnableWith(
    //         navigator, factory, previousStatus, status, route, autoIncrementEnabled
    //     )
    //     val rawLocation = mockk<Location>()
    //     runnable.updateRawLocation(rawLocation)
    //
    //     runnable.run()
    //
    //     verify(exactly = 0) { navigator.updateLegIndex(any()) }
    // }

    private fun buildRouteProcessorRunnableWith(
        navigator: MapboxNavigator,
        processor: NavigationRouteProcessor,
        status: NavigationStatus,
        route: DirectionsRoute
    ): RouteProcessorRunnable {
        val options = MapboxNavigationOptions.Builder().build()
        every { navigator.retrieveStatus(any(), any()) } returns status
        val navigation = mockk<MapboxNavigation>(relaxed = true)
        every { navigation.options() } returns options
        every { navigation.route } returns route
        every { navigation.retrieveMapboxNavigator() } returns navigator
        every { navigation.retrieveEngineFactory() } returns NavigationEngineFactory()
        return RouteProcessorRunnable(
            processor,
            navigation,
            mockk<Handler>(relaxed = true),
            mockk<Handler>(relaxed = true),
            mockk<RouteProcessorBackgroundThread.Listener>()
        )
    }

    private fun buildRouteProcessorRunnableWith(
        navigator: MapboxNavigator,
        factory: NavigationEngineFactory,
        status: NavigationStatus,
        route: DirectionsRoute,
        autoIncrementEnabled: Boolean
    ): RouteProcessorRunnable {
        val options = MapboxNavigationOptions.Builder()
            .enableAutoIncrementLegIndex(autoIncrementEnabled)
            .build()
        every { navigator.retrieveStatus(any(), any()) } returns status
        val navigation = mockk<MapboxNavigation>()
        every { navigation.options() } returns options
        every { navigation.route } returns route
        every { navigation.retrieveMapboxNavigator() } returns navigator
        every { navigation.retrieveEngineFactory() } returns factory
        return RouteProcessorRunnable(
            mock(NavigationRouteProcessor::class.java),
            navigation,
            mockk<Handler>(relaxed = true),
            mockk<Handler>(relaxed = true),
            mockk<RouteProcessorBackgroundThread.Listener>()
        )
    }

    private fun buildRouteProcessorRunnableWith(
        navigator: MapboxNavigator,
        factory: NavigationEngineFactory,
        previousStatus: NavigationStatus,
        status: NavigationStatus,
        route: DirectionsRoute,
        autoIncrementEnabled: Boolean
    ): RouteProcessorRunnable {
        val options = MapboxNavigationOptions.Builder()
            .enableAutoIncrementLegIndex(autoIncrementEnabled)
            .build()
        every { navigator.retrieveStatus(any(), any()) } returns status
        val navigation = mockk<MapboxNavigation>(relaxed = true)
        every { navigation.options() } returns options
        every { navigation.route } returns route
        every { navigation.retrieveMapboxNavigator() } returns navigator
        every { navigation.retrieveEngineFactory() } returns factory
        val routeProcessor = mockk<NavigationRouteProcessor>(relaxed = true)
        every { routeProcessor.retrievePreviousStatus() } returns previousStatus
        return RouteProcessorRunnable(
            routeProcessor,
            navigation,
            mockk<Handler>(relaxed = true),
            mockk<Handler>(relaxed = true),
            mockk<RouteProcessorBackgroundThread.Listener>()
        )
    }

    private fun buildRouteProcessorRunnableWith(
        factory: NavigationEngineFactory,
        status: NavigationStatus
    ): RouteProcessorRunnable {
        val options = MapboxNavigationOptions.Builder().build()
        val navigator = mockk<MapboxNavigator>()
        every { navigator.retrieveStatus(any(), any()) } returns status
        val navigation = mockk<MapboxNavigation>(relaxed = true)
        every { navigation.options() } returns options
        every { navigation.route } returns mockk<DirectionsRoute>(relaxed = true)
        every { navigation.retrieveMapboxNavigator() } returns navigator
        every { navigation.retrieveEngineFactory() } returns factory
        return RouteProcessorRunnable(
            mockk<NavigationRouteProcessor>(relaxed = true),
            navigation,
            mockk<Handler>(relaxed = true),
            mockk<Handler>(relaxed = true),
            mockk<RouteProcessorBackgroundThread.Listener>()
        )
    }

    private fun buildMockFactory(snapToRoute: SnapToRoute): NavigationEngineFactory {
        val factory = mockk<NavigationEngineFactory>()
        every { factory.retrieveSnapEngine() } returns snapToRoute
        every { factory.retrieveOffRouteEngine() } returns mockk<OffRouteDetector>(relaxed = true)
        every { factory.retrieveFasterRouteEngine() } returns mockk<FasterRouteDetector>(relaxed = true)
        every { factory.retrieveCameraEngine() } returns mockk<SimpleCamera>(relaxed = true)
        return factory
    }

    private fun buildMockFactory(detector: OffRouteDetector): NavigationEngineFactory {
        val factory = mockk<NavigationEngineFactory>()
        every { factory.retrieveSnapEngine() } returns mockk<SnapToRoute>(relaxed = true)
        every { factory.retrieveOffRouteEngine() } returns detector
        every { factory.retrieveFasterRouteEngine() } returns mockk<FasterRouteDetector>(relaxed = true)
        every { factory.retrieveCameraEngine() } returns mockk<SimpleCamera>(relaxed = true)
        return factory
    }

    private fun buildMockStatus(): NavigationStatus {
        val status = mockk<NavigationStatus>(relaxed = true)
        val location = Point.fromLngLat(0.0, 0.0)
        every { status.location.coordinate } returns location
        every { status.location.time } returns Date()
        every { status.location.bearing } returns 0.0f
        return status
    }

    private fun buildTwoLegRoute(): DirectionsRoute {
        val route = mockk<DirectionsRoute>()
        val routeLegs = ArrayList<RouteLeg>()
        routeLegs.add(mockk<RouteLeg>())
        routeLegs.add(mockk<RouteLeg>())
        every { route.legs() } returns routeLegs
        return route
    }
}
