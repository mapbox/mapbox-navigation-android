package com.mapbox.navigation.ui.androidauto.search

import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.model.SearchTemplate
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.internal.extensions.addBackPressedHandler
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAutoFailure
import com.mapbox.navigation.ui.androidauto.navigation.CarDistanceFormatter
import com.mapbox.navigation.ui.androidauto.preview.CarRoutePreviewRequestCallback
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.search.result.SearchSuggestion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * This screen allows the user to search for a destination.
 */
internal class PlaceSearchScreen @UiThread constructor(
    private val searchCarContext: SearchCarContext,
) : Screen(searchCarContext.carContext) {

    @VisibleForTesting
    internal var uiState: SearchUiState =
        SearchUiState.NoItems(R.string.car_search_no_results)
        private set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    private var searchJob: Job? = null

    private val carRouteRequestCallback = object : CarRoutePreviewRequestCallback {

        override fun onRoutesReady(placeRecord: PlaceRecord, routes: List<NavigationRoute>) {
            val screenManager = searchCarContext.mapboxScreenManager
            if (
                !screenManager.isScreenBelowTop(MapboxScreen.NAVIGATION) ||
                !screenManager.goBack()
            ) {
                MapboxScreenManager.push(MapboxScreen.ROUTE_PREVIEW)
            }
        }

        override fun onUnknownCurrentLocation() {
            uiState = SearchUiState.NoItems(R.string.car_search_unknown_current_location)
        }

        override fun onDestinationLocationUnknown() {
            uiState = SearchUiState.NoItems(R.string.car_search_unknown_search_location)
        }

        override fun onNoRoutesFound() {
            uiState = SearchUiState.NoItems(R.string.car_search_no_results)
        }
    }

    init {
        addBackPressedHandler {
            searchCarContext.mapboxScreenManager.goBack()
        }
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    MapboxNavigationApp.registerObserver(searchCarContext.routePreviewRequest)
                    MapboxNavigationApp.registerObserver(searchCarContext.carPlaceSearch)
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    MapboxNavigationApp.unregisterObserver(searchCarContext.routePreviewRequest)
                    MapboxNavigationApp.unregisterObserver(searchCarContext.carPlaceSearch)
                }
            },
        )
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun onGetTemplate(): Template {
        val builder = SearchTemplate.Builder(
            object : SearchTemplate.SearchCallback {
                override fun onSearchTextChanged(searchText: String) {
                    doSearch(searchText, debounce = true)
                }

                override fun onSearchSubmitted(searchTerm: String) {
                    doSearch(searchTerm, debounce = false)
                }
            },
        )
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                searchCarContext.mapboxCarContext.options.actionStripProvider
                    .getActionStrip(this, MapboxScreen.SEARCH),
            )
            .setShowKeyboardByDefault(false)
        val itemList = when (val state = uiState) {
            is SearchUiState.Content -> ItemList.Builder().apply {
                state.suggestions.forEach { addItem(searchItemRow(it)) }
            }.build()
            is SearchUiState.NoItems -> buildNoItemsList(state.messageRes)
            is SearchUiState.Error -> buildNoItemsList(R.string.car_search_error)
            SearchUiState.Loading -> null
        }
        // SearchTemplate.Builder throws if both loading and an item list are set, so isLoading is
        // derived from itemList being null rather than tracked as a second, separately-set flag.
        builder.setLoading(itemList == null)
        if (itemList != null) {
            builder.setItemList(itemList)
        }
        return builder.build()
    }

    @VisibleForTesting
    internal fun doSearch(searchText: String, debounce: Boolean) {
        searchJob?.cancel()
        uiState = SearchUiState.Loading
        searchJob = lifecycleScope.launch {
            try {
                if (debounce) {
                    delay(TYPING_DEBOUNCE_MILLIS)
                }
                val result = searchCarContext.carPlaceSearch.search(searchText)
                uiState = result.fold(
                    onSuccess = { suggestions ->
                        if (suggestions.isEmpty()) {
                            SearchUiState.NoItems(R.string.car_search_no_results)
                        } else {
                            SearchUiState.Content(suggestions)
                        }
                    },
                    onFailure = { e -> searchFailed(e) },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = searchFailed(e)
            }
        }
    }

    private fun searchFailed(e: Throwable): SearchUiState.Error {
        logAndroidAutoFailure("doSearch failed: ${e.message}", e)
        return SearchUiState.Error(e)
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
                    carRouteRequestCallback,
                )
            }
        }
    }

    private fun buildNoItemsList(@StringRes stringRes: Int) = ItemList.Builder()
        .setNoItemsMessage(carContext.getString(stringRes))
        .build()

    private companion object {
        // Debounces onSearchTextChanged before it reaches CarPlaceSearchImpl.search(), which
        // separately passes its own, much shorter SearchOptions#requestDebounce to the Search
        // SDK itself -- the two are independent knobs on the same keystroke-to-request path.
        private const val TYPING_DEBOUNCE_MILLIS = 300L
    }
}

/**
 * Represents what [PlaceSearchScreen] should render for the current search query: a request in
 * flight, a non-empty suggestion list, a successfully resolved but empty result, or a failure.
 * A superseded (cancelled) search never reaches any of these states.
 */
internal sealed class SearchUiState {
    object Loading : SearchUiState()
    data class Content(val suggestions: List<SearchSuggestion>) : SearchUiState()
    data class NoItems(@StringRes val messageRes: Int) : SearchUiState()
    data class Error(val throwable: Throwable) : SearchUiState()
}
