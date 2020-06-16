package com.mapbox.navigation.ui.internal.route

import android.graphics.drawable.Drawable
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.SymbolLayer

interface RouteLayerProvider {
    fun initializePrimaryRouteCasingLayer(style: Style, scale: Float, color: Int): LineLayer
    fun initializeAlternativeRouteCasingLayer(style: Style, scale: Float, color: Int): LineLayer
    fun initializePrimaryRouteLayer(style: Style, roundedLineCap: Boolean, scale: Float, color: Int): LineLayer
    fun initializePrimaryRouteTrafficLayer(style: Style, roundedLineCap: Boolean, scale: Float, color: Int): LineLayer
    fun initializeAlternativeRouteLayer(style: Style, roundedLineCap: Boolean, scale: Float, color: Int): LineLayer
    fun initializeWayPointLayer(style: Style, originIcon: Drawable, destinationIcon: Drawable): SymbolLayer
}
