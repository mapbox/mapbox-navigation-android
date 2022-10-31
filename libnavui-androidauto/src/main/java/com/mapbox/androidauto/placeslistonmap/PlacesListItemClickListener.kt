package com.mapbox.androidauto.placeslistonmap

import androidx.annotation.UiThread
import com.mapbox.androidauto.search.PlaceRecord

@UiThread
interface PlacesListItemClickListener {
    fun onItemClick(placeRecord: PlaceRecord)
}
