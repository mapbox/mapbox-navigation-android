package com.mapbox.androidauto.search

class GetPlacesError internal constructor(
    val errorMessage: String,
    val throwable: Throwable?
)
