package com.mapbox.services.android.navigation.v5.internal.navigation

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.NavigationMetricListener
import com.mapbox.services.android.navigation.v5.location.RawLocationListener
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener
import com.mapbox.services.android.navigation.v5.navigation.EnhancedLocationListener
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener
import com.mapbox.services.android.navigation.v5.route.FasterRouteListener
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.utils.RouteUtils
import java.util.concurrent.CopyOnWriteArrayList
import timber.log.Timber

internal class NavigationEventDispatcher {

    private val navigationEventListeners: CopyOnWriteArrayList<NavigationEventListener>
    private val milestoneEventListeners: CopyOnWriteArrayList<MilestoneEventListener>
    private val progressChangeListeners: CopyOnWriteArrayList<ProgressChangeListener>
    private val offRouteListeners: CopyOnWriteArrayList<OffRouteListener>
    private val fasterRouteListeners: CopyOnWriteArrayList<FasterRouteListener>
    private val rawLocationListeners: CopyOnWriteArrayList<RawLocationListener>
    private val enhancedLocationListeners: CopyOnWriteArrayList<EnhancedLocationListener>
    private val routeUtils: RouteUtils
    private var metricEventListener: NavigationMetricListener? = null

    constructor() : this(RouteUtils())

    constructor(routeUtils: RouteUtils) {
        this.routeUtils = routeUtils
        navigationEventListeners = CopyOnWriteArrayList()
        milestoneEventListeners = CopyOnWriteArrayList()
        progressChangeListeners = CopyOnWriteArrayList()
        offRouteListeners = CopyOnWriteArrayList()
        fasterRouteListeners = CopyOnWriteArrayList()
        rawLocationListeners = CopyOnWriteArrayList()
        enhancedLocationListeners = CopyOnWriteArrayList()
    }

    fun addMilestoneEventListener(milestoneEventListener: MilestoneEventListener) {
        if (milestoneEventListeners.contains(milestoneEventListener)) {
            Timber.w("The specified MilestoneEventListener has already been added to the stack.")
            return
        }
        milestoneEventListeners.add(milestoneEventListener)
    }

    fun removeMilestoneEventListener(milestoneEventListener: MilestoneEventListener?) {
        if (milestoneEventListener == null) {
            milestoneEventListeners.clear()
        } else if (!milestoneEventListeners.contains(milestoneEventListener)) {
            Timber.w("The specified MilestoneEventListener isn't found in stack, therefore, cannot be removed.")
        } else {
            milestoneEventListeners.remove(milestoneEventListener)
        }
    }

    fun addProgressChangeListener(progressChangeListener: ProgressChangeListener) {
        if (progressChangeListeners.contains(progressChangeListener)) {
            Timber.w("The specified ProgressChangeListener has already been added to the stack.")
            return
        }
        progressChangeListeners.add(progressChangeListener)
    }

    fun removeProgressChangeListener(progressChangeListener: ProgressChangeListener?) {
        if (progressChangeListener == null) {
            progressChangeListeners.clear()
        } else if (!progressChangeListeners.contains(progressChangeListener)) {
            Timber.w("The specified ProgressChangeListener isn't found in stack, therefore, cannot be removed.")
        } else {
            progressChangeListeners.remove(progressChangeListener)
        }
    }

    fun addOffRouteListener(offRouteListener: OffRouteListener) {
        if (offRouteListeners.contains(offRouteListener)) {
            Timber.w("The specified OffRouteListener has already been added to the stack.")
            return
        }
        offRouteListeners.add(offRouteListener)
    }

    fun removeOffRouteListener(offRouteListener: OffRouteListener?) {
        if (offRouteListener == null) {
            offRouteListeners.clear()
        } else if (!offRouteListeners.contains(offRouteListener)) {
            Timber.w("The specified OffRouteListener isn't found in stack, therefore, cannot be removed.")
        } else {
            offRouteListeners.remove(offRouteListener)
        }
    }

    fun addNavigationEventListener(navigationEventListener: NavigationEventListener) {
        if (navigationEventListeners.contains(navigationEventListener)) {
            Timber.w("The specified NavigationEventListener has already been added to the stack.")
            return
        }
        this.navigationEventListeners.add(navigationEventListener)
    }

    fun removeNavigationEventListener(navigationEventListener: NavigationEventListener?) {
        if (navigationEventListener == null) {
            navigationEventListeners.clear()
        } else if (!navigationEventListeners.contains(navigationEventListener)) {
            Timber.w("The specified NavigationEventListener isn't found in stack, therefore, cannot be removed.")
        } else {
            navigationEventListeners.remove(navigationEventListener)
        }
    }

