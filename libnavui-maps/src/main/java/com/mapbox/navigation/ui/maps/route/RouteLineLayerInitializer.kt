package com.mapbox.navigation.ui.maps.route

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AnyRes
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.maps.Style
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.route.routeline.api.RouteLineResourceProvider
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLayerProviderFactory.getLayerProvider
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineResourceProviderFactory.getRouteLineResourceProvider
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.getDefaultBelowLayer
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.initializeRouteLineLayers
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxWayPointIconProvider
import com.mapbox.navigation.ui.maps.route.routeline.internal.RouteLayerProvider
import com.mapbox.navigation.ui.maps.route.routeline.internal.WayPointIconProvider
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteStyleDescriptor
import internal.ThemeUtil

class RouteLineLayerInitializer private constructor(
    private val resourceProvider: RouteLineResourceProvider,
    private val routeLayerProvider: RouteLayerProvider,
    private val wayPointIconProvider: WayPointIconProvider,
    private val routeLineBelowLayerId: String?
) {

    fun initializeLayers(style: Style) {
        val belowLayerIdToUse: String = getDefaultBelowLayer(routeLineBelowLayerId, style)
        initializeRouteLineLayers(style, resourceProvider, routeLayerProvider, wayPointIconProvider, belowLayerIdToUse)
    }

    class Builder(private val context: Context) {
        private var styleRes: Int? = null
        private var routeLineResourceProvider: RouteLineResourceProvider? = null
        private var routeLayerProvider: RouteLayerProvider? = null
        private var routeStyleDescriptors: List<RouteStyleDescriptor> = listOf()
        private var routeLineBelowLayerId: String? = null

        fun withStyleResource(styleRes: Int): Builder =
            apply { this.styleRes = styleRes }

        fun withRouteLineResourceProvider(resourceProvider: RouteLineResourceProvider): Builder =
            apply { this.routeLineResourceProvider = resourceProvider }

        fun withRouteLayerProvider(layerProvider: RouteLayerProvider): Builder =
            apply { this.routeLayerProvider = layerProvider }

        fun withRouteStyleDescriptors(routeStyleDescriptors: List<RouteStyleDescriptor>): Builder =
            apply { this.routeStyleDescriptors = routeStyleDescriptors }

        fun withRouteLineBelowLayerId(layerId: String) : Builder =
            apply { this.routeLineBelowLayerId = layerId }

        fun build(): RouteLineLayerInitializer {
            val styleResource: Int = styleRes ?: ThemeUtil.retrieveAttrResourceId(
                context, R.attr.navigationViewRouteStyle, R.style.MapboxStyleNavigationMapRoute
            )
            val resourceProvider: RouteLineResourceProvider = routeLineResourceProvider
                ?: getRouteLineResourceProvider(context, styleResource)
            val routeLineLayerProvider: RouteLayerProvider = routeLayerProvider
                ?: getLayerProvider(routeStyleDescriptors, context, styleResource)
            val originIcon = AppCompatResources.getDrawable(context, resourceProvider.getOriginWaypointIcon())
            val destinationIcon = AppCompatResources.getDrawable(context, resourceProvider.getDestinationWaypointIcon())

            return RouteLineLayerInitializer(
                resourceProvider,
                routeLineLayerProvider,
                MapboxWayPointIconProvider(originIcon!!, destinationIcon!!),
                routeLineBelowLayerId
            )
        }
    }
}
