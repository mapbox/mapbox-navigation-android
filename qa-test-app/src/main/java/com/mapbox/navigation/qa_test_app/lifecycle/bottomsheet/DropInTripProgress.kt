package com.mapbox.navigation.qa_test_app.lifecycle.bottomsheet

import android.view.View
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.DistanceRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.ui.tripprogress.model.PercentDistanceTraveledFormatter
import com.mapbox.navigation.ui.tripprogress.model.TimeRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInTripProgress(
    private val stopView: View,
    private val tripProgressView: MapboxTripProgressView
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        stopView.setOnClickListener {
            mapboxNavigation.setRoutes(listOf())
            mapboxNavigation.stopTripSession()
        }

        val applicationContext = mapboxNavigation.navigationOptions.applicationContext
        val tripProgressUpdateFormatter = TripProgressUpdateFormatter.Builder(applicationContext)
            .distanceRemainingFormatter(
                DistanceRemainingFormatter(
                    mapboxNavigation.navigationOptions.distanceFormatterOptions
                )
            )
            .timeRemainingFormatter(TimeRemainingFormatter(applicationContext))
            .percentRouteTraveledFormatter(PercentDistanceTraveledFormatter())
            .estimatedTimeToArrivalFormatter(
                EstimatedTimeToArrivalFormatter(applicationContext, TimeFormat.NONE_SPECIFIED)
            )
            .build()

        val tripProgressApi = MapboxTripProgressApi(tripProgressUpdateFormatter)

        coroutineScope.launch {
            mapboxNavigation.flowRouteProgress().collect { routeProgress ->
                tripProgressView.render(
                    tripProgressApi.getTripProgress(routeProgress)
                )
            }
        }
    }
}
