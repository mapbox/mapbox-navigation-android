package com.mapbox.navigation.dropin.map.longpress

import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.util.HapticFeedback
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.extension.dispatch
import com.mapbox.navigation.ui.app.internal.showDestinationPreview
import com.mapbox.navigation.ui.base.lifecycle.UIComponent

internal class FreeDriveLongPressMapComponent(
    private val store: Store,
    private val mapView: MapView,
    private val context: NavigationViewContext,
) : UIComponent() {

    private var hapticFeedback: HapticFeedback? = null

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        hapticFeedback =
            HapticFeedback.create(mapboxNavigation.navigationOptions.applicationContext)
        mapView.gestures.addOnMapLongClickListener(longClickListener)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        mapView.gestures.removeOnMapLongClickListener(longClickListener)
        hapticFeedback = null
    }

    private val longClickListener = OnMapLongClickListener { point ->
        if (context.options.enableMapLongClickIntercept.value) {
            store.dispatch(showDestinationPreview(point))
            hapticFeedback?.tick()
        }
        false
    }
}
