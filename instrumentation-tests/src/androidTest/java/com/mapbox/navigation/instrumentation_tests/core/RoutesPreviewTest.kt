package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.preview.RoutesPreviewExtra
import com.mapbox.navigation.core.preview.RoutesPreviewUpdate
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesPreviewUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.utils.routes.requestMockRoutes
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RoutesPreviewTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

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
                    .build(),
            )
        }
    }

    @Test
    fun transitions_free_drive_to_preview_to_active_guidance_to_free_drive() = sdkTest {
        var currentRoutesPreview: RoutesPreviewUpdate? = null
        mapboxNavigation.registerRoutesPreviewObserver { update ->
            currentRoutesPreview = update
        }
        var currentRoutes: RoutesUpdatedResult? = null
        mapboxNavigation.registerRoutesObserver { update ->
            currentRoutes = update
        }
        // initial free drive
        mapboxNavigation.startTripSession()
        assertNull(currentRoutesPreview)
        assertNull(currentRoutes)
        // set routes preview
        val routes = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            RoutesProvider.dc_very_short(activity),
        )
        mapboxNavigation.setRoutesPreview(routes)
        mapboxNavigation.routesPreviewUpdates()
            .first { it.reason == RoutesPreviewExtra.PREVIEW_NEW }
        assertEquals(RoutesPreviewExtra.PREVIEW_NEW, currentRoutesPreview?.reason)
        assertEquals(routes, currentRoutesPreview!!.routesPreview!!.routesList)
        assertNull(currentRoutes)
        // start active guidance
        mapboxNavigation.setNavigationRoutes(currentRoutesPreview!!.routesPreview!!.routesList)
        mapboxNavigation.setRoutesPreview(emptyList())
        mapboxNavigation.routesUpdates()
            .first { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW }
        mapboxNavigation.routeProgressUpdates()
            .first { it.currentState == RouteProgressState.TRACKING }
        mapboxNavigation.routesPreviewUpdates()
            .first { it.reason == RoutesPreviewExtra.PREVIEW_CLEAN_UP }
        assertEquals(RoutesPreviewExtra.PREVIEW_CLEAN_UP, currentRoutesPreview?.reason)
        assertNull(currentRoutesPreview!!.routesPreview)
        assertEquals(RoutesExtra.ROUTES_UPDATE_REASON_NEW, currentRoutes!!.reason)
        assertEquals(routes, currentRoutes!!.navigationRoutes)
        // back to free drive
        mapboxNavigation.setNavigationRoutes(emptyList())
        mapboxNavigation.routesUpdates()
            .first { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP }
        assertEquals(RoutesPreviewExtra.PREVIEW_CLEAN_UP, currentRoutesPreview?.reason)
        assertNull(currentRoutesPreview!!.routesPreview)
        assertEquals(RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP, currentRoutes!!.reason)
        assertEquals(emptyList<NavigationRoute>(), currentRoutes!!.navigationRoutes)
    }

    @Test
    fun route_preview_in_parallel_to_active_guidance() = sdkTest {
        var currentRoutesPreview: RoutesPreviewUpdate? = null
        mapboxNavigation.registerRoutesPreviewObserver { update ->
            currentRoutesPreview = update
        }
        var currentRoutes: RoutesUpdatedResult? = null
        mapboxNavigation.registerRoutesObserver { update ->
            currentRoutes = update
        }
        // initial free drive
        mapboxNavigation.startTripSession()
        assertNull(currentRoutesPreview)
        assertNull(currentRoutes)
        // set routes preview
        val initialRoutes = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            RoutesProvider.dc_very_short(activity),
        )
        mapboxNavigation.setRoutesPreview(initialRoutes)
        mapboxNavigation.routesPreviewUpdates()
            .first { it.reason == RoutesPreviewExtra.PREVIEW_NEW }
        // start active guidance
        mapboxNavigation.setNavigationRoutes(currentRoutesPreview!!.routesPreview!!.routesList)
        mapboxNavigation.setRoutesPreview(emptyList())
        mapboxNavigation.routeProgressUpdates()
            .first { it.currentState == RouteProgressState.TRACKING }
        // preview a different route not leaving action guidance
        val updatedRoutes = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            RoutesProvider.dc_very_short_two_legs(activity),
        )
        mapboxNavigation.setRoutesPreview(updatedRoutes)
        mapboxNavigation.routesPreviewUpdates()
            .first { it.routesPreview?.routesList == updatedRoutes }
        assertEquals(
            "active guidance should track initial routes",
            initialRoutes,
            currentRoutes?.navigationRoutes,
        )
        // user decided to switch to previewed routes
        mapboxNavigation.setNavigationRoutes(currentRoutesPreview!!.routesPreview!!.routesList)
        mapboxNavigation.setRoutesPreview(emptyList())
        mapboxNavigation.routeProgressUpdates().first {
            it.navigationRoute == updatedRoutes[0]
        }
    }

    @Test
    fun start_active_guidance_from_previewed_alternative_route() = sdkTest {
        // set routes preview
        val routes = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            RoutesProvider.dc_short_with_alternative(activity),
        )
        mapboxNavigation.setRoutesPreview(routes)
        val preview = mapboxNavigation.routesPreviewUpdates()
            .first { it.reason == RoutesPreviewExtra.PREVIEW_NEW }
        // switch to alternative route
        mapboxNavigation.changeRoutesPreviewPrimaryRoute(
            preview.routesPreview!!.originalRoutesList[1],
        )
        val updatedPreview = mapboxNavigation.routesPreviewUpdates()
            .first { it != preview }
        // start active guidance
        mapboxNavigation.setNavigationRoutes(updatedPreview.routesPreview!!.routesList)
        mapboxNavigation.setRoutesPreview(emptyList())
        val routesUpdate = mapboxNavigation.routesUpdates()
            .first { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW }
        assertEquals(
            listOf(
                routes[1],
                routes[0],
            ),
            routesUpdate.navigationRoutes,
        )
        val previewAlternativeMetadata = updatedPreview.routesPreview!!.alternativesMetadata.first()
        val activeGuidanceAlternativeMetadata = mapboxNavigation
            .getAlternativeMetadataFor(routes[0])!!
        assertEquals(
            previewAlternativeMetadata.infoFromStartOfPrimary,
            activeGuidanceAlternativeMetadata.infoFromStartOfPrimary,
        )
        assertEquals(
            previewAlternativeMetadata.forkIntersectionOfAlternativeRoute,
            activeGuidanceAlternativeMetadata.forkIntersectionOfAlternativeRoute,
        )
        assertEquals(
            previewAlternativeMetadata.infoFromFork,
            activeGuidanceAlternativeMetadata.infoFromFork,
        )
        assertEquals(
            previewAlternativeMetadata.forkIntersectionOfPrimaryRoute,
            activeGuidanceAlternativeMetadata.forkIntersectionOfPrimaryRoute,
        )
    }
}
