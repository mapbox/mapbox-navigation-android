package com.mapbox.navigation.ui.maps.internal.route.arrow

import android.graphics.drawable.Drawable

interface RouteArrowDrawableProvider {
    fun getArrowHeadIconDrawable(): Drawable
    fun getArrowHeadIconCasingDrawable(): Drawable
}
