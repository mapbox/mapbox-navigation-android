package com.mapbox.navigation.ui.androidauto.navigation.speedlimit

import android.graphics.Rect
import androidx.annotation.VisibleForTesting
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedUnit
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.MapboxCarOptions
import com.mapbox.navigation.ui.androidauto.internal.extensions.mapboxNavigationForward
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.roundToInt

/**
 * Create a speed limit sign. This class is demonstrating how to create a renderer.
 * To Create a new speed limit sign experience, try creating a new class.
 */
@OptIn(MapboxExperimental::class)
class CarSpeedLimitRenderer
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal constructor(
    private val services: CarSpeedLimitServices,
    private val options: MapboxCarOptions,
) : MapboxCarMapObserver {

    /**
     * Public constructor and the internal constructor is for unit testing.
     */
    constructor(mapboxCarContext: MapboxCarContext) : this(
        CarSpeedLimitServices(),
        mapboxCarContext.options,
    )

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var speedLimitWidget: SpeedLimitWidget? = null

    private var distanceFormatterOptions: DistanceFormatterOptions? = null
    private val navigationObserver = mapboxNavigationForward(this::onAttached, this::onDetached)

    private val locationObserver = object : LocationObserver {
        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            updateSpeed(locationMatcherResult)
        }

        override fun onNewRawLocation(rawLocation: com.mapbox.common.location.Location) {
            // no op
        }
    }

    private lateinit var scope: CoroutineScope

    private fun onAttached(mapboxNavigation: MapboxNavigation) {
        distanceFormatterOptions = mapboxNavigation
            .navigationOptions.distanceFormatterOptions
        mapboxNavigation.registerLocationObserver(locationObserver)
    }

    private fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        distanceFormatterOptions = null
    }

    private fun updateSpeed(locationMatcherResult: LocationMatcherResult) {
        val speedLimitOptions = options.speedLimitOptions.value
        val signFormat = speedLimitOptions.forcedSignFormat
            ?: locationMatcherResult.speedLimitInfo.sign
        val threshold = speedLimitOptions.warningThreshold
        when (distanceFormatterOptions!!.unitType) {
            UnitType.IMPERIAL -> {
                val speedLimit = when (locationMatcherResult.speedLimitInfo.unit) {
                    SpeedUnit.MILES_PER_HOUR ->
                        locationMatcherResult.speedLimitInfo.speed?.toDouble()

                    SpeedUnit.KILOMETERS_PER_HOUR ->
                        locationMatcherResult.speedLimitInfo.speed?.toDouble()?.kmphToMph()

                    SpeedUnit.METERS_PER_SECOND ->
                        locationMatcherResult.speedLimitInfo.speed?.toDouble()?.mtpsToMph()
                }
                speedLimitWidget?.update(
                    speedLimit = speedLimit?.roundToInt(),
                    speed = locationMatcherResult.enhancedLocation.speed
                        ?.mtpsToMph()
                        ?.roundToInt()
                        ?: 0,
                    signFormat = signFormat,
                    threshold = threshold,
                )
            }

            UnitType.METRIC -> {
                val speedLimit = when (locationMatcherResult.speedLimitInfo.unit) {
                    SpeedUnit.MILES_PER_HOUR ->
                        locationMatcherResult.speedLimitInfo.speed?.toDouble()?.mphToKmph()

                    SpeedUnit.KILOMETERS_PER_HOUR ->
                        locationMatcherResult.speedLimitInfo.speed?.toDouble()

                    SpeedUnit.METERS_PER_SECOND ->
                        locationMatcherResult.speedLimitInfo.speed?.toDouble()?.mtpsToKmph()
                }?.roundToInt()
                speedLimitWidget?.update(
                    speedLimit = speedLimit,
                    speed = locationMatcherResult.enhancedLocation.speed
                        ?.mtpsToKmph()
                        ?.roundToInt()
                        ?: 0,
                    signFormat = signFormat,
                    threshold = threshold,
                )
            }
        }
    }

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("CarSpeedLimitRenderer carMapSurface loaded")
        val signFormat = options.speedLimitOptions.value.forcedSignFormat
            ?: SpeedLimitSign.MUTCD
        val speedLimitWidget = services.speedLimitWidget(signFormat).also { speedLimitWidget = it }
        mapboxCarMapSurface.mapSurface.addWidget(speedLimitWidget)
        MapboxNavigationApp.registerObserver(navigationObserver)
        scope = MainScope()
        options.speedLimitOptions
            .onEach { speedLimitWidget.update(it.forcedSignFormat, it.warningThreshold) }
            .launchIn(scope)
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("CarSpeedLimitRenderer carMapSurface detached")
        MapboxNavigationApp.unregisterObserver(navigationObserver)
        speedLimitWidget?.let { mapboxCarMapSurface.mapSurface.removeWidget(it) }
        speedLimitWidget = null
        scope.cancel()
    }

    override fun onVisibleAreaChanged(visibleArea: Rect, edgeInsets: EdgeInsets) {
        val currentPosition = speedLimitWidget?.getPosition() ?: return
        speedLimitWidget?.setPosition(
            currentPosition.toBuilder()
                .apply {
                    offsetX = -SpeedLimitWidget.MARGIN_X - edgeInsets.right.toFloat()
                    offsetY = -SpeedLimitWidget.MARGIN_Y - edgeInsets.bottom.toFloat()
                }.build(),
        )
    }

    private companion object {
        /**
         * Convert meters per second to kilometers per hour.
         */
        private fun Double.mtpsToKmph(): Double = this * 3.6

        /**
         * Convert meters per second to miles per hour.
         */
        private fun Double.mtpsToMph(): Double = this * 2.23694

        /**
         * Convert kilometers per hour to miles per hour.
         */
        private fun Double.kmphToMph(): Double = this / 1.60934

        /**
         * Convert miles per hour to kilometers per hour.
         */
        private fun Double.mphToKmph(): Double = this * 1.60934
    }
}
