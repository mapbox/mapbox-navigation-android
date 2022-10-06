package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import android.util.Log
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.internal.HistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.extensions.registerHistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.clearNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.collectMetadata
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.routesUpdates
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.sdkTest
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.setNavigationRoutesAndAwaitError
import com.mapbox.navigation.instrumentation_tests.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoute
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class CopilotIntegrationTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var mockRoute: MockRoute

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()
    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    override fun setupMockLocation(): Location {
        mockRoute = RoutesProvider.dc_very_short(activity)
        return mockLocationUpdatesRule.generateLocationUpdate {
            latitude = mockRoute.routeWaypoints.first().latitude()
            longitude = mockRoute.routeWaypoints.first().longitude()
        }
    }

    @Before
    fun setUp() {
        runOnMainSync {
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(activity)
                    .accessToken(getMapboxAccessTokenFromResources(activity))
                    .build()
            )
            mapboxHistoryTestRule.historyRecorder = mapboxNavigation.historyRecorder
        }
    }

    @Test
    fun copilotObserverReceivesSameIdsAsDeveloperMetadata() = sdkTest {
        val expectedMetaInfoCount = 7
        val startedSessionObserverIds = mutableListOf<String>()
        val historyRecordingStateObserver = object : HistoryRecordingStateChangeObserver {
            override fun onShouldStartRecording(state: NavigationSessionState) {
                Log.i("[HistoryRecordingStateChangeObserver]", "start recording for $state")
                startedSessionObserverIds.add(state.sessionId)
            }
            override fun onShouldStopRecording(state: NavigationSessionState) {
            }

            override fun onShouldCancelRecording(state: NavigationSessionState) {
            }
        }
        val metadatas = mapboxNavigation.collectMetadata(expectedMetaInfoCount)
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        mapboxNavigation.historyRecorder.startRecording()
        mapboxNavigation.registerHistoryRecordingStateChangeObserver(historyRecordingStateObserver)

        // Current: Idle
        // Free Drive
        var requestedRoutes: List<NavigationRoute> = emptyList()
        mapboxNavigation.startTripSession()
        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(activity)
                .baseUrl(mockWebServerRule.baseUrl)
                .coordinatesList(mockRoute.routeWaypoints).build(),
            object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    requestedRoutes = routes
                    mapboxNavigation.setNavigationRoutes(routes)
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    logE("onFailure reasons=$reasons", "DEBUG")
                }

                override fun onCanceled(
                    routeOptions: RouteOptions,
                    routerOrigin: RouterOrigin
                ) {
                    logE("onCanceled", "DEBUG")
                }
            }
        )
        // Active Guidance
        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW }
            .first()
        // Free Drive
        mapboxNavigation.clearNavigationRoutesAndWaitForUpdate()

        // setRoutes fail - ActiveGuidance + FreeDrive
        mapboxNavigation.setNavigationRoutesAndAwaitError(requestedRoutes, 6)

        // Idle
        mapboxNavigation.stopTripSession()

        val collectedIds = metadatas.take(expectedMetaInfoCount).toList().map { it.copilotSessionId }
        // first and last are idle: not recorded by HistoryRecordingStateChangeObserver
        assertEquals(startedSessionObserverIds, collectedIds.drop(1).dropLast(1))
    }
}
