package com.mapbox.navigation.ui.androidauto.placeslistonmap

import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.internal.extensions.addBackPressedHandler
import com.mapbox.navigation.ui.androidauto.location.CarLocationRenderer
import com.mapbox.navigation.ui.androidauto.navigation.CarLocationsOverviewCamera
import com.mapbox.navigation.ui.androidauto.preview.CarRoutePreviewRequestCallback
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.ui.androidauto.search.PlaceRecord
import com.mapbox.navigation.ui.androidauto.search.SearchCarContext
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

internal class PlacesListOnMapScreen @UiThread constructor(
    private val searchCarContext: SearchCarContext,
    placesProvider: PlacesListOnMapProvider,
    @MapboxScreen.Key private val mapboxScreenKey: String,
) : Screen(searchCarContext.carContext) {

    @VisibleForTesting
    var itemList = buildErrorItemList(R.string.car_search_no_results)

    private val carNavigationCamera = CarLocationsOverviewCamera()
    private var carLocationRenderer = CarLocationRenderer()
    private val placesListOnMapManager = PlacesListOnMapManager(placesProvider)

    init {
        addBackPressedHandler {
            searchCarContext.mapboxScreenManager.goBack()
        }
        repeatOnResumed {
            placesListOnMapManager.placeRecords.collect { placeRecords ->
                onPlaceRecordsChanged(placeRecords)
            }
        }
        repeatOnResumed {
            placesListOnMapManager.placeSelected.filterNotNull().collect { placeRecord ->
                onPlaceRecordSelected(placeRecord)
            }
        }
        repeatOnResumed {
            placesListOnMapManager.itemList.collect { invalidate() }
        }
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    MapboxNavigationApp.registerObserver(searchCarContext.routePreviewRequest)
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    MapboxNavigationApp.unregisterObserver(searchCarContext.routePreviewRequest)
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
            },
        )
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun onGetTemplate(): Template {
        val placesItemList = placesListOnMapManager.itemList.value
        return PlaceListNavigationTemplate.Builder()
            .setItemList(placesItemList)
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                searchCarContext.mapboxCarContext.options.actionStripProvider
                    .getActionStrip(this, mapboxScreenKey),
            )
            .build()
    }

    private fun onPlaceRecordsChanged(placeRecords: List<PlaceRecord>) {
        invalidate()
        val coordinates = placeRecords.mapNotNull { it.coordinate }
        carNavigationCamera.updateWithLocations(coordinates)
    }

    private fun onPlaceRecordSelected(placeRecord: PlaceRecord) {
        val carRouteRequestCallback = object : CarRoutePreviewRequestCallback {
            override fun onRoutesReady(placeRecord: PlaceRecord, routes: List<NavigationRoute>) {
                MapboxScreenManager.push(MapboxScreen.ROUTE_PREVIEW)
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
        searchCarContext.routePreviewRequest.request(placeRecord, carRouteRequestCallback)
    }

    private fun onErrorItemList(@StringRes stringRes: Int) {
        itemList = buildErrorItemList(stringRes)
        invalidate()
    }

    private fun buildErrorItemList(@StringRes stringRes: Int) = ItemList.Builder()
        .setNoItemsMessage(carContext.getString(stringRes))
        .build()

    private fun repeatOnResumed(block: suspend () -> Unit) {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                block()
            }
        }
    }
}
