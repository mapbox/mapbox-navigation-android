package com.mapbox.navigation.ui.maps.internal.route.line

import android.graphics.drawable.Drawable
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer

interface RouteLayerProvider {
    fun initializePrimaryRouteCasingLayer(style: Style, color: Int): LineLayer
    fun initializeAlternativeRouteCasingLayer(style: Style, color: Int): LineLayer
    fun initializePrimaryRouteLayer(
        style: Style,
        roundedLineCap: Boolean,
        color: Int
    ): LineLayer

    fun initializePrimaryRouteTrafficLayer(
        style: Style,
        roundedLineCap: Boolean,
        color: Int
    ): LineLayer

    fun initializeAlternativeRouteLayer(
        style: Style,
        roundedLineCap: Boolean,
        color: Int
    ): LineLayer

    fun initializeWayPointLayer(
        style: Style,
        originIcon: Drawable,
        destinationIcon: Drawable
    ): SymbolLayer
}