    fun addFasterRouteListener(fasterRouteListener: FasterRouteListener) {
        if (fasterRouteListeners.contains(fasterRouteListener)) {
            Timber.w("The specified FasterRouteListener has already been added to the stack.")
            return
        }
        fasterRouteListeners.add(fasterRouteListener)
    }

    fun removeFasterRouteListener(fasterRouteListener: FasterRouteListener?) {
        if (fasterRouteListener == null) {
            fasterRouteListeners.clear()
        } else if (!fasterRouteListeners.contains(fasterRouteListener)) {
            Timber.w("The specified FasterRouteListener isn't found in stack, therefore, cannot be removed.")
        } else {
            fasterRouteListeners.remove(fasterRouteListener)
        }
    }

    fun addRawLocationListener(rawLocationListener: RawLocationListener) {
        if (rawLocationListeners.contains(rawLocationListener)) {
            Timber.w("The specified RawLocationListener has already been added to the stack.")
            return
        }
        rawLocationListeners.add(rawLocationListener)
    }

    fun removeRawLocationListener(rawLocationListener: RawLocationListener?) {
        if (rawLocationListener == null) {
            rawLocationListeners.clear()
        } else if (!rawLocationListeners.contains(rawLocationListener)) {
            Timber.w("The specified RawLocationListener isn't found in stack, therefore, cannot be removed.")
        } else {
            rawLocationListeners.remove(rawLocationListener)
        }
    }

    fun addEnhancedLocationListener(enhancedLocationListener: EnhancedLocationListener) {
        if (enhancedLocationListeners.contains(enhancedLocationListener)) {
            Timber.w("The specified EnhancedLocationListener has already been added to the stack.")
            return
        }
        enhancedLocationListeners.add(enhancedLocationListener)
    }

    fun removeEnhancedLocationListener(enhancedLocationListener: EnhancedLocationListener?) {
        if (enhancedLocationListener == null) {
            enhancedLocationListeners.clear()
        } else if (!enhancedLocationListeners.contains(enhancedLocationListener)) {
            Timber.w("The specified EnhancedLocationListener isn't found in stack, therefore, cannot be removed.")
        } else {
            enhancedLocationListeners.remove(enhancedLocationListener)
        }
    }

    fun onMilestoneEvent(routeProgress: RouteProgress, instruction: String, milestone: Milestone) {
        checkForArrivalEvent(routeProgress)
        for (milestoneEventListener in milestoneEventListeners) {
            milestoneEventListener.onMilestoneEvent(routeProgress, instruction, milestone)
        }
    }

    fun onProgressChange(location: Location, routeProgress: RouteProgress) {
        sendMetricProgressUpdate(routeProgress)
        for (progressChangeListener in progressChangeListeners) {
            progressChangeListener.onProgressChange(location, routeProgress)
        }
    }

    fun onNavigationEvent(isRunning: Boolean) {
        for (navigationEventListener in navigationEventListeners) {
            navigationEventListener.onRunning(isRunning)
        }
    }

    fun onUserOffRoute(location: Location) {
        for (offRouteListener in offRouteListeners) {
            offRouteListener.userOffRoute(location)
        }
        metricEventListener?.onOffRouteEvent(location)
    }

    fun onFasterRouteEvent(directionsRoute: DirectionsRoute) {
        for (fasterRouteListener in fasterRouteListeners) {
            fasterRouteListener.fasterRouteFound(directionsRoute)
        }
    }

    fun onLocationUpdate(location: Location) {
        for (listener in rawLocationListeners) {
            listener.onLocationUpdate(location)
        }
    }

    fun onEnhancedLocationUpdate(location: Location) {
        for (listener in enhancedLocationListeners) {
            listener.onEnhancedLocationUpdate(location)
        }
    }

    fun addMetricEventListeners(eventListeners: NavigationMetricListener) {
        if (metricEventListener == null) {
            metricEventListener = eventListeners
        }
    }

    private fun checkForArrivalEvent(routeProgress: RouteProgress) {
        metricEventListener?.let { navigationMetricListener ->
            if (routeUtils.isArrivalEvent(routeProgress)) {
                navigationMetricListener.onArrival(routeProgress)
                if (routeUtils.isLastLeg(routeProgress)) {
                    metricEventListener = null
                }
            }
        }
    }

    private fun sendMetricProgressUpdate(routeProgress: RouteProgress) {
        metricEventListener?.onRouteProgressUpdate(routeProgress)
    }
}
