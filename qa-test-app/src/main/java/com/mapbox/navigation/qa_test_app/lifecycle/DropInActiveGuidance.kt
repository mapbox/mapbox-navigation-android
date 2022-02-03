package com.mapbox.navigation.qa_test_app.lifecycle

import android.view.View
import android.widget.Toast
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.DistanceRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.ui.tripprogress.model.PercentDistanceTraveledFormatter
import com.mapbox.navigation.ui.tripprogress.model.TimeRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInActiveGuidance(
    private val stopView: View,
    private val maneuverView: MapboxManeuverView,
    private val tripProgressView: MapboxTripProgressView
) : MapboxNavigationObserver {

    // Set when attached
    private var maneuverApi: MapboxManeuverApi? = null

    private val tripSessionStateObserver = TripSessionStateObserver {
        // Another solution here is to create a flowable or livedata stream. That way we can
        // combine the logic between RoutesObserver and TripSessionStateObserver. This example
        // is choosing to duplicate the logic at the moment. Some kind of framework that turns
        // all MapboxNavigation data into flowable streams would make it so these individual
        // components do not have to implement it themselves.
        val visible = it == TripSessionState.STARTED &&
            MapboxNavigationApp.current()?.getRoutes()?.isNotEmpty() == true
        updateVisibility(visible)
    }

    private val routesObserver = RoutesObserver {
        // See the comment in the TripSessionStateObserver above.
        val visible = it.routes.isNotEmpty() &&
            MapboxNavigationApp.current()?.getTripSessionState() == TripSessionState.STARTED
        updateVisibility(visible)
    }

    private fun updateVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        stopView.visibility = visibility
        maneuverView.visibility = visibility
        tripProgressView.visibility = visibility
    }

    // The onAttached will happen after onStart when it is added to the MapboxNavigationApp
    // MapboxNavigationApp.registerObserver(navigationObserver)
    private lateinit var routeProgressObserver: RouteProgressObserver

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        updateVisibility(false)
        stopView.setOnClickListener {
            mapboxNavigation.setRoutes(listOf())
            mapboxNavigation.stopTripSession()
        }
        val maneuverApi = MapboxManeuverApi(
            MapboxDistanceFormatter(
                mapboxNavigation.navigationOptions.distanceFormatterOptions
            )
        )
        routeProgressObserver = createBannerRouteProgressObserver(mapboxNavigation, maneuverApi)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerTripSessionStateObserver(tripSessionStateObserver)

        this.maneuverApi = maneuverApi
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterTripSessionStateObserver(tripSessionStateObserver)

        maneuverApi?.cancel()
        maneuverApi = null
    }

    private fun createBannerRouteProgressObserver(
        mapboxNavigation: MapboxNavigation,
        maneuverApi: MapboxManeuverApi,
    ): RouteProgressObserver {
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

        return RouteProgressObserver { routeProgress ->
            val maneuvers = maneuverApi.getManeuvers(routeProgress)
            maneuvers.fold(
                { error ->
                    Toast.makeText(
                        maneuverView.context,
                        error.errorMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                },
                {
                    maneuverView.renderManeuvers(maneuvers)
                }
            )

            tripProgressView.render(
                tripProgressApi.getTripProgress(routeProgress)
            )
        }
    }
}
