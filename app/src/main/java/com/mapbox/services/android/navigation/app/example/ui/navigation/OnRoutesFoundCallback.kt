package com.mapbox.services.android.navigation.app.example.ui.navigation

import com.mapbox.api.directions.v5.models.DirectionsRoute

interface OnRoutesFoundCallback {

  fun onRoutesFound(routes: List<DirectionsRoute>)

  fun onError(error: String)
}