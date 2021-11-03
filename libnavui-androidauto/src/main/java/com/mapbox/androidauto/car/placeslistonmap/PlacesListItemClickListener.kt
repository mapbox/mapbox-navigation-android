package com.mapbox.androidauto.car.placeslistonmap

import com.mapbox.androidauto.car.search.PlaceRecord

interface PlacesListItemClickListener {
    fun onItemClick(placeRecord: PlaceRecord)
}
