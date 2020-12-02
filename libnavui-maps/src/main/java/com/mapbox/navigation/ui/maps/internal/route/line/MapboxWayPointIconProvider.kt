package com.mapbox.navigation.ui.maps.internal.route.line

import android.graphics.drawable.Drawable

class MapboxWayPointIconProvider(
    private val originIcon: Drawable,
    private val destinationIcon: Drawable
) : WayPointIconProvider {
    override fun getDestinationIconDrawable() = destinationIcon
    override fun getOriginIconDrawable() = originIcon
}
