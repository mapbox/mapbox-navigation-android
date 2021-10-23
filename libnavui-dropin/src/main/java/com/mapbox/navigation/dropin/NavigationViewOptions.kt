package com.mapbox.navigation.dropin

import com.mapbox.maps.Style
import java.lang.IllegalArgumentException

class NavigationViewOptions private constructor(
    val renderManeuvers: Boolean,
    val renderSpeedLimit: Boolean,
    val renderTripProgress: Boolean,
    val renderVolumeButton: Boolean,
    val renderRecenterButton: Boolean,
    val renderRouteOverviewButton: Boolean,
    val enableVanishingRouteLine: Boolean,
    val mapStyleUrlDarkTheme: String,
    val mapStyleUrlLightTheme: String,
    val darkTheme: DropInTheme,
    val lightTheme: DropInTheme,
) {

    fun toBuilder(): Builder = Builder().apply {
        renderManeuvers(renderManeuvers)
        renderSpeedLimit(renderSpeedLimit)
        renderTripProgress(renderTripProgress)
        renderVolumeButton(renderVolumeButton)
        renderRecenterButton(renderRecenterButton)
        renderRouteOverviewButton(renderRouteOverviewButton)
        enableVanishingRouteLine(enableVanishingRouteLine)
        mapStyleUrlDarkTheme(mapStyleUrlDarkTheme)
        mapStyleUrlLightTheme(mapStyleUrlLightTheme)
        darkTheme(darkTheme)
        lightTheme(lightTheme)
    }

    class Builder {
        private var renderManeuvers: Boolean = true
        private var renderSpeedLimit: Boolean = true
        private var renderTripProgress: Boolean = true
        private var renderVolumeButton: Boolean = true
        private var renderRecenterButton: Boolean = true
        private var renderRouteOverviewButton: Boolean = true
        private var enableVanishingRouteLine: Boolean = true
        private var mapStyleUrlDarkTheme: String = Style.LIGHT
        private var mapStyleUrlLightTheme: String = Style.DARK
        private val lightColors = Colors(
            100,
            100,
            0,
            0,
            0,
            0,
            0,
            0,
            0
        )
        private val darkColors = Colors(
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0
        )
        private var darkTheme: DropInTheme = DropInTheme.DarkTheme(darkColors, Typography())
        private var lightTheme: DropInTheme = DropInTheme.LightTheme(lightColors, Typography())

        fun renderManeuvers(renderManeuvers: Boolean): Builder = apply  {
            this.renderManeuvers = renderManeuvers
        }

        fun renderSpeedLimit(renderSpeedLimit: Boolean): Builder = apply  {
            this.renderSpeedLimit = renderSpeedLimit
        }

        fun renderTripProgress(renderTripProgress: Boolean): Builder = apply  {
            this.renderTripProgress = renderTripProgress
        }

        fun renderVolumeButton(renderVolumeButton: Boolean): Builder = apply  {
            this.renderVolumeButton = renderVolumeButton
        }

        fun renderRecenterButton(renderRecenterButton: Boolean): Builder = apply  {
            this.renderRecenterButton = renderRecenterButton
        }

        fun renderRouteOverviewButton(renderRouteOverviewButton: Boolean): Builder = apply  {
            this.renderRouteOverviewButton = renderRouteOverviewButton
        }

        fun enableVanishingRouteLine(enableVanishingRouteLine: Boolean): Builder = apply  {
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
            renderManeuvers,
            renderSpeedLimit,
            renderTripProgress,
            renderVolumeButton,
            renderRecenterButton,
            renderRouteOverviewButton,
            enableVanishingRouteLine,
            mapStyleUrlDarkTheme,
            mapStyleUrlLightTheme,
            darkTheme,
            lightTheme
        )
    }
}
