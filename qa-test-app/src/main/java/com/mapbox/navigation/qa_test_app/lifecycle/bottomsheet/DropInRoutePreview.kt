package com.mapbox.navigation.qa_test_app.lifecycle.bottomsheet

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.lifecycle.viewmodel.DropInReplayComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInRoutePreview(
    private val routes: List<DirectionsRoute>,
    private val routesContainer: LinearLayout,
    private val clear: ImageView,
    private val startNavigation: View
) : UIComponent() {

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun View.clicks() = callbackFlow {
        setOnClickListener {
            trySend(Unit)
        }
        awaitClose { setOnClickListener(null) }
    }

    @SuppressLint("SetTextI18n")
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        val distanceFormatter: DistanceFormatter = MapboxDistanceFormatter(
            mapboxNavigation.navigationOptions.distanceFormatterOptions
        )

        val routesPreviewFirst = routesContainer.findViewById<TextView>(R.id.routesPreviewFirst)
        val routesPreviewSecond = routesContainer.findViewById<TextView>(R.id.routesPreviewSecond)

        if (routes.isNotEmpty()) {
            val routeTitle = routeTitle(distanceFormatter, routes[0])
            routesPreviewFirst.text = routeTitle
            routesPreviewFirst.setBackgroundResource(R.color.colorAccent)
            routesPreviewSecond.setBackgroundColor(Color.TRANSPARENT)
        }
        if (routes.size >= 2) {
            val routeTitle = routeTitle(distanceFormatter, routes[1])
            routesPreviewSecond.text = routeTitle
            routesPreviewSecond.setOnClickListener {
                val newRouteOrder = routes.toMutableList()
                routesPreviewSecond.setBackgroundResource(R.color.colorAccent)
                routesPreviewFirst.setBackgroundColor(Color.TRANSPARENT)
                // TODO need a way to prevent the screen from reloading. For example, we may
                //   need a route preview selection that does not update mapboxNavigation. Or some
                //   other refactor, such as changing DropInRoutesInteractor
                val swap = newRouteOrder[0]
                newRouteOrder[0] = newRouteOrder[1]
                newRouteOrder[1] = swap
                mapboxNavigation.setRoutes(newRouteOrder)
            }
            routesPreviewSecond.visibility = View.VISIBLE
        } else {
            routesPreviewSecond.visibility = View.GONE
            routesPreviewSecond.setOnClickListener { }
        }

        coroutineScope.launch {
            clear.clicks().collect {
                mapboxNavigation.setRoutes(emptyList())
            }
        }
        coroutineScope.launch {
            startNavigation.clicks().collect {
                MapboxNavigationApp.getObserver(DropInReplayComponent::class).startSimulation()
                mapboxNavigation.startReplayTripSession()
            }
        }
    }

    private fun routeTitle(
        distanceFormatter: DistanceFormatter,
        route: DirectionsRoute
    ): String {
        val duration = distanceFormatter.formatDistance(route.distance())
        val title = route.legs()?.first()?.summary()
            ?: route.routeOptions()?.coordinates()
        return "$duration $title"
    }
}
