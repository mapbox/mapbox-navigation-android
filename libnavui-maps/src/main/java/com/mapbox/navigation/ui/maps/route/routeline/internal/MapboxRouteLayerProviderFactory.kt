package com.mapbox.navigation.ui.maps.route.routeline.internal

import android.content.Context
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.getRouteLineScalingValues
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteLineScaleValue
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteStyleDescriptor

/**
 * An internal Mapbox factory for creating a LayerProvider used for creating the layers needed
 * to display route line related geometry on the map.
 */
object MapboxRouteLayerProviderFactory {

    /**
     * Creates a MapboxRouteLayerProvider.
     *
     * @param routeStyleDescriptors used for programatic styling of the route lines based on a
     * route property which can be used for overriding the color styling defined in the theme.
     */
    @JvmStatic
    fun getLayerProvider(
        routeStyleDescriptors: List<RouteStyleDescriptor>,
        context: Context,
        styleRes: Int
    ): MapboxRouteLayerProvider {
        return object : MapboxRouteLayerProvider {
            override val routeStyleDescriptors: List<RouteStyleDescriptor> = routeStyleDescriptors
            override val routeLineScaleValues: List<RouteLineScaleValue> =
                getRouteLineScalingValues(
                    styleRes,
                    context,
                    R.styleable.MapboxStyleNavigationMapRoute_routeLineScaleStops,
                    R.styleable.MapboxStyleNavigationMapRoute_routeLineScaleMultipliers,
                    R.styleable.MapboxStyleNavigationMapRoute_routeLineScales,
                    R.styleable.MapboxStyleNavigationMapRoute
                )
            override val routeLineTrafficScaleValues: List<RouteLineScaleValue> =
                getRouteLineScalingValues(
                    styleRes,
                    context,
                    R.styleable.MapboxStyleNavigationMapRoute_routeLineTrafficScaleStops,
                    R.styleable.MapboxStyleNavigationMapRoute_routeLineTrafficScaleMultipliers,
                    R.styleable.MapboxStyleNavigationMapRoute_routeLineTrafficScales,
                    R.styleable.MapboxStyleNavigationMapRoute
                )
            override val routeLineCasingScaleValues: List<RouteLineScaleValue> =
                getRouteLineScalingValues(
                    styleRes,
                    context,
                    R.styleable.MapboxStyleNavigationMapRoute_routeLineCasingScaleStops,
                    R.styleable.MapboxStyleNavigationMapRoute_routeLineCasingScaleMultipliers,
                    R.styleable.MapboxStyleNavigationMapRoute_routeLineCasingScales,
                    R.styleable.MapboxStyleNavigationMapRoute
                )
        }
    }
}
