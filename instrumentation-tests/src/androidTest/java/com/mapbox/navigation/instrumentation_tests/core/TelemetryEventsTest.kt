package com.mapbox.navigation.instrumentation_tests.core

import android.content.Context
import android.location.Location
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.bindgen.Value
import com.mapbox.common.SettingsServiceFactory
import com.mapbox.common.SettingsServiceStorageType
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.telemetry.events.UserFeedback
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.events.EventsAccumulatorRule
import com.mapbox.navigation.instrumentation_tests.utils.events.domain.EventAlternativeRoute
import com.mapbox.navigation.instrumentation_tests.utils.events.domain.EventArrive
import com.mapbox.navigation.instrumentation_tests.utils.events.domain.EventBase
import com.mapbox.navigation.instrumentation_tests.utils.events.domain.EventCancel
import com.mapbox.navigation.instrumentation_tests.utils.events.domain.EventDepart
import com.mapbox.navigation.instrumentation_tests.utils.events.domain.EventFeedback
import com.mapbox.navigation.instrumentation_tests.utils.events.domain.EventFreeDrive
import com.mapbox.navigation.instrumentation_tests.utils.events.domain.EventNavigationStateChanged
import com.mapbox.navigation.instrumentation_tests.utils.events.domain.EventReroute
import com.mapbox.navigation.instrumentation_tests.utils.events.verify.verifyEvents
import com.mapbox.navigation.instrumentation_tests.utils.http.EventsRequestHandle
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
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
import kotlinx.coroutines.flow.sample
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
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

    //    private lateinit var mapboxNavigation: MapboxNavigation
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
        private suspend fun waitForEventsBeSent(expectEvents: Int, current: () -> Int) {
            IntRange(0, 4).forEach { _ ->
                if (expectEvents == current()) {
                    return
                } else {
                    delay(1_000)
                }
            }
        }

        private fun List<String>.prettyErrors(): String = joinToString(separator = "") { "\n$it" }
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
        val mapboxNavigation = createMapboxNavigation(activity)

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
        val mapboxNavigation = createMapboxNavigation(activity)
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
    fun freeDriveWithFeedback() = sdkTest {
        val dcShorRoute = RoutesProvider.dc_very_short_two_legs(activity)
        val mapboxNavigation = createMapboxNavigation(activity)
        val feedbackType = "feedbackType"
        val description = "description"
        val feedbackSubTypes = arrayOf("subType1", "subType2")

        mapboxNavigation.startTripSession()
        mockLocationReplayerRule.playRoute(dcShorRoute.routeResponse.routes().first())
        mapboxNavigation.rawLocationUpdates().passed(20.0).first()
        mapboxNavigation.postUserFeedback(
            UserFeedback.Builder(feedbackType, description)
                .feedbackSubTypes(feedbackSubTypes)
                .build(),
        )
        mapboxNavigation.rawLocationUpdates().passed(20.0).first()
        mapboxNavigation.stopTripSession()

        verifyResult(
            EventFreeDrive(EventFreeDrive.Type.Start),
            EventFeedback(
                driverMode = EventBase.DriverMode.FreeDrive,
                feedbackType = feedbackType,
                description = description,
                feedbackSubType = feedbackSubTypes,
            ),
            EventFreeDrive(EventFreeDrive.Type.Stop),
        )
    }

    @Ignore("reroute is not set at the very beginning of the route")
    @Test
    fun activeGuidanceReroute() = sdkTest(200_000) {
        val dcShorRoute = RoutesProvider.dc_very_short(activity).apply {
            setRouteAsOriginLocation()
        }.toNavigationRoutes()
        val dcShortReroute = DirectionsResponse.fromJson(
            readRawFileText(
                activity,
                R.raw.reroute_response_dc_very_short
            )
        )
        val offRouteLocationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = dcShorRoute.first().waypoints!!.first().location().latitude() + 0.002
            longitude = dcShorRoute.first().waypoints!!.first().location().longitude()
        }
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                jsonResponse = dcShortReroute.toJson(),
                expectedCoordinates = listOf(
                    Point.fromLngLat(
                        offRouteLocationUpdate.longitude,
                        offRouteLocationUpdate.latitude
                    ),
                    dcShorRoute.first().routeOptions.coordinatesList().last()
                ),
                relaxedExpectedCoordinates = true
            )
        )
        val mapboxNavigation = createMapboxNavigation(activity)

        mapboxNavigation.setNavigationRoutes(dcShorRoute)
        mapboxNavigation.startTripSession()
        mapboxNavigation.routeProgressUpdates()
            .first { it.currentState == RouteProgressState.TRACKING }
        // go off route
        mockLocationReplayerRule.loopUpdate(offRouteLocationUpdate, times = 5)
        mapboxNavigation.routeProgressUpdates()
            .first { it.currentState == RouteProgressState.TRACKING }
        mockLocationReplayerRule.playRoute(dcShorRoute.first().directionsRoute)
        mapboxNavigation.rawLocationUpdates().passed(150.0)

        // play new route
        mockLocationReplayerRule.playbackSpeed(0.0)
        mockLocationReplayerRule.playRoute(dcShortReroute.routes().first())
        mockLocationReplayerRule.playbackSpeed(2.0)
        mapboxNavigation.routeProgressUpdates()
            .first { it.currentState == RouteProgressState.COMPLETE }
        mapboxNavigation.stopTripSession()

        verifyResult(
            EventNavigationStateChanged(EventNavigationStateChanged.State.NavStarted),
            EventDepart(),
            EventReroute(),
            EventArrive(),
            EventNavigationStateChanged(EventNavigationStateChanged.State.NavEnded),
        )
    }

    @Ignore("alternative route event is not sent. FIXME add link")
    @Test
    fun activeGuidanceSwitchToAlternative() = sdkTest {
        val dcShorWithAlternativeRoute = RoutesProvider.dc_short_with_alternative(activity).apply {
            setRouteAsOriginLocation()
        }.toNavigationRoutes()
        val mapboxNavigation = createMapboxNavigation(activity)
        mapboxNavigation.setNavigationRoutes(dcShorWithAlternativeRoute)
        val alternativeToRide = dcShorWithAlternativeRoute[1]

        mapboxNavigation.startTripSession()
        // play alternative route
        mockLocationReplayerRule.playRoute(alternativeToRide.directionsRoute)
        mockLocationReplayerRule.playbackSpeed(2.0)

        mapboxNavigation.routeProgressUpdates().sample(100)
            .first { it.navigationRoute == alternativeToRide }
        mapboxNavigation.routeProgressUpdates()
            .first { it.currentState == RouteProgressState.COMPLETE }
        mapboxNavigation.stopTripSession()

        verifyResult(
            EventNavigationStateChanged(EventNavigationStateChanged.State.NavStarted),
            EventDepart(),
            EventAlternativeRoute(),
            EventArrive(),
            EventNavigationStateChanged(EventNavigationStateChanged.State.NavEnded),
        )
    }

    /**
     * Reroute cancel in 150 (threshold is 100 meters) meters should have the full stack of events:
     * - start nav session
     * - departure
     * - cancel
     * - end nav session
     */
