package com.mapbox.androidauto.car.navigation.speedlimit

import android.graphics.Rect
import android.location.Location
import com.mapbox.androidauto.car.MainCarContext
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import kotlin.math.roundToInt

/**
 * Create a speed limit sign. This class is demonstrating how to
 * create a renderer. To Create a new speed limit sign experience, try creating a new class.
 */
@OptIn(MapboxExperimental::class)
class CarSpeedLimitRenderer(
    private val mainCarContext: MainCarContext,
) : MapboxCarMapObserver {
    private var speedLimitWidget: SpeedLimitWidget? = null
    private val locationObserver = object : LocationObserver {

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            updateSpeed(locationMatcherResult)
        }

        override fun onNewRawLocation(rawLocation: Location) {
            // no op
        }
    }

    private val navigationObserver = object : MapboxNavigationObserver {
        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            mapboxNavigation.registerLocationObserver(locationObserver)
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            mapboxNavigation.unregisterLocationObserver(locationObserver)
        }
    }

    private fun updateSpeed(locationMatcherResult: LocationMatcherResult) {
        val speedKmph =
            locationMatcherResult.enhancedLocation.speed / METERS_IN_KILOMETER * SECONDS_IN_HOUR
        val signFormat = locationMatcherResult.speedLimit?.speedLimitSign
        when (mainCarContext.mapboxNavigation.navigationOptions.distanceFormatterOptions.unitType) {
            UnitType.IMPERIAL -> {
                val speedLimit =
                    locationMatcherResult.speedLimit?.speedKmph?.let { speedLimitKmph ->
                        5 * (speedLimitKmph / KILOMETERS_IN_MILE / 5).roundToInt()
                    }
                val speed = speedKmph / KILOMETERS_IN_MILE
                speedLimitWidget?.update(speedLimit, speed.roundToInt(), signFormat, threshold = 0)
            }
            UnitType.METRIC -> {
                val speedLimit = locationMatcherResult.speedLimit?.speedKmph
                speedLimitWidget?.update(speedLimit, speedKmph.roundToInt(), signFormat, threshold = 0)
            }
        }
    }

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("CarSpeedLimitRenderer carMapSurface loaded")
        val speedLimitWidget = SpeedLimitWidget().also { speedLimitWidget = it }
        mapboxCarMapSurface.mapSurface.addWidget(speedLimitWidget)
        MapboxNavigationApp.registerObserver(navigationObserver)
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("CarSpeedLimitRenderer carMapSurface detached")
        MapboxNavigationApp.unregisterObserver(navigationObserver)
        speedLimitWidget?.let { mapboxCarMapSurface.mapSurface.removeWidget(it) }
        speedLimitWidget = null
    }

    override fun onVisibleAreaChanged(visibleArea: Rect, edgeInsets: EdgeInsets) {
        speedLimitWidget?.setTranslation(-edgeInsets.right.toFloat(), -edgeInsets.bottom.toFloat())
    }

    private companion object {
        private const val METERS_IN_KILOMETER = 1000.0
        private const val KILOMETERS_IN_MILE = 1.609
        private const val SECONDS_IN_HOUR = 3600
    }
}
