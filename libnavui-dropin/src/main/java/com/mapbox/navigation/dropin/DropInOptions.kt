package com.mapbox.navigation.dropin

import com.mapbox.maps.Style
import com.mapbox.navigation.core.MapboxNavigation

class DropInOptions private constructor(
    val enableReroute: Boolean,
    val enableRefreshRoutes: Boolean,
    val enableScreenDimming: Boolean,
    val enableScreenLocking: Boolean,
    val enableRouteSimulation: Boolean,
    val enableVanishingRouteLine: Boolean,
    val enableRerouteWhenArriving: Boolean,
    val enableBuildingHighlightUponArrival: Boolean,
    val alwaysShowDarkTheme: Boolean,
    val alwaysShowLightTheme: Boolean,
    val enableAutoLightDarkThemeTransition: Boolean,
    val accessToken: String,
    val mapStyleUrlDarkTheme: String,
    val mapStyleUrlLightTheme: String,
    val mapboxNavigation: MapboxNavigation?
) {

    fun toBuilder(): Builder = Builder().apply {
        enableReroute(enableReroute)
        enableRefreshRoutes(enableRefreshRoutes)
        enableScreenDimming(enableScreenDimming)
        enableScreenLocking(enableScreenLocking)
        enableRouteSimulation(enableRouteSimulation)
        enableVanishingRouteLine(enableVanishingRouteLine)
        enableRerouteWhenArriving(enableRerouteWhenArriving)
        enableBuildingHighlightUponArrival(enableBuildingHighlightUponArrival)
        alwaysShowDarkTheme(alwaysShowDarkTheme)
        alwaysShowLightTheme(alwaysShowLightTheme)
        enableAutoLightDarkThemeTransition(enableAutoLightDarkThemeTransition)
        accessToken(accessToken)
        mapStyleUrlDarkTheme(mapStyleUrlDarkTheme)
        mapStyleUrlLightTheme(mapStyleUrlLightTheme)
        mapboxNavigation(mapboxNavigation)
    }

    class Builder {
        private var enableReroute: Boolean = true
        private var enableRefreshRoutes: Boolean = true
        private var enableScreenDimming: Boolean = true
        private var enableScreenLocking: Boolean = true
        private var enableRouteSimulation: Boolean = true
        private var enableVanishingRouteLine: Boolean = true
        private var enableRerouteWhenArriving: Boolean = false
        private var enableBuildingHighlightUponArrival: Boolean = true
        private var alwaysShowDarkTheme: Boolean = false
        private var alwaysShowLightTheme: Boolean = false
        private var enableAutoLightDarkThemeTransition: Boolean = true
        private var accessToken: String = ""
        private var mapStyleUrlDarkTheme: String = Style.LIGHT
        private var mapStyleUrlLightTheme: String = Style.DARK
        private var mapboxNavigation: MapboxNavigation? = null

        fun enableReroute(enableReroute: Boolean): Builder = apply  {
            this.enableReroute = enableReroute
        }

        fun enableRefreshRoutes(enableRefreshRoutes: Boolean): Builder = apply  {
            this.enableRefreshRoutes = enableRefreshRoutes
        }

        fun enableScreenDimming(enableScreenDimming: Boolean): Builder = apply  {
            this.enableScreenDimming = enableScreenDimming
        }

        fun enableScreenLocking(enableScreenLocking: Boolean): Builder = apply  {
            this.enableScreenLocking = enableScreenLocking
        }

        fun enableRouteSimulation(enableRouteSimulation: Boolean): Builder = apply  {
            this.enableRouteSimulation = enableRouteSimulation
        }

        fun enableVanishingRouteLine(enableVanishingRouteLine: Boolean): Builder = apply  {
            this.enableVanishingRouteLine = enableVanishingRouteLine
        }

        fun enableRerouteWhenArriving(enableRerouteWhenArriving: Boolean): Builder = apply  {
            this.enableRerouteWhenArriving = enableRerouteWhenArriving
        }

        fun enableBuildingHighlightUponArrival(
            enableBuildingHighlightUponArrival: Boolean
        ): Builder = apply  {
            this.enableBuildingHighlightUponArrival = enableBuildingHighlightUponArrival
        }

        fun alwaysShowDarkTheme(alwaysShowDarkTheme: Boolean): Builder = apply  {
            this.alwaysShowDarkTheme = alwaysShowDarkTheme
        }

        fun alwaysShowLightTheme(alwaysShowLightTheme: Boolean): Builder = apply  {
            this.alwaysShowLightTheme = alwaysShowLightTheme
        }

        fun enableAutoLightDarkThemeTransition(
            enableAutoLightDarkThemeTransition: Boolean
        ): Builder = apply  {
            this.enableAutoLightDarkThemeTransition = enableAutoLightDarkThemeTransition
        }

        fun accessToken(accessToken: String): Builder {
            if (accessToken.isEmpty()) {
                throw IllegalArgumentException("Access token $accessToken cannot be empty")
            }
            this.accessToken = accessToken
            return this
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

        fun mapboxNavigation(mapboxNavigation: MapboxNavigation?): Builder {
            if (mapboxNavigation == null) {
                throw IllegalArgumentException("$mapboxNavigation cannot be null")
            }
            this.mapboxNavigation = mapboxNavigation
            return this
        }

        fun build(): DropInOptions = DropInOptions(
            enableReroute,
            enableRefreshRoutes,
            enableScreenDimming,
            enableScreenLocking,
            enableRouteSimulation,
            enableVanishingRouteLine,
            enableRerouteWhenArriving,
            enableBuildingHighlightUponArrival,
            alwaysShowDarkTheme,
            alwaysShowLightTheme,
            enableAutoLightDarkThemeTransition,
            accessToken,
            mapStyleUrlDarkTheme,
            mapStyleUrlLightTheme,
            mapboxNavigation
        )
    }
}
