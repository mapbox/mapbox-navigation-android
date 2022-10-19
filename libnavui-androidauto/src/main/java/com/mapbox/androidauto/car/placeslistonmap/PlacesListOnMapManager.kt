package com.mapbox.androidauto.car.placeslistonmap

import androidx.car.app.model.ItemList
import com.mapbox.androidauto.car.location.CarLocationProvider
import com.mapbox.androidauto.car.search.PlaceRecord
import com.mapbox.androidauto.internal.car.extensions.handleStyleOnAttached
import com.mapbox.androidauto.internal.car.extensions.handleStyleOnDetached
import com.mapbox.androidauto.internal.car.extensions.mapboxNavigationForward
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(MapboxExperimental::class)
class PlacesListOnMapManager(
    private val placesListOnMapProvider: PlacesListOnMapProvider,
) : MapboxCarMapObserver {

    private var carMapSurface: MapboxCarMapSurface? = null
    private var coroutineScope: CoroutineScope? = null
    private var styleLoadedListener: OnStyleLoadedListener? = null
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

        styleLoadedListener = mapboxCarMapSurface.handleStyleOnAttached {
            placesLayerUtil.initializePlacesListOnMapLayer(
                it,
                mapboxCarMapSurface.carContext.resources
            )
            loadPlaceRecords()
        }
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        super.onDetached(mapboxCarMapSurface)
        mapboxCarMapSurface.handleStyleOnDetached(styleLoadedListener)?.let {
            placesLayerUtil.removePlacesListOnMapLayer(it)
        }
        styleLoadedListener = null
        MapboxNavigationApp.unregisterObserver(navigationObserver)
        carMapSurface = null
        coroutineScope?.cancel()
        coroutineScope = null
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
        coroutineScope?.launch {
            val expectedPlaceRecords = withContext(Dispatchers.IO) {
                placesListOnMapProvider.getPlaces()
            }
            _placeRecords.value = emptyList()
            expectedPlaceRecords.fold(
                {
                    logAndroidAuto(
                        "PlacesListOnMapScreen ${it.errorMessage}, ${it.throwable?.stackTrace}"
                    )
                },
                {
                    _placeRecords.value = it
                    addPlaceIconsToMap(it)
                }
            )
        }
    }

    private fun addPlaceIconsToMap(places: List<PlaceRecord>) {
        logAndroidAuto("PlacesListOnMapScreen addPlaceIconsToMap with ${places.size} places.")
        carMapSurface?.mapSurface?.getMapboxMap()?.let { mapboxMap ->
            val features = places.filter { it.coordinate != null }.map {
                Feature.fromGeometry(
                    Point.fromLngLat(it.coordinate!!.longitude(), it.coordinate.latitude())
                )
            }
            val featureCollection = FeatureCollection.fromFeatures(features)
            mapboxMap.getStyle()?.let {
                placesLayerUtil.updatePlacesListOnMapLayer(it, featureCollection)
            }
        }
    }
}
