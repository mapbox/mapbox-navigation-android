package com.mapbox.navigation.ui.androidauto.placeslistonmap

import androidx.annotation.StringRes
import androidx.annotation.UiThread
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
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.internal.extensions.addBackPressedHandler
import com.mapbox.navigation.ui.androidauto.location.CarLocationRenderer
import com.mapbox.navigation.ui.androidauto.navigation.CarLocationsOverviewCamera
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.search.PlaceRecord
import com.mapbox.navigation.ui.androidauto.search.ResumedRoutePreviewCallback
import com.mapbox.navigation.ui.androidauto.search.SearchCarContext
import com.mapbox.navigation.ui.androidauto.search.showSearchToast
import kotlinx.coroutines.launch

internal class PlacesListOnMapScreen @UiThread constructor(
    private val searchCarContext: SearchCarContext,
    placesProvider: PlacesListOnMapProvider,
    @MapboxScreen.Key private val mapboxScreenKey: String,
    private val placesListOnMapManager: PlacesListOnMapManager =
        PlacesListOnMapManager(placesProvider),
) : Screen(searchCarContext.carContext) {

    private val carNavigationCamera = CarLocationsOverviewCamera()
    private val carLocationRenderer = CarLocationRenderer()

    private val carRouteRequestCallback = ResumedRoutePreviewCallback(
        lifecycle,
        searchCarContext.mapboxScreenManager,
        searchCarContext.routePreviewRequest,
    ) { messageRes -> carContext.showSearchToast(messageRes) }

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
            placesListOnMapManager.placeClicks.collect { placeRecord ->
                onPlaceRecordSelected(placeRecord)
            }
        }
        repeatOnResumed {
            placesListOnMapManager.state.collect { invalidate() }
        }
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
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
        val builder = PlaceListNavigationTemplate.Builder()
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                searchCarContext.mapboxCarContext.options.actionStripProvider
                    .getActionStrip(this, mapboxScreenKey),
            )
        // PlaceListNavigationTemplate.Builder throws if both loading and an item list are set.
        when (val state = placesListOnMapManager.state.value) {
            PlacesListState.Loading -> builder.setLoading(true)
            is PlacesListState.Loaded -> builder.setItemList(
                if (state.itemList.items.isEmpty()) {
                    buildNoItemsList(R.string.car_search_no_results)
                } else {
                    state.itemList
                },
            )
            PlacesListState.Failed -> builder.setItemList(
                buildNoItemsList(R.string.car_search_error),
            )
        }
        return builder.build()
    }

    private fun buildNoItemsList(@StringRes stringRes: Int) = ItemList.Builder()
        .setNoItemsMessage(carContext.getString(stringRes))
        .build()

    private fun onPlaceRecordsChanged(placeRecords: List<PlaceRecord>) {
        invalidate()
        val coordinates = placeRecords.mapNotNull { it.coordinate }
        carNavigationCamera.updateWithLocations(coordinates)
    }

    private fun onPlaceRecordSelected(placeRecord: PlaceRecord) {
        searchCarContext.routePreviewRequest.request(placeRecord, carRouteRequestCallback)
    }

    private fun repeatOnResumed(block: suspend () -> Unit) {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                block()
            }
        }
    }
}
