package com.mapbox.navigation.ui.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.placeslistonmap.PlacesListOnMapScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.ui.androidauto.search.SearchCarContext

/**
 * Default screen for [MapboxScreen.GEO_DEEPLINK].
 *
 * The places are read from [MapboxCarContext.geoDeeplinkPlacesProvider]. When it is not set and
 * this is the first screen, for example when a new car session restores the screen of a previous
 * one, the default map screen is shown instead. Otherwise [create] throws: a
 * [MapboxScreenManager.push] or [MapboxScreenManager.replaceTop] to this screen is logged and
 * ignored, and [MapboxScreenManager.createScreen] throws.
 */
class GeoDeeplinkPlacesCarScreenFactory(
    private val mapboxCarContext: MapboxCarContext,
) : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen {
        val placesProvider = mapboxCarContext.geoDeeplinkPlacesProvider
        if (placesProvider == null) {
            // Shows the map when this is the first screen, otherwise throws.
            return mapboxCarContext.mapboxScreenManager.createDefaultMapScreenInstead(
                carContext,
                "GeoDeeplinkPlacesCarScreenFactory the geoDeeplinkPlacesProvider is not set",
            )
        }
        return PlacesListOnMapScreen(
            SearchCarContext(mapboxCarContext),
            placesProvider,
            MapboxScreen.GEO_DEEPLINK,
        )
    }
}
