package com.mapbox.navigation.ui.maps.route.routearrow.internal

import android.graphics.drawable.Drawable

class MapboxRouteArrowDrawableProvider(
    private val arrowHeadDrawable: Drawable,
    private val arrowHeadCasingDrawable: Drawable
) : RouteArrowDrawableProvider {

    override fun getArrowHeadIconCasingDrawable(): Drawable = arrowHeadCasingDrawable

    override fun getArrowHeadIconDrawable(): Drawable = arrowHeadDrawable
}
