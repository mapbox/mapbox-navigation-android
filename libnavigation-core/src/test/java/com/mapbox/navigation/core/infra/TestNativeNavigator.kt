package com.mapbox.navigation.core.infra

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.base.common.logger.Logger
import com.mapbox.bindgen.Expected
import com.mapbox.common.TileStore
import com.mapbox.common.TilesetDescriptor
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.core.infra.factories.createNavigationStatus
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.NativeNavigatorRecreationObserver
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.CacheHandle
import com.mapbox.navigator.ElectronicHorizonObserver
import com.mapbox.navigator.Experimental
import com.mapbox.navigator.FallbackVersionsObserver
import com.mapbox.navigator.FixLocation
import com.mapbox.navigator.GraphAccessor
import com.mapbox.navigator.HistoryRecorderHandle
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.NavigationStatusOrigin
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.NavigatorObserver
import com.mapbox.navigator.PredictiveCacheController
import com.mapbox.navigator.RoadObjectMatcher
import com.mapbox.navigator.RoadObjectsStore
import com.mapbox.navigator.RoadObjectsStoreObserver
import com.mapbox.navigator.RouteAlternativesControllerInterface
import com.mapbox.navigator.RouteInfo
import com.mapbox.navigator.RouterError
import com.mapbox.navigator.RouterInterface
import com.mapbox.navigator.TilesConfig
import io.mockk.mockk

/***
 * Emulates the behavior of the native navigator for testing.
 *
 * [TestNativeNavigator] doesn't cover all scenarios.
 * If you find an empty method instead of behavior emulation feel free to implement it.
 * Don't hesitate to write tests for this test double.
 *
 * Some behavior can't be easily emulated.
 * It's hard to parse a route and emulate location to trigger the status updates.
 * Use [TestNativeNavigator] as a stub for status update emulation.
 * Call [updateStatus] method to trigger all observers.
 */
class TestNativeNavigator private constructor() : MapboxNativeNavigator {

    companion object {
        fun create(): TestNativeNavigator = TestNativeNavigator()
    }

    override fun create(
        deviceProfile: DeviceProfile,
        navigatorConfig: NavigatorConfig,
        tilesConfig: TilesConfig,
        historyDir: String?,
        logger: Logger,
        accessToken: String
    ): MapboxNativeNavigator {
        return this
    }

    override fun recreate(
        deviceProfile: DeviceProfile,
        navigatorConfig: NavigatorConfig,
        tilesConfig: TilesConfig,
        historyDir: String?,
        logger: Logger,
        accessToken: String
    ) {
    }

    override fun resetRideSession() {}

    var updateLocationDelegate: suspend TestNativeNavigator.(rawLocation: FixLocation) -> Boolean =
        { location: FixLocation ->
            updateStatus(
                NavigationStatusOrigin.LOCATION_UPDATE,
                createNavigationStatus(location = location)
            )
            true
        }
    override suspend fun updateLocation(rawLocation: FixLocation): Boolean {
        return updateLocationDelegate.invoke(this, rawLocation)
    }

    override fun addNavigatorObserver(navigatorObserver: NavigatorObserver) {
        navigatorObservers.add(navigatorObserver)
    }

    override fun removeNavigatorObserver(navigatorObserver: NavigatorObserver) {}

    override suspend fun setRoute(routes: List<DirectionsRoute>, legIndex: Int): RouteInfo? {
        return null
    }

    override suspend fun updateAnnotations(route: DirectionsRoute) {}

    override suspend fun getCurrentBannerInstruction(): BannerInstruction? {
        return null
    }

    var updateLegIndexDelegate: suspend (legIndex: Int) -> Boolean = { true }
    override suspend fun updateLegIndex(legIndex: Int): Boolean {
        return updateLegIndexDelegate.invoke(legIndex)
    }

    override suspend fun getRoute(url: String): Expected<RouterError, String> {
        return mockk()
    }

    override fun getHistoryRecorderHandle(): HistoryRecorderHandle? {
        return null
    }

    override fun setElectronicHorizonObserver(eHorizonObserver: ElectronicHorizonObserver?) {}

    override fun addRoadObjectsStoreObserver(roadObjectsStoreObserver: RoadObjectsStoreObserver) {}

    override fun removeRoadObjectsStoreObserver(
        roadObjectsStoreObserver: RoadObjectsStoreObserver
    ) { }

    override fun setFallbackVersionsObserver(
        fallbackVersionsObserver: FallbackVersionsObserver?
    ) { }

    override fun setNativeNavigatorRecreationObserver(
        nativeNavigatorRecreationObserver: NativeNavigatorRecreationObserver
    ) { }

    override fun unregisterAllObservers() { }

    override fun createMapsPredictiveCacheControllerTileVariant(
        tileStore: TileStore,
        tileVariant: String,
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions
    ): PredictiveCacheController {
        return mockk()
    }

    override fun createMapsPredictiveCacheController(
        tileStore: TileStore,
        tilesetDescriptor: TilesetDescriptor,
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions
    ): PredictiveCacheController {
        return mockk()
    }

    override fun createNavigationPredictiveCacheController(
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions
    ): PredictiveCacheController {
        return mockk()
    }

    override fun createRouteAlternativesController(): RouteAlternativesControllerInterface {
        return mockk()
    }

    private val navigatorObservers = mutableListOf<NavigatorObserver>()

    override val graphAccessor: GraphAccessor?
        get() = null
    override val roadObjectsStore: RoadObjectsStore?
        get() = null
    override val cache: CacheHandle
        get() = mockk()
    override val roadObjectMatcher: RoadObjectMatcher?
        get() = null
    override val experimental: Experimental
        get() = mockk()
    override val router: RouterInterface
        get() = mockk()

    fun updateStatus(origin: NavigationStatusOrigin, navigationStatus: NavigationStatus) {
        for (o in navigatorObservers) {
            o.onStatus(origin, navigationStatus)
        }
    }
}
