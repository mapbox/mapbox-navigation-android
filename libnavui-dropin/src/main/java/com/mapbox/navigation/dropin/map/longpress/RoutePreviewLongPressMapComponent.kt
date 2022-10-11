package com.mapbox.navigation.dropin.map.longpress

import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.util.HapticFeedback
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.utils.internal.logW
import com.mapbox.navigation.utils.internal.toPoint

internal class RoutePreviewLongPressMapComponent(
    private val store: Store,
    private val mapView: MapView,
    private val context: NavigationViewContext
) : UIComponent() {

    private var mapboxNavigation: MapboxNavigation? = null
    private var hapticFeedback: HapticFeedback? = null

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        this.mapboxNavigation = mapboxNavigation
        hapticFeedback =
            HapticFeedback.create(mapboxNavigation.navigationOptions.applicationContext)
        mapView.gestures.addOnMapLongClickListener(longClickListener)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        mapView.gestures.removeOnMapLongClickListener(longClickListener)
        hapticFeedback = null
        this.mapboxNavigation = null
    }

    private val longClickListener = OnMapLongClickListener { point ->
        val mapboxNavigation = mapboxNavigation ?: return@OnMapLongClickListener false
        if (context.options.enableMapLongClickIntercept.value) {
            val location = store.state.value.location?.enhancedLocation
            location?.toPoint()?.also { lastPoint ->
                store.dispatch(DestinationAction.SetDestination(Destination(point)))
                val options =
                    context.routeOptionsProvider.getOptions(mapboxNavigation, lastPoint, point)
                store.dispatch(RoutePreviewAction.FetchOptions(options))
                hapticFeedback?.tick()
            } ?: logW(TAG, "Current location is unknown so map long press does nothing")
        }
        false
    }

    private companion object {
        private val TAG = this::class.java.simpleName
    }
}
