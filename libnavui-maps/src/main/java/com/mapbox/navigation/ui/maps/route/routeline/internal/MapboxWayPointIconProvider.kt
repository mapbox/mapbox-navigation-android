package com.mapbox.navigation.ui.maps.route.routeline.internal

import android.graphics.drawable.Drawable

class MapboxWayPointIconProvider(
    private val originIcon: Drawable,
    private val destinationIcon: Drawable
) : WayPointIconProvider {
    override fun getDestinationIconDrawable() = destinationIcon
    override fun getOriginIconDrawable() = originIcon
}
