package com.mapbox.navigation.ui.maps.route

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.maps.Style
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.LAYER_ABOVE_UPCOMING_MANEUVER_ARROW
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.internal.ThemeUtil
import com.mapbox.navigation.ui.maps.internal.route.arrow.MapboxRouteArrowDrawableProvider
import com.mapbox.navigation.ui.maps.internal.route.arrow.MapboxRouteArrowResourceProviderFactory.getRouteArrowResourceProvider
import com.mapbox.navigation.ui.maps.internal.route.arrow.RouteArrowDrawableProvider
import com.mapbox.navigation.ui.maps.internal.route.arrow.RouteArrowUtils.initRouteArrowLayers
import com.mapbox.navigation.ui.maps.route.arrow.api.RouteArrowResourceProvider

/**
 *
 * @param routeArrowResourceProvider
 * @param routeArrowDrawableProvider
 * @param aboveLayerId
 */
class RouteArrowLayerInitializer private constructor(
    private val routeArrowResourceProvider: RouteArrowResourceProvider,
    private val routeArrowDrawableProvider: RouteArrowDrawableProvider,
    private val aboveLayerId: String
) {

    /**
     *
     * @param style
     */
    fun initializeLayers(style: Style) {
        initRouteArrowLayers(
            style,
            routeArrowDrawableProvider,
            routeArrowResourceProvider,
            aboveLayerId
        )
    }

    /**
     *
     * @param context
     */
    class Builder(private val context: Context) {
        private var styleRes: Int? = null
        private var routeArrowResourceProvider: RouteArrowResourceProvider? = null
        private var aboveLayerId: String? = null

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
        fun withRouteArrowResourceProvider(resourceProvider: RouteArrowResourceProvider): Builder =
            apply { this.routeArrowResourceProvider = resourceProvider }

        /**
         *
         * @param layerId
         */
        fun withAboveLayerId(layerId: String): Builder =
            apply { this.aboveLayerId = layerId }

        /**
         *
         */
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
