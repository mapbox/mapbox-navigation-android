package com.mapbox.navigation.ui.androidauto.placeslistonmap

import androidx.annotation.VisibleForTesting
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.ItemList
import com.mapbox.annotation.MapboxExperimental
import com.mapbox.common.dispatchers.SdkDispatchers
import com.mapbox.common.location.Location
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.androidauto.internal.extensions.contentLimit
import com.mapbox.navigation.ui.androidauto.internal.extensions.getStyle
import com.mapbox.navigation.ui.androidauto.internal.extensions.mapboxNavigationForward
import com.mapbox.navigation.ui.androidauto.internal.extensions.styleFlow
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAutoFailure
import com.mapbox.navigation.ui.androidauto.internal.rowTitle
import com.mapbox.navigation.ui.androidauto.location.CarLocationProvider
import com.mapbox.navigation.ui.androidauto.search.PlaceRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.ensureActive
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
    private val navigationObserver = mapboxNavigationForward(
        { mapboxNavigation -> startItemList(mapboxNavigation) },
        { onNavigationDetached() },
    )

    // The public placeRecords and itemList flows need a value before anything is loaded, so they
    // start empty. Their internal counterparts, loadedPlaceRecords (null until the first load)
    // and state (Loading until the first list), keep "not loaded yet" apart from "loaded, but
    // empty". Loaded data reaches both members of a pair together, through publishPlaces() and
    // publishItemList(); only Failed is set on state alone, as it has no public counterpart.
    private val _placeRecords = MutableStateFlow(listOf<PlaceRecord>())

    /**
     * The places shown in the list, as markers on the map, and framed by the camera. Only places
     * with a coordinate and a name or description are kept, and no more than the host shows in
     * its list (its content limit). Empty until the places are loaded.
     */
    val placeRecords: StateFlow<List<PlaceRecord>> = _placeRecords.asStateFlow()

    private val loadedPlaceRecords = MutableStateFlow<List<PlaceRecord>?>(null)

    private val _state = MutableStateFlow<PlacesListState>(PlacesListState.Loading)

    /**
     * What the places list should render. Starts as [PlacesListState.Loading] and stays there
     * until the places are loaded. A list is shown even without a location, then without
     * distances, and it is rebuilt with distances when the first location arrives.
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

    /**
     * Whether [itemList] is shown in a generic list template instead of a place list template.
     * A list template is limited as a generic list, and does not draw the place markers of its
     * rows, so its rows carry the marker icon as their image. Set before the manager is attached.
     * Defaults to a place list template.
     */
    internal var showsInListTemplate: Boolean = false

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
        // Registering starts the list right away when a MapboxNavigation is attached. Without
        // one the list is built too, with the distance units of the device locale.
        MapboxNavigationApp.registerObserver(navigationObserver)
        if (itemListJob == null) {
            startItemList(mapboxNavigation = null)
        }

        loadPlaceRecords(
            mapboxCarMapSurface.carContext.contentLimit(
                if (showsInListTemplate) {
                    ConstraintManager.CONTENT_LIMIT_TYPE_LIST
                } else {
                    ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST
                },
            ),
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
        // Cleared first, so the navigation detach below doesn't start a new list.
        carMapSurface = null
        MapboxNavigationApp.unregisterObserver(navigationObserver)
        coroutineScope.cancel()
        itemListJob = null
    }

    private fun startItemList(mapboxNavigation: MapboxNavigation?) {
        itemListJob?.cancel()
        itemListJob = null
        val carContext = carMapSurface?.carContext ?: return
        val unitType = mapboxNavigation?.navigationOptions?.distanceFormatterOptions?.unitType
            ?: DistanceFormatterOptions.Builder(carContext).build().unitType
        itemListJob = coroutineScope.launch {
            collectItemList(PlacesListItemMapper(PlaceMarkerRenderer(carContext), unitType))
        }
    }

    private fun onNavigationDetached() {
        // A list on screen stays as it is: rebuilding it without navigation would drop its
        // distances and the units of the navigation options. A list still loading is built
        // without navigation, so it never stays loading.
        if (_state.value is PlacesListState.Loaded) {
            itemListJob?.cancel()
            itemListJob = null
        } else {
            startItemList(mapboxNavigation = null)
        }
    }

    private suspend fun collectItemList(placesListItemMapper: PlacesListItemMapper) {
        loadedPlaceRecords.filterNotNull().collectLatest { placeRecords ->
            if (placeRecords.isEmpty()) {
                publishItemList(ItemList.Builder().build())
                return@collectLatest
            }
            fun itemList(location: Location?) = placesListItemMapper.mapToItemList(
                location,
                placeRecords,
                placeClickListener,
                withRowImages = showsInListTemplate,
            )
            // Places can load before the first location fix, and a fix may never come (no
            // signal, or no trip session). Show the places without distances after a short
            // wait, and add the distances when the first location arrives. A place list
            // template rejects rows without a distance, so it gets an empty list until then.
            val locationProvider = CarLocationProvider.getRegisteredInstance()
            val location = locationProvider.lastLocation()
                ?: locationProvider.waitForLocationOrNull()
            publishItemList(
                if (location != null || showsInListTemplate) {
                    itemList(location)
                } else {
                    ItemList.Builder().build()
                },
            )
            if (location == null) {
                publishItemList(itemList(locationProvider.validLocation()))
            }
        }
    }

    // The list, the map markers and the camera all use these records. Only places that can be
    // shown on the map are kept, and no more than the host shows in the list.
    @OptIn(MapboxExperimental::class)
    private fun loadPlaceRecords(placeLimit: Int) {
        coroutineScope.launch {
            // The provider can be implemented by the integrator, so a failure in it must not
            // crash the app.
            val expectedPlaceRecords = try {
                withContext(SdkDispatchers.IO) {
                    placesListOnMapProvider.getPlaces()
                }
            } catch (e: Throwable) {
                // Rethrows only when this manager was cancelled. A cancellation from inside the
                // provider, such as its own timeout, is a failed load.
                if (e is CancellationException) ensureActive()
                logAndroidAutoFailure("PlacesListOnMapScreen getPlaces failed", e)
                onLoadFailed()
                return@launch
            }
            expectedPlaceRecords.fold(
                {
                    logAndroidAutoFailure(
                        "PlacesListOnMapScreen ${it.errorMessage}\n" +
                            it.throwable?.stackTraceToString().orEmpty(),
                    )
                    onLoadFailed()
                },
                { placeRecords -> publishPlaces(placeRecords.placesToShow(placeLimit)) },
            )
        }
    }

    private fun onLoadFailed() {
        // Places loaded earlier stay on screen when reloading them fails.
        if (loadedPlaceRecords.value == null) {
            _state.value = PlacesListState.Failed
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
 * The places shown in the list and on the map: only places with a coordinate and something to
 * title their row with, and no more than the host shows in the list.
 */
internal fun List<PlaceRecord>.placesToShow(placeLimit: Int): List<PlaceRecord> =
    filter { it.coordinate != null && rowTitle(it.name, it.description) != null }
        .take(placeLimit)
