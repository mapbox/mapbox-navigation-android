package com.mapbox.navigation.ui.androidauto.placeslistonmap

import com.mapbox.bindgen.Expected
import com.mapbox.navigation.ui.androidauto.search.GetPlacesError
import com.mapbox.navigation.ui.androidauto.search.PlaceRecord

interface PlacesListOnMapProvider {
    suspend fun getPlaces(): Expected<GetPlacesError, List<PlaceRecord>>
    fun cancel()
}
