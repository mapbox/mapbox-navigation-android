package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.adevinta.android.barista.rule.cleardata.ClearFilesRule
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.common.TileDataDomain
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.trip.model.eh.EHorizonPosition
import com.mapbox.navigation.base.trip.model.eh.MatchablePoint
import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectEnterExitInfo
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectMatcherError
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectPassInfo
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.RoadObjectDistanceInfo
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.eh.EHorizonObserver
import com.mapbox.navigation.core.trip.session.eh.RoadObjectMatcherObserver
import com.mapbox.navigation.core.trip.session.eh.RoadObjectsStore
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import com.mapbox.navigation.testing.ui.utils.coroutines.versionSwitchObserver
import com.mapbox.navigation.testing.utils.createTileStoreWithFastRetryBackoff
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.location.moveAlongTheRouteUntilTracking
import com.mapbox.navigation.testing.utils.location.stayOnPosition
import com.mapbox.navigation.testing.utils.offline.Tileset
import com.mapbox.navigation.testing.utils.offline.unpackTiles
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import com.mapbox.navigation.testing.utils.withoutInternet
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMapboxNavigationAPI::class)
class RoadObjectMatcherTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    /**
     * Files cleanup is required because the test expects the navigator to have no tiles for the
     * route before it falls back to the unpacked tileset.
     */
    @get:Rule
    val clearFilesRule = ClearFilesRule()

    override fun setupMockLocation(): Location {
        val origin = RoutesProvider.near_munich_with_waypoints(context).routeWaypoints.first()
        return mockLocationUpdatesRule.generateLocationUpdate {
            latitude = origin.latitude()
            longitude = origin.longitude()
        }
    }

    /**
     * Shows the expected usage of [MapboxNavigation.roadObjectMatcher]: register the observer once,
     * then match objects whenever needed. Objects are matched against the tiles present on the
     * device, and the observer keeps receiving results after navigation switches to another
     * tiles version.
     *
     * Online tiles come from the mock server, which has none, so the navigator falls back to the
     * unpacked tileset once it gives up on resolving the online version.
     */
    @Test
    fun custom_road_objects_are_matched_after_switch_to_fallback_tiles_version() =
        sdkTest(120_000) {
            val mockRoute = RoutesProvider.near_munich_with_waypoints(context)
            val originLocation = mockRoute.routeWaypoints.first()
            mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
            val tilesVersion = context.unpackTiles(Tileset.NearMunich)[TileDataDomain.NAVIGATION]!!
            withMapboxNavigation(
                tileStore = createTileStoreWithFastRetryBackoff(),
                historyRecorderRule = mapboxHistoryTestRule,
            ) { navigation ->
                // the observer has to be registered before matching, otherwise results are lost
                val matchResults = Channel<Expected<RoadObjectMatcherError, RoadObject>>(
                    Channel.UNLIMITED,
                )
                val matcherObserver = RoadObjectMatcherObserver { matchResults.trySend(it) }
                navigation.roadObjectMatcher.registerRoadObjectMatcherObserver(matcherObserver)

                // start listening now: the switch must not be missed while matching below
                val fallbackSwitch = async(start = CoroutineStart.UNDISPATCHED) {
                    navigation.versionSwitchObserver().first { it == tilesVersion }
                }

                val routes = stayOnPosition(originLocation, bearing = 0.0f) {
                    navigation.startTripSession()
                    navigation.requestRoutes(
                        RouteOptions.builder()
                            .applyDefaultNavigationOptions()
                            .applyLanguageAndVoiceUnitOptions(context)
                            .baseUrl(mockWebServerRule.baseUrl)
                            .coordinatesList(mockRoute.routeWaypoints)
                            .build(),
                    ).getSuccessfulResultOrThrowException().routes
                }
                navigation.setNavigationRoutesAsync(routes)
                navigation.moveAlongTheRouteUntilTracking(
                    routes[0],
                    mockLocationReplayerRule,
                    1,
                    endReplay = false,
                )

                val matchablePoint = MatchablePoint(OBJECT_ID, mockRoute.routeWaypoints[1])

                assertFalse("tiles version switched too early", fallbackSwitch.isCompleted)

                // the online version is still unresolved, so there are no tiles to match against:
                // matching against preloaded tiles only fails fast
                navigation.roadObjectMatcher.matchPointObjects(
                    listOf(matchablePoint),
                    useOnlyPreloadedTiles = true,
                )
                val resultBeforeSwitch = matchResults.receiveMatch()
                assertTrue(
                    "expected a matching error before the switch: $resultBeforeSwitch",
                    resultBeforeSwitch.isError,
                )

                withoutInternet {
                    fallbackSwitch.await()
                    // let the navigator settle on the new tiles version before matching
                    navigation.routeProgressUpdates()
                        .first { it.currentState == RouteProgressState.TRACKING }

                    // the unpacked tileset is in use now: the match loads the tiles it needs from it
                    navigation.roadObjectMatcher.matchPointObjects(listOf(matchablePoint))
                    assertMatched(OBJECT_ID, matchResults.receiveMatch())
                }

                navigation.roadObjectMatcher.unregisterRoadObjectMatcherObserver(matcherObserver)
            }
        }

    /**
     * Shows what happens to custom road objects in [MapboxNavigation.roadObjectsStore] when the
     * tiles version switches: objects added through [RoadObjectsStore.addCustomRoadObject] are
     * removed from the store, so they have to be matched and added again on the new version.
     *
     * The object is matched and added while the navigator runs on the unpacked tileset, which it
     * falls back to without a network. Back online, the navigator returns to the online tiles
     * version, which is the switch under test. Online tiles come from the real tile server.
     */
    @Test
    fun custom_road_objects_in_the_store_are_removed_on_tiles_version_switch() =
        sdkTest(180_000) {
            val mockRoute = RoutesProvider.near_munich_with_waypoints(context)
            val originLocation = mockRoute.routeWaypoints.first()
            mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
            val tilesVersion = context.unpackTiles(Tileset.NearMunich)[TileDataDomain.NAVIGATION]!!
            withMapboxNavigation(
                useRealTiles = true,
                tileStore = createTileStoreWithFastRetryBackoff(),
                historyRecorderRule = mapboxHistoryTestRule,
            ) { navigation ->
                val matchResults = Channel<Expected<RoadObjectMatcherError, RoadObject>>(
                    Channel.UNLIMITED,
                )
                val matcherObserver = RoadObjectMatcherObserver { matchResults.trySend(it) }
                navigation.roadObjectMatcher.registerRoadObjectMatcherObserver(matcherObserver)

                // store changes reach the app through the electronic horizon observer
                val addedIds = Channel<String>(Channel.UNLIMITED)
                val removedIds = Channel<String>(Channel.UNLIMITED)
                val eHorizonObserver = object : EHorizonObserver {
                    override fun onPositionUpdated(
                        position: EHorizonPosition,
                        distances: List<RoadObjectDistanceInfo>,
                    ) = Unit

                    override fun onRoadObjectEnter(objectEnterExitInfo: RoadObjectEnterExitInfo) =
                        Unit

                    override fun onRoadObjectExit(objectEnterExitInfo: RoadObjectEnterExitInfo) =
                        Unit

                    override fun onRoadObjectPassed(objectPassInfo: RoadObjectPassInfo) = Unit

                    override fun onRoadObjectAdded(roadObjectId: String) {
                        addedIds.trySend(roadObjectId)
                    }

                    override fun onRoadObjectUpdated(roadObjectId: String) = Unit

                    override fun onRoadObjectRemoved(roadObjectId: String) {
                        removedIds.trySend(roadObjectId)
                    }
                }
                navigation.registerEHorizonObserver(eHorizonObserver)

                val routes = stayOnPosition(originLocation, bearing = 0.0f) {
                    navigation.startTripSession()
                    navigation.requestRoutes(
                        RouteOptions.builder()
                            .applyDefaultNavigationOptions()
                            .applyLanguageAndVoiceUnitOptions(context)
                            .baseUrl(mockWebServerRule.baseUrl)
                            .coordinatesList(mockRoute.routeWaypoints)
                            .build(),
                    ).getSuccessfulResultOrThrowException().routes
                }
                navigation.setNavigationRoutesAsync(routes)
                navigation.moveAlongTheRouteUntilTracking(
                    routes[0],
                    mockLocationReplayerRule,
                    1,
                    endReplay = false,
                )

                val matchablePoint = MatchablePoint(OBJECT_ID, mockRoute.routeWaypoints[1])
                withoutInternet {
                    navigation.versionSwitchObserver().first { it == tilesVersion }
                    // let the navigator settle on the unpacked tileset before matching
                    navigation.routeProgressUpdates()
                        .first { it.currentState == RouteProgressState.TRACKING }

                    navigation.roadObjectMatcher.matchPointObjects(listOf(matchablePoint))
                    val matched = matchResults.receiveMatch()
                    assertMatched(OBJECT_ID, matched)

                    navigation.roadObjectsStore.addCustomRoadObject(matched.value!!)
                    addedIds.receiveId(OBJECT_ID)
                    assertNotNull(navigation.roadObjectsStore.getRoadObject(OBJECT_ID))
                }

                // back online the navigator returns to the online tiles version once the tiles
                // around the vehicle are downloaded
                mockLocationReplayerRule.stopAndClearEvents()
                stayOnPosition(originLocation, bearing = 0.0f) {
                    navigation.versionSwitchObserver().first { it != tilesVersion }
                    // the switch clears the store; a custom object keeps no geometry to be
                    // matched again, so it is gone for good
                    removedIds.receiveId(OBJECT_ID)
                    assertNull(navigation.roadObjectsStore.getRoadObject(OBJECT_ID))
                }

                navigation.unregisterEHorizonObserver(eHorizonObserver)
                navigation.roadObjectMatcher.unregisterRoadObjectMatcherObserver(matcherObserver)
            }
        }

    private suspend fun Channel<String>.receiveId(expectedId: String) =
        withTimeout(30.seconds) { receiveAsFlow().first { it == expectedId } }

    private suspend fun Channel<Expected<RoadObjectMatcherError, RoadObject>>.receiveMatch() =
        withTimeout(30.seconds) { receive() }

    private fun assertMatched(
        expectedId: String,
        result: Expected<RoadObjectMatcherError, RoadObject>,
    ) {
        assertTrue("matching $expectedId failed: ${result.error}", result.isValue)
        val roadObject = result.value!!
        assertEquals(expectedId, roadObject.id)
        assertEquals(RoadObjectType.CUSTOM, roadObject.objectType)
    }

    private companion object {
        const val OBJECT_ID = "custom-road-object"
    }
}
