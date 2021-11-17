package com.mapbox.navigation.dropin

import android.content.Context
import com.mapbox.maps.Style
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import java.lang.IllegalArgumentException

class NavigationViewOptions private constructor(
    val mapboxRouteLineOptions: MapboxRouteLineOptions,
    val routeArrowOptions: RouteArrowOptions,
    val mapStyleUrlDarkTheme: String,
    val mapStyleUrlLightTheme: String,
    val darkTheme: DropInTheme,
    val lightTheme: DropInTheme,
) {

    fun toBuilder(context: Context): Builder = Builder(context).apply {
        mapboxRouteLineOptions(mapboxRouteLineOptions)
        routeArrowOptions(routeArrowOptions)
        mapStyleUrlDarkTheme(mapStyleUrlDarkTheme)
        mapStyleUrlLightTheme(mapStyleUrlLightTheme)
        darkTheme(darkTheme)
        lightTheme(lightTheme)
    }

    class Builder(context: Context) {
        private var mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(context).build()
        private var routeArrowOptions = RouteArrowOptions.Builder(context).build()
        private var mapStyleUrlDarkTheme: String = Style.LIGHT
        private var mapStyleUrlLightTheme: String = Style.DARK
        private val lightColors = Colors(
            light = 100,
            warmth = 100,
            contrast = 0,
            saturation = 0,
            primary = 0,
            secondary = 0,
            background = 0,
            onPrimary = 0,
            onSecondary = 0
        )
        private val darkColors = Colors(
            light = 0,
            warmth = 0,
            contrast = 0,
            saturation = 0,
            primary = 0,
            secondary = 0,
            background = 0,
            onPrimary = 0,
            onSecondary = 0
        )
        private var darkTheme: DropInTheme = DropInTheme.DarkTheme(darkColors, Typography())
        private var lightTheme: DropInTheme = DropInTheme.LightTheme(lightColors, Typography())

        fun mapboxRouteLineOptions(options: MapboxRouteLineOptions): Builder = apply {
            this.mapboxRouteLineOptions = options
        }

        fun routeArrowOptions(options: RouteArrowOptions): Builder = apply {
            this.routeArrowOptions = options
        }

        fun mapStyleUrlDarkTheme(mapStyleUrlDarkTheme: String): Builder {
            if (mapStyleUrlDarkTheme.isEmpty()) {
                throw IllegalArgumentException("Style url $mapStyleUrlDarkTheme cannot be empty")
            }
            this.mapStyleUrlDarkTheme = mapStyleUrlDarkTheme
            return this
        }

        fun mapStyleUrlLightTheme(mapStyleUrlLightTheme: String): Builder {
            if (mapStyleUrlLightTheme.isEmpty()) {
                throw IllegalArgumentException("Style url $mapStyleUrlLightTheme cannot be empty")
            }
            this.mapStyleUrlLightTheme = mapStyleUrlLightTheme
            return this
        }

        fun darkTheme(darkTheme: DropInTheme): Builder = apply {
            this.darkTheme = darkTheme
        }

        fun lightTheme(lightTheme: DropInTheme): Builder = apply {
            this.lightTheme = lightTheme
        }

        fun build(): NavigationViewOptions = NavigationViewOptions(
            mapboxRouteLineOptions,
            routeArrowOptions,
            mapStyleUrlDarkTheme,
            mapStyleUrlLightTheme,
            darkTheme,
            lightTheme
        )
    }
}
