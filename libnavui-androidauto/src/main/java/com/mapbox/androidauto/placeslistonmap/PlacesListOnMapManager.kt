package com.mapbox.androidauto.placeslistonmap

import androidx.car.app.model.ItemList
import com.mapbox.androidauto.internal.extensions.getStyle
import com.mapbox.androidauto.internal.extensions.mapboxNavigationForward
import com.mapbox.androidauto.internal.extensions.styleFlow
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.location.CarLocationProvider
import com.mapbox.androidauto.search.PlaceRecord
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlacesListOnMapManager(
    private val placesListOnMapProvider: PlacesListOnMapProvider,
) : MapboxCarMapObserver {

    private var carMapSurface: MapboxCarMapSurface? = null
    private lateinit var coroutineScope: CoroutineScope
    private var placesListItemMapper: PlacesListItemMapper? = null
    private val placesLayerUtil: PlacesListOnMapLayerUtil = PlacesListOnMapLayerUtil()
    private val navigationObserver = mapboxNavigationForward(this::onAttached) { onDetached() }

    private val _placeRecords = MutableStateFlow(listOf<PlaceRecord>())
    val placeRecords: StateFlow<List<PlaceRecord>> = _placeRecords.asStateFlow()

    private val _placeSelected = MutableStateFlow<PlaceRecord?>(null)
    val placeSelected: StateFlow<PlaceRecord?> = _placeSelected.asStateFlow()

    private val placeClickListener = object : PlacesListItemClickListener {
        override fun onItemClick(placeRecord: PlaceRecord) {
            logAndroidAuto("PlacesListOnMapScreen request $placeRecord")
            _placeSelected.value = placeRecord
        }
    }

    fun currentItemList(): ItemList? {
        val currentLocation = CarLocationProvider.getRegisteredInstance().lastLocation()
            ?: return null
        return placesListItemMapper?.mapToItemList(
            currentLocation,
            placeRecords.value,
            placeClickListener
        )
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
        placesListItemMapper = PlacesListItemMapper(
            PlaceMarkerRenderer(carMapSurface?.carContext!!),
            mapboxNavigation
                .navigationOptions
                .distanceFormatterOptions
                .unitType
        )
    }

    private fun onDetached() {
        placesListItemMapper = null
    }

    private fun loadPlaceRecords() {
        coroutineScope.launch {
            val expectedPlaceRecords = withContext(Dispatchers.IO) {
                placesListOnMapProvider.getPlaces()
            }
            _placeRecords.value = expectedPlaceRecords.fold(
                {
                    logAndroidAuto(
                        "PlacesListOnMapScreen ${it.errorMessage}, ${it.throwable?.stackTrace}"
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
