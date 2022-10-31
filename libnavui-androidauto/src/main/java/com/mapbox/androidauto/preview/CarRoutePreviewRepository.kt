package com.mapbox.androidauto.preview

import com.mapbox.androidauto.screenmanager.MapboxScreen
import com.mapbox.androidauto.search.PlaceRecord
import com.mapbox.navigation.base.route.NavigationRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for passing the route preview data to [MapboxScreen.ROUTE_PREVIEW].
 */
class CarRoutePreviewRepository {
    private val mutablePlaceRecord = MutableStateFlow<PlaceRecord?>(null)
    private val mutableRoutes = MutableStateFlow<List<NavigationRoute>>(emptyList())

    val placeRecord: StateFlow<PlaceRecord?> = mutablePlaceRecord.asStateFlow()
    val routes: StateFlow<List<NavigationRoute>> = mutableRoutes.asStateFlow()

    fun setRoutePreview(placeRecord: PlaceRecord, routes: List<NavigationRoute>) {
        mutablePlaceRecord.value = placeRecord
        mutableRoutes.value = routes
    }
}