//    @Ignore("doesn't work as expected")
    @Test
    fun activeGuidanceRouteCancelIn150Meters() = sdkTest {
        val dcShorRoute = RoutesProvider.dc_very_short(activity).apply {
            setRouteAsOriginLocation()
        }.toNavigationRoutes()
        val mapboxNavigation = createMapboxNavigation(activity)
        mockLocationReplayerRule.playbackSpeed(2.0)

        mapboxNavigation.setNavigationRoutes(dcShorRoute)
        mapboxNavigation.startTripSession()
        mockLocationReplayerRule.playRoute(dcShorRoute.first().directionsRoute)
        mapboxNavigation.routeProgressUpdates().first { it.distanceTraveled >= 150 }
        mapboxNavigation.stopTripSession()

        verifyResult(
            EventNavigationStateChanged(EventNavigationStateChanged.State.NavStarted),
            EventDepart(),
            EventCancel(),
            EventNavigationStateChanged(EventNavigationStateChanged.State.NavEnded),
        )
    }

    /**
     * Reroute cancel in 50 (threshold is 100 meters) meters should not have departure and cancel events:
     * - start nav session
     * - end nav session
     */
    @Ignore("doesn't work as expected")
    @Test
    fun activeGuidanceRouteCancelIn50Meters() = sdkTest {
        val dcShorRoute = RoutesProvider.dc_very_short(activity).apply {
            setRouteAsOriginLocation()
        }.toNavigationRoutes()
        val mapboxNavigation = createMapboxNavigation(activity)
        mapboxNavigation.setNavigationRoutes(dcShorRoute)

        mapboxNavigation.startTripSession()
        mockLocationReplayerRule.playRoute(dcShorRoute.first().directionsRoute)
        mapboxNavigation.routeProgressUpdates().first { it.distanceTraveled >= 50 }
        mapboxNavigation.stopTripSession()

        verifyResult(
            EventNavigationStateChanged(EventNavigationStateChanged.State.NavStarted),
            EventNavigationStateChanged(EventNavigationStateChanged.State.NavEnded),
        )
    }

    @Ignore("doesn't work as expected")
    @Test
    fun activeGuidance2LegRoutePassed() = sdkTest {
        val dcShor2LegsRoute = RoutesProvider.dc_very_short_two_legs(activity).apply {
            setRouteAsOriginLocation()
        }.toNavigationRoutes()
        val mapboxNavigation = createMapboxNavigation(activity)
        mapboxNavigation.setNavigationRoutes(dcShor2LegsRoute)

        mapboxNavigation.startTripSession()
        mockLocationReplayerRule.playRoute(dcShor2LegsRoute.first().directionsRoute)
        mapboxNavigation.routeProgressUpdates()
            .first { it.currentState == RouteProgressState.COMPLETE }
        mapboxNavigation.stopTripSession()

        verifyResult(
            EventNavigationStateChanged(EventNavigationStateChanged.State.NavStarted),
            EventDepart(),
            EventArrive(),
            EventDepart(),
            EventArrive(),
            EventNavigationStateChanged(EventNavigationStateChanged.State.NavEnded),
        )
    }

    @Ignore("doesn't work as expected")
    @Test
    fun activeGuidance2LegRouteCanceledOnSecondLeg() = sdkTest {
        val dcShor2LegsRoute = RoutesProvider.dc_very_short_two_legs(activity).apply {
            setRouteAsOriginLocation()
        }.toNavigationRoutes()
        val mapboxNavigation = createMapboxNavigation(activity)
        mapboxNavigation.setNavigationRoutes(dcShor2LegsRoute)

        mapboxNavigation.startTripSession()
        mockLocationReplayerRule.playRoute(dcShor2LegsRoute.first().directionsRoute)
        // traveled 50 meters on second leg
        mapboxNavigation.routeProgressUpdates().first {
            it.currentLegProgress!!.legIndex == 1 && it.currentLegProgress!!.distanceTraveled > 50
        }
        mapboxNavigation.stopTripSession()

        verifyResult(
            EventNavigationStateChanged(EventNavigationStateChanged.State.NavStarted),
            EventDepart(),
            EventArrive(),
            EventDepart(),
            EventCancel(),
            EventNavigationStateChanged(EventNavigationStateChanged.State.NavEnded),
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
