package com.mapbox.navigation.dropin

import com.mapbox.maps.Style
import java.lang.IllegalArgumentException

class NavigationViewOptions private constructor(
    val enableVanishingRouteLine: Boolean,
    val mapStyleUrlDarkTheme: String,
    val mapStyleUrlLightTheme: String,
    val darkTheme: DropInTheme,
    val lightTheme: DropInTheme,
) {

    fun toBuilder(): Builder = Builder().apply {
        enableVanishingRouteLine(enableVanishingRouteLine)
        mapStyleUrlDarkTheme(mapStyleUrlDarkTheme)
        mapStyleUrlLightTheme(mapStyleUrlLightTheme)
        darkTheme(darkTheme)
        lightTheme(lightTheme)
    }

    class Builder {
        private var enableVanishingRouteLine: Boolean = true
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

        fun enableVanishingRouteLine(enableVanishingRouteLine: Boolean): Builder = apply {
            this.enableVanishingRouteLine = enableVanishingRouteLine
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
            enableVanishingRouteLine,
            mapStyleUrlDarkTheme,
            mapStyleUrlLightTheme,
            darkTheme,
            lightTheme
        )
    }
}
