package com.mapbox.navigation.qa_test_app.view

import android.app.Activity
import android.os.Bundle
import android.view.View
import com.mapbox.bindgen.Value
import com.mapbox.common.TileDataDomain
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.common.TileStore
import com.mapbox.common.TileStoreOptions
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.core.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.routealternatives.OnlineRouteAlternativesSwitch
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityNavigationOfflineOnlineViewBinding
import com.mapbox.navigation.qa_test_app.databinding.LayoutDrawerMenuNavViewBinding
import com.mapbox.navigation.qa_test_app.utils.startActivity
import com.mapbox.navigation.qa_test_app.view.base.DrawerActivity
import com.mapbox.navigation.qa_test_app.view.customnavview.NavigationViewController
import com.mapbox.navigation.utils.internal.toPoint
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMapboxNavigationAPI::class)
class MapboxNavigationViewOfflineOnlineRouteSwitchActivity : DrawerActivity() {

    private lateinit var binding: LayoutActivityNavigationOfflineOnlineViewBinding
    private lateinit var menuBinding: LayoutDrawerMenuNavViewBinding

    override fun onCreateContentView(): View {
        binding = LayoutActivityNavigationOfflineOnlineViewBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreateMenuView(): View {
        menuBinding = LayoutDrawerMenuNavViewBinding.inflate(layoutInflater)
        return menuBinding.root
    }

    private lateinit var controller: NavigationViewController

    private var onlineRouteSwitch = OnlineRouteAlternativesSwitch()

    private val tilesLoadingObserver = LoadAreaAroundObserver { state ->
        setTitle("${state.requestedTiles}/${state.requestedTiles}, ${state.primaryRouteId}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val tileStore = TileStore.create()
        tileStore.setOption(
            TileStoreOptions.MAPBOX_ACCESS_TOKEN,
            TileDataDomain.NAVIGATION,
            Value.valueOf(getString(R.string.mapbox_access_token))
        )
        MapboxNavigationApp.setup(
            NavigationOptions.Builder(applicationContext)
                .accessToken(getString(R.string.mapbox_access_token))
                .routingTilesOptions(
                    RoutingTilesOptions.Builder()
                        .tileStore(tileStore)
                        .build()
                )
                .build()
        )
        super.onCreate(savedInstanceState)
        controller = NavigationViewController(this, binding.navigationView)

        menuBinding.toggleReplay.isChecked = binding.navigationView.api.isReplayEnabled()
        menuBinding.toggleReplay.setOnCheckedChangeListener { _, isChecked ->
            binding.navigationView.api.routeReplayEnabled(isChecked)
        }

        MapboxNavigationApp.registerObserver(onlineRouteSwitch)
        MapboxNavigationApp.registerObserver(tilesLoadingObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        MapboxNavigationApp.unregisterObserver(onlineRouteSwitch)
        MapboxNavigationApp.unregisterObserver(tilesLoadingObserver)
    }

    companion object {
        fun startActivity(
            parent: Activity,
        ) {
            parent.startActivity<MapboxNavigationViewOfflineOnlineRouteSwitchActivity>()
        }
    }
}

data class UIState(val requestedTiles: Long, val loaded: Long, val primaryRouteId: String)

class LoadAreaAroundObserver(
    val progressCallback: (UIState) -> Unit
) : MapboxNavigationObserver {

    private lateinit var context: CoroutineScope

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        context = CoroutineScope(Dispatchers.Main + SupervisorJob())
        context.launch {
            mapboxNavigation.flowLocationMatcherResult()
                .map { it.enhancedLocation.toPoint() }
                .distinctUntilChanged { old, new -> TurfMeasurement.distance(old, new) < 1 }
                .flatMapMerge {
                    val radius = 2.0
                    val halfTheRadius = radius / 2
                    val navTilesetDescriptor = mapboxNavigation.tilesetDescriptorFactory.getLatest()
                    // Do not test it near greenwich or equator
                    val region = Polygon.fromLngLats(
                        listOf(
                            listOf(
                                Point.fromLngLat(
                                    it.longitude() - halfTheRadius,
                                    it.latitude() - halfTheRadius
                                ),
                                Point.fromLngLat(
                                    it.longitude() - halfTheRadius,
                                    it.latitude() + halfTheRadius
                                ),
                                Point.fromLngLat(
                                    it.longitude() + halfTheRadius,
                                    it.latitude() + halfTheRadius
                                ),
                                Point.fromLngLat(
                                    it.longitude() + halfTheRadius,
                                    it.latitude() - halfTheRadius
                                ),
                            )
                        )
                    )
                    val tileRegionLoadOptions = TileRegionLoadOptions.Builder()
                        .geometry(region)
                        .descriptors(listOf(navTilesetDescriptor))
                        .build()
                    val tileStore = mapboxNavigation.navigationOptions
                        .routingTilesOptions
                        .tileStore!!
                    callbackFlow {
                        val cancellation = tileStore.loadTileRegion(
                            "$it-radius-$radius",
                            tileRegionLoadOptions,
                            { progres -> this.trySend(progres) },
                            { progres -> this.close() },
                        )
                        awaitClose { cancellation.cancel() }
                    }
                }
                .combine(mapboxNavigation.flowRoutesUpdated()) { progres, routes ->
                    UIState(
                        progres.requiredResourceCount,
                        progres.completedResourceCount,
                        routes.navigationRoutes.firstOrNull()?.id ?: "null"
                    )
                }
                .collect {
                    progressCallback(it)
                }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        context.cancel()
    }
}
