package com.mapbox.navigation.ui.androidauto.navigation

import android.text.SpannableString
import androidx.car.app.model.Distance
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.TravelEstimate
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.androidauto.internal.extensions.mapboxNavigationForward

/**
 * Object for formatting distances. Set the [DistanceFormatterOptions] through the
 * [NavigationOptions].
 */
object CarDistanceFormatter {

    private lateinit var delegate: CarDistanceFormatterDelegate
    private lateinit var mapboxDistanceFormatter: MapboxDistanceFormatter
    private val navigationObserver = mapboxNavigationForward(this::onAttached) { }

    init {
        MapboxNavigationApp.registerObserver(navigationObserver)
    }

    /**
     * The distance formatted to locale and unit type, which can be shown in a human viewable
     * text element.
     *
     * @param distance in meters.
     * @return [SpannableString] formatted with bold and relative size.
     *
     * @see DistanceFormatter
     */
    @JvmStatic
    fun formatDistance(distance: Double): SpannableString {
        return mapboxDistanceFormatter.formatDistance(distance)
    }

    /**
     * Provides a [Distance] object used in [Template] objects like [RoutingInfo] and
     * [TravelEstimate].
     */
    @JvmStatic
    fun carDistance(distanceMeters: Double): Distance {
        return delegate.carDistance(distanceMeters)
    }

    private fun onAttached(mapboxNavigation: MapboxNavigation) {
        val distanceFormatterOptions = mapboxNavigation.navigationOptions.distanceFormatterOptions
        val unitType = distanceFormatterOptions.unitType
        val roundingIncrement = distanceFormatterOptions.roundingIncrement
        delegate = CarDistanceFormatterDelegate(unitType, roundingIncrement)
        mapboxDistanceFormatter = MapboxDistanceFormatter(distanceFormatterOptions)
    }
}
