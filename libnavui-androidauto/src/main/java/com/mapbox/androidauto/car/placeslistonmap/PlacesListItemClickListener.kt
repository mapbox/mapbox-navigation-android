package com.mapbox.androidauto.car.placeslistonmap

import androidx.annotation.UiThread
import com.mapbox.androidauto.car.search.PlaceRecord

@UiThread
interface PlacesListItemClickListener {
    fun onItemClick(placeRecord: PlaceRecord)
}
