package com.mapbox.navigation.ui.components.tripprogress.internal.ui

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.tripdata.progress.api.MapboxTripProgressApi
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.components.tripprogress.view.MapboxTripProgressView
import com.mapbox.navigation.ui.utils.internal.Provider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

interface TripProgressComponentContract {
    val previewRoutes: Flow<List<NavigationRoute>>
}

class TripProgressComponent(
    private val tripProgressView: MapboxTripProgressView,
    private val contactProvider: Provider<TripProgressComponentContract>,
    tripProgressFormatter: TripProgressUpdateFormatter,
    private val tripProgressApi: MapboxTripProgressApi = MapboxTripProgressApi(
        tripProgressFormatter,
    ),
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        val contract = contactProvider.get()

        contract.previewRoutes
            .filter { it.isNotEmpty() }
            .map { tripProgressApi.getTripDetails(it.first()) }
            .observe { tripProgressView.renderTripOverview(it) }

        mapboxNavigation.flowRouteProgress()
            .map { tripProgressApi.getTripProgress(it) }
            .observe { tripProgressView.render(it) }
    }
}
