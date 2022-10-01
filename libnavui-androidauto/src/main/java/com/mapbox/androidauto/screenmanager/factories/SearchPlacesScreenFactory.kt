package com.mapbox.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.androidauto.car.MapboxCarContext
import com.mapbox.androidauto.car.search.PlaceSearchScreen
import com.mapbox.androidauto.car.search.SearchCarContext
import com.mapbox.androidauto.screenmanager.MapboxScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenFactory

/**
 * Default screen for [MapboxScreen.SEARCH].
 */
class SearchPlacesScreenFactory(
    private val mapboxCarContext: MapboxCarContext
) : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen {
        return PlaceSearchScreen(SearchCarContext(mapboxCarContext))
    }
}
