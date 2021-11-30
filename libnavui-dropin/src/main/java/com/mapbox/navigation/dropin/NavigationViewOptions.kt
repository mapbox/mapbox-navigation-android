package com.mapbox.navigation.dropin

import android.content.Context
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.speedlimit.model.SpeedLimitFormatter
import com.mapbox.navigation.ui.tripprogress.model.DistanceRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.ui.tripprogress.model.TimeRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter
import java.lang.IllegalArgumentException

class NavigationViewOptions private constructor(
    val mapboxRouteLineOptions: MapboxRouteLineOptions,
    val routeArrowOptions: RouteArrowOptions,
    val distanceFormatterOptions: DistanceFormatterOptions,
    val distanceFormatter: DistanceFormatter,
    val tripProgressUpdateFormatter: TripProgressUpdateFormatter,
    val speedLimitFormatter: SpeedLimitFormatter,
    val mapStyleUrlDarkTheme: String,
    val mapStyleUrlLightTheme: String,
    val darkTheme: DropInTheme,
    val lightTheme: DropInTheme,
    val useReplayEngine: Boolean
) {

    fun toBuilder(context: Context): Builder = Builder(context).apply {
        mapboxRouteLineOptions(mapboxRouteLineOptions)
        routeArrowOptions(routeArrowOptions)
        distanceFormatterOptions(distanceFormatterOptions)
        distanceFormatter(distanceFormatter)
        tripProgressUpdateFormatter(tripProgressUpdateFormatter)
        speedLimitFormatter(speedLimitFormatter)
        mapStyleUrlDarkTheme(mapStyleUrlDarkTheme)
        mapStyleUrlLightTheme(mapStyleUrlLightTheme)
        darkTheme(darkTheme)
        lightTheme(lightTheme)
        useReplayEngine(useReplayEngine)
    }

    class Builder(context: Context) {
        private var mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(context)
            .withRouteLineBelowLayerId("road-label")
            .withVanishingRouteLineEnabled(true)
            .build()
        private var routeArrowOptions = RouteArrowOptions.Builder(context).build()
        private var distanceFormatterOptions = DistanceFormatterOptions.Builder(context).build()
        private var tripProgressUpdateFormatter = TripProgressUpdateFormatter.Builder(context)
            .distanceRemainingFormatter(DistanceRemainingFormatter(distanceFormatterOptions))
            .timeRemainingFormatter(TimeRemainingFormatter(context))
            .estimatedTimeToArrivalFormatter(EstimatedTimeToArrivalFormatter(context))
            .build()
        private var distanceFormatter: DistanceFormatter =
            MapboxDistanceFormatter(distanceFormatterOptions)
        private var speedLimitFormatter = SpeedLimitFormatter(context)
        private var mapStyleUrlDarkTheme: String = NavigationStyles.NAVIGATION_NIGHT_STYLE
        private var mapStyleUrlLightTheme: String = NavigationStyles.NAVIGATION_DAY_STYLE
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
        private var useReplayEngine = false

        fun mapboxRouteLineOptions(options: MapboxRouteLineOptions): Builder = apply {
            this.mapboxRouteLineOptions = options
        }

        fun routeArrowOptions(options: RouteArrowOptions): Builder = apply {
            this.routeArrowOptions = options
        }

        fun distanceFormatterOptions(options: DistanceFormatterOptions): Builder = apply {
            this.distanceFormatterOptions = options
        }

        fun tripProgressUpdateFormatter(formatter: TripProgressUpdateFormatter): Builder = apply {
            this.tripProgressUpdateFormatter = formatter
        }

        fun distanceFormatter(formatter: DistanceFormatter): Builder = apply {
            this.distanceFormatter = formatter
        }

        fun speedLimitFormatter(formatter: SpeedLimitFormatter): Builder = apply {
            this.speedLimitFormatter = formatter
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

        fun useReplayEngine(useTheReplayEngine: Boolean): Builder = apply {
            this.useReplayEngine = useTheReplayEngine
        }

        fun build(): NavigationViewOptions = NavigationViewOptions(
            mapboxRouteLineOptions,
            routeArrowOptions,
            distanceFormatterOptions,
            distanceFormatter,
            tripProgressUpdateFormatter,
            speedLimitFormatter,
            mapStyleUrlDarkTheme,
            mapStyleUrlLightTheme,
            darkTheme,
            lightTheme,
            useReplayEngine
        )
    }
}
