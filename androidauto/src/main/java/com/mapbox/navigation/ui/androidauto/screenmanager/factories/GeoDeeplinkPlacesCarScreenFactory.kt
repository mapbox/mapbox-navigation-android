package com.mapbox.navigation.ui.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.placeslistonmap.PlacesListOnMapScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenFactory
import com.mapbox.navigation.ui.androidauto.search.SearchCarContext

/**
 * Default screen for [MapboxScreen.GEO_DEEPLINK].
 */
class GeoDeeplinkPlacesCarScreenFactory(
    private val mapboxCarContext: MapboxCarContext,
) : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen {
        val placesProvider = mapboxCarContext.geoDeeplinkPlacesProvider
        checkNotNull(placesProvider) {
            "When a deep link is found the geoDeeplinkPlacesProvider needs to be set"
        }
        return PlacesListOnMapScreen(
            SearchCarContext(mapboxCarContext),
            placesProvider,
            MapboxScreen.GEO_DEEPLINK,
        )
    }
}
