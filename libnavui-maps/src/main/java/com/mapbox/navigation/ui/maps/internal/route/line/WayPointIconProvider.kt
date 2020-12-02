package com.mapbox.navigation.ui.maps.internal.route.line

import android.graphics.drawable.Drawable

interface WayPointIconProvider {
    fun getOriginIconDrawable(): Drawable
    fun getDestinationIconDrawable(): Drawable
}
