package com.mapbox.androidauto.car.search

class GetPlacesError internal constructor(
    val errorMessage: String,
    val throwable: Throwable?
)
