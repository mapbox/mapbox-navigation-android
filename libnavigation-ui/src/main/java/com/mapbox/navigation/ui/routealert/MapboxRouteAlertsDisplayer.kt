package com.mapbox.navigation.ui.routealert

import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.Property.ICON_ANCHOR_CENTER
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyValue
import com.mapbox.navigation.base.trip.model.alert.RouteAlert
import com.mapbox.navigation.base.trip.model.alert.RouteAlertType

/**
 * Default implementation to show different route alerts, refer to [RouteAlertType]
 *
 * @param options for building/displaying the route alert views
 */
class MapboxRouteAlertsDisplayer @JvmOverloads constructor(
    private var options: MapboxRouteAlertsDisplayerOptions,
    private val tollCollectionAlertDisplayer: TollCollectionAlertDisplayer =
        TollCollectionAlertDisplayer(
            TollCollectionAlertDisplayerOptions.Builder(options.context).build()
        )
) {

    /**
     * When [Style] changes, re-add the route alerts to the new style.
     *
     * @param style the latest [Style]
     */
    fun onStyleLoaded(style: Style?) {
        style?.let {
            if (options.showToll) {
                tollCollectionAlertDisplayer.onStyleLoaded(style)
            }
        }
    }

    /**
     * Display supported [RouteAlert] on the map.
     * Which types of [RouteAlert] are supported relies on the [options] value. Only supported
     * [RouteAlert] can be handle and displayed on the map.
     *
     * @param routeAlerts a list of route alerts
     */
    fun onNewRouteAlerts(routeAlerts: List<RouteAlert>) {
        if (options.showToll) {
            tollCollectionAlertDisplayer.onNewRouteAlerts(routeAlerts)
        }
    }

    companion object {
        /**
         * Mapbox pre-defined symbol layer properties for route alert.
         * @see [TollCollectionAlertDisplayer]
         */
        internal fun getMapboxRouteAlertSymbolLayerProperties(): Array<PropertyValue<out Any>> =
            arrayOf(
                PropertyFactory.iconSize(1.5f),
                PropertyFactory.iconAnchor(ICON_ANCHOR_CENTER),
                PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true)
            )
    }
}
