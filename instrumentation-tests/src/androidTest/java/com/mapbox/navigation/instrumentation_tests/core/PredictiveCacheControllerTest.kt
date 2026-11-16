package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.adevinta.android.barista.rule.cleardata.ClearFilesRule
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Value
import com.mapbox.common.TileDataDomain
import com.mapbox.common.TileStoreOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMapsOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.base.options.PredictiveCacheMapsOptions
import com.mapbox.navigation.base.options.PredictiveCacheNavigationOptions
import com.mapbox.navigation.base.options.PredictiveCacheOptions
import com.mapbox.navigation.base.options.PredictiveCacheSearchOptions
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
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
import com.mapbox.navigation.ui.maps.PredictiveCacheController
import com.mapbox.search.offline.OfflineSearchEngine
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

/**
 * Shows the expected usage of [PredictiveCacheController]: create it once and keep it for as
 * long as navigation is used. The controller still requests tiles after navigation switches to
 * another tiles version, for every kind of tiles it caches.
 *
 * Predictive caching is observed through the tile requests it sends to the mock tile server.
 * Online navigation tiles come from the mock server, which has none, so the navigator falls back
 * to the unpacked tileset once it gives up on resolving the online version. That is the switch
 * under test. The caching radius reaches beyond the unpacked tileset, so tiles have to be
 * requested from the server.
 */
