package com.mapbox.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.androidauto.car.MapboxCarContext
import com.mapbox.androidauto.car.feedback.ui.CarFeedbackAction
import com.mapbox.androidauto.car.placeslistonmap.PlacesListOnMapScreen
import com.mapbox.androidauto.car.search.SearchCarContext
import com.mapbox.androidauto.screenmanager.MapboxScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenFactory

/**
 * Default screen for [MapboxScreen.GEO_DEEPLINK].
 */
class GeoDeeplinkPlacesCarScreenFactory(
    private val mapboxCarContext: MapboxCarContext
) : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen {
        val placesProvider = mapboxCarContext.geoDeeplinkPlacesProvider
        checkNotNull(placesProvider) {
            "When a deep link is found the geoDeeplinkPlacesProvider needs to be set"
        }
        return PlacesListOnMapScreen(
            SearchCarContext(mapboxCarContext),
            placesProvider,
            listOf(CarFeedbackAction(MapboxScreen.FAVORITES_FEEDBACK))
        )
    }
}
