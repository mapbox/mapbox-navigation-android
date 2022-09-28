package com.mapbox.androidauto.car.placeslistonmap

import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mapbox.androidauto.R
import com.mapbox.androidauto.car.action.MapboxActionProvider
import com.mapbox.androidauto.car.location.CarLocationRenderer
import com.mapbox.androidauto.car.navigation.CarLocationsOverviewCamera
import com.mapbox.androidauto.car.preview.CarRoutePreviewScreen
import com.mapbox.androidauto.car.preview.CarRouteRequestCallback
import com.mapbox.androidauto.car.preview.RoutePreviewCarContext
import com.mapbox.androidauto.car.search.PlaceRecord
import com.mapbox.androidauto.car.search.SearchCarContext
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.maps.MapboxExperimental
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@MapboxExperimental
class PlacesListOnMapScreen @UiThread constructor(
    private val searchCarContext: SearchCarContext,
    placesProvider: PlacesListOnMapProvider,
    private val actionProviders: List<MapboxActionProvider>,
) : Screen(searchCarContext.carContext) {

    @VisibleForTesting
    var itemList = buildErrorItemList(R.string.car_search_no_results)

    private val carNavigationCamera = CarLocationsOverviewCamera()
    private var carLocationRenderer = CarLocationRenderer()
    private val placesListOnMapManager = PlacesListOnMapManager(placesProvider)

    init {
        lifecycleScope.launch {
            placesListOnMapManager.placeRecords.collect { placeRecords ->
                onPlaceRecordsChanged(placeRecords)
            }
        }
        lifecycleScope.launch {
            placesListOnMapManager.placeSelected.filterNotNull().collect { placeRecord ->
                onPlaceRecordSelected(placeRecord)
            }
        }
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                super.onCreate(owner)
                MapboxNavigationApp.registerObserver(searchCarContext.carRouteRequest)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                MapboxNavigationApp.unregisterObserver(searchCarContext.carRouteRequest)
            }

            override fun onResume(owner: LifecycleOwner) {
                searchCarContext.mapboxCarMap
                    .registerObserver(carNavigationCamera)
                    .registerObserver(carLocationRenderer)
                    .registerObserver(placesListOnMapManager)
            }

            override fun onPause(owner: LifecycleOwner) {
                super.onPause(owner)
                searchCarContext.mapboxCarMap
                    .unregisterObserver(carNavigationCamera)
                    .unregisterObserver(carLocationRenderer)
                    .unregisterObserver(placesListOnMapManager)
            }
        })
    }

    override fun onGetTemplate(): Template {
        val placesItemList = placesListOnMapManager.currentItemList() ?: ItemList.Builder().build()
        val actionStrip = ActionStrip.Builder().apply {
            actionProviders.forEach {
                when (it) {
                    is MapboxActionProvider.ScreenActionProvider -> {
                        this.addAction(it.getAction(this@PlacesListOnMapScreen))
                    }
                    is MapboxActionProvider.ActionProvider -> {
                        this.addAction(it.getAction())
                    }
                }
            }
        }.build()

        return PlaceListNavigationTemplate.Builder()
            .setItemList(placesItemList)
            .setHeaderAction(Action.BACK)
            .setActionStrip(actionStrip)
            .build()
    }

    private fun onPlaceRecordsChanged(placeRecords: List<PlaceRecord>) {
        invalidate()
        val coordinates = placeRecords.mapNotNull { it.coordinate }
        carNavigationCamera.updateWithLocations(coordinates)
    }

    private fun onPlaceRecordSelected(placeRecord: PlaceRecord) {
        val carRouteRequestCallback = object : CarRouteRequestCallback {
            override fun onRoutesReady(placeRecord: PlaceRecord, routes: List<NavigationRoute>) {
                onPlaceRecordSelectedRoutesReady(placeRecord, routes)
            }

            override fun onUnknownCurrentLocation() {
                onErrorItemList(R.string.car_search_unknown_current_location)
            }

            override fun onDestinationLocationUnknown() {
                onErrorItemList(R.string.car_search_unknown_search_location)
            }

            override fun onNoRoutesFound() {
                onErrorItemList(R.string.car_search_no_results)
            }
        }
        searchCarContext.carRouteRequest.request(placeRecord, carRouteRequestCallback)
    }

    private fun onPlaceRecordSelectedRoutesReady(
        placeRecord: PlaceRecord,
        routes: List<NavigationRoute>
    ) {
        logAndroidAuto("PlacesListOnMapScreen go to CarRoutePreviewScreen ${routes.size}")
        val routePreviewCarContext = RoutePreviewCarContext(searchCarContext.mainCarContext)
        screenManager.push(CarRoutePreviewScreen(routePreviewCarContext, placeRecord, routes))
    }

    private fun onErrorItemList(@StringRes stringRes: Int) {
        itemList = buildErrorItemList(stringRes)
        invalidate()
    }

    private fun buildErrorItemList(@StringRes stringRes: Int) = ItemList.Builder()
        .setNoItemsMessage(carContext.getString(stringRes))
        .build()
}