@OptIn(ExperimentalMapboxNavigationAPI::class, ExperimentalPreviewMapboxNavigationAPI::class)
class PredictiveCacheControllerTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

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
     * The navigation controller is pinned to the version of the unpacked tileset, because a
     * controller without a version follows the online version, which the mock server never
     * provides.
     */
    @Test
    fun navigation_tiles_are_requested_after_tiles_version_switch() = sdkTest(120_000) {
        val mockRoute = RoutesProvider.near_munich_with_waypoints(context)
        val originLocation = mockRoute.routeWaypoints.first()
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        val tilesVersion = context.unpackTiles(Tileset.NearMunich)[TileDataDomain.NAVIGATION]!!

        val tileRequestsAfterSwitch = Channel<String>(Channel.UNLIMITED)
        val allRequests = Collections.synchronizedList(mutableListOf<String>())
        val switchedToFallback = AtomicBoolean(false)
        mockWebServerRule.requestHandlers.add(
            MockRequestHandler { request ->
                // drop the query: it carries the access token
                val path = request.path?.substringBefore('?')
                if (path != null) {
                    allRequests.add(path)
                    if (switchedToFallback.get() && path.startsWith("/route-tiles/v2/")) {
                        tileRequestsAfterSwitch.trySend(path)
                    }
                }
                null // the request is recorded only, let the mock server respond as usual
            },
        )

        // the navigator points only a tile store it creates itself at the configured tiles host
        val tileStore = createTileStoreWithFastRetryBackoff().apply {
            setOption(
                TileStoreOptions.MAPBOX_APIURL,
                TileDataDomain.NAVIGATION,
                Value(mockWebServerRule.baseUrl),
            )
        }
        withMapboxNavigation(
            tileStore = tileStore,
            historyRecorderRule = mapboxHistoryTestRule,
        ) { navigation ->
            val predictiveCacheController = PredictiveCacheController(
                navigation,
                PredictiveCacheOptions.Builder()
                    .predictiveCacheNavigationOptions(
                        PredictiveCacheNavigationOptions.Builder()
                            .tilesConfiguration(
                                tilesDataset = TILES_DATASET,
                                tilesVersion = tilesVersion,
                                includeAdas = false,
                            )
                            .predictiveCacheLocationOptions(
                                PredictiveCacheLocationOptions.Builder()
                                    .currentLocationRadiusInMeters(100_000)
                                    .build(),
                            )
                            .build(),
                    )
                    .build(),
            )

            // start listening now: the switch must not be missed while driving below
            val fallbackSwitch = async(start = CoroutineStart.UNDISPATCHED) {
                navigation.versionSwitchObserver().first { it == tilesVersion }
                switchedToFallback.set(true)
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

            assertFalse("tiles version switched too early", fallbackSwitch.isCompleted)
            fallbackSwitch.await()

            // the controller recreated on the switch requests tiles of the pinned version
            val request = withTimeoutOrNull(30.seconds) {
                tileRequestsAfterSwitch.receiveAsFlow().first { it.contains(tilesVersion) }
            }
            assertNotNull(
                "no navigation tile request after the switch, requests seen: " +
                    synchronized(allRequests) { allRequests.joinToString("\n") },
                request,
            )

            predictiveCacheController.onDestroy()
        }
    }

    /**
     * ADAS tiles are cached together with the navigation tiles of the same pinned version.
     */
    @Test
    fun adas_tiles_are_requested_after_tiles_version_switch() = sdkTest(120_000) {
        val mockRoute = RoutesProvider.near_munich_with_waypoints(context)
        val originLocation = mockRoute.routeWaypoints.first()
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        val tilesVersion = context.unpackTiles(Tileset.NearMunich)[TileDataDomain.NAVIGATION]!!

        val tileRequestsAfterSwitch = Channel<String>(Channel.UNLIMITED)
        val allRequests = Collections.synchronizedList(mutableListOf<String>())
        val switchedToFallback = AtomicBoolean(false)
        mockWebServerRule.requestHandlers.add(
            MockRequestHandler { request ->
                // drop the query: it carries the access token
                val path = request.path?.substringBefore('?')
                if (path != null) {
                    allRequests.add(path)
                    if (switchedToFallback.get() && path.startsWith("/adas-tiles/v1/")) {
                        tileRequestsAfterSwitch.trySend(path)
                    }
                }
                null // the request is recorded only, let the mock server respond as usual
            },
        )

        // the navigator points only a tile store it creates itself at the configured tiles host
        val tileStore = createTileStoreWithFastRetryBackoff().apply {
            setOption(
                TileStoreOptions.MAPBOX_APIURL,
                TileDataDomain.ADAS,
                Value(mockWebServerRule.baseUrl),
            )
        }
        withMapboxNavigation(
            tileStore = tileStore,
            historyRecorderRule = mapboxHistoryTestRule,
        ) { navigation ->
            val predictiveCacheController = PredictiveCacheController(
                navigation,
                PredictiveCacheOptions.Builder()
                    .predictiveCacheNavigationOptions(
                        PredictiveCacheNavigationOptions.Builder()
                            .tilesConfiguration(
                                tilesDataset = TILES_DATASET,
                                tilesVersion = tilesVersion,
                                includeAdas = true,
                            )
                            .predictiveCacheLocationOptions(
                                PredictiveCacheLocationOptions.Builder()
                                    .currentLocationRadiusInMeters(100_000)
                                    .build(),
                            )
                            .build(),
                    )
                    .build(),
            )

            // start listening now: the switch must not be missed while driving below
            val fallbackSwitch = async(start = CoroutineStart.UNDISPATCHED) {
                navigation.versionSwitchObserver().first { it == tilesVersion }
                switchedToFallback.set(true)
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

            assertFalse("tiles version switched too early", fallbackSwitch.isCompleted)
            fallbackSwitch.await()

            // the controller recreated on the switch requests tiles of the pinned version
            val request = withTimeoutOrNull(30.seconds) {
                tileRequestsAfterSwitch.receiveAsFlow().first { it.contains(tilesVersion) }
            }
            assertNotNull(
                "no ADAS tile request after the switch, requests seen: " +
                    synchronized(allRequests) { allRequests.joinToString("\n") },
                request,
            )

            predictiveCacheController.onDestroy()
        }
    }

    /**
     * The map is only the owner of the maps controller: it is neither shown nor started. It is
     * created after the tileset is unpacked, because a map opens the tile store on creation and
     * would keep working with the database that unpacking replaces.
     */
    @Test
    fun maps_tiles_are_requested_after_tiles_version_switch() = sdkTest(120_000) {
        val mockRoute = RoutesProvider.near_munich_with_waypoints(context)
        val originLocation = mockRoute.routeWaypoints.first()
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        val tilesVersion = context.unpackTiles(Tileset.NearMunich)[TileDataDomain.NAVIGATION]!!

        val tileRequestsAfterSwitch = Channel<String>(Channel.UNLIMITED)
        val allRequests = Collections.synchronizedList(mutableListOf<String>())
        val switchedToFallback = AtomicBoolean(false)
        mockWebServerRule.requestHandlers.add(
            MockRequestHandler { request ->
                // drop the query: it carries the access token
                val path = request.path?.substringBefore('?')
                if (path != null) {
                    allRequests.add(path)
                    if (switchedToFallback.get() && path.startsWith("/tilepack/v1/")) {
                        tileRequestsAfterSwitch.trySend(path)
                    }
                }
                null // the request is recorded only, let the mock server respond as usual
            },
        )

        // maps controllers cache into the tile store configured for maps
        val tileStore = createTileStoreWithFastRetryBackoff().apply {
            setOption(
                TileStoreOptions.MAPBOX_APIURL,
                TileDataDomain.MAPS,
                Value(mockWebServerRule.baseUrl),
            )
        }
        MapboxMapsOptions.tileStore = tileStore
        val mapView = MapView(context)
        try {
            withMapboxNavigation(
                tileStore = tileStore,
                historyRecorderRule = mapboxHistoryTestRule,
            ) { navigation ->
                val predictiveCacheController = PredictiveCacheController(
                    navigation,
                    PredictiveCacheOptions.Builder().build(),
                )
                predictiveCacheController.createTilesetsMapController(
                    mapView.mapboxMap,
                    listOf(
                        PredictiveCacheMapsOptions.Builder()
                            .tilesets(listOf(MAPS_TILESET))
                            .predictiveCacheLocationOptions(
                                PredictiveCacheLocationOptions.Builder()
                                    .currentLocationRadiusInMeters(100_000)
                                    .build(),
                            )
                            .build(),
                    ),
                )

                // start listening now: the switch must not be missed while driving below
                val fallbackSwitch = async(start = CoroutineStart.UNDISPATCHED) {
                    navigation.versionSwitchObserver().first { it == tilesVersion }
                    switchedToFallback.set(true)
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

                assertFalse("tiles version switched too early", fallbackSwitch.isCompleted)
                fallbackSwitch.await()

                // the controller recreated on the switch requests tiles
                val request = withTimeoutOrNull(30.seconds) { tileRequestsAfterSwitch.receive() }
                assertNotNull(
                    "no maps tile request after the switch, requests seen: " +
                        synchronized(allRequests) { allRequests.joinToString("\n") },
                    request,
                )

                predictiveCacheController.onDestroy()
            }
        } finally {
            mapView.onDestroy()
            MapboxMapsOptions.tileStore = null
        }
    }

    @Test
    fun search_tiles_are_requested_after_tiles_version_switch() = sdkTest(120_000) {
        val mockRoute = RoutesProvider.near_munich_with_waypoints(context)
        val originLocation = mockRoute.routeWaypoints.first()
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        val tilesVersion = context.unpackTiles(Tileset.NearMunich)[TileDataDomain.NAVIGATION]!!

        val tileRequestsAfterSwitch = Channel<String>(Channel.UNLIMITED)
        val allRequests = Collections.synchronizedList(mutableListOf<String>())
        val switchedToFallback = AtomicBoolean(false)
        mockWebServerRule.requestHandlers.add(
            MockRequestHandler { request ->
                // drop the query: it carries the access token
                val path = request.path?.substringBefore('?')
                if (path != null) {
                    allRequests.add(path)
                    if (
                        switchedToFallback.get() &&
                        path.startsWith("/offline-search/v2/get-tile/")
                    ) {
                        tileRequestsAfterSwitch.trySend(path)
                    }
                }
                null // the request is recorded only, let the mock server respond as usual
            },
        )

        // search controllers cache into the tile store configured for maps
        val tileStore = createTileStoreWithFastRetryBackoff().apply {
            setOption(
                TileStoreOptions.MAPBOX_APIURL,
                TileDataDomain.SEARCH,
                Value(mockWebServerRule.baseUrl),
            )
        }
        MapboxMapsOptions.tileStore = tileStore
        try {
            withMapboxNavigation(
                tileStore = tileStore,
                historyRecorderRule = mapboxHistoryTestRule,
            ) { navigation ->
                val predictiveCacheController = PredictiveCacheController(
                    navigation,
                    PredictiveCacheOptions.Builder()
                        .predictiveCacheSearchOptionsList(
                            listOf(
                                PredictiveCacheSearchOptions.Builder(
                                    OfflineSearchEngine.createTilesetDescriptor(),
                                )
                                    .predictiveCacheLocationOptions(
                                        PredictiveCacheLocationOptions.Builder()
                                            .currentLocationRadiusInMeters(100_000)
                                            .build(),
                                    )
                                    .build(),
                            ),
                        )
                        .build(),
                )

                // start listening now: the switch must not be missed while driving below
                val fallbackSwitch = async(start = CoroutineStart.UNDISPATCHED) {
                    navigation.versionSwitchObserver().first { it == tilesVersion }
                    switchedToFallback.set(true)
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

                assertFalse("tiles version switched too early", fallbackSwitch.isCompleted)
                fallbackSwitch.await()

                // the controller recreated on the switch requests tiles
                val request = withTimeoutOrNull(30.seconds) { tileRequestsAfterSwitch.receive() }
                assertNotNull(
                    "no search tile request after the switch, requests seen: " +
                        synchronized(allRequests) { allRequests.joinToString("\n") },
                    request,
                )

                predictiveCacheController.onDestroy()
            }
        } finally {
            MapboxMapsOptions.tileStore = null
        }
    }

    private companion object {
        const val TILES_DATASET = "mapbox/driving-traffic"
        const val MAPS_TILESET = "mapbox://mapbox.mapbox-streets-v8"
    }
}
