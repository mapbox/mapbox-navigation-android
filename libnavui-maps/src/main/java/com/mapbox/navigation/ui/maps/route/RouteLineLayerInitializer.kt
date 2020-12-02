package com.mapbox.navigation.ui.maps.route

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.maps.Style
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.internal.ThemeUtil
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLayerProviderFactory.getLayerProvider
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineResourceProviderFactory.getRouteLineResourceProvider
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.getDefaultBelowLayer
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.initializeRouteLineLayers
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxWayPointIconProvider
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLayerProvider
import com.mapbox.navigation.ui.maps.internal.route.line.WayPointIconProvider
import com.mapbox.navigation.ui.maps.route.line.api.RouteLineResourceProvider
import com.mapbox.navigation.ui.maps.route.line.model.RouteStyleDescriptor

/**
 *
 * @param resourceProvider
 * @param routeLayerProvider
 * @param wayPointIconProvider
 * @param routeLineBelowLayerId
 */
class RouteLineLayerInitializer private constructor(
    private val resourceProvider: RouteLineResourceProvider,
    private val routeLayerProvider: RouteLayerProvider,
    private val wayPointIconProvider: WayPointIconProvider,
    private val routeLineBelowLayerId: String?
) {

    /**
     *
     * @param style
     */
    fun initializeLayers(style: Style) {
        val belowLayerIdToUse: String = getDefaultBelowLayer(routeLineBelowLayerId, style)
        initializeRouteLineLayers(
            style,
            resourceProvider,
            routeLayerProvider,
            wayPointIconProvider,
            belowLayerIdToUse
        )
    }

    /**
     *
     * @param context
     */
    class Builder(private val context: Context) {
        private var styleRes: Int? = null
        private var routeLineResourceProvider: RouteLineResourceProvider? = null
        private var routeLayerProvider: RouteLayerProvider? = null
        private var routeStyleDescriptors: List<RouteStyleDescriptor> = listOf()
        private var routeLineBelowLayerId: String? = null

        /**
         *
         * @param styleRes
         */
        fun withStyleResource(styleRes: Int): Builder =
            apply { this.styleRes = styleRes }

        /**
         *
         * @param resourceProvider
         */
        fun withRouteLineResourceProvider(resourceProvider: RouteLineResourceProvider): Builder =
            apply { this.routeLineResourceProvider = resourceProvider }

        /**
         *
         * @param layerProvider
         */
        fun withRouteLayerProvider(layerProvider: RouteLayerProvider): Builder =
            apply { this.routeLayerProvider = layerProvider }

        /**
         *
         * @param routeStyleDescriptors
         */
        fun withRouteStyleDescriptors(routeStyleDescriptors: List<RouteStyleDescriptor>): Builder =
            apply { this.routeStyleDescriptors = routeStyleDescriptors }

        /**
         *
         * @param layerId
         */
        fun withRouteLineBelowLayerId(layerId: String): Builder =
            apply { this.routeLineBelowLayerId = layerId }

        /**
         *
         */
        fun build(): RouteLineLayerInitializer {
            val styleResource: Int = styleRes ?: ThemeUtil.retrieveAttrResourceId(
                context,
                R.attr.navigationViewRouteStyle,
                R.style.MapboxStyleNavigationMapRoute
            )
            val resourceProvider: RouteLineResourceProvider = routeLineResourceProvider
                ?: getRouteLineResourceProvider(context, styleResource)
            val routeLineLayerProvider: RouteLayerProvider = routeLayerProvider
                ?: getLayerProvider(routeStyleDescriptors, context, styleResource)
            val originIcon = AppCompatResources.getDrawable(
                context,
                resourceProvider.getOriginWaypointIcon()
            )
            val destinationIcon = AppCompatResources.getDrawable(
                context,
                resourceProvider.getDestinationWaypointIcon()
            )

            return RouteLineLayerInitializer(
                resourceProvider,
                routeLineLayerProvider,
                MapboxWayPointIconProvider(originIcon!!, destinationIcon!!),
                routeLineBelowLayerId
            )
        }
    }
}
