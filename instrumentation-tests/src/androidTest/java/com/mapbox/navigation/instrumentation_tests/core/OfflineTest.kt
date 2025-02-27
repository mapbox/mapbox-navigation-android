@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.adevinta.android.barista.rule.cleardata.ClearFilesRule
import com.mapbox.common.TileStore
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.NavigationVersionSwitchObserver
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.instrumentation_tests.utils.tiles.OfflineRegion
import com.mapbox.navigation.instrumentation_tests.utils.tiles.unpackOfflineTiles
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import com.mapbox.navigation.testing.utils.withoutInternet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
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
            val version = context.unpackOfflineTiles(OfflineRegion.Berlin)
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
            val version = context.unpackOfflineTiles(OfflineRegion.Berlin)
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
