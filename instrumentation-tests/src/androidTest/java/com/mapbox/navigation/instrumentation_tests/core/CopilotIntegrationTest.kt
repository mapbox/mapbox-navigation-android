package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.DeveloperMetadata
import com.mapbox.navigation.core.DeveloperMetadataObserver
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.internal.HistoryRecordingSessionState
import com.mapbox.navigation.core.internal.HistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.extensions.registerHistoryRecordingStateChangeObserver
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.clearNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndAwaitError
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.routes.MockRoute
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class CopilotIntegrationTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var mockRoute: MockRoute
    private lateinit var routes: List<NavigationRoute>

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    override fun setupMockLocation(): Location {
        mockRoute = RoutesProvider.dc_very_short(context)
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
                    .build(),
            )
            mapboxHistoryTestRule.historyRecorder = mapboxNavigation.historyRecorder
        }
    }

    @Test
    fun copilotObserverReceivesSameIdsAsDeveloperMetadata() = sdkTest {
        val collectedDeveloperMetadatas = mutableListOf<DeveloperMetadata>()
        val developerMetadataObserver = DeveloperMetadataObserver {
            collectedDeveloperMetadatas.add(it)
        }
        val startedSessionObserverIds = mutableListOf<String>()
        val historyRecordingStateObserver = object : HistoryRecordingStateChangeObserver {
            override fun onShouldStartRecording(state: HistoryRecordingSessionState) {
                startedSessionObserverIds.add(state.sessionId)
            }

            override fun onShouldStopRecording(state: HistoryRecordingSessionState) {
            }

            override fun onShouldCancelRecording(state: HistoryRecordingSessionState) {
            }
        }
        mapboxNavigation.registerDeveloperMetadataObserver(developerMetadataObserver)
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        mapboxNavigation.historyRecorder.startRecording()
        mapboxNavigation.registerHistoryRecordingStateChangeObserver(historyRecordingStateObserver)
        routes = mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(context)
                .baseUrl(mockWebServerRule.baseUrl)
                .coordinatesList(mockRoute.routeWaypoints)
                .build(),
        ).getSuccessfulResultOrThrowException().routes

        transitionFromIdleToFreeDrive()
        transitionFromFreeDriveToActiveGuidance()
        transitionFromActiveGuidanceToFreeDrive()
        transitionFromFreeDriveToStubActiveGuidanceAndBackToFreeDrive()
        transitionFromFreeDriveToIdle()

        val collectedIds = collectedDeveloperMetadatas.map { it.copilotSessionId }
        // first and last are idle: not recorded by HistoryRecordingStateChangeObserver
        assertEquals(startedSessionObserverIds, collectedIds.drop(1).dropLast(1))
    }

    private fun transitionFromIdleToFreeDrive() {
        mapboxNavigation.startTripSession()
    }

    private suspend fun transitionFromFreeDriveToActiveGuidance() {
        mapboxNavigation.setNavigationRoutes(routes)
        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW }
            .first()
    }

    private suspend fun transitionFromActiveGuidanceToFreeDrive() {
        mapboxNavigation.clearNavigationRoutesAndWaitForUpdate()
    }

    private suspend fun transitionFromFreeDriveToStubActiveGuidanceAndBackToFreeDrive() {
        mapboxNavigation.setNavigationRoutesAndAwaitError(routes, 6)
    }

    private fun transitionFromFreeDriveToIdle() {
        mapboxNavigation.stopTripSession()
    }
}
