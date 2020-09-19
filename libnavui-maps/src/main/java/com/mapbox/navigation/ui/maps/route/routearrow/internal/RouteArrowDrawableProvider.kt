package com.mapbox.navigation.ui.maps.route.routearrow.internal

import android.graphics.drawable.Drawable

interface RouteArrowDrawableProvider {
    fun getArrowHeadIconDrawable(): Drawable
    fun getArrowHeadIconCasingDrawable(): Drawable
}
