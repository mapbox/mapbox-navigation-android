package com.mapbox.androidauto.car.search

import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.model.SearchTemplate
import androidx.car.app.model.Template
import androidx.lifecycle.lifecycleScope
import com.mapbox.androidauto.R
import com.mapbox.androidauto.car.feedback.ui.CarFeedbackAction
import com.mapbox.androidauto.car.navigation.CarDistanceFormatter
import com.mapbox.androidauto.car.preview.CarRoutePreviewRequestCallback
import com.mapbox.androidauto.internal.car.extensions.addBackPressedHandler
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.internal.logAndroidAutoFailure
import com.mapbox.androidauto.screenmanager.MapboxScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.internal.extensions.attachCreated
import com.mapbox.search.result.SearchSuggestion
import kotlinx.coroutines.launch

/**
 * This screen allows the user to search for a destination.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class PlaceSearchScreen @UiThread constructor(
    private val searchCarContext: SearchCarContext,
) : Screen(searchCarContext.carContext) {

    @VisibleForTesting
    internal var itemList = buildNoItemsList(R.string.car_search_no_results)
        private set(value) {
            field = value
            invalidate()
        }

    private val carRouteRequestCallback = object : CarRoutePreviewRequestCallback {

        override fun onRoutesReady(placeRecord: PlaceRecord, routes: List<NavigationRoute>) {
            MapboxScreenManager.push(MapboxScreen.ROUTE_PREVIEW)
        }

        override fun onUnknownCurrentLocation() {
            itemList = buildNoItemsList(R.string.car_search_unknown_current_location)
        }

        override fun onDestinationLocationUnknown() {
            itemList = buildNoItemsList(R.string.car_search_unknown_search_location)
        }

        override fun onNoRoutesFound() {
            itemList = buildNoItemsList(R.string.car_search_no_results)
        }
    }

    init {
        addBackPressedHandler {
            searchCarContext.mapboxScreenManager.goBack()
        }
        attachCreated(searchCarContext.routePreviewRequest, searchCarContext.carPlaceSearch)
    }

    override fun onGetTemplate(): Template {
        return SearchTemplate.Builder(
            object : SearchTemplate.SearchCallback {
                override fun onSearchTextChanged(searchText: String) {
                    doSearch(searchText)
                }

                override fun onSearchSubmitted(searchTerm: String) {
                    logAndroidAutoFailure("onSearchSubmitted not implemented $searchTerm")
                }
            }
        )
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        CarFeedbackAction(
                            MapboxScreen.SEARCH_FEEDBACK
                        ).getAction(this@PlaceSearchScreen)
                    )
                    .build()
            )
            .setShowKeyboardByDefault(false)
            .setItemList(itemList)
            .build()
    }

    @VisibleForTesting
    internal fun doSearch(searchText: String) {
        lifecycleScope.launch {
            val suggestions = searchCarContext.carPlaceSearch.search(searchText)
                .getOrDefault(emptyList())
            itemList = if (suggestions.isEmpty()) {
                buildNoItemsList(R.string.car_search_no_results)
            } else {
                val builder = ItemList.Builder()
                suggestions.forEach { suggestion ->
                    builder.addItem(searchItemRow(suggestion))
                }
                builder.build()
            }
        }
    }

    private fun searchItemRow(suggestion: SearchSuggestion) = Row.Builder()
        .setTitle(suggestion.name)
        .addText(formatDistance(suggestion))
        .setOnClickListener { onClickSearch(suggestion) }
        .build()

    private fun formatDistance(searchSuggestion: SearchSuggestion): CharSequence {
        val distanceMeters = searchSuggestion.distanceMeters ?: return ""
        return CarDistanceFormatter.formatDistance(distanceMeters)
    }

    private fun onClickSearch(searchSuggestion: SearchSuggestion) {
        logAndroidAuto("onClickSearch $searchSuggestion")
        lifecycleScope.launch {
            val searchResults = searchCarContext.carPlaceSearch.select(searchSuggestion)
                .getOrDefault(emptyList())
            logAndroidAuto("onClickSearch select ${searchResults.joinToString()}")
            if (searchResults.isNotEmpty()) {
                searchCarContext.routePreviewRequest.request(
                    PlaceRecordMapper.fromSearchResult(searchResults.first()),
                    carRouteRequestCallback
                )
            }
        }
    }

    private fun buildNoItemsList(@StringRes stringRes: Int) = ItemList.Builder()
        .setNoItemsMessage(carContext.getString(stringRes))
        .build()
}
