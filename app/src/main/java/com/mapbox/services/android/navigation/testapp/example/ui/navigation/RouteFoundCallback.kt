package com.mapbox.services.android.navigation.testapp.example.ui.navigation

import com.mapbox.api.directions.v5.models.DirectionsRoute

interface RouteFoundCallback {
    fun routeFound(routes: List<DirectionsRoute>)
}