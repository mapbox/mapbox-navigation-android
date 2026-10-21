package com.mapbox.navigation.ui.androidauto.placeslistonmap

import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.car.app.Screen
import androidx.car.app.annotations.RequiresCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.extensions.supportsUnifiedNavigation
import com.mapbox.navigation.ui.androidauto.internal.extensions.addBackPressedHandler
import com.mapbox.navigation.ui.androidauto.internal.setHeaderCompat
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
    @param:MapboxScreen.Key private val mapboxScreenKey: String,
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
        placesListOnMapManager.showsInListTemplate = carContext.supportsUnifiedNavigation()
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

    override fun onGetTemplate(): Template {
        return if (carContext.supportsUnifiedNavigation()) {
            buildMapWithContentTemplate()
        } else {
            buildPlaceListNavigationTemplate()
        }
    }

    @RequiresCarApi(7)
    private fun buildMapWithContentTemplate(): MapWithContentTemplate {
        val listTemplate = ListTemplate.Builder()
            .setHeaderCompat(carContext, title = null, startAction = Action.BACK)
            .setLoadingOrItemList(
                setLoading = { setLoading(it) },
                setItemList = { setSingleList(it) },
            )
            .build()
        return MapWithContentTemplate.Builder()
            .setContentTemplate(listTemplate)
            .setActionStrip(actionStrip())
            .build()
    }

    // PlaceListNavigationTemplate is deprecated with Car API 7 and is used only below it, where
    // MapWithContentTemplate is unavailable.
    @Suppress("DEPRECATION")
    private fun buildPlaceListNavigationTemplate(): Template {
        return androidx.car.app.navigation.model.PlaceListNavigationTemplate.Builder()
            .setHeaderCompat(carContext, title = null, startAction = Action.BACK)
            .setActionStrip(actionStrip())
            .setLoadingOrItemList(
                setLoading = { setLoading(it) },
                setItemList = { setItemList(it) },
            )
            .build()
    }

    // Both list templates throw if loading and an item list are set together.
    private inline fun <B> B.setLoadingOrItemList(
        setLoading: B.(Boolean) -> Unit,
        setItemList: B.(ItemList) -> Unit,
    ): B = apply {
        val itemList = itemListOrNull()
        if (itemList == null) setLoading(true) else setItemList(itemList)
    }

    /**
     * The list to show for the current state, or `null` while the places are loading.
     */
    private fun itemListOrNull(): ItemList? =
        when (val state = placesListOnMapManager.state.value) {
            PlacesListState.Loading -> null
            is PlacesListState.Loaded -> if (state.itemList.items.isEmpty()) {
                buildNoItemsList(R.string.car_search_no_results)
            } else {
                state.itemList
            }
            PlacesListState.Failed -> buildNoItemsList(R.string.car_search_error)
        }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun actionStrip(): ActionStrip =
        searchCarContext.mapboxCarContext.options.actionStripProvider
            .getActionStrip(this, mapboxScreenKey)

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
