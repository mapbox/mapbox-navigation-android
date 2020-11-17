package com.mapbox.navigation.ui.maps.route

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.maps.Style
import com.mapbox.navigation.ui.internal.route.RouteConstants.LAYER_ABOVE_UPCOMING_MANEUVER_ARROW
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.route.routearrow.api.RouteArrowResourceProvider
import com.mapbox.navigation.ui.maps.route.routearrow.internal.MapboxRouteArrowDrawableProvider
import com.mapbox.navigation.ui.maps.route.routearrow.internal.MapboxRouteArrowResourceProviderFactory.getRouteArrowResourceProvider
import com.mapbox.navigation.ui.maps.route.routearrow.internal.RouteArrowDrawableProvider
import com.mapbox.navigation.ui.maps.route.routearrow.internal.RouteArrowUtils.initRouteArrowLayers
import internal.ThemeUtil

class RouteArrowLayerInitializer private constructor(
    private val routeArrowResourceProvider: RouteArrowResourceProvider,
    private val routeArrowDrawableProvider: RouteArrowDrawableProvider,
    private val aboveLayerId: String
) {

    fun initializeLayers(style: Style) {
        initRouteArrowLayers(
            style,
            routeArrowDrawableProvider,
            routeArrowResourceProvider,
            aboveLayerId
        )
    }

    class Builder(private val context: Context) {
        private var styleRes: Int? = null
        private var routeArrowResourceProvider: RouteArrowResourceProvider? = null
        private var aboveLayerId: String? = null

        fun withStyleResource(styleRes: Int): Builder =
            apply { this.styleRes = styleRes }

        fun withRouteArrowResourceProvider(resourceProvider: RouteArrowResourceProvider): Builder =
            apply { this.routeArrowResourceProvider = resourceProvider }

        fun withAboveLayerId(layerId: String): Builder =
            apply { this.aboveLayerId = layerId }

        fun build(): RouteArrowLayerInitializer {
            val styleResource: Int = styleRes ?: ThemeUtil.retrieveAttrResourceId(
                context,
                R.attr.navigationViewRouteStyle,
                R.style.MapboxStyleNavigationMapRoute
            )
            val resourceProvider = routeArrowResourceProvider
                ?: getRouteArrowResourceProvider(context, styleResource)
            val arrowHeadIcon = AppCompatResources.getDrawable(
                context,
                resourceProvider.getArrowHeadIcon()
            )
            val arrowHeadCasingIcon = AppCompatResources.getDrawable(
                context,
                resourceProvider.getArrowHeadCasingIcon()
            )
            val routeArrowAboveLayerId: String = aboveLayerId ?: LAYER_ABOVE_UPCOMING_MANEUVER_ARROW

            return RouteArrowLayerInitializer(
                resourceProvider,
                MapboxRouteArrowDrawableProvider(arrowHeadIcon!!, arrowHeadCasingIcon!!),
                routeArrowAboveLayerId
            )
        }
    }
}
