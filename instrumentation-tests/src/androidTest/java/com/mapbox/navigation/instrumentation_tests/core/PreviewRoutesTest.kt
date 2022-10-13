package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.NavigationSessionStateV2
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.routesUpdates
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.sdkTest
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.waitForNewRoute
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.waitForPreviewRoute
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.waitForRoutesCleanUp
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider.toNavigationRoutes
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Objects
import kotlin.reflect.typeOf

class PreviewRoutesTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = 38.894721
        longitude = -77.031991
    }

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()
    private lateinit var mapboxNavigation: MapboxNavigation

    @Before
    fun setUp() {
        runOnMainSync {
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(activity)
                    .accessToken(getMapboxAccessTokenFromResources(activity))
                    .build()
            )
        }
    }

    @Test
    fun preview_route() = sdkTest {
        val routes = RoutesProvider.dc_very_short(activity).toNavigationRoutes()
        pushPrimaryRouteOriginAsLocation(routes)
        mapboxNavigation.startTripSession()
        val previewedRouteDeffer = async {
            mapboxNavigation.waitForPreviewRoute()
        }

        mapboxNavigation.previewNavigationRoutes(routes)

        val previewRouteUpdate = previewedRouteDeffer.await()
        assertEquals(routes, previewRouteUpdate.navigationRoutes)
        assertEquals(RoutesExtra.ROUTES_UPDATE_REASON_PREVIEW, previewRouteUpdate.reason)
        assertEquals(routes, mapboxNavigation.getPreviewedNavigationRoutes())
        assertEquals(emptyList<NavigationRoute>(), mapboxNavigation.getNavigationRoutes())
        assertIs<NavigationSessionState.FreeDrive>(mapboxNavigation.getNavigationSessionState())
        assertIs<NavigationSessionStateV2.RoutePreview>(mapboxNavigation.getNavigationSessionStateV2())
    }

    @Test
    fun preview_route_with_alternative() = sdkTest {
        val routes = RoutesProvider.dc_short_with_alternative(activity).toNavigationRoutes()
        pushPrimaryRouteOriginAsLocation(routes)
        mapboxNavigation.startTripSession()
        val previewedRouteDeffer = async {
            mapboxNavigation.waitForPreviewRoute()
        }

        mapboxNavigation.previewNavigationRoutes(routes)

        val previewRouteUpdate = previewedRouteDeffer.await()
        val previewedRouteMetadata = mapboxNavigation.getAlternativeMetadataFor(
            previewRouteUpdate.navigationRoutes[1]
        )
        assertNotNull(previewedRouteMetadata)
    }

    @Test
    fun start_active_guidance_after_preview() = sdkTest {
        val routes = RoutesProvider.dc_short_with_alternative(activity).toNavigationRoutes()
        pushPrimaryRouteOriginAsLocation(routes)
        mapboxNavigation.startTripSession()
        val previewedRouteDeferred = async {
            mapboxNavigation.waitForPreviewRoute()
        }
        mapboxNavigation.previewNavigationRoutes(routes)
        previewedRouteDeferred.await()
        val activeGuidanceRouteUpdateDefer = async {
            mapboxNavigation.waitForNewRoute()
        }

        mapboxNavigation.setNavigationRoutes(mapboxNavigation.getPreviewedNavigationRoutes())
        val activeGuidanceRouteUpdate = activeGuidanceRouteUpdateDefer.await()

        assertEquals(RoutesExtra.ROUTES_UPDATE_REASON_NEW, activeGuidanceRouteUpdate.reason)
        assertEquals(routes, activeGuidanceRouteUpdate.navigationRoutes)
        assertIs<NavigationSessionState.ActiveGuidance>(mapboxNavigation.getNavigationSessionState())
        assertIs<NavigationSessionStateV2.ActiveGuidance>(mapboxNavigation.getNavigationSessionStateV2())
    }

    @Test
    fun switch_to_free_drive_after_preview() = sdkTest {
        val routes = RoutesProvider.dc_very_short(activity).toNavigationRoutes()
        pushPrimaryRouteOriginAsLocation(routes)
        mapboxNavigation.startTripSession()
        val previewedRouteDeffer = async {
            mapboxNavigation.waitForPreviewRoute()
        }
        mapboxNavigation.previewNavigationRoutes(routes)
        previewedRouteDeffer.await()
        val freeDriveRoutesUpdateDeferred = async {
            mapboxNavigation.waitForRoutesCleanUp()
        }

        mapboxNavigation.clearRoutes()

        val freeDriveRoutesUpdate = freeDriveRoutesUpdateDeferred.await()
        assertEquals(emptyList<NavigationRoute>(), freeDriveRoutesUpdate.navigationRoutes)
        assertEquals(emptyList<NavigationRoute>(), mapboxNavigation.getNavigationRoutes())
        assertIs<NavigationSessionState.FreeDrive>(mapboxNavigation.getNavigationSessionState())
        assertIs<NavigationSessionStateV2.FreeDrive>(mapboxNavigation.getNavigationSessionStateV2())
    }

    @Test
    fun start_preview_after_active_guidance() = sdkTest {
        val routes = RoutesProvider.dc_short_with_alternative(activity).toNavigationRoutes()
        pushPrimaryRouteOriginAsLocation(routes)
        mapboxNavigation.startTripSession()
        mapboxNavigation.setNavigationRoutes(routes)
        mapboxNavigation.waitForNewRoute()
        val activeGuidanceRouteProgress = mapboxNavigation.routeProgressUpdates().first()

        val routeUpdates = mutableListOf<RoutesUpdatedResult>()
        mapboxNavigation.registerRoutesObserver {
            routeUpdates.add(it)
        }
        mapboxNavigation.previewNavigationRoutes(mapboxNavigation.getNavigationRoutes())
        mapboxNavigation.waitForPreviewRoute()
        val previewRouteProgress = withTimeoutOrNull(1) {
            mapboxNavigation.routeProgressUpdates().first()
        }

        assertEquals(
            listOf(RoutesExtra.ROUTES_UPDATE_REASON_NEW, RoutesExtra.ROUTES_UPDATE_REASON_PREVIEW),
            routeUpdates.map { it.reason }
        )
        assertNotNull(activeGuidanceRouteProgress)
        assertNull(previewRouteProgress)
        assertIs<NavigationSessionState.FreeDrive>(mapboxNavigation.getNavigationSessionState())
        assertIs<NavigationSessionStateV2.RoutePreview>(mapboxNavigation.getNavigationSessionStateV2())
    }

    private fun pushPrimaryRouteOriginAsLocation(routes: List<NavigationRoute>) {
        val origin = routes.first().routeOptions.coordinatesList().first()
        mockLocationUpdatesRule.pushLocationUpdate {
            latitude = origin.latitude()
            longitude = origin.longitude()
        }
    }
}

private inline fun <reified T> assertIs(obj: Any) {
    assertTrue(
        "expected an instance of ${T::class.java.name}, but it is $obj (${obj.javaClass.name})",
        obj is T
    )
}
