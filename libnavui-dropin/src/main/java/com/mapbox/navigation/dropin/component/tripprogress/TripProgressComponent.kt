package com.mapbox.navigation.dropin.component.tripprogress

import androidx.annotation.StyleRes
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.DistanceRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.ui.tripprogress.model.TimeRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
internal class TripProgressComponent(
    val store: Store,
    @StyleRes val styles: Int,
    val distanceFormatterOptions: DistanceFormatterOptions,
    private val tripProgressView: MapboxTripProgressView
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        tripProgressView.updateStyle(styles)
        val tripProgressFormatter = TripProgressUpdateFormatter
            .Builder(tripProgressView.context)
            .distanceRemainingFormatter(
                DistanceRemainingFormatter(distanceFormatterOptions)
            )
            .timeRemainingFormatter(
                TimeRemainingFormatter(tripProgressView.context)
            )
            .estimatedTimeToArrivalFormatter(
                EstimatedTimeToArrivalFormatter(tripProgressView.context)
            )
            .build()
        val tripProgressApi = MapboxTripProgressApi(tripProgressFormatter)
        coroutineScope.launch {
            store.select { it.previewRoutes }.collect {
                if (it is RoutePreviewState.Ready) {
                    val value = tripProgressApi.getTripDetails(it.routes.first())
                    tripProgressView.renderTripOverview(value)
                }
            }
        }
        coroutineScope.launch {
            mapboxNavigation.flowRouteProgress().collect {
                val value = tripProgressApi.getTripProgress(it)
                tripProgressView.render(value)
            }
        }
    }
}
