package com.mapbox.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.androidauto.MapboxCarContext
import com.mapbox.androidauto.screenmanager.MapboxScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenFactory
import com.mapbox.androidauto.search.PlaceSearchScreen
import com.mapbox.androidauto.search.SearchCarContext

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
