package com.mapbox.androidauto.placeslistonmap

import com.mapbox.androidauto.search.GetPlacesError
import com.mapbox.androidauto.search.PlaceRecord
import com.mapbox.bindgen.Expected

interface PlacesListOnMapProvider {
    suspend fun getPlaces(): Expected<GetPlacesError, List<PlaceRecord>>
    fun cancel()
}
