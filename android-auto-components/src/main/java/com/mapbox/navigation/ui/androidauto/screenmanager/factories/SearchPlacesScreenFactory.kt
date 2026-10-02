package com.mapbox.navigation.ui.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenFactory
import com.mapbox.navigation.ui.androidauto.search.PlaceSearchScreen
import com.mapbox.navigation.ui.androidauto.search.SearchCarContext

/**
 * Default screen for [MapboxScreen.SEARCH].
 */
class SearchPlacesScreenFactory(
    private val mapboxCarContext: MapboxCarContext,
) : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen {
        return PlaceSearchScreen(SearchCarContext(mapboxCarContext))
    }
}
