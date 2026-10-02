package com.mapbox.navigation.ui.androidauto.placeslistonmap

import androidx.annotation.UiThread
import com.mapbox.navigation.ui.androidauto.search.PlaceRecord

@UiThread
interface PlacesListItemClickListener {
    fun onItemClick(placeRecord: PlaceRecord)
}
