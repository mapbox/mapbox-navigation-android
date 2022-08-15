package com.mapbox.androidauto.car.search

import androidx.annotation.StringRes
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
import com.mapbox.androidauto.car.feedback.core.CarFeedbackSearchOptions
import com.mapbox.androidauto.car.feedback.core.CarFeedbackSender
import com.mapbox.androidauto.car.feedback.ui.CarFeedbackAction
import com.mapbox.androidauto.car.preview.CarRoutePreviewScreen
import com.mapbox.androidauto.car.preview.CarRouteRequestCallback
import com.mapbox.androidauto.car.preview.RoutePreviewCarContext
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.internal.logAndroidAutoFailure
import com.mapbox.maps.MapboxExperimental
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.internal.extensions.attachCreated
import com.mapbox.search.result.SearchSuggestion
import kotlinx.coroutines.launch

/**
 * This screen allows the user to search for a destination.
 */
@OptIn(MapboxExperimental::class, ExperimentalPreviewMapboxNavigationAPI::class)
class PlaceSearchScreen(
    private val searchCarContext: SearchCarContext,
) : Screen(searchCarContext.carContext) {

    @VisibleForTesting
    internal var itemList = buildErrorItemList(R.string.car_search_no_results)

    // Cached to send to feedback.
    private var searchSuggestions: List<SearchSuggestion> = emptyList()

    private val carRouteRequestCallback = object : CarRouteRequestCallback {

        override fun onRoutesReady(placeRecord: PlaceRecord, routes: List<NavigationRoute>) {
            val routePreviewCarContext = RoutePreviewCarContext(searchCarContext.mainCarContext)

            screenManager.push(CarRoutePreviewScreen(routePreviewCarContext, placeRecord, routes))
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

    init {
        attachCreated(searchCarContext.carPlaceSearch)
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
            })
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        CarFeedbackAction(
                            searchCarContext.mainCarContext.mapboxCarMap,
                            CarFeedbackSender(),
                            searchCarContext.feedbackPollProvider
                                .getSearchFeedbackPoll(searchCarContext.carContext),
                        ) {
                            CarFeedbackSearchOptions(searchSuggestions = searchSuggestions)
                        }.getAction(this@PlaceSearchScreen)
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
                .onFailure { logAndroidAutoFailure("Search query failed", it) }
                .getOrDefault(emptyList())
            searchSuggestions = suggestions
            if (suggestions.isEmpty()) {
                onErrorItemList(R.string.car_search_no_results)
            } else {
                val builder = ItemList.Builder()
                suggestions.forEach { suggestion ->
                    builder.addItem(searchItemRow(suggestion))
                }
                itemList = builder.build()
                invalidate()
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
        return searchCarContext.distanceFormatter.formatDistance(distanceMeters)
    }

    private fun onClickSearch(searchSuggestion: SearchSuggestion) {
        logAndroidAuto("onClickSearch $searchSuggestion")
        lifecycleScope.launch {
            val searchResults = searchCarContext.carPlaceSearch.select(searchSuggestion)
                .onFailure { logAndroidAutoFailure("Search select failed", it) }
                .getOrDefault(emptyList())
            logAndroidAuto("onClickSearch select ${searchResults.joinToString()}")
            if (searchResults.isNotEmpty()) {
                searchCarContext.carRouteRequest.request(
                    PlaceRecordMapper.fromSearchResult(searchResults.first()),
                    carRouteRequestCallback
                )
            }
        }
    }

    private fun onErrorItemList(@StringRes stringRes: Int) {
        itemList = buildErrorItemList(stringRes)
        invalidate()
    }

    private fun buildErrorItemList(@StringRes stringRes: Int) = ItemList.Builder()
        .setNoItemsMessage(carContext.getString(stringRes))
        .build()
}
