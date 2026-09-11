package com.mapbox.navigation.instrumentation_tests.ui

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.mapbox.maps.CameraState
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.plugin.locationcomponent.LocationComponentConstants
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.LocaleEx
import com.mapbox.navigation.base.internal.time.TimeFormatter
import com.mapbox.navigation.testing.ui.utils.coroutines.awaitViewAnnotations
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.utils.routes.MockRoute
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.route.callout.api.DefaultRouteCalloutAdapter
import com.mapbox.navigation.ui.maps.route.callout.model.DefaultRouteCalloutAdapterOptions
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCalloutType
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Verifies that, once a route with alternatives is loaded and the camera enters the route
 * overview state, a route callout is displayed for every route, with the primary route's callout
 * visually distinguished from the alternatives'.
 */
class RouteOverviewCalloutsTest : SimpleMapViewNavigationTest() {

    override fun getRoute(context: Context): MockRoute = RoutesProvider.multiple_routes(context)

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @Test
    fun callouts_are_displayed_for_every_route_in_overview_state() = sdkTest(60_000) {
        val testRoutes = mapboxNavigation.routesUpdates().first().navigationRoutes
        assertTrue("test expects alternatives", testRoutes.size > 1)

        // Pin the puck at the origin: otherwise the base class keeps replaying the route, which
        // keeps re-evaluating (and shrinking) the OVERVIEW frame as progress advances.
        val originLocation = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = mockRoute.routeWaypoints.first().latitude()
            longitude = mockRoute.routeWaypoints.first().longitude()
        }
        mockLocationReplayerRule.loopUpdate(originLocation, times = 60)

        addRouteLineWithCallouts()
        addNavigationCamera()

        val padding = 40.0 * Resources.getSystem().displayMetrics.density
        mapboxNavigationViewportDataSource.overviewPadding =
            EdgeInsets(padding, padding, padding, padding)

        awaitCameraOverview()
        awaitCameraStable()

        val calloutViews = activity.binding.mapView.viewAnnotationManager
            .awaitViewAnnotations(expectedCount = testRoutes.size)
        assertEquals(testRoutes.size, calloutViews.size)

        // Text: compare against the same formatter production uses, not a hardcoded string, so
        // the assertion stays correct regardless of the test device's locale.
        val expectedEtaTexts = testRoutes.map { route ->
            val locale = LocaleEx.getLocaleDirectionsRoute(route.directionsRoute, activity)
            TimeFormatter.formatTimeRemaining(
                activity,
                route.directionsRoute.duration(),
                locale,
            ).toString()
        }.sorted()
        val actualEtaTexts = calloutViews.map { it.etaTextView().text.toString() }.sorted()
        assertEquals(expectedEtaTexts, actualEtaTexts)

        // Style: exactly one selected (primary) callout, correct text color per role.
        val primaryViews = calloutViews.filter { it.isSelected }
        assertEquals("expected exactly one primary callout", 1, primaryViews.size)
        assertEquals(
            ContextCompat.getColor(activity, R.color.mapbox_selected_route_callout_text),
            primaryViews.single().etaTextView().currentTextColor,
        )
        calloutViews.filterNot { it.isSelected }.forEach { alternative ->
            assertEquals(
                ContextCompat.getColor(activity, R.color.mapbox_route_callout_text),
                alternative.etaTextView().currentTextColor,
            )
        }
    }

    private fun View.etaTextView(): TextView =
        (this as ViewGroup).findViewById(R.id.eta)

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun addRouteLineWithCallouts() {
        val apiOptions = MapboxRouteLineApiOptions.Builder()
            .isRouteCalloutsEnabled(true)
            .build()
        val viewOptions = MapboxRouteLineViewOptions.Builder(activity)
            .routeLineBelowLayerId(LocationComponentConstants.LOCATION_INDICATOR_LAYER)
            .build()
        routeLineApi = MapboxRouteLineApi(apiOptions)
        routeLineView = MapboxRouteLineView(viewOptions).apply {
            setCalloutAdapter(
                activity.mapboxMap,
                activity.binding.mapView.viewAnnotationManager,
                DefaultRouteCalloutAdapter(
                    activity,
                    DefaultRouteCalloutAdapterOptions.Builder()
                        .routeCalloutType(RouteCalloutType.ROUTES_OVERVIEW)
                        .build(),
                ),
            )
        }

        mapboxNavigation.registerRoutesObserver { result ->
            routeLineApi.setNavigationRoutes(
                result.navigationRoutes,
                mapboxNavigation.getAlternativeMetadataFor(result.navigationRoutes),
            ) { lineResult ->
                routeLineView.renderRouteDrawData(activity.mapboxMap.style!!, lineResult)
            }
        }
    }

    private suspend fun awaitCameraOverview() {
        val transitionEnded = CompletableDeferred<Unit>()
        navigationCamera.requestNavigationCameraToOverview(
            transitionEndListener = { transitionEnded.complete(Unit) },
        )
        withTimeout(10.seconds) { transitionEnded.await() }
        assertEquals(NavigationCameraState.OVERVIEW, navigationCamera.state)
    }

    // Reaching OVERVIEW state doesn't guarantee the camera stopped moving, so wait for several
    // consecutive identical reads before treating the position as settled.
    private suspend fun awaitCameraStable() {
        val requiredStableSamples = 5
        var stableCount = 0
        var previous: CameraState? = null
        withTimeout(25.seconds) {
            while (stableCount < requiredStableSamples) {
                val current = activity.mapboxMap.cameraState
                val isStable = previous?.let { isSameCamera(it, current) } ?: false
                stableCount = if (isStable) stableCount + 1 else 0
                previous = current
                delay(300.milliseconds)
            }
        }
    }

    private fun isSameCamera(a: CameraState, b: CameraState): Boolean {
        val zoomDelta = kotlin.math.abs(a.zoom - b.zoom)
        val centerDelta = kotlin.math.abs(a.center.latitude() - b.center.latitude()) +
            kotlin.math.abs(a.center.longitude() - b.center.longitude())
        return zoomDelta < 0.001 && centerDelta < 0.00001
    }
}
