package com.mapbox.androidauto.car.placeslistonmap

import com.mapbox.androidauto.car.search.GetPlacesError
import com.mapbox.androidauto.car.search.PlaceRecord
import com.mapbox.bindgen.Expected

interface PlacesListOnMapProvider {
    suspend fun getPlaces(): Expected<GetPlacesError, List<PlaceRecord>>
    fun cancel()
}
