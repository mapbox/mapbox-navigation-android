package com.mapbox.navigation.ui.maps.route.routearrow.internal

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.route.routearrow.api.RouteArrowResourceProvider

object MapboxRouteArrowResourceProviderFactory {

    @JvmStatic
    fun getRouteArrowResourceProvider(context: Context, styleRes: Int): RouteArrowResourceProvider {
        val routeArrowColor: Int = getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_upcomingManeuverArrowColor,
            R.color.mapbox_navigation_route_upcoming_maneuver_arrow_color,
            context,
            styleRes
        )

        val routeArrowBorderColor: Int = getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_upcomingManeuverArrowBorderColor,
            R.color.mapbox_navigation_route_upcoming_maneuver_arrow_border_color,
            context,
            styleRes
        )

        return MapboxRouteArrowResourceProvider(routeArrowColor, routeArrowBorderColor)
    }

    /**
     * Returns a resource value from the style or a default value
     * @param index the index of the item in the styled attributes.
     * @param colorResourceId the default value to use if no value is found
     * @param context the context to obtain the resource from
     * @param styleRes the style resource to look in
     *
     * @return the resource value
     */
    @ColorInt
    private fun getStyledColor(index: Int, colorResourceId: Int, context: Context, styleRes: Int): Int {
        val typedArray =
            context.obtainStyledAttributes(styleRes, R.styleable.MapboxStyleNavigationMapRoute)
        return typedArray.getColor(
            index,
            ContextCompat.getColor(
                context,
                colorResourceId
            )
        ).also {
            typedArray.recycle()
        }
    }
}
