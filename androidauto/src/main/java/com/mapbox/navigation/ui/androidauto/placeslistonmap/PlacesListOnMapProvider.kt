package com.mapbox.navigation.ui.androidauto.placeslistonmap

import com.mapbox.bindgen.Expected
import com.mapbox.navigation.ui.androidauto.search.GetPlacesError
import com.mapbox.navigation.ui.androidauto.search.PlaceRecord

interface PlacesListOnMapProvider {
    suspend fun getPlaces(): Expected<GetPlacesError, List<PlaceRecord>>

    // TODO: Remove in the next major Nav SDK release. Since [getPlaces] is a suspend function,
    //  cancellation is handled via structured concurrency (coroutine scope cancellation).
    //  A separate [cancel] function is redundant and is never called by [PlacesListOnMapManager].
    @Deprecated("Use coroutine scope cancellation instead.")
    fun cancel()
}
