package com.mapbox.navigation.ui.androidauto.placeslistonmap

import androidx.car.app.model.ItemList
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.androidauto.internal.extensions.getStyle
import com.mapbox.navigation.ui.androidauto.internal.extensions.mapboxNavigationForward
import com.mapbox.navigation.ui.androidauto.internal.extensions.styleFlow
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.location.CarLocationProvider
import com.mapbox.navigation.ui.androidauto.search.PlaceRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlacesListOnMapManager(
    private val placesListOnMapProvider: PlacesListOnMapProvider,
) : MapboxCarMapObserver {

    private var carMapSurface: MapboxCarMapSurface? = null
    private lateinit var coroutineScope: CoroutineScope
    private val placesLayerUtil: PlacesListOnMapLayerUtil = PlacesListOnMapLayerUtil()
    private val navigationObserver = mapboxNavigationForward(this::onAttached) { }

    private val _placeRecords = MutableStateFlow(listOf<PlaceRecord>())
    val placeRecords: StateFlow<List<PlaceRecord>> = _placeRecords.asStateFlow()

    private val _placeSelected = MutableStateFlow<PlaceRecord?>(null)
    val placeSelected: StateFlow<PlaceRecord?> = _placeSelected.asStateFlow()

    private val _itemList = MutableStateFlow(ItemList.Builder().build())
    val itemList: StateFlow<ItemList> = _itemList.asStateFlow()

    private val placeClickListener = object : PlacesListItemClickListener {
        override fun onItemClick(placeRecord: PlaceRecord) {
            logAndroidAuto("PlacesListOnMapScreen request $placeRecord")
            _placeSelected.value = placeRecord
        }
    }

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        super.onAttached(mapboxCarMapSurface)
        carMapSurface = mapboxCarMapSurface
        coroutineScope = MainScope()
        MapboxNavigationApp.registerObserver(navigationObserver)

        loadPlaceRecords()
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

        coroutineScope.launch {
            placeRecords.collect { placeRecords ->
                _itemList.value = CarLocationProvider.getRegisteredInstance().lastLocation()
                    ?.let { currentLocation ->
                        placesListItemMapper.mapToItemList(
                            currentLocation,
                            placeRecords,
                            placeClickListener,
                        )
                    } ?: ItemList.Builder().build()
            }
        }
    }

    private fun loadPlaceRecords() {
        coroutineScope.launch {
            val expectedPlaceRecords = withContext(Dispatchers.IO) {
                placesListOnMapProvider.getPlaces()
            }
            _placeRecords.value = expectedPlaceRecords.fold(
                {
                    logAndroidAuto(
                        "PlacesListOnMapScreen ${it.errorMessage}, ${it.throwable?.stackTrace}",
                    )
                    emptyList()
                },
                { it },
            )
        }
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
