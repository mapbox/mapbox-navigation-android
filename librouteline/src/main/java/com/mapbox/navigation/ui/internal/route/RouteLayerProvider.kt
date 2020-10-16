package com.mapbox.navigation.ui.internal.route

import android.graphics.drawable.Drawable
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.style.layers.generated.LineLayer
import com.mapbox.maps.plugin.style.layers.generated.SymbolLayer


interface RouteLayerProvider {
    fun initializePrimaryRouteCasingLayer(style: Style, scale: Double, color: Int): LineLayer
    fun initializeAlternativeRouteCasingLayer(style: Style, scale: Double, color: Int): LineLayer
    fun initializePrimaryRouteLayer(
        style: Style,
        roundedLineCap: Boolean,
        scale: Double,
        color: Int
    ): LineLayer

    fun initializePrimaryRouteTrafficLayer(
        style: Style,
        roundedLineCap: Boolean,
        scale: Double,
        color: Int
    ): LineLayer

    fun initializeAlternativeRouteLayer(
        style: Style,
        roundedLineCap: Boolean,
        scale: Double,
        color: Int
    ): LineLayer

    fun initializeWayPointLayer(
        style: Style,
        originIcon: Drawable,
        destinationIcon: Drawable
    ): SymbolLayer
}
