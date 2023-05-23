package com.mapbox.navigation.instrumentation_tests.core

import android.content.Context
import android.location.Location
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.bindgen.Value
import com.mapbox.common.SettingsServiceFactory
import com.mapbox.common.SettingsServiceStorageType
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.events.EventsAccumulatorRule
import com.mapbox.navigation.instrumentation_tests.utils.events.domain.EventArrive
import com.mapbox.navigation.instrumentation_tests.utils.events.domain.EventBase
import com.mapbox.navigation.instrumentation_tests.utils.events.domain.EventDepart
import com.mapbox.navigation.instrumentation_tests.utils.events.domain.EventFeedback
import com.mapbox.navigation.instrumentation_tests.utils.events.domain.EventFreeDrive
import com.mapbox.navigation.instrumentation_tests.utils.events.domain.EventNavigationStateChanged
import com.mapbox.navigation.instrumentation_tests.utils.events.verify.verifyEvents
import com.mapbox.navigation.instrumentation_tests.utils.http.EventsRequestHandle
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoute
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider.toNavigationRoutes
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.passed
import com.mapbox.navigation.testing.ui.utils.coroutines.rawLocationUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TelemetryEventsTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val eventsAccumulatorRule = EventsAccumulatorRule(
        getMapboxAccessTokenFromResources(InstrumentationRegistry.getInstrumentation().targetContext)
    )

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    private val coordinates = listOf(
        Point.fromLngLat(-77.031991, 38.894721),
        Point.fromLngLat(-77.031991, 38.895433),
        Point.fromLngLat(-77.030923, 38.895433),
    )

    private lateinit var mapboxNavigation: MapboxNavigation
    private val eventsRequestHandle = EventsRequestHandle()

    private companion object {
        private const val LOG_CATEGORY = "TelemetryEventsTest"

        /* See mapbox_common_settings_internal.hpp in common repo */
        private const val TELEMETRY_EVENTS_BASE_URL_PROPERTY =
            "com.mapbox.common.telemetry.internal.events_base_url_override"

        private fun createMapboxNavigation(context: Context): MapboxNavigation =
            MapboxNavigationProvider.create(
                NavigationOptions.Builder(context)
                    .accessToken(getMapboxAccessTokenFromResources(context))
                    .deviceProfile(
                        DeviceProfile.Builder()
                            .customConfig(
                                """
                                    {
                                        "features": {
                                            "useTelemetryNavigationEvents": true
                                        },
                                        "telemetry": {
                                            "eventsPriority": "Immediate"
                                        }
                                    }
                                """.trimIndent()
                            )
                            .build()
                    )
                    .build()
            )

        /**
         * Give the chance to send events: 5 attempts with 1 seconds delay
         */
        private suspend fun waitForEventsBeSent(expectedEvents: Int, current: () -> Int) {
            IntRange(0, 4).forEach { _ ->
                if (expectedEvents == current()) {
                    return
                } else {
                    delay(1_000)
                }
            }
        }

        private fun List<String>.prettyErrors(): String = joinToString(postfix = "\n")
    }

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = coordinates[0].latitude()
        longitude = coordinates[0].longitude()
    }

    @Before
    fun setup() {
        runOnMainSync {
            // substitute telemetry base url
            SettingsServiceFactory.getInstance(SettingsServiceStorageType.NON_PERSISTENT).apply {
                set(TELEMETRY_EVENTS_BASE_URL_PROPERTY, Value(mockWebServerRule.baseUrl))
            }

            mockWebServerRule.requestHandlers.add(eventsRequestHandle)
        }
    }

    @Test
    fun freeDrivePlain() = sdkTest {
        val dcShorRoute = RoutesProvider.dc_short_with_alternative(activity).apply {
            setRouteAsOriginLocation()
        }
        mapboxNavigation = createMapboxNavigation(activity)

        mapboxNavigation.startTripSession()
        mockLocationReplayerRule.playRoute(dcShorRoute.routeResponse.routes().first())
        mapboxNavigation.rawLocationUpdates().passed(50.0).first()
        mapboxNavigation.stopTripSession()

        verifyResult(
            EventFreeDrive(EventFreeDrive.Type.Start),
            EventFreeDrive(EventFreeDrive.Type.Stop),
        )
    }

    @Test
    fun activeGuidancePlain() = sdkTest {
        val dcShortRoute = RoutesProvider.dc_short_with_alternative(activity).apply {
            setRouteAsOriginLocation()
        }
        mapboxNavigation = createMapboxNavigation(activity)
        mockLocationReplayerRule.playbackSpeed(3.0)

        mapboxNavigation.setNavigationRoutes(dcShortRoute.toNavigationRoutes())
        mapboxNavigation.startTripSession()
        mockLocationReplayerRule.playRoute(dcShortRoute.routeResponse.routes().first())
        mapboxNavigation.routeProgressUpdates().first {
            it.currentState == RouteProgressState.COMPLETE
        }
        mapboxNavigation.stopTripSession()

        verifyResult(
            EventNavigationStateChanged(EventNavigationStateChanged.State.NavStarted),
            EventDepart(),
            EventArrive(),
            EventNavigationStateChanged(EventNavigationStateChanged.State.NavEnded),
        )
    }

    @Test
    fun freeDriveWithFeedback() = sdkTest(200_000) {
        val dcShorRoute = RoutesProvider.dc_very_short_two_legs(activity)
        mapboxNavigation = createMapboxNavigation(activity)
        val feedbackType = "feedbackType"
        val description = "description"
        val screenshot = "screenshot"
        val feedbackSubType = arrayOf("subType1", "subType2")

        mapboxNavigation.startTripSession()
        mockLocationReplayerRule.playRoute(dcShorRoute.routeResponse.routes().first())
        mapboxNavigation.rawLocationUpdates().passed(20.0).first()
        mapboxNavigation.postUserFeedback(
            feedbackType = feedbackType,
            description = description,
            feedbackSource = "na",
            screenshot = "screenshot",
            feedbackSubType = feedbackSubType
        )
        mapboxNavigation.rawLocationUpdates().passed(20.0).first()
        mapboxNavigation.stopTripSession()

        verifyResult(
            EventFreeDrive(EventFreeDrive.Type.Start),
            EventFeedback(
                driverMode = EventBase.DriverMode.FreeDrive,
                feedbackType = feedbackType,
                description = description,
                feedbackSubType = feedbackSubType,
                // TODO fixme when NN implement data_ref
                screenshot = "c2NyZWVuc2hvdA==",
            ),
            EventFreeDrive(EventFreeDrive.Type.Stop),
        )
    }

    private suspend fun verifyResult(vararg expected: EventBase) {
        waitForEventsBeSent(expected.size) { eventsAccumulatorRule.events.size }
        val result = expected.asList().verifyEvents(eventsAccumulatorRule.events)

        check(result.isEmpty()) {
            result.prettyErrors()
        }
    }

    private fun MockRoute.setRouteAsOriginLocation() {
        mockLocationUpdatesRule.pushLocationUpdate(
            mockLocationUpdatesRule.generateLocationUpdate {
                latitude = this@setRouteAsOriginLocation.routeWaypoints.first().latitude()
                longitude = this@setRouteAsOriginLocation.routeWaypoints.first().longitude()
            }
        )
    }
}
