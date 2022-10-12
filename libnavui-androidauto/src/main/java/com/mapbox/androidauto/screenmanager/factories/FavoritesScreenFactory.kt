package com.mapbox.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.androidauto.car.MapboxCarContext
import com.mapbox.androidauto.car.feedback.ui.CarFeedbackAction
import com.mapbox.androidauto.car.placeslistonmap.PlacesListOnMapScreen
import com.mapbox.androidauto.car.search.SearchCarContext
import com.mapbox.androidauto.internal.car.search.FavoritesApi
import com.mapbox.androidauto.screenmanager.MapboxScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenFactory
import com.mapbox.search.ServiceProvider

/**
 * Default screen for [MapboxScreen.FAVORITES].
 */
class FavoritesScreenFactory(
    private val mapboxCarContext: MapboxCarContext
) : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen {
        val searchDataProvider = ServiceProvider.INSTANCE.favoritesDataProvider()
        val placesProvider = FavoritesApi(searchDataProvider)

        return PlacesListOnMapScreen(
            SearchCarContext(mapboxCarContext),
            placesProvider,
            listOf(CarFeedbackAction(MapboxScreen.FAVORITES_FEEDBACK))
        )
    }
}
