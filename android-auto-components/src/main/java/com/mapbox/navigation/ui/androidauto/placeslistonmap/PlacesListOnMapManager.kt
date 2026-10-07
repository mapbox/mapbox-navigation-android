package com.mapbox.navigation.ui.androidauto.placeslistonmap

import androidx.annotation.VisibleForTesting
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.ItemList
import com.mapbox.common.dispatchers.SdkDispatchers
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.androidauto.internal.extensions.contentLimit
import com.mapbox.navigation.ui.androidauto.internal.extensions.getStyle
import com.mapbox.navigation.ui.androidauto.internal.extensions.mapboxNavigationForward
import com.mapbox.navigation.ui.androidauto.internal.extensions.styleFlow
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.location.CarLocationProvider
import com.mapbox.navigation.ui.androidauto.search.PlaceRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlacesListOnMapManager(
    private val placesListOnMapProvider: PlacesListOnMapProvider,
) : MapboxCarMapObserver {

    private var carMapSurface: MapboxCarMapSurface? = null
    private lateinit var coroutineScope: CoroutineScope
    private val placesLayerUtil: PlacesListOnMapLayerUtil = PlacesListOnMapLayerUtil()
    private var itemListJob: Job? = null
    private val navigationObserver = mapboxNavigationForward(this::onAttached) {
        itemListJob?.cancel()
        itemListJob = null
    }

    // The public placeRecords and itemList flows need a value before anything is loaded, so they
    // start empty. Their internal counterparts, loadedPlaceRecords (null until the first load)
    // and state (Loading until the first list), keep "not loaded yet" apart from "loaded, but
    // empty". Loaded data reaches both members of a pair together, through publishPlaces() and
    // publishItemList(); only Failed is set on state alone, as it has no public counterpart.
    private val _placeRecords = MutableStateFlow(listOf<PlaceRecord>())
    val placeRecords: StateFlow<List<PlaceRecord>> = _placeRecords.asStateFlow()

    private val loadedPlaceRecords = MutableStateFlow<List<PlaceRecord>?>(null)

    private val _state = MutableStateFlow<PlacesListState>(PlacesListState.Loading)

    /**
     * What the places list should render. Starts as [PlacesListState.Loading] and stays there
     * until the places are loaded and a location is known to measure their distance from.
     */
    internal val state: StateFlow<PlacesListState> = _state.asStateFlow()

    private val _placeSelected = MutableStateFlow<PlaceRecord?>(null)

    /**
     * The place that was selected last. Being a [StateFlow], it replays that selection to every
     * new collector, and selecting an equal place again emits nothing.
     */
    val placeSelected: StateFlow<PlaceRecord?> = _placeSelected.asStateFlow()

    private val _placeClicks = MutableSharedFlow<PlaceRecord>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * Emits once for every tap on a place, including repeated taps on the same place, and
     * replays nothing to new collectors.
     */
    internal val placeClicks: SharedFlow<PlaceRecord> = _placeClicks.asSharedFlow()

    private val _itemList = MutableStateFlow(ItemList.Builder().build())
    val itemList: StateFlow<ItemList> = _itemList.asStateFlow()

    @VisibleForTesting
    internal val placeClickListener = object : PlacesListItemClickListener {
        override fun onItemClick(placeRecord: PlaceRecord) {
            logAndroidAuto("PlacesListOnMapScreen request $placeRecord")
            _placeSelected.value = placeRecord
            _placeClicks.tryEmit(placeRecord)
        }
    }

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        super.onAttached(mapboxCarMapSurface)
        carMapSurface = mapboxCarMapSurface
        coroutineScope = MainScope()
        MapboxNavigationApp.registerObserver(navigationObserver)

        loadPlaceRecords(
            mapboxCarMapSurface.carContext
                .contentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST),
        )
        coroutineScope.launch {
            mapboxCarMapSurface.styleFlow().collectLatest { style ->
                val resources = mapboxCarMapSurface.carContext.resources
                placesLayerUtil.initializePlacesListOnMapLayer(style, resources)
                placeRecords.collect { addPlaceIconsToMap(style, it) }
            }
        }
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        super.onDetached(mapboxCarMapSurface)
        mapboxCarMapSurface.getStyle()?.let { placesLayerUtil.removePlacesListOnMapLayer(it) }
        MapboxNavigationApp.unregisterObserver(navigationObserver)
        carMapSurface = null
        coroutineScope.cancel()
    }

    private fun onAttached(mapboxNavigation: MapboxNavigation) {
        val placesListItemMapper = PlacesListItemMapper(
            PlaceMarkerRenderer(carMapSurface?.carContext!!),
            mapboxNavigation
                .navigationOptions
                .distanceFormatterOptions
                .unitType,
        )

        itemListJob?.cancel()
        itemListJob = coroutineScope.launch {
            loadedPlaceRecords.filterNotNull().collectLatest { placeRecords ->
                val itemList = if (placeRecords.isEmpty()) {
                    ItemList.Builder().build()
                } else {
                    // Places can load before the first location fix. Wait for it rather than
                    // building a list without distances that would never be rebuilt.
                    val locationProvider = CarLocationProvider.getRegisteredInstance()
                    val currentLocation = locationProvider.lastLocation()
                        ?: locationProvider.validLocation()
                    placesListItemMapper.mapToItemList(
                        currentLocation,
                        placeRecords,
                        placeClickListener,
                    )
                }
                publishItemList(itemList)
            }
        }
    }

    // The list, the map markers and the camera all use these records. Only places that can be
    // shown on the map are kept, and no more than the host shows in the list.
    private fun loadPlaceRecords(placeLimit: Int) {
        coroutineScope.launch {
            val expectedPlaceRecords = withContext(SdkDispatchers.IO) {
                placesListOnMapProvider.getPlaces()
            }
            expectedPlaceRecords.fold(
                {
                    logAndroidAuto(
                        "PlacesListOnMapScreen ${it.errorMessage}, ${it.throwable?.stackTrace}",
                    )
                    // Places loaded earlier stay on screen when reloading them fails.
                    if (loadedPlaceRecords.value == null) {
                        _state.value = PlacesListState.Failed
                    }
                },
                { placeRecords -> publishPlaces(placeRecords.placesToShow(placeLimit)) },
            )
        }
    }

    private fun publishPlaces(placeRecords: List<PlaceRecord>) {
        _placeRecords.value = placeRecords
        loadedPlaceRecords.value = placeRecords
    }

    private fun publishItemList(itemList: ItemList) {
        _itemList.value = itemList
        _state.value = PlacesListState.Loaded(itemList)
    }

    private fun addPlaceIconsToMap(style: Style, places: List<PlaceRecord>) {
        logAndroidAuto("PlacesListOnMapScreen addPlaceIconsToMap with ${places.size} places.")
        val features = places.mapNotNull { place ->
            val coordinate = place.coordinate ?: return@mapNotNull null
            Feature.fromGeometry(Point.fromLngLat(coordinate.longitude(), coordinate.latitude()))
        }
        val featureCollection = FeatureCollection.fromFeatures(features)
        placesLayerUtil.updatePlacesListOnMapLayer(style, featureCollection)
    }
}

/**
 * What [PlacesListOnMapScreen] renders for its places.
 */
internal sealed interface PlacesListState {
    object Loading : PlacesListState
    data class Loaded(val itemList: ItemList) : PlacesListState
    object Failed : PlacesListState
}

/**
 * The places shown in the list and on the map: only places with a coordinate, and no more than
 * the host shows in the list.
 */
internal fun List<PlaceRecord>.placesToShow(placeLimit: Int): List<PlaceRecord> =
    filter { it.coordinate != null }.take(placeLimit)
