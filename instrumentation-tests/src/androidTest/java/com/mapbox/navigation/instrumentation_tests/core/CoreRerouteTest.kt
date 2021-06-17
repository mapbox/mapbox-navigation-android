package com.mapbox.navigation.instrumentation_tests.core

import androidx.test.espresso.Espresso
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.assertions.RouteProgressStateTransitionAssertion
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.idling.RouteProgressStateIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.runOnMainSync
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CoreRerouteTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private lateinit var mapboxNavigation: MapboxNavigation

    private lateinit var offRouteIdlingResource: RouteProgressStateIdlingResource
    private lateinit var locationTrackingIdlingResource: RouteProgressStateIdlingResource

    @Before
    fun setup() {
        Espresso.onIdle()

        mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(activity)
                .accessToken(getMapboxAccessTokenFromResources(activity))
                .build()
        )
        locationTrackingIdlingResource = RouteProgressStateIdlingResource(
            mapboxNavigation,
            RouteProgressState.TRACKING
        )
        offRouteIdlingResource = RouteProgressStateIdlingResource(
            mapboxNavigation,
            RouteProgressState.OFF_ROUTE
        )
    }

    @Test
    fun reroute_completes() {
        // prepare
        val mockRoute = MockRoutesProvider.dc_very_short(activity)

        val originLocation = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = mockRoute.routeWaypoints.first().latitude()
            longitude = mockRoute.routeWaypoints.first().longitude()
        }

        val offRouteLocationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = originLocation.latitude + 0.002
            longitude = originLocation.longitude
        }

        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                jsonResponse = readRawFileText(activity, R.raw.offroute_response_dc_very_short),
                expectedCoordinates = listOf(
                    Point.fromLngLat(
                        offRouteLocationUpdate.longitude,
                        offRouteLocationUpdate.latitude
                    ),
                    mockRoute.routeWaypoints.last()
                ),
                omitCoordinates = true
            )
        )
        locationTrackingIdlingResource.register()

        val expectedStates = RouteProgressStateTransitionAssertion(mapboxNavigation) {
            requiredState(RouteProgressState.TRACKING)
            requiredState(RouteProgressState.OFF_ROUTE)
            optionalState(RouteProgressState.INITIALIZED)
            requiredState(RouteProgressState.TRACKING)
        }

        // start a route
        runOnMainSync {
            mockLocationUpdatesRule.pushLocationUpdate(originLocation)
            mapboxNavigation.startTripSession()
            mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                    .applyDefaultNavigationOptions()
                    .applyLanguageAndVoiceUnitOptions(activity)
                    .baseUrl(mockWebServerRule.baseUrl)
                    .accessToken(getMapboxAccessTokenFromResources(activity))
                    .coordinates(mockRoute.routeWaypoints).build(),
                object : RoutesRequestCallback {
                    override fun onRoutesReady(routes: List<DirectionsRoute>) {
                        mapboxNavigation.setRoutes(routes)
                    }

                    override fun onRoutesRequestFailure(
                        throwable: Throwable,
                        routeOptions: RouteOptions
                    ) {
                        // no impl
                    }

                    override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
                        // no impl
                    }
                }
            )
        }

        // wait for tracking to start
        Espresso.onIdle()
        locationTrackingIdlingResource.unregister()

        // push off route location and wait for the off route event
        offRouteIdlingResource.register()
        runOnMainSync {
            var locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.8949721
                longitude = -77.03195067
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.8949721
                longitude = -77.03195025
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.8949723
                longitude = -77.03194909
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.8950173
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89506227
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89510723
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.8951522
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89519716
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89524213
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.8952871
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89533206
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89537703
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.895422
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89546696
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89551193
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89555689
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89560186
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89564683
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89569179
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89573676
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89578172
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89582669
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89587166
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89591662
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89596159
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89600655
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89605152
                longitude = -77.03195
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89605156
                longitude = -77.03194945
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)

            locationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.89605192
                longitude = -77.03195069
            }
            mockLocationReplayerRule.loopUpdate(locationUpdate, times = 1)
        }
        Espresso.onIdle()
        offRouteIdlingResource.unregister()

        // wait for tracking to start again
        locationTrackingIdlingResource.register()
        Espresso.onIdle()
        locationTrackingIdlingResource.unregister()

        // assert results
        expectedStates.assert()
    }
}
