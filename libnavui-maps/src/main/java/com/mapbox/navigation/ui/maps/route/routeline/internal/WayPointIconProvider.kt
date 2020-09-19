package com.mapbox.navigation.ui.maps.route.routeline.internal

import android.graphics.drawable.Drawable

interface WayPointIconProvider {
    fun getOriginIconDrawable(): Drawable
    fun getDestinationIconDrawable(): Drawable
}
