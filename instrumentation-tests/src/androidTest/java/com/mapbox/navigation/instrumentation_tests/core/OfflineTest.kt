@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.adevinta.android.barista.rule.cleardata.ClearFilesRule
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.TileDataDomain
import com.mapbox.common.TileStore
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.NavigationVersionSwitchObserver
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.location.stayOnPosition
import com.mapbox.navigation.testing.utils.offline.Tileset
import com.mapbox.navigation.testing.utils.offline.unpackTiles
import com.mapbox.navigation.testing.utils.setTestRouteRefreshInterval
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import com.mapbox.navigation.testing.utils.withoutInternet
import com.mapbox.navigation.utils.internal.toPoint
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class OfflineTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    @get:Rule
    val locationReplayRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    /**
     * Offline tests assumes that navigator has no cached data
     */
    @get:Rule
    val clearFilesRule = ClearFilesRule()

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            longitude = 13.381453
            latitude = 52.516323
        }
    }

    @Test
    fun startNavigatorOfflineWithTilesAvailableAndVersionSpecified() = sdkTest {
        withoutInternet {
            val version = context.unpackTiles(Tileset.Berlin)[TileDataDomain.NAVIGATION]!!
            locationReplayRule.playGeometry(TEST_BERLIN_ROUTE_GEOMETRY)
            withMapboxNavigation(
                historyRecorderRule = mapboxHistoryTestRule,
                tileStore = TileStore.create(),
                tilesVersion = version,
            ) { navigation ->
                navigation.registerNavigationVersionSwitchObserver(
                    object : NavigationVersionSwitchObserver {
                        override fun onSwitchToFallbackVersion(tilesVersion: String?) {
                            fail("Navigator switched to fallback version")
                        }

                        override fun onSwitchToTargetVersion(tilesVersion: String?) {
                            fail("Navigator switched to target version")
                        }
                    },
                )
                navigation.startTripSession()
                val firstLocationTimeout = 20.seconds
                val locationsInRow = 4
                val notDegradedLocationsPresent = withTimeoutOrNull(firstLocationTimeout) {
                    waitForNotDegradedLocationsInRow(navigation, locationsInRow)
                    true
                }
                assertNotNull(
                    "no $locationsInRow matched location in row for $firstLocationTimeout",
                    notDegradedLocationsPresent,
                )
            }
        }
    }

    /**
     * Timeout is increased because switch to unspecified version of tiles doesn't happen
     * fast
     */
    @Test
    fun startNavigatorOfflineWithTilesAvailableVersionNotSpecified() = sdkTest(timeout = 60_000) {
        withoutInternet {
            val version = context.unpackTiles(Tileset.Berlin)[TileDataDomain.NAVIGATION]
            locationReplayRule.playGeometry(TEST_BERLIN_ROUTE_GEOMETRY)
            withMapboxNavigation(
                historyRecorderRule = mapboxHistoryTestRule,
                tileStore = TileStore.create(),
                tilesVersion = "",
            ) { navigation ->
                var navigatorFellBackToTilesVersion: String? = null
                var versionSwitchCount = 0
                navigation.registerNavigationVersionSwitchObserver(
                    object : NavigationVersionSwitchObserver {
                        override fun onSwitchToFallbackVersion(tilesVersion: String?) {
                            navigatorFellBackToTilesVersion = tilesVersion
                            versionSwitchCount++
                        }

                        override fun onSwitchToTargetVersion(tilesVersion: String?) {
                            navigatorFellBackToTilesVersion = tilesVersion
                            versionSwitchCount++
                        }
                    },
                )
                navigation.startTripSession()
                val firstLocationTimeout = 30.seconds
                val locationsInRow = 4
                val notDegradedLocationsInRowArePresent = withTimeoutOrNull(firstLocationTimeout) {
                    waitForNotDegradedLocationsInRow(navigation, 4)
                    true
                }
                assertNotNull(
                    "no $locationsInRow matched location in row for $firstLocationTimeout",
                    notDegradedLocationsInRowArePresent,
                )
                assertEquals(
                    version,
                    navigatorFellBackToTilesVersion,
                )
                assertEquals(1, versionSwitchCount)
            }
        }
    }

    @Test
    fun offline_route_refresh() = sdkTest {
        val refreshesAttempts = AtomicInteger(0)
        val testRefreshIntervalMilliseconds = 100L
        mockWebServerRule.requestHandlers.add(
            object : MockRequestHandler {
                override fun handle(request: RecordedRequest): MockResponse? {
                    if (request.path?.contains("directions-refresh") == true) {
                        refreshesAttempts.incrementAndGet()
                    }
                    return null
                }
            },
        )
        val version = context.unpackTiles(Tileset.Berlin)[TileDataDomain.NAVIGATION]!!
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            tileStore = TileStore.create(),
            tilesVersion = version,
            routeRefreshOptions = RouteRefreshOptions
                .Builder()
                .build().apply {
                    setTestRouteRefreshInterval(testRefreshIntervalMilliseconds)
                },
        ) { navigation ->
            val testOrigin = Point.fromLngLat(13.389456, 52.518050)
            stayOnPosition(
                point = testOrigin,
                bearing = 270.0f,
                frequencyHz = 1,
            ) {
                navigation.startTripSession()
                val currentPosition = navigation.flowLocationMatcherResult().first {
                    TurfMeasurement.distance(it.enhancedLocation.toPoint(), testOrigin) < 0.1
                }
                withoutInternet {
                    val response = navigation.requestRoutes(
                        RouteOptions
                            .builder()
                            .applyDefaultNavigationOptions()
                            .baseUrl(mockWebServerRule.baseUrl)
                            .coordinatesList(
                                listOf(
                                    currentPosition.enhancedLocation.toPoint(),
                                    Point.fromLngLat(13.389507, 52.518734),
                                ),
                            )
                            .build(),
                    ).getSuccessfulResultOrThrowException()
                    navigation.setNavigationRoutes(response.routes)
                }
                delay(testRefreshIntervalMilliseconds * 3)
                assertEquals(0, refreshesAttempts.get())
            }
        }
    }

    private suspend fun waitForNotDegradedLocationsInRow(
        navigation: MapboxNavigation,
        locationsInRow: Int,
    ) {
        navigation.flowLocationMatcherResult()
            .scan(0) { notDegradedAccumulator, location ->
                if (!location.isDegradedMapMatching) {
                    notDegradedAccumulator + 1
                } else {
                    0
                }
            }
            .first { it == locationsInRow }
    }
}

private const val TEST_BERLIN_ROUTE_GEOMETRY = "y`jdcBesvoX{@q\\_Ai^_Aw_@OaGm@gVSgHMkE[uKK{DO{EY" +
    "iKoA{c@a@{NaAyZGkBYkJ[uJEcAw@o]cBuu@q@gYuAon@I}CQeIa@yJmBsr@UqIcAs^KwBWeG[aG}CmfA}@_[UwHc@" +
    "oTQoCUmBYuBc@sBg@sC[iCQyBW{F{@_]SgJeB}o@GaBOqG}@{]o@mVSkGSeIoC}dAe@mP_@kNQaHMmEwAgm@i@qNSk" +
    "C[kDc@cD}@_GcAqFmAyFsA_FwDyMmGwU{AeFkJ{[iHaWkFeSyDeNkO}h@sE_OuC_IiS_g@_D}H}U{k@s@_BcC_G}D" +
    "_JyFeM_@_AyJ}VyKiXm@yA}F}LgKgWkTmi@g^i}@]{@cCmGkB{E"
